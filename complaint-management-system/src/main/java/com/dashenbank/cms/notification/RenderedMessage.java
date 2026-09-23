package com.dashenbank.cms.notification;

/**
 * @param subject email subject; {@code null} for SMS
 */
public record RenderedMessage(String subject, String body) {
}
