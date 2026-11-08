package com.sirius.sirius.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final SecurityErrorResponseWriter errorResponseWriter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                (request, response, authException) ->
                                        errorResponseWriter.write(
                                                request,
                                                response,
                                                HttpStatus.UNAUTHORIZED,
                                                "Authentication required"
                                        )
                        )
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) ->
                                        errorResponseWriter.write(
                                                request,
                                                response,
                                                HttpStatus.FORBIDDEN,
                                                "Access denied"
                                        )
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/rooms/**"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/hotels/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/reservations/**"
                        ).authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}