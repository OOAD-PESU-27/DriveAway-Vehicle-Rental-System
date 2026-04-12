package com.driveaway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * EmailService - Sends emails via JavaMailSender when SMTP is configured,
 * or falls back to a simulated log-only mode when no password is provided.
 *
 * Defaults to Gmail SMTP with the DriveAway rental-vehicle sender mailbox.
 * Override any setting via the matching environment variable:
 *   MAIL_HOST     – SMTP host (default: smtp.gmail.com)
 *   MAIL_PORT     – SMTP port (default: 587)
 *   MAIL_USERNAME – SMTP username / sender address (default: rentalvehicle.driveaway@gmail.com)
 *   MAIL_PASSWORD – SMTP password / Gmail App Password (required for real delivery)
 *   MAIL_FROM     – From address shown to recipients (defaults to MAIL_USERNAME)
 */
@Slf4j
@Service
public class EmailService {

    /** Injected only when spring.mail.host is configured (not required). */
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:rentalvehicle.driveaway@gmail.com}")
    private String fromAddress;

    /**
     * Send a plain-text email.
     * If {@link JavaMailSender} is not configured (no SMTP host in env),
     * the message is logged in simulation mode so the rest of the flow
     * continues to work without any SMTP infrastructure.
     *
     * @param to      recipient address
     * @param subject email subject
     * @param body    plain-text body
     */
    public void sendEmail(String to, String subject, String body) {
        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromAddress);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                log.info("[EMAIL SENT] To={} Subject='{}'", to, subject);
            } catch (Exception e) {
                log.error("[EMAIL ERROR] Failed to send email to={} subject='{}': {}",
                        to, subject, e.getMessage(), e);
                throw e;
            }
        } else {
            // Simulated mode – MAIL_PASSWORD not set; set it to enable real Gmail SMTP delivery
            log.info("[SIMULATED EMAIL] To={} Subject='{}' Body={}", to, subject, body);
        }
    }

    /** Returns true when a real JavaMailSender is wired in. */
    public boolean isRealMailConfigured() {
        return mailSender != null;
    }
}
