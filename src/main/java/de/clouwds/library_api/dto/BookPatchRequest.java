package de.clouwds.library_api.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Min;

public record BookPatchRequest(
        @Nullable String title,
        @Nullable String genre,
        @Nullable @Min(1000) Integer publicationYear,
        @Nullable Boolean available,
        @Nullable String isbn,
        @Nullable Long authorId) {}
