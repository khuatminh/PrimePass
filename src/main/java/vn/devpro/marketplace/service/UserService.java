package vn.devpro.marketplace.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.devpro.marketplace.dto.UserDto;
import vn.devpro.marketplace.entity.User;
import vn.devpro.marketplace.repository.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(UserDto dto) {
        if (userRepository.findByUsernameOrEmail(dto.getUsername(), dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Username or email already exists");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setRole(User.UserRole.customer);
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsernameOrEmail(username, username);
    }
}
