package com.homekm.auth;

import com.homekm.common.AppProperties;
import com.homekm.common.CorsConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorsConfig corsConfig;
    private final AppProperties appProperties;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, CorsConfig corsConfig, AppProperties appProperties) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.corsConfig = corsConfig;
        this.appProperties = appProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/refresh",
                        "/api/auth/password-reset/request", "/api/auth/password-reset/confirm").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/push/vapid-public-key",
                        "/api/auth/invitations/*", "/api/share/*", "/api/files/share/*",
                        "/api/reminders/me.ics", "/api/info").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info",
                        "/actuator/prometheus").permitAll()
                .requestMatchers("/api/docs", "/api/docs/**", "/api/openapi", "/api/openapi/**",
                        "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(e -> e.authenticationEntryPoint(unauthorizedEntryPoint()))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(appProperties.getBcrypt().getCost());
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    }
}
