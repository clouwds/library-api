package de.clouwds.library_api.service;

import de.clouwds.library_api.exception.InvalidTokenException;
import de.clouwds.library_api.exception.TokenAlreadyUsedException;
import de.clouwds.library_api.exception.TokenExpiredException;
import de.clouwds.library_api.model.Member;
import de.clouwds.library_api.model.RefreshToken;
import de.clouwds.library_api.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String generateRefreshToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    public RefreshToken validateToken(String refreshTokenString) {
        String hashedToken = hashToken(refreshTokenString);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashedToken).orElseThrow(() -> new InvalidTokenException("Invalid refresh token."));
        if (refreshToken.isUsed()) {
            throw new TokenAlreadyUsedException("Token has already been used.");
        }

        if (new Date().after(refreshToken.getExpiresAt())) {
            throw new TokenExpiredException("Refresh token has expired.");
        }
        return refreshToken;
    }

    public void markUsed(RefreshToken refreshToken) {
        refreshToken.setUsed(true);
        refreshTokenRepository.save(refreshToken);
    }

    void persistRefreshToken(String refreshToken, Member member) {
        RefreshToken newRefreshToken = RefreshToken.issuedNow(hashToken(refreshToken), member, Duration.ofDays(7));
        refreshTokenRepository.save(newRefreshToken);
    }

    private static String hashToken(String refreshToken) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        byte[] hashBytes = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
    }

}
