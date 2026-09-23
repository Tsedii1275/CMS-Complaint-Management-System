package com.dashenbank.cms.notification.provider;

/**
 * @param to       recipient in E.164 format, for example {@code +251912345678}
 * @param senderId gateway sender ID / short code; may be blank
 */
public record SmsMessage(String to, String senderId, String body) {
}
