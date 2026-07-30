package de.clouwds.library_api.dto;

import de.clouwds.library_api.model.Role;

public record MemberResponse(Long id, String firstName, String lastName, Role role) {}
