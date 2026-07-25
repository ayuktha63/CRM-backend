package com.orque.crm.google;

import com.orque.crm.google.config.GoogleWorkspaceProperties;
import com.orque.crm.google.entity.GoogleWorkspaceCredential;
import com.orque.crm.google.repository.GoogleWorkspaceCredentialRepository;
import com.orque.crm.google.token.GoogleNotConnectedException;
import com.orque.crm.google.token.GoogleTokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleTokenManagerTest {

    @Mock
    private GoogleWorkspaceCredentialRepository credentialRepository;
    @Mock
    private GoogleWorkspaceProperties properties;

    private GoogleTokenManager tokenManager;

    @BeforeEach
    void setUp() {
        tokenManager = new GoogleTokenManager(credentialRepository, properties);
    }

    @Test
    void requireConnected_throwsWhenNoConnectionExists() {
        when(credentialRepository.findByOwnerIgnoreCase("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenManager.requireConnected("alice"))
                .isInstanceOf(GoogleNotConnectedException.class);
    }

    @Test
    void requireConnected_throwsWhenRevoked() {
        GoogleWorkspaceCredential revoked = GoogleWorkspaceCredential.builder()
                .owner("alice").connected(false).revoked(true).build();
        when(credentialRepository.findByOwnerIgnoreCase("alice")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> tokenManager.requireConnected("alice"))
                .isInstanceOf(GoogleNotConnectedException.class);
    }

    @Test
    void requireConnected_succeedsForActiveConnection() {
        GoogleWorkspaceCredential active = GoogleWorkspaceCredential.builder()
                .owner("alice").connected(true).revoked(false).build();
        when(credentialRepository.findByOwnerIgnoreCase("alice")).thenReturn(Optional.of(active));

        assertThat(tokenManager.requireConnected("alice")).isEqualTo(active);
    }

    @Test
    void twoUsersConnectionsAreCompletelyIndependent() {
        GoogleWorkspaceCredential alice = GoogleWorkspaceCredential.builder()
                .owner("alice").connected(true).build();
        GoogleWorkspaceCredential bob = GoogleWorkspaceCredential.builder()
                .owner("bob").connected(true).build();

        when(credentialRepository.findByOwnerIgnoreCase("alice")).thenReturn(Optional.of(alice));

        tokenManager.recordIfRevoked("alice", new RuntimeException("invalid_grant"));

        verify(credentialRepository, never()).findByOwnerIgnoreCase("someone-else");
        assertThat(bob.getConnected()).isTrue();
    }

    @Test
    void recordIfRevoked_flipsConnectionOffOnlyForGenuineRevocation() {
        GoogleWorkspaceCredential credential = GoogleWorkspaceCredential.builder()
                .owner("alice").connected(true).revoked(false).build();
        when(credentialRepository.findByOwnerIgnoreCase("alice")).thenReturn(Optional.of(credential));

        boolean revoked = tokenManager.recordIfRevoked("alice", new RuntimeException("invalid_grant: token revoked"));

        assertThat(revoked).isTrue();
        assertThat(credential.getConnected()).isFalse();
        assertThat(credential.getRevoked()).isTrue();
        verify(credentialRepository).save(credential);
    }

    @Test
    void recordIfRevoked_ignoresTransientFailures() {
        boolean revoked = tokenManager.recordIfRevoked("alice", new RuntimeException("Connection timed out"));

        assertThat(revoked).isFalse();
        verifyNoInteractions(credentialRepository);
    }
}
