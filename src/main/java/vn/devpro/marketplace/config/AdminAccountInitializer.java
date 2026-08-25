package vn.devpro.marketplace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.devpro.marketplace.entity.User;
import vn.devpro.marketplace.repository.UserRepository;

import java.util.Objects;
import java.util.Optional;

@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String email;
    private final String password;

    public AdminAccountInitializer(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        @Value("${ADMIN_USERNAME:}") String username,
        @Value("${ADMIN_EMAIL:}") String email,
        @Value("${ADMIN_PASSWORD:}") String password
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean hasUsername = StringUtils.hasText(username);
        boolean hasEmail = StringUtils.hasText(email);
        boolean hasPassword = StringUtils.hasText(password);

        if (!hasUsername && !hasEmail && !hasPassword) {
            return;
        }
        if (!hasUsername || !hasEmail || !hasPassword) {
            throw new IllegalStateException(
                "ADMIN_USERNAME, ADMIN_EMAIL, and ADMIN_PASSWORD must be configured together");
        }

        Optional<User> usernameOwner = userRepository.findByUsername(username);
        Optional<User> emailOwner = userRepository.findByEmail(email);

        if (usernameOwner.isEmpty()) {
            if (emailOwner.isPresent()) {
                throw new IllegalStateException("Configured admin email is already in use");
            }
            createAdmin();
            return;
        }

        User admin = usernameOwner.get();
        if (admin.getRole() != User.UserRole.admin) {
            throw new IllegalStateException("Configured admin username belongs to a non-admin user");
        }
        if (emailOwner.isPresent() && !Objects.equals(emailOwner.get().getId(), admin.getId())) {
            throw new IllegalStateException("Configured admin email is already in use");
        }

        boolean changed = false;
        if (!email.equals(admin.getEmail())) {
            admin.setEmail(email);
            changed = true;
        }
        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            admin.setPasswordHash(passwordEncoder.encode(password));
            changed = true;
        }
        if (changed) {
            userRepository.save(admin);
        }
    }

    private void createAdmin() {
        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setFullName("Administrator");
        admin.setRole(User.UserRole.admin);
        userRepository.save(admin);
    }
}
