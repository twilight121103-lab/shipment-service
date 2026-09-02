package com.example.logistics.infrastructure.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extracts granted authorities from a Keycloak-issued JWT.
 *
 * <p>Handles both a top-level {@code roles} claim and the nested
 * {@code realm_access.roles} structure, mapping each role to a
 * {@code ROLE_<NAME>} authority. The default {@code SCOPE_*} authorities are retained.
 */
public class JwtRoleAuthorityExtractor implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        final List<GrantedAuthority> authorities = new ArrayList<>(defaultConverter.convert(jwt));

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) {
            Object ra = jwt.getClaim("realm_access");
            if (ra instanceof java.util.Map<?, ?> map && map.get("roles") instanceof List<?> list) {
                roles = list.stream().map(String::valueOf).collect(Collectors.toList());
            }
        }
        if (roles != null) {
            for (String role : roles) {
                if (!role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                }
                authorities.add(new SimpleGrantedAuthority(role));
            }
        }
        // Ensure the specialised logistics roles are always normalised to ROLE_* names
        // even if the token carries them as plain strings.
        return authorities.stream()
                .map(a -> new SimpleGrantedAuthority(a.getAuthority().startsWith("ROLE_")
                        ? a.getAuthority() : "ROLE_" + a.getAuthority()))
                .distinct()
                .collect(Collectors.toList());
    }
}
