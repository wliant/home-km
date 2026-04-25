package com.homekm.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final long id;
    private final String email;
    private final boolean admin;
    private final boolean child;

    public UserPrincipal(long id, String email, boolean admin, boolean child) {
        this.id = id;
        this.email = email;
        this.admin = admin;
        this.child = child;
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.isAdmin(), user.isChild());
    }

    public long getId() { return id; }
    public boolean isAdmin() { return admin; }
    public boolean isChild() { return child; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (admin) return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"));
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return null; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
