package com.dashenbank.cms.security;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SecretSourceReporter {

    private final Environment environment;

    public SecretSourceReporter(Environment environment) {
        this.environment = environment;
    }

    public String jwtSecretSource() {
        return source("APP_JWT_SECRET");
    }

    public String ldapCredentialSource() {
        return source("LDAP_BIND_PASSWORD");
    }

    public String smtpCredentialSource() {
        if (StringUtils.hasText(environment.getProperty("GMAIL_SMTP_PASSWORD"))) {
            return source("GMAIL_SMTP_PASSWORD");
        }
        if (StringUtils.hasText(environment.getProperty("SPRING_MAIL_PASSWORD"))) {
            return source("SPRING_MAIL_PASSWORD");
        }
        return "NOT_REQUIRED";
    }

    private String source(String envName) {
        if (StringUtils.hasText(environment.getProperty("VAULT_TOKEN"))
                || StringUtils.hasText(environment.getProperty("SPRING_CLOUD_VAULT_URI"))) {
            return "VAULT";
        }
        if (StringUtils.hasText(environment.getProperty("SPRING_CLOUD_CONFIG_URI"))) {
            return "CONFIG_SERVER";
        }
        if (StringUtils.hasText(environment.getProperty(envName))) {
            return "ENVIRONMENT";
        }
        return "NOT_CONFIGURED";
    }
}
