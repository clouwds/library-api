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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final String TOKEN_VALIDATION_ERROR = "Token validation error {}";
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final AuthenticationManager authenticationManager;
    private final String secretBase64;

    public JwtService(AuthenticationManager authenticationManager, @Value("${jwt.secret}") String secretBase64) {
        this.authenticationManager = authenticationManager;
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

    private @NonNull SecretKey decodeSecretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
    }

    public MemberPrincipal authenticate(String email, String password) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(email, password);
        Authentication auth = authenticationManager.authenticate(token);
        return  (MemberPrincipal) auth.getPrincipal();
    }

    public Jws<Claims> validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(decodeSecretKey())
                    .build()
                    .parseSignedClaims(token);
        } catch (ExpiredJwtException | UnsupportedJwtException | SignatureException | MalformedJwtException | IllegalArgumentException e) {
            log.warn(TOKEN_VALIDATION_ERROR, e.getMessage());
        }
        return null;
    }

}
