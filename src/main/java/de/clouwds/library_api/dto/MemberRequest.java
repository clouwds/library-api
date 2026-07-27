package de.clouwds.library_api.dto;

import jakarta.validation.constraints.NotBlank;

public record MemberRequest(@NotBlank String firstName, @NotBlank String lastName) {}
