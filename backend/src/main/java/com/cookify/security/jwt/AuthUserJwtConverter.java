package com.cookify.security.jwt;

import com.cookify.user.entity.Role;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/** Turns a verified JWT into an authentication whose principal is {@link AuthUser} and authority is ROLE_&lt;role&gt;. */
@Component
public class AuthUserJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Role role = Role.valueOf(jwt.getClaimAsString(JwtService.CLAIM_ROLE));
        AuthUser user = new AuthUser(Long.valueOf(jwt.getSubject()), jwt.getClaimAsString(JwtService.CLAIM_USERNAME), role);
        return new UsernamePasswordAuthenticationToken(user, jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }
}
