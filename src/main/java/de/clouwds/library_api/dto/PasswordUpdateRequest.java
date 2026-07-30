package de.clouwds.library_api.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordUpdateRequest(@NotBlank String password) {}
