package vn.devpro.marketplace.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.devpro.marketplace.entity.User;
import vn.devpro.marketplace.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void skipsBootstrapWhenNoAdminVariablesAreConfigured() throws Exception {
        initializer("", "", "").run(new DefaultApplicationArguments());

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void rejectsPartiallyConfiguredAdmin() {
        AdminAccountInitializer initializer = initializer("admin", "", "secret");

        assertThrows(IllegalStateException.class,
            () -> initializer.run(new DefaultApplicationArguments()));
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void createsAdminWhenAccountDoesNotExist() throws Exception {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("encoded");

        initializer("admin", "admin@example.com", "secret")
            .run(new DefaultApplicationArguments());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("admin", saved.getUsername());
        assertEquals("admin@example.com", saved.getEmail());
        assertEquals("encoded", saved.getPasswordHash());
        assertEquals(User.UserRole.admin, saved.getRole());
    }

    @Test
    void updatesExistingAdminSeedAccount() throws Exception {
        User existing = User.builder()
            .id(1)
            .username("admin")
            .email("old@example.com")
            .passwordHash("old-hash")
            .role(User.UserRole.admin)
            .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("secret", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded");

        initializer("admin", "admin@example.com", "secret")
            .run(new DefaultApplicationArguments());

        assertEquals("admin@example.com", existing.getEmail());
        assertEquals("encoded", existing.getPasswordHash());
        verify(userRepository).save(existing);
    }

    @Test
    void refusesToPromoteExistingCustomer() {
        User existing = User.builder()
            .id(1)
            .username("admin")
            .email("customer@example.com")
            .passwordHash("hash")
            .role(User.UserRole.customer)
            .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());

        AdminAccountInitializer initializer = initializer(
            "admin", "admin@example.com", "secret");

        assertThrows(IllegalStateException.class,
            () -> initializer.run(new DefaultApplicationArguments()));
        verify(userRepository, never()).save(existing);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void rejectsEmailOwnedByAnotherUser() {
        User emailOwner = User.builder()
            .id(2)
            .username("other")
            .email("admin@example.com")
            .role(User.UserRole.customer)
            .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(emailOwner));

        AdminAccountInitializer initializer = initializer(
            "admin", "admin@example.com", "secret");

        assertThrows(IllegalStateException.class,
            () -> initializer.run(new DefaultApplicationArguments()));
        verify(userRepository, never()).save(emailOwner);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void rejectsEmailOwnedByDifferentUserWhenAdminUsernameExists() {
        User admin = User.builder()
            .id(1)
            .username("admin")
            .email("old@example.com")
            .role(User.UserRole.admin)
            .build();
        User emailOwner = User.builder()
            .id(2)
            .username("other")
            .email("admin@example.com")
            .role(User.UserRole.customer)
            .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(emailOwner));

        AdminAccountInitializer initializer = initializer(
            "admin", "admin@example.com", "secret");

        assertThrows(IllegalStateException.class,
            () -> initializer.run(new DefaultApplicationArguments()));
        verify(userRepository, never()).save(admin);
        verifyNoInteractions(passwordEncoder);
    }

    private AdminAccountInitializer initializer(String username, String email, String password) {
        return new AdminAccountInitializer(
            userRepository, passwordEncoder, username, email, password);
    }
}
