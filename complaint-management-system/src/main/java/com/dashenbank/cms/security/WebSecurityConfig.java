package com.dashenbank.cms.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;

    public WebSecurityConfig(UserDetailsServiceImpl userDetailsService, AuthEntryPointJwt unauthorizedHandler) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
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
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
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
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://127.0.0.1:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
