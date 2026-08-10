package de.clouwds.library_api.dto;

public record TokenResponse(String accessToken, String tokenType, String refreshToken) {}
