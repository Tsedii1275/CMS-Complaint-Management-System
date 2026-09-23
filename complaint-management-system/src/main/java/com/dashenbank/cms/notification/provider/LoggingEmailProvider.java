package com.dashenbank.cms.notification.provider;

import com.dashenbank.cms.notification.RecipientFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Non-delivering provider for environments without an approved mail transport.
 * Messages are recorded as SKIPPED in {@code notifications}; bodies are never logged.
 */
@Component
public class LoggingEmailProvider implements EmailProvider {

    public static final String ID = "log";
    private static final Logger log = LoggerFactory.getLogger(LoggingEmailProvider.class);

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ProviderResult send(EmailMessage message) {
        log.info("[EMAIL:{}] Delivery disabled; recorded only. to={} subject={}",
                ID, RecipientFormat.maskEmail(message.to()), message.subject());
        return ProviderResult.notDelivered("Email provider 'log' records messages without delivering them");
    }
}
