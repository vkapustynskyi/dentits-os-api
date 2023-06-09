package com.vkapustynskyi.dentist.os.core.authentication;

import com.vkapustynskyi.dentist.os.core.authentication.service.JwtService;
import com.vkapustynskyi.dentist.os.core.authentication.util.AuthenticationUtil;
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
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authenticationHeader = AuthenticationUtil.extractAuthenticationHeader(request);
        if (!AuthenticationUtil.isJwtToken(authenticationHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwtToken = AuthenticationUtil.extractJwtToken(authenticationHeader);
        String username = jwtService.extractUsername(jwtToken);
        if (Objects.isNull(username) || Objects.nonNull(SecurityContextHolder.getContext().getAuthentication())) {
            filterChain.doFilter(request, response);
            return;
        }
        authenticateUser(request, jwtToken, username);
    }

    private void authenticateUser(HttpServletRequest request, String jwtToken, String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (jwtService.isTokenValid(jwtToken, userDetails)) {
            UsernamePasswordAuthenticationToken authenticationToken = buildAuthenticationToken(request, userDetails);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
    }

    private static UsernamePasswordAuthenticationToken buildAuthenticationToken(HttpServletRequest request, UserDetails userDetails) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authenticationToken;
    }

}
