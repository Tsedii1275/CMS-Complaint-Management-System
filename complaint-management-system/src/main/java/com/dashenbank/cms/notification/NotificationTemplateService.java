package com.dashenbank.cms.notification;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads customer message templates from
 * {@code <NOTIFICATION_TEMPLATES_LOCATION>messages_<lang>.properties} (UTF-8)
 * and renders {@code {placeholder}} variables. Copy can be changed by pointing
 * the location at an external directory, without a code change.
 */
@Component
public class NotificationTemplateService {

    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_AMHARIC = "am";
    private static final List<String> SUPPORTED_LANGUAGES = List.of(LANGUAGE_ENGLISH, LANGUAGE_AMHARIC);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z]\\w*)}");

    private final Map<String, Properties> templatesByLanguage = new HashMap<>();

    public NotificationTemplateService(ResourceLoader resourceLoader, NotificationProperties properties) {
        String location = properties.getTemplates().getLocation();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        for (String language : SUPPORTED_LANGUAGES) {
            Resource resource = resourceLoader.getResource(location + "messages_" + language + ".properties");
            Properties templates = load(resource);
            requireAllKeys(templates, resource);
            templatesByLanguage.put(language, templates);
        }
    }

    public static String resolveLanguage(String preferredLanguage) {
        if (preferredLanguage != null) {
            String value = preferredLanguage.trim();
            if ("amharic".equalsIgnoreCase(value) || LANGUAGE_AMHARIC.equalsIgnoreCase(value)) {
                return LANGUAGE_AMHARIC;
            }
        }
        return LANGUAGE_ENGLISH;
    }

    public RenderedMessage render(NotificationEventType eventType, NotificationChannel channel, String language,
            Map<String, String> variables) {
        Properties templates = templatesByLanguage.get(resolveLanguage(language));
        String prefix = keyPrefix(eventType, channel);
        String subject = channel == NotificationChannel.EMAIL
                ? fill(templates.getProperty(prefix + "subject"), variables)
                : null;
        return new RenderedMessage(subject, fill(templates.getProperty(prefix + "body"), variables));
    }

    static String fill(String template, Map<String, String> variables) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String value = variables == null ? null : variables.get(matcher.group(1));
            matcher.appendReplacement(out, Matcher.quoteReplacement(value == null ? "" : value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String keyPrefix(NotificationEventType eventType, NotificationChannel channel) {
        return eventType.templateKey() + "." + channel.templateSegment() + ".";
    }

    private static Properties load(Resource resource) {
        if (!resource.exists()) {
            throw new IllegalStateException("Notification template file not found: " + resource.getDescription());
        }
        Properties templates = new Properties();
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            templates.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read notification templates: " + resource.getDescription(), e);
        }
        return templates;
    }

    private static void requireAllKeys(Properties templates, Resource resource) {
        List<String> missing = new ArrayList<>();
        for (NotificationEventType eventType : NotificationEventType.values()) {
            String emailPrefix = keyPrefix(eventType, NotificationChannel.EMAIL);
            String smsPrefix = keyPrefix(eventType, NotificationChannel.SMS);
            for (String key : List.of(emailPrefix + "subject", emailPrefix + "body", smsPrefix + "body")) {
                if (templates.getProperty(key, "").isBlank() && !missing.contains(key)) {
                    missing.add(key);
                }
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Notification templates " + resource.getDescription()
                    + " are missing keys: " + String.join(", ", missing));
        }
    }
}
