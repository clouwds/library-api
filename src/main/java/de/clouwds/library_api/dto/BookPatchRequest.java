package de.clouwds.library_api.dto;

import jakarta.annotation.Nullable;

public record BookPatchRequest(
        @Nullable String title,
        @Nullable String genre,
        @Nullable Integer publicationYear,
        @Nullable Boolean available,
        @Nullable String isbn,
        @Nullable Long authorId) {}
