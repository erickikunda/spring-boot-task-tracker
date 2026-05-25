package com.example.taskflow.service;

import com.example.taskflow.domain.Role;
import com.example.taskflow.domain.User;
import com.example.taskflow.repository.UserRepository;
import com.example.taskflow.security.JwtService;
import com.example.taskflow.security.UserPrincipal;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserService userService;
    @Mock UserRepository userRepository;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock MeterRegistry meterRegistry;
    @Mock Counter counter;
    @InjectMocks AuthService authService;

    private User alice() {
        return User.builder()
                .id(UUID.randomUUID()).email("alice@example.com")
                .displayName("Alice").passwordHash("h").role(Role.MEMBER).build();
    }

    @Test
    void register_returnsAuthResponse_withTokenAndUser() {
        var user = alice();
        when(userService.create(anyString(), anyString(), anyString(), any())).thenReturn(user);
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("signed.jwt");
        when(meterRegistry.counter("auth.registrations")).thenReturn(counter);

        var result = authService.register("alice@example.com", "Alice", "pass1234", Role.MEMBER);

        assertThat(result.token()).isEqualTo("signed.jwt");
        assertThat(result.user().email()).isEqualTo("alice@example.com");
    }

    @Test
    void login_delegatesToAuthenticationManager_thenReturnsToken() {
        var user = alice();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("signed.jwt");

        var result = authService.login("alice@example.com", "pass1234");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertThat(result.token()).isEqualTo("signed.jwt");
    }

    @Test
    void login_propagatesBadCredentialsException_beforeLoadingUser() {
        doThrow(new BadCredentialsException("bad")).when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login("alice@example.com", "wrong"))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmail(any());
    }
}
