package com.dashenbank.cms.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * All notification settings. Values come from {@code NOTIFICATION_*} environment
 * variables via application.properties; secrets belong in gitignored
 * {@code .env.<env>.local} files or the deployment secret store.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    private boolean enabled = true;
    private final Email email = new Email();
    private final Sms sms = new Sms();
    private final Retry retry = new Retry();
    private final Dispatcher dispatcher = new Dispatcher();
    private final Templates templates = new Templates();

    @Getter
    @Setter
    public static class Email {
        private boolean enabled = true;
        private String provider = "log";
        private String from = "";
        private String fromName = "";
        private String replyTo = "";
        /** Optional second SMTP host (HA NAT). Tried only after the primary fails to connect. */
        private String backupHost = "";
        private final Gmail gmail = new Gmail();
    }

    /**
     * Optional Gmail SMTP for {@code @gmail.com} recipients. Exchange on UAT
     * rejects Gmail RCPT; this path uses smtp.gmail.com when enabled.
     * Username/app password belong in {@code .env.*.local}.
     */
    @Getter
    @Setter
    public static class Gmail {
        private boolean enabled = false;
        private String host = "smtp.gmail.com";
        private int port = 587;
        private String username = "";
        private String password = "";
        private String from = "";
    }

    @Getter
    @Setter
    public static class Sms {
        private boolean enabled = true;
        private String provider = "log";
        private String senderId = "";
        private String defaultCountryCode = "251";
        private final Gateway gateway = new Gateway();
    }

    /**
     * Connection settings for a future bank SMS gateway provider. Unused by the
     * built-in {@code log} provider.
     */
    @Getter
    @Setter
    public static class Gateway {
        private String baseUrl = "";
        private String username = "";
        private String password = "";
        private String apiKey = "";
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 10000;
    }

    @Getter
    @Setter
    public static class Retry {
        private int maxAttempts = 5;
        private long initialDelaySeconds = 60;
        private double multiplier = 5.0;
        private long maxDelaySeconds = 7200;
    }

    @Getter
    @Setter
    public static class Dispatcher {
        private boolean enabled = true;
        private int batchSize = 50;
        private int corePoolSize = 2;
        private int maxPoolSize = 4;
        private int queueCapacity = 500;
        private long staleAfterSeconds = 300;
    }

    @Getter
    @Setter
    public static class Templates {
        private String location = "classpath:notification/templates/";
    }
}
