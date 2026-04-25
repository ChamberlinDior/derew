package com.oviro;

import com.oviro.dto.request.RegisterRequest;
import com.oviro.dto.response.UserResponse;
import com.oviro.enums.Role;
import com.oviro.exception.BusinessException;
import com.oviro.model.User;
import com.oviro.repository.UserRepository;
import com.oviro.repository.WalletRepository;
import com.oviro.security.JwtUtil;
import com.oviro.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests AuthService")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock WalletRepository walletRepository;
    @Mock com.oviro.repository.SessionTokenRepository sessionTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks AuthService authService;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setFirstName("Jean");
        validRequest.setLastName("Dupont");
        validRequest.setPhoneNumber("+24177123456");
        validRequest.setPassword("Test@1234");
        validRequest.setRole(Role.CLIENT);
    }

    @Test
    @DisplayName("Inscription réussie d'un nouveau client")
    void register_success() {
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return u;
        });

        UserResponse result = authService.register(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Jean");
        assertThat(result.getRole()).isEqualTo(Role.CLIENT);
        verify(walletRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Inscription échoue si numéro déjà utilisé")
    void register_fails_when_phone_exists() {
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("téléphone");
    }
}
