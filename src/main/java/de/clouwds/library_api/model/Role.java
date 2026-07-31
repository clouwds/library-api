package de.clouwds.library_api.model;

public enum Role {
    MEMBER,
    LIBRARIAN;

    public String authority() {
        return "ROLE_" + name();
    }
}
