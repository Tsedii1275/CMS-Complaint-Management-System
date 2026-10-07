package com.dashenbank.cms.notification;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationConfig {

    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}
