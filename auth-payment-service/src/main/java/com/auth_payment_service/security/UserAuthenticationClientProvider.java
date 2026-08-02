package com.auth_payment_service.security;

import com.auth_payment_service.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
    public  class UserAuthenticationClientProvider implements AuthenticationProvider {
        private final UserDetailsServiceImpl securityUserService;
        private final PasswordEncoder encoder;

    @Override
    public Authentication authenticate(Authentication authentication) {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        User user = securityUserService.loadUserByUsername(username);

        if (user == null || !encoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid username/password");
        }

        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> auth) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(auth);
    }


    }