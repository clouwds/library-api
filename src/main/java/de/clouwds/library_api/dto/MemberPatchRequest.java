package de.clouwds.library_api.dto;

import jakarta.annotation.Nullable;

public record MemberPatchRequest(@Nullable String firstName, @Nullable String lastName) {}
