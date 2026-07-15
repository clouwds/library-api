package de.clouwds.library_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuthorRequest(@NotBlank String name) {}
