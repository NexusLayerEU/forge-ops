package eu.forgeops.domain.user;

import eu.forgeops.infra.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AppUser createUser(String username, String email, String password, String fullName, Set<String> roles) {
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(CONFLICT, "Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(CONFLICT, "Email already in use: " + email);
        }
        AppUser user = AppUser.builder()
            .username(username)
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .fullName(fullName)
            .roles(roles != null ? roles : Set.of("VIEWER"))
            .isActive(true)
            .mustChangePassword(true)
            .build();
        return userRepository.save(user);
    }

    @Transactional
    public AppUser updateUser(UUID id, String fullName, Set<String> roles, Boolean active) {
        AppUser user = getById(id);
        if (fullName != null) user.setFullName(fullName);
        if (roles != null) user.setRoles(roles);
        if (active != null) user.setActive(active);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(UUID id, String oldPassword, String newPassword) {
        AppUser user = getById(id);
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(UUID id, String newPassword) {
        AppUser user = getById(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public AppUser getById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<AppUser> list(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
}
