package com.projects.distributed_lovable.common_lib.security;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthUtil authUtil;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            log.info("Incoming request: {} {}", request.getMethod(), request.getRequestURI());
            final String requestHeaderToken = request.getHeader("Authorization");
            if (requestHeaderToken == null || !requestHeaderToken.startsWith("Bearer ")) {
                if (isPublicEndpoint(request) || HttpMethod.OPTIONS.matches(request.getMethod())) {
                    filterChain.doFilter(request, response);
                    return;
                }
                log.warn("Missing or invalid Authorization header for protected endpoint: {}", request.getRequestURI());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Unauthorized: Missing or invalid Bearer token");
                return;
            } else {
                String jwtToken = requestHeaderToken.substring(7);// Remove "Bearer " prefix
                JwtUserPrincipal user = authUtil.verifyAccessToken(jwtToken);

                if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            user,
                            jwtToken, user.authorities());
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
                filterChain.doFilter(request, response);
            }
        } catch (Exception ex) {
            handlerExceptionResolver.resolveException(request, response, null, ex);
        }
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/") || path.startsWith("/webhooks/")
                || path.startsWith("/actuator/") || path.startsWith("/internal/");
    }
}
