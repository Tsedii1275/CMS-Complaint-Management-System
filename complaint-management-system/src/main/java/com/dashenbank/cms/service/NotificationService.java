package com.dashenbank.cms.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final SmsService smsService;

    public NotificationService(@Autowired(required = false) JavaMailSender mailSender, SmsService smsService) {
        this.mailSender = mailSender;
        this.smsService = smsService;
    }

    public void sendEmail(String to, String subject, String body) {
        if (!StringUtils.hasText(to)) {
            throw new IllegalArgumentException("Email recipient is required");
        }
        if (mailSender == null) {
            throw new IllegalStateException(
                    "No JavaMailSender configured; set spring.mail properties in application.properties");
        }

        CompletableFuture.runAsync(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(body, false);
                mailSender.send(message);
                log.info("[EMAIL] sent via SMTP to={} subject={}", to, subject);
            } catch (MailException | MessagingException ex) {
                log.error("[EMAIL] SMTP send failed: {}", ex.getMessage());
            }
        });
    }

    public void sendSms(String phone, String message) {
        if (!StringUtils.hasText(phone)) {
            throw new IllegalArgumentException("Phone number is required");
        }

        CompletableFuture.runAsync(() -> {
            boolean success = smsService.sendSms(phone, message);
            if (!success) {
                log.info("[SMS] Failed or Twilio not configured. Fallback: log the message only. to={} message={}", phone, message);
            }
        });
    }
}
