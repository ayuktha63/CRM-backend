package com.orque.crm.settings.smtp.service;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.settings.smtp.dto.SmtpConfigRequest;
import com.orque.crm.settings.smtp.dto.SmtpConfigResponse;
import com.orque.crm.settings.smtp.dto.SmtpTestResult;
import com.orque.crm.settings.smtp.entity.SmtpConfig;
import com.orque.crm.settings.smtp.repository.SmtpConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpConfigServiceImpl implements SmtpConfigService {

    private final SmtpConfigRepository repository;

    @Override
    public List<SmtpConfigResponse> listForCurrentUser() {
        String owner = UserContextHelper.currentUsername();
        return repository.findAllByOwnerOrderByIsDefaultDescCreatedAtDesc(owner)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public SmtpConfigResponse create(SmtpConfigRequest request) {
        String owner = UserContextHelper.currentUsername();
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearExistingDefault(owner);
        }
        SmtpConfig config = SmtpConfig.builder()
                .owner(owner)
                .displayName(request.getDisplayName())
                .host(request.getHost())
                .port(request.getPort())
                .username(request.getUsername())
                .password(request.getPassword())
                .fromAddress(request.getFromAddress())
                .fromName(request.getFromName())
                .sslEnabled(Boolean.TRUE.equals(request.getSslEnabled()))
                .tlsEnabled(!Boolean.FALSE.equals(request.getTlsEnabled()))
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .build();
        log.info("SmtpConfig created for owner={}", owner);
        return toResponse(repository.save(config));
    }

    @Override
    @Transactional
    public SmtpConfigResponse update(Long id, SmtpConfigRequest request) {
        String owner = UserContextHelper.currentUsername();
        SmtpConfig config = repository.findById(id)
                .filter(c -> c.getOwner().equals(owner))
                .orElseThrow(() -> new IllegalArgumentException("SMTP config not found: " + id));

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(config.getIsDefault())) {
            clearExistingDefault(owner);
        }
        config.setDisplayName(request.getDisplayName());
        config.setHost(request.getHost());
        config.setPort(request.getPort());
        config.setUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            config.setPassword(request.getPassword());
        }
        config.setFromAddress(request.getFromAddress());
        config.setFromName(request.getFromName());
        config.setSslEnabled(Boolean.TRUE.equals(request.getSslEnabled()));
        config.setTlsEnabled(!Boolean.FALSE.equals(request.getTlsEnabled()));
        config.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
        config.setVerified(false);
        log.info("SmtpConfig updated id={} for owner={}", id, owner);
        return toResponse(repository.save(config));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        String owner = UserContextHelper.currentUsername();
        SmtpConfig config = repository.findById(id)
                .filter(c -> c.getOwner().equals(owner))
                .orElseThrow(() -> new IllegalArgumentException("SMTP config not found: " + id));
        repository.delete(config);
        log.info("SmtpConfig deleted id={} for owner={}", id, owner);
    }

    @Override
    @Transactional
    public SmtpTestResult testConnection(Long id) {
        String owner = UserContextHelper.currentUsername();
        SmtpConfig config = repository.findById(id)
                .filter(c -> c.getOwner().equals(owner))
                .orElseThrow(() -> new IllegalArgumentException("SMTP config not found: " + id));
        try {
            JavaMailSenderImpl sender = buildSender(config);
            sender.testConnection();
            config.setVerified(true);
            config.setLastTestedAt(LocalDateTime.now());
            repository.save(config);
            log.info("SMTP test succeeded for id={} host={}", id, config.getHost());
            return SmtpTestResult.builder().success(true).message("Connection successful").build();
        } catch (Exception ex) {
            config.setVerified(false);
            config.setLastTestedAt(LocalDateTime.now());
            repository.save(config);
            log.warn("SMTP test failed for id={}: {}", id, ex.getMessage());
            return SmtpTestResult.builder().success(false).message(ex.getMessage()).build();
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void clearExistingDefault(String owner) {
        repository.findByOwnerAndIsDefaultTrue(owner).ifPresent(c -> {
            c.setIsDefault(false);
            repository.save(c);
        });
    }

    private JavaMailSenderImpl buildSender(SmtpConfig config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        sender.setPort(config.getPort());
        sender.setUsername(config.getUsername());
        sender.setPassword(config.getPassword());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", Boolean.TRUE.equals(config.getTlsEnabled()) ? "true" : "false");
        props.put("mail.smtp.ssl.enable", Boolean.TRUE.equals(config.getSslEnabled()) ? "true" : "false");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        return sender;
    }

    private SmtpConfigResponse toResponse(SmtpConfig c) {
        return SmtpConfigResponse.builder()
                .id(c.getId())
                .displayName(c.getDisplayName())
                .host(c.getHost())
                .port(c.getPort())
                .username(c.getUsername())
                .fromAddress(c.getFromAddress())
                .fromName(c.getFromName())
                .sslEnabled(c.getSslEnabled())
                .tlsEnabled(c.getTlsEnabled())
                .isDefault(c.getIsDefault())
                .verified(c.getVerified())
                .lastTestedAt(c.getLastTestedAt())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
