package com.projects.distributed_lovable.common_lib.security;

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record JwtUserPrincipal(
                Long userId,
                String name,
                String username,
                String password,
                List<GrantedAuthority> authorities) implements UserDetails {

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of();
        }

        @Override
        public @Nullable String getPassword() {
                return password;
        }

        @Override
        public String getUsername() {
                return username;
        }
}
