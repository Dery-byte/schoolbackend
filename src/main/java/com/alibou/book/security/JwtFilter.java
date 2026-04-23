package com.alibou.book.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getServletPath();
        String authHeader = request.getHeader("Authorization");

        // Debug log
        System.out.println("[JwtFilter] Path=" + requestPath + ", Authorization=" + authHeader);

        // Skip JWT check for auth and oauth2 endpoints
        if (requestPath.startsWith("/oauth2/")
                || requestPath.startsWith("/login/oauth2/")
                || requestPath.startsWith("/auth/login")
                || requestPath.startsWith("/auth/register")
                || requestPath.startsWith("/auth/authenticate")
                || requestPath.contains("/error")
                || requestPath.contains("/api/v1/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        // If no Authorization header or not Bearer token → just continue
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = authHeader.substring(7);
            String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    System.out.println("[JwtFilter] Set Authentication for user=" + userEmail);
                } else {
                    System.out.println("[JwtFilter] Invalid JWT for user=" + userEmail);
                }
            }
        } catch (Exception e) {
            System.err.println("[JwtFilter] Exception while validating JWT: " + e.getMessage());
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }
}
