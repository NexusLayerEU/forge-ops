package eu.forgeops.api.controller;

import eu.forgeops.domain.user.AppUser;
import eu.forgeops.infra.persistence.UserRepository;
import eu.forgeops.infra.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        boolean mustChangePassword
    ) {}

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        try {
            var auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
            );

            // auth.getName() is the userId (set by ForgeUserDetailsService)
            UUID userId = UUID.fromString(auth.getName());
            AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found"));

            if (!user.isActive()) {
                throw new ResponseStatusException(UNAUTHORIZED, "Account disabled");
            }

            String accessToken = jwtService.generateAccessToken(userId, user.getUsername(), user.getRoles());
            String refreshToken = jwtService.generateRefreshToken(userId);

            return ResponseEntity.ok(new TokenResponse(
                accessToken, refreshToken, "Bearer", 900L, user.isMustChangePassword()
            ));
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Missing refreshToken");
        }

        try {
            Claims claims = jwtService.parseToken(refreshToken);
            if (!jwtService.isRefreshToken(claims)) {
                throw new ResponseStatusException(UNAUTHORIZED, "Not a refresh token");
            }

            UUID userId = UUID.fromString(claims.getSubject());
            AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found"));

            String newAccess = jwtService.generateAccessToken(userId, user.getUsername(), user.getRoles());
            return ResponseEntity.ok(Map.of("accessToken", newAccess, "tokenType", "Bearer"));
        } catch (JwtException e) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid refresh token");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Stateless JWT: client discards the token
        return ResponseEntity.noContent().build();
    }
}
