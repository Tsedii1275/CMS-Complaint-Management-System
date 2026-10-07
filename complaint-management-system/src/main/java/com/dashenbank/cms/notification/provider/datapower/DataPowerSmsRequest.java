package com.dashenbank.cms.notification.provider.datapower;

/**
 * Dashen DataPower {@code POST /SMS/send} body. Field names match the SMS
 * team's UAT collection.
 */
public record DataPowerSmsRequest(String sendFor, String message, String phoneNumber, String sendDate) {
}
