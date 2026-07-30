package de.clouwds.library_api.dto;

import de.clouwds.library_api.model.Role;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;

public record MemberPatchRequest(
        @Nullable String firstName,
        @Nullable String lastName,
        @Nullable @Email String email,
        @Nullable String password,
        @Nullable Role role) {}
