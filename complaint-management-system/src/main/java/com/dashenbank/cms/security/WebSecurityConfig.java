package com.dashenbank.cms.security;

import com.dashenbank.cms.config.AppHttpProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;
    private final AppHttpProperties appHttpProperties;
    private final PasswordEncoder passwordEncoder;

    public WebSecurityConfig(UserDetailsServiceImpl userDetailsService, AuthEntryPointJwt unauthorizedHandler,
            AppHttpProperties appHttpProperties, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
        this.appHttpProperties = appHttpProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    @SuppressWarnings("deprecation")
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/expired-password").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/auth/password").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/complaints/start").permitAll()
                        .requestMatchers("/api/complaints/status", "/api/complaints/status/**").permitAll()
                        .requestMatchers("/api/complaints/attachments/**").permitAll()
                        .requestMatchers("/api/complaints/upload-evidence").permitAll()
                        .requestMatchers("/api/customer-feedback").permitAll()
                        .requestMatchers("/api/customer-feedback/validate").permitAll()
                        .requestMatchers("/api/hierarchy").permitAll()
                        .requestMatchers("/api/complaints/staff-submit", "/api/complaints/fcr-resolve").authenticated()
                        .requestMatchers("/api/users/password-status").authenticated()
                        .requestMatchers("/api/users/officers").hasAnyAuthority(
                                "ROLE_CUSTOMER_CARE_OFFICER",
                                "ROLE_CUSTOMER_CARE_TEAM_LEADER",
                                "ROLE_CUSTOMER_CARE_SENIOR_MANAGER",
                                "ROLE_SERVICE_QUALITY_DIRECTOR",
                                "ROLE_ADMIN")
                        .requestMatchers("/api/admin/**", "/api/users", "/api/users/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/sla/config/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/rca/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/nbe-compliance-reports/**", "/api/nbe-compliance-reports")
                        .hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/complaints/nbe-reports").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/complainant-related-information",
                                "/api/complainant-related-information/**")
                        .hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/customer-feedback/init-db").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/customer-feedback/list", "/api/customer-feedback/analytics",
                                "/api/customer-feedback/distributions", "/api/customer-feedback/trends")
                        .hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/audit/logs", "/api/audit/analytics/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/audit/sla/**").authenticated()
                        .requestMatchers("/api/cmd/analytics/**").authenticated()
                        .requestMatchers("/api/tasks/**", "/api/process/**").authenticated()
                        .anyRequest().authenticated());

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        String[] origins = appHttpProperties.corsAllowedOrigins();
        if (origins.length > 0) {
            configuration.setAllowedOrigins(Arrays.asList(origins));
        }
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of(JwtUtils.HEADER_NEW_ACCESS_TOKEN));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
