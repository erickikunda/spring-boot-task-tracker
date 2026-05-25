package com.example.taskflow.security;

import com.example.taskflow.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// Wraps the domain User for Spring Security — keeps security concerns out of the JPA entity.
public record UserPrincipal(User user) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() { return user.getPasswordHash(); }

    // getUsername() is the identity key Spring Security uses for authentication.name in SpEL.
    @Override
    public String getUsername() { return user.getEmail(); }
}
