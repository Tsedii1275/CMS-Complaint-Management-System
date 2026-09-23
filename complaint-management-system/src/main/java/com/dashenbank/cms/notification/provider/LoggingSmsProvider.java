package com.dashenbank.cms.notification.provider;

import com.dashenbank.cms.notification.RecipientFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Non-delivering provider used until the bank SMS gateway is approved.
 * Messages are recorded as SKIPPED in {@code notifications}; bodies are never logged.
 */
@Component
public class LoggingSmsProvider implements SmsProvider {

    public static final String ID = "log";
    private static final Logger log = LoggerFactory.getLogger(LoggingSmsProvider.class);

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ProviderResult send(SmsMessage message) {
        log.info("[SMS:{}] Delivery disabled; recorded only. to={} length={}",
                ID, RecipientFormat.maskPhone(message.to()), message.body() == null ? 0 : message.body().length());
        return ProviderResult.notDelivered("SMS provider 'log' records messages without delivering them");
    }
}
