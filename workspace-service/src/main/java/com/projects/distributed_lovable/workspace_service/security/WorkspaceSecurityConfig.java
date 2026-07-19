package com.projects.distributed_lovable.workspace_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.projects.distributed_lovable.common_lib.security.JwtAuthFilter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class WorkspaceSecurityConfig {
        private final JwtAuthFilter jwtAuthFilter;
        private final HandlerExceptionResolver handlerExceptionResolver;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
                return httpSecurity
                                .csrf(csrfConfig -> csrfConfig.disable())
                                .cors(Customizer.withDefaults())
                                .sessionManagement(
                                                sessionConfig -> sessionConfig
                                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(exceptionConfig -> exceptionConfig
                                                .authenticationEntryPoint((request, response, authException) -> response
                                                                .sendError(
                                                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                                                "Unauthorized: Missing or invalid Bearer token"))
                                                .accessDeniedHandler((request, response,
                                                                accessDeniedException) -> response.sendError(
                                                                                HttpServletResponse.SC_FORBIDDEN,
                                                                                "Forbidden: Insufficient permissions")))
                                .authorizeHttpRequests(auth -> auth
                                                .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                                                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                                                .requestMatchers("/actuator/**", "/internal/**").permitAll()
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .exceptionHandling(exceptionHandlingConfigurer -> exceptionHandlingConfigurer
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        handlerExceptionResolver.resolveException(request, response,
                                                                        null, accessDeniedException);
                                                }))
                                .build();
        }
}