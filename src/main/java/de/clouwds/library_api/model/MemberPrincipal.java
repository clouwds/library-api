package de.clouwds.library_api.model;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class MemberPrincipal implements UserDetails {

    private final Long id;

    private final String username;

    private final String password;

    private final List<GrantedAuthority> authorities;

    public MemberPrincipal(Long id, String username, String password, List<GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public String getPrimaryRole() {
        if (authorities == null || authorities.isEmpty()) {
            throw new IllegalStateException("MemberPrincipal '" + username + "' has no authorities assigned");
        }
        return authorities.getFirst().getAuthority();
    }


}
