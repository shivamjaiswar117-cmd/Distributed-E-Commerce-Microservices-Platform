package com.distributed.ecommerce.user.service;

import com.distributed.ecommerce.user.entity.User;
import com.distributed.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("test@example.com", "hashedPassword123", "Test User");
        sampleUser.setId(1L);
    }

    @Test
    void registerUser_shouldHashPasswordBeforeSaving() {
        User rawUser = new User("new@example.com", "plainPassword", "New User");

        when(passwordEncoder.encode("plainPassword")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = userService.registerUser(rawUser);

        assertEquals("hashedPassword123", savedUser.getPassword());
        verify(passwordEncoder, times(1)).encode("plainPassword");
        verify(userRepository, times(1)).save(rawUser);
    }

    @Test
    void login_shouldReturnTrue_whenCredentialsAreValid() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("correctPassword", "hashedPassword123")).thenReturn(true);

        boolean result = userService.login("test@example.com", "correctPassword");

        assertTrue(result);
    }

    @Test
    void login_shouldReturnFalse_whenPasswordIsIncorrect() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword123")).thenReturn(false);

        boolean result = userService.login("test@example.com", "wrongPassword");

        assertFalse(result);
    }

    @Test
    void login_shouldReturnFalse_whenUserDoesNotExist() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        boolean result = userService.login("nonexistent@example.com", "anyPassword");

        assertFalse(result);
    }
}