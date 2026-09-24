package com.dashenbank.cms.security.ldap;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LdapProperties.class)
public class LdapConfiguration {
}
