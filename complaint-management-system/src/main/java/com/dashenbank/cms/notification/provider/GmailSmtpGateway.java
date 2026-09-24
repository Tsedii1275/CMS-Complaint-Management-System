package com.dashenbank.cms.notification.provider;

import com.dashenbank.cms.notification.NotificationProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Properties;

/**
 * Optional smtp.gmail.com transport for Gmail recipients. Bank Exchange on UAT
 * rejects {@code @gmail.com} RCPT; this gateway is used only when
 * {@code GMAIL_SMTP_ENABLED=true} and an app password is configured.
 */
@Component
public class GmailSmtpGateway {

    private final NotificationProperties.Gmail settings;

    public GmailSmtpGateway(NotificationProperties properties) {
        this.settings = properties.getEmail().getGmail();
    }

    public boolean handles(String recipient) {
        return configured() && isGmailAddress(recipient);
    }

    public boolean configured() {
        return settings.isEnabled()
                && StringUtils.hasText(settings.getHost())
                && StringUtils.hasText(settings.getUsername())
                && StringUtils.hasText(settings.getPassword())
                && StringUtils.hasText(fromAddress());
    }

    public void validateIfEnabled() {
        if (!settings.isEnabled()) {
            return;
        }
        if (!configured()) {
            throw new IllegalStateException(
                    "GMAIL_SMTP_ENABLED=true requires GMAIL_SMTP_USERNAME, GMAIL_SMTP_PASSWORD, and GMAIL_SMTP_FROM (or username as From) in .env.*.local");
        }
    }

    public ProviderResult send(EmailMessage message) {
        if (!configured()) {
            return ProviderResult.retryable("Gmail SMTP is not configured");
        }
        String from = fromAddress();
        JavaMailSenderImpl sender = mailSender();
        try {
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            if (StringUtils.hasText(message.fromName())) {
                helper.setFrom(from, message.fromName());
            } else {
                helper.setFrom(from);
            }
            if (StringUtils.hasText(message.replyTo())) {
                helper.setReplyTo(message.replyTo());
            }
            helper.setTo(message.to());
            helper.setSubject(message.subject() == null ? "" : message.subject());
            helper.setText(message.body() == null ? "" : message.body(), false);
            sender.send(mime);
            return ProviderResult.delivered(mime.getMessageID());
        } catch (MailAuthenticationException e) {
            return ProviderResult.retryable("Gmail SMTP authentication failed");
        } catch (MailParseException | MailPreparationException e) {
            return ProviderResult.permanent(rootDetail(e));
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            return ProviderResult.retryable(rootDetail(e));
        }
    }

    static boolean isGmailAddress(String recipient) {
        if (!StringUtils.hasText(recipient)) {
            return false;
        }
        String lower = recipient.trim().toLowerCase(Locale.ROOT);
        return lower.endsWith("@gmail.com") || lower.endsWith("@googlemail.com");
    }

    private String fromAddress() {
        if (StringUtils.hasText(settings.getFrom())) {
            return settings.getFrom().trim();
        }
        return settings.getUsername() == null ? "" : settings.getUsername().trim();
    }

    private JavaMailSenderImpl mailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.getHost().trim());
        sender.setPort(Math.max(1, settings.getPort()));
        sender.setUsername(settings.getUsername().trim());
        sender.setPassword(settings.getPassword());
        sender.setProtocol("smtp");
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.checkserveridentity", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        return sender;
    }

    private static String rootDetail(Exception error) {
        Throwable current = error;
        String text = error.getClass().getSimpleName();
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                text = current.getClass().getSimpleName() + ": " + current.getMessage();
            }
            current = current.getCause();
        }
        return text.length() > 300 ? text.substring(0, 300) : text;
    }
}
