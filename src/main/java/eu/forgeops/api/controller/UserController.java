package eu.forgeops.api.controller;

import eu.forgeops.api.dto.PageResponse;
import eu.forgeops.domain.user.AppUser;
import eu.forgeops.domain.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    public record UserResponse(UUID id, String username, String email, String fullName,
                               boolean active, boolean mustChangePassword, Set<String> roles,
                               OffsetDateTime createdAt) {
        static UserResponse from(AppUser u) {
            return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(),
                u.isActive(), u.isMustChangePassword(), u.getRoles(), u.getCreatedAt());
        }
    }

    public record CreateUserRequest(@NotBlank String username, @Email @NotBlank String email,
                                    @NotBlank String password, String fullName, Set<String> roles) {}

    public record UpdateUserRequest(String fullName, Set<String> roles, Boolean active) {}

    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {}

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        var user = userService.createUser(req.username(), req.email(), req.password(), req.fullName(), req.roles());
        return ResponseEntity
            .created(URI.create("/api/v1/users/" + user.getId()))
            .body(UserResponse.from(user));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("username").ascending());
        return PageResponse.from(userService.list(pageable).map(UserResponse::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #id.toString()")
    public UserResponse get(@PathVariable UUID id) {
        return UserResponse.from(userService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable UUID id, @RequestBody UpdateUserRequest req) {
        return UserResponse.from(userService.updateUser(id, req.fullName(), req.roles(), req.active()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/change-password")
    @PreAuthorize("authentication.name == #id.toString()")
    public ResponseEntity<Void> changePassword(
        @PathVariable UUID id,
        @Valid @RequestBody ChangePasswordRequest req,
        @AuthenticationPrincipal UserDetails principal
    ) {
        userService.changePassword(id, req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id, @RequestBody java.util.Map<String, String> body) {
        userService.resetPassword(id, body.get("newPassword"));
        return ResponseEntity.noContent().build();
    }
}
