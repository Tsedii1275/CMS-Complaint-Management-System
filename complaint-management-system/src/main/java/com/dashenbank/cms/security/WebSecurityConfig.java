package com.dashenbank.cms.security;

import com.dashenbank.cms.config.AppHttpProperties;
import com.dashenbank.cms.model.Role;
import com.dashenbank.cms.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    private static final String ROLE_ADMIN = Role.ROLE_ADMIN.name();

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;
    private final AppHttpProperties appHttpProperties;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public WebSecurityConfig(UserDetailsServiceImpl userDetailsService, AuthEntryPointJwt unauthorizedHandler,
            AppHttpProperties appHttpProperties, PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
        this.appHttpProperties = appHttpProperties;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public ApiRateLimitFilter apiRateLimitFilter() {
        return new ApiRateLimitFilter();
    }

    @Bean
    public MustChangePasswordFilter mustChangePasswordFilter() {
        return new MustChangePasswordFilter(userRepository);
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
                .headers(headers -> {
                    headers.contentTypeOptions(Customizer.withDefaults());
                    headers.frameOptions(frame -> frame.deny());
                    headers.referrerPolicy(policy -> policy.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    headers.permissionsPolicy(
                            policy -> policy.policy("camera=(), microphone=(), geolocation=()"));
                    headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"));
                })
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
                        .requestMatchers("/api/customer-profile/**", "/api/customers/by-account/**").authenticated()
                        .requestMatchers("/api/users/password-status").authenticated()
                        .requestMatchers("/api/users/officers").hasAnyAuthority(
                                "ROLE_CUSTOMER_CARE_OFFICER",
                                "ROLE_CUSTOMER_CARE_TEAM_LEADER",
                                "ROLE_CUSTOMER_CARE_SENIOR_MANAGER",
                                "ROLE_SERVICE_QUALITY_DIRECTOR",
                                ROLE_ADMIN)
                        .requestMatchers("/api/admin/**", "/api/users", "/api/users/**").hasAuthority(ROLE_ADMIN)
                        .requestMatchers("/api/sla/alerts", "/api/sla/alerts/**").authenticated()
                        .requestMatchers("/api/sla/config/**").hasAuthority(ROLE_ADMIN)
                        .requestMatchers("/api/rca/**").hasAuthority(ROLE_ADMIN)
                        .requestMatchers("/api/nbe-compliance-reports/**", "/api/nbe-compliance-reports")
                        .hasAuthority(ROLE_ADMIN)
                        .requestMatchers("/api/complaints/nbe-reports").hasAuthority(ROLE_ADMIN)
                        .requestMatchers("/api/complainant-related-information",
                                "/api/complainant-related-information/**")
                        .hasAuthority(ROLE_ADMIN)
                        .requestMatchers("/api/customer-feedback/list", "/api/customer-feedback/analytics",
                                "/api/customer-feedback/distributions", "/api/customer-feedback/trends")
                        .hasAuthority(ROLE_ADMIN)
                        .requestMatchers("/api/audit/logs", "/api/audit/analytics/**").hasAuthority(ROLE_ADMIN)
                        .requestMatchers("/api/audit/sla/**").authenticated()
                        .requestMatchers("/api/cmd/analytics/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/process/**").hasAuthority(ROLE_ADMIN)
                        .requestMatchers("/api/tasks/**", "/api/process/**").authenticated()
                        .anyRequest().authenticated());

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(apiRateLimitFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(mustChangePasswordFilter(), AuthTokenFilter.class);

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
