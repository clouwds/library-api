package de.clouwds.library_api.dto;

public record BookResponse(
        Long id,
        String title,
        String genre,
        Integer publicationYear,
        Boolean available,
        String isbn,
        String authorName
) {
}
