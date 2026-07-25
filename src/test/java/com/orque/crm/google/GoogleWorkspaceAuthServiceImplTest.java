package com.orque.crm.google;

import com.orque.crm.auth.entity.User;
import com.orque.crm.auth.repository.UserRepository;
import com.orque.crm.google.auth.GoogleWorkspaceAuthServiceImpl;
import com.orque.crm.google.config.GoogleWorkspaceProperties;
import com.orque.crm.google.entity.GoogleWorkspaceCredential;
import com.orque.crm.google.repository.GoogleWorkspaceCredentialRepository;
import com.orque.crm.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleWorkspaceAuthServiceImplTest {

    @Mock
    private GoogleWorkspaceProperties properties;
    @Mock
    private GoogleWorkspaceCredentialRepository repository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private RestTemplate restTemplate;

    private GoogleWorkspaceAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GoogleWorkspaceAuthServiceImpl(properties, repository, userRepository, jwtService, restTemplate);
        ReflectionTestUtils.setField(service, "crmAppUrl", "http://localhost:4300");
    }

    @Test
    void generateAuthorizationUrl_requestsOfflineAccessAndConsentWithSignedState() {
        when(jwtService.generateOAuthStateToken("alice")).thenReturn("signed-state-token");
        when(properties.getClientId()).thenReturn("client-123");
        when(properties.getRedirectUri()).thenReturn("http://localhost:8085/callback");
        when(properties.getScope()).thenReturn("openid email profile https://www.googleapis.com/auth/calendar");

        String url = service.generateAuthorizationUrl("alice");

        assertThat(url).contains("client_id=client-123");
        assertThat(url).contains("state=signed-state-token");
        assertThat(url).contains("access_type=offline");
        assertThat(url).contains("prompt=consent");
        assertThat(url).contains("include_granted_scopes=true");
    }

    @Test
    void handleCallback_rejectsInvalidOrExpiredState() {
        when(jwtService.extractUsernameFromStateToken("bad-state")).thenReturn(null);

        String redirect = service.handleCallback("some-code", "bad-state");

        assertThat(redirect).contains("google=error");
        assertThat(redirect).contains("reason=invalid_state");
        verifyNoInteractions(restTemplate);
    }

    @Test
    void handleCallback_rejectsUnknownUserEncodedInState() {
        when(jwtService.extractUsernameFromStateToken("state")).thenReturn("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        String redirect = service.handleCallback("code", "state");

        assertThat(redirect).contains("reason=unknown_user");
    }

    @Test
    void handleCallback_persistsSingleCredentialCoveringAllGrantedScopes() {
        User alice = User.builder().username("alice").organizationId("org-1").build();
        when(jwtService.extractUsernameFromStateToken("state")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(properties.getClientId()).thenReturn("client-123");
        when(properties.getClientSecret()).thenReturn("secret");
        when(properties.getRedirectUri()).thenReturn("http://localhost:8085/callback");

        String grantedScopes = "openid email profile https://www.googleapis.com/auth/gmail.modify "
                + "https://www.googleapis.com/auth/calendar https://www.googleapis.com/auth/tasks";
        Map<String, Object> tokenResponse = Map.of(
                "access_token", "access-abc",
                "refresh_token", "refresh-xyz",
                "expires_in", 3600,
                "token_type", "Bearer",
                "scope", grantedScopes
        );
        when(restTemplate.postForObject(contains("oauth2.googleapis.com/token"), any(), eq(Map.class)))
                .thenReturn(tokenResponse);
        when(restTemplate.exchange(contains("userinfo"), any(), any(), eq(Map.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok(Map.of("email", "alice@gmail.com", "id", "google-uid-1")));
        when(repository.findByOwnerIgnoreCase("alice")).thenReturn(Optional.empty());

        String redirect = service.handleCallback("auth-code", "state");

        assertThat(redirect).contains("google=connected");

        ArgumentCaptor<GoogleWorkspaceCredential> captor = ArgumentCaptor.forClass(GoogleWorkspaceCredential.class);
        verify(repository).save(captor.capture());
        GoogleWorkspaceCredential saved = captor.getValue();
        assertThat(saved.getOwner()).isEqualTo("alice");
        assertThat(saved.getOrganizationId()).isEqualTo("org-1");
        assertThat(saved.getEmail()).isEqualTo("alice@gmail.com");
        assertThat(saved.getGoogleUserId()).isEqualTo("google-uid-1");
        assertThat(saved.getAccessToken()).isEqualTo("access-abc");
        assertThat(saved.getRefreshToken()).isEqualTo("refresh-xyz");
        assertThat(saved.getConnected()).isTrue();
        assertThat(saved.hasScope("gmail.modify")).isTrue();
        assertThat(saved.hasScope("calendar")).isTrue();
        assertThat(saved.hasScope("tasks")).isTrue();
    }

    @Test
    void handleCallback_keepsExistingRefreshTokenWhenGoogleOmitsIt() {
        User alice = User.builder().username("alice").organizationId("org-1").build();
        GoogleWorkspaceCredential existing = GoogleWorkspaceCredential.builder()
                .owner("alice").refreshToken("original-refresh-token").build();

        when(jwtService.extractUsernameFromStateToken("state")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(repository.findByOwnerIgnoreCase("alice")).thenReturn(Optional.of(existing));
        when(properties.getClientId()).thenReturn("client-123");
        when(properties.getClientSecret()).thenReturn("secret");
        when(properties.getRedirectUri()).thenReturn("http://localhost:8085/callback");

        Map<String, Object> tokenResponse = Map.of("access_token", "new-access", "expires_in", 3600);
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(tokenResponse);
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok(Map.of("email", "alice@gmail.com")));

        service.handleCallback("auth-code", "state");

        ArgumentCaptor<GoogleWorkspaceCredential> captor = ArgumentCaptor.forClass(GoogleWorkspaceCredential.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRefreshToken()).isEqualTo("original-refresh-token");
    }

    @Test
    void disconnect_revokesTokenAtGoogleThenDeletesLocalConnection() {
        GoogleWorkspaceCredential credential = GoogleWorkspaceCredential.builder()
                .owner("alice").accessToken("access-abc").build();
        when(repository.findByOwnerIgnoreCase("alice")).thenReturn(Optional.of(credential));

        service.disconnect("alice");

        verify(restTemplate).postForObject(contains("oauth2.googleapis.com/revoke"), isNull(), eq(Void.class));
        verify(repository).delete(credential);
    }

    @Test
    void disconnect_isNoOpWhenUserHasNoConnection() {
        when(repository.findByOwnerIgnoreCase("bob")).thenReturn(Optional.empty());

        service.disconnect("bob");

        verify(repository, never()).delete(any());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void getConnection_isScopedToRequestingUserOnly() {
        GoogleWorkspaceCredential aliceCredential = GoogleWorkspaceCredential.builder().owner("alice").build();
        when(repository.findByOwnerIgnoreCase("alice")).thenReturn(Optional.of(aliceCredential));
        when(repository.findByOwnerIgnoreCase("bob")).thenReturn(Optional.empty());

        assertThat(service.getConnection("alice")).contains(aliceCredential);
        assertThat(service.getConnection("bob")).isEmpty();
        verify(repository, never()).findAll();
    }
}
