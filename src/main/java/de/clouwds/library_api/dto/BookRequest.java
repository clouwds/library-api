package de.clouwds.library_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookRequest(
        @NotBlank String title,
        @NotBlank String genre,
        @NotNull Integer publicationYear,
        @NotNull Boolean available,
        @NotNull String isbn,
        @NotNull Long authorId) {}
