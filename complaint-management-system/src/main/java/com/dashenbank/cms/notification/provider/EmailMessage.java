package com.dashenbank.cms.notification.provider;

/**
 * @param from     configured sender; blank lets the provider apply its own default
 * @param fromName optional display name
 * @param replyTo  optional reply-to address
 */
public record EmailMessage(String to, String from, String fromName, String replyTo, String subject, String body) {
}
