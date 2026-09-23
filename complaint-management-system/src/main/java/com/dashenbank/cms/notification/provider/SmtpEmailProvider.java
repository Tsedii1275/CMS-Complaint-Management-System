package com.dashenbank.cms.notification.provider;

import com.dashenbank.cms.notification.NotificationProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;

/**
 * Standard SMTP submission through Spring {@link JavaMailSender}. Covers an
 * internal SMTP relay and Exchange / Office 365 SMTP submission; the target is
 * chosen entirely by {@code SPRING_MAIL_*} settings.
 */
@Component
public class SmtpEmailProvider implements EmailProvider {

    public static final String ID = "smtp";
    private static final int MAX_DETAIL_LENGTH = 300;

    private final ObjectProvider<JavaMailSender> mailSender;
    private final Environment environment;
    private final NotificationProperties properties;

    public SmtpEmailProvider(ObjectProvider<JavaMailSender> mailSender, Environment environment,
            NotificationProperties properties) {
        this.mailSender = mailSender;
        this.environment = environment;
        this.properties = properties;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void validateConfiguration() {
        if (!StringUtils.hasText(environment.getProperty("spring.mail.host")) || mailSender.getIfAvailable() == null) {
            throw new IllegalStateException(
                    "NOTIFICATION_EMAIL_PROVIDER=smtp requires SPRING_MAIL_HOST (and SPRING_MAIL_PORT)");
        }
        if (!StringUtils.hasText(senderAddress(properties.getEmail().getFrom()))) {
            throw new IllegalStateException(
                    "NOTIFICATION_EMAIL_PROVIDER=smtp requires NOTIFICATION_EMAIL_FROM (or SPRING_MAIL_USERNAME)");
        }
    }

    @Override
    public ProviderResult send(EmailMessage message) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            return ProviderResult.retryable("SMTP transport is not configured");
        }
        String from = senderAddress(message.from());
        if (!StringUtils.hasText(from)) {
            return ProviderResult.permanent("No sender address configured (NOTIFICATION_EMAIL_FROM)");
        }
        MimeMessage mime;
        try {
            mime = sender.createMimeMessage();
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
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            return ProviderResult.permanent("Message could not be built: " + describe(e));
        }

        try {
            sender.send(mime);
            return ProviderResult.delivered(messageId(mime));
        } catch (MailAuthenticationException e) {
            return ProviderResult.retryable("SMTP authentication failed");
        } catch (MailSendException e) {
            ProviderResult classified = hasRejectedRecipient(e)
                    ? ProviderResult.permanent("Recipient address rejected by SMTP server")
                    : ProviderResult.retryable(describe(e));
            if (classified.outcome() == ProviderResult.Outcome.RETRYABLE_FAILURE) {
                ProviderResult failover = tryBackup(sender, mime);
                if (failover != null) {
                    return failover;
                }
            }
            return classified;
        } catch (MailParseException | MailPreparationException e) {
            return ProviderResult.permanent(describe(e));
        } catch (MailException e) {
            ProviderResult failover = tryBackup(sender, mime);
            return failover != null ? failover : ProviderResult.retryable(describe(e));
        }
    }

    /**
     * Second NAT / Exchange host after a connection-level failure on the primary.
     */
    private ProviderResult tryBackup(JavaMailSender primary, MimeMessage mime) {
        String backupHost = properties.getEmail().getBackupHost();
        if (!StringUtils.hasText(backupHost) || !(primary instanceof JavaMailSenderImpl primaryImpl)) {
            return null;
        }
        if (backupHost.trim().equalsIgnoreCase(primaryImpl.getHost())) {
            return null;
        }
        JavaMailSenderImpl backup = copyTransport(primaryImpl);
        backup.setHost(backupHost.trim());
        try {
            backup.send(mime);
            return ProviderResult.delivered(messageId(mime));
        } catch (MailAuthenticationException e) {
            return ProviderResult.retryable("SMTP authentication failed on backup host");
        } catch (MailSendException e) {
            return hasRejectedRecipient(e)
                    ? ProviderResult.permanent("Recipient address rejected by SMTP server")
                    : ProviderResult.retryable(describe(e));
        } catch (MailParseException | MailPreparationException e) {
            return ProviderResult.permanent(describe(e));
        } catch (MailException e) {
            return ProviderResult.retryable(describe(e));
        }
    }

    private static JavaMailSenderImpl copyTransport(JavaMailSenderImpl primary) {
        JavaMailSenderImpl copy = new JavaMailSenderImpl();
        copy.setPort(primary.getPort());
        copy.setProtocol(primary.getProtocol());
        copy.setUsername(primary.getUsername());
        copy.setPassword(primary.getPassword());
        copy.setDefaultEncoding(primary.getDefaultEncoding());
        if (primary.getJavaMailProperties() != null) {
            copy.setJavaMailProperties(primary.getJavaMailProperties());
        }
        return copy;
    }

    private String senderAddress(String configuredFrom) {
        if (StringUtils.hasText(configuredFrom)) {
            return configuredFrom.trim();
        }
        return environment.getProperty("spring.mail.username", "").trim();
    }

    private static boolean hasRejectedRecipient(MailSendException e) {
        return e.getFailedMessages().values().stream()
                .anyMatch(cause -> cause instanceof SendFailedException sfe
                        && sfe.getInvalidAddresses() != null
                        && sfe.getInvalidAddresses().length > 0);
    }

    private static String messageId(MimeMessage mime) {
        try {
            return mime.getMessageID();
        } catch (MessagingException e) {
            return null;
        }
    }

    private static String describe(Exception e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String text = root.getClass().getSimpleName()
                + (root.getMessage() == null ? "" : ": " + root.getMessage());
        return text.length() > MAX_DETAIL_LENGTH ? text.substring(0, MAX_DETAIL_LENGTH) : text;
    }
}
