package com.barclays.eagle_bank_api.security;

import com.barclays.eagle_bank_api.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;
  private final UserRepository userRepository;

  private static final String BEARER_PREFIX = "Bearer ";

  public JwtAuthFilter(JwtProvider jwtProvider, UserRepository userRepository) {
    this.jwtProvider = jwtProvider;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    final var authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      final var jwt = authHeader.substring(BEARER_PREFIX.length());

      if (SecurityContextHolder.getContext().getAuthentication() == null
          && jwtProvider.isTokenValid(jwt)) {

        final var userId = jwtProvider.extractUserId(jwt);
        userRepository
            .findById(userId)
            .ifPresent(
                user -> {
                  final var authToken =
                      new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

                  authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                  SecurityContextHolder.getContext().setAuthentication(authToken);
                });
      }
    } catch (Exception e) {
      logger.error("Unexpected error during JWT authentication", e);
    }

    filterChain.doFilter(request, response);
  }
}
