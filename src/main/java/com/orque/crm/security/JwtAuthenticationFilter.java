package com.orque.crm.security;

import com.orque.crm.auth.entity.User;
import com.orque.crm.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    )
            throws ServletException,
            IOException {

        final String authHeader =
                request.getHeader("Authorization");

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String jwt = authHeader.substring(7).trim();

        System.out.println("JWT = [" + jwt + "]");

        String username =
                jwtService.extractUsername(jwt);

        if (username != null &&
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        == null) {

            User user =
                    userRepository
                            .findByUsername(username)
                            .orElse(null);

            if (user != null &&
                    jwtService
                            .isTokenValid(
                                    jwt,
                                    user
                            )) {

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                null
                        );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(
                                authToken
                        );
            }
        }

        filterChain.doFilter(
                request,
                response
        );
    }
}