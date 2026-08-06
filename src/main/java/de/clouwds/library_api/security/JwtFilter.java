package de.clouwds.library_api.security;

import de.clouwds.library_api.model.MemberPrincipal;
import de.clouwds.library_api.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Jws<Claims> claims = jwtService.validateToken(token);

            if (claims != null) {
                UsernamePasswordAuthenticationToken auth = buildAuthentication(claims);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }

    private static @NonNull UsernamePasswordAuthenticationToken buildAuthentication(Jws<Claims> claims) {
        Long id = claims.getPayload().get("id", Long.class);
        String username = claims.getPayload().getSubject();
        String roleString = claims.getPayload().get("roleString", String.class);

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleString));
        UserDetails memberPrincipal = new MemberPrincipal(id, username, null, authorities);

        //principal (UserDetails), credentials, authorities
        return new UsernamePasswordAuthenticationToken(memberPrincipal, null, authorities);
    }
}
