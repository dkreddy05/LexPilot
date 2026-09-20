package com.lexpilot.gateway.controller;

import com.lexpilot.gateway.security.JwtService;
import com.lexpilot.gateway.security.entity.UserAccountEntity;
import com.lexpilot.gateway.security.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserAccountRepository userAccountRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        Optional<UserAccountEntity> userOpt = userAccountRepository.findByEmail(email);

        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }

        UserAccountEntity user = userOpt.get();
        String token = jwtService.generateToken(user.getId().toString(), user.getTenantId(), user.getRole());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "id", user.getId().toString(),
                "email", user.getEmail(),
                "name", user.getName() != null ? user.getName() : user.getEmail(),
                "role", user.getRole(),
                "tenantId", user.getTenantId().toString()
        ));
    }
}
