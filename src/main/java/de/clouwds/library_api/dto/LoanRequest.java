package de.clouwds.library_api.dto;

import jakarta.validation.constraints.NotNull;

public record LoanRequest(
        @NotNull Long memberId,
        @NotNull Long bookId) {}
