package de.clouwds.library_api.service;

import de.clouwds.library_api.model.MemberPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class JwtService {

    private static final String TOKEN_VALIDATION_ERROR = "Token validation error {}";
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final String secretBase64;

    public JwtService(@Value("${jwt.secret}") String secretBase64) {
        this.secretBase64 = secretBase64;
    }

    public String generateToken(MemberPrincipal memberPrincipal) {
        SecretKey secret = decodeSecretKey();

        Instant now = Instant.now();
        Date iat = Date.from(now);
        Date exp = Date.from(now.plus(Duration.ofMinutes(15)));

        JwtBuilder jwtBuilder = Jwts.builder();
        jwtBuilder.subject(memberPrincipal.getUsername());
        jwtBuilder.claim("id", memberPrincipal.getId());
        jwtBuilder.claim("roleString", memberPrincipal.getPrimaryRole());
        jwtBuilder.issuedAt(iat);
        jwtBuilder.expiration(exp);
        jwtBuilder.signWith(secret);

        return jwtBuilder.compact();
    }

    public Optional<Jws<Claims>> validateToken(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(decodeSecretKey())
                    .build()
                    .parseSignedClaims(token));
        } catch (ExpiredJwtException | UnsupportedJwtException | SignatureException | MalformedJwtException |
                 IllegalArgumentException e) {
            log.warn(TOKEN_VALIDATION_ERROR, e.getMessage());
        }
        return Optional.empty();
    }

    @NonNull
    public UsernamePasswordAuthenticationToken buildAuthentication(Jws<Claims> claims) {
        Long id = claims.getPayload().get("id", Long.class);
        String username = claims.getPayload().getSubject();
        String roleString = claims.getPayload().get("roleString", String.class);

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleString));
        UserDetails memberPrincipal = new MemberPrincipal(id, username, null, authorities);

        //principal (UserDetails), credentials, authorities
        return new UsernamePasswordAuthenticationToken(memberPrincipal, null, authorities);
    }

    private @NonNull SecretKey decodeSecretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
    }

}
