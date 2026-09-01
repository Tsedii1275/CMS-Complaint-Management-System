package com.dashenbank.cms.service;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import jakarta.annotation.PostConstruct;

@Service
public class SmsService {
    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-phone:}")
    private String fromPhone;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (StringUtils.hasText(accountSid) && StringUtils.hasText(authToken)) {
            try {
                Twilio.init(accountSid, authToken);
                initialized = true;
                log.info("Twilio SMS Service successfully initialized.");
            } catch (Exception e) {
                log.error("Failed to initialize Twilio client: {}", e.getMessage());
            }
        } else {
            log.warn("Twilio credentials not fully configured (accountSid or authToken missing). SMS sending is disabled.");
        }
    }

    public boolean sendSms(String phoneNumber, String messageText) {
        if (!initialized) {
            log.warn("Twilio SMS client not initialized. Fallback log for message to {}: {}", phoneNumber, messageText);
            return false;
        }
        if (!StringUtils.hasText(phoneNumber)) {
            log.error("Recipient phone number is required to send SMS.");
            return false;
        }
        if (!StringUtils.hasText(fromPhone)) {
            log.error("Twilio from-phone is not configured.");
            return false;
        }

        try {
            Message message = Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(fromPhone),
                messageText
            ).create();

            log.info("[SMS] Sent successfully to {} via Twilio. Message SID: {}", phoneNumber, message.getSid());
            return true;
        } catch (ApiException e) {
            log.error("[SMS] Twilio API error sending to {}: {}", phoneNumber, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("[SMS] Unexpected error sending to {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }
}
