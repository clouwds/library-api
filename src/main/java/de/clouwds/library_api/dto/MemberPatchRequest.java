package de.clouwds.library_api.dto;

import de.clouwds.library_api.model.Role;
import jakarta.annotation.Nullable;

public record MemberPatchRequest(@Nullable String firstName, @Nullable String lastName, @Nullable Role role) {}
