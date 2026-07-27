package de.clouwds.library_api.dto;

import java.time.LocalDate;

public record LoanResponse(
        Long id,
        String bookTitle,
        String memberName,
        LocalDate borrowDate,
        LocalDate dueDate,
        LocalDate returnDate
) {}
