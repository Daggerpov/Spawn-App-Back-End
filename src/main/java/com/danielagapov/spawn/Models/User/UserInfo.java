package com.danielagapov.spawn.Models.User;

import com.danielagapov.spawn.Enums.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * This class implements the UserDetails interface which is used by Spring Security to authenticate
 * incoming requests. It is populated with the username and password from a User entity.
 * For JWTs and in our application, only getPassword and getUsername are needed
 */
public class UserInfo implements UserDetails {
    private final String username;
    private final String password;
    private final UserStatus status;

    public UserInfo(String username, String password, UserStatus status) {
        this.username = username;
        this.password = password;
        this.status = status;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = status == UserStatus.ACTIVE ? "ROLE_ACTIVE" : "ROLE_ONBOARDING";

        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
