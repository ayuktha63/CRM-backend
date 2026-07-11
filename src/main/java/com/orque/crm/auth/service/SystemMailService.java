package com.orque.crm.auth.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * System-level transactional email (password reset, etc.) sent from the app's own
 * mailbox — separate from the per-user connected-mailbox Gmail OAuth feature under
 * com.orque.crm.email, since not every user has one connected, and password reset
 * must work regardless.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@orque.com}")
    private String fromAddress;

    @Value("${app.mail.from-name:Orque CRM}")
    private String fromName;

    public void send(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("System email sent to {} — subject: {}", toEmail, subject);
        } catch (Exception e) {
            log.error("Failed to send system email to {}: {}", toEmail, e.getMessage());
        }
    }
}
