package com.levon.taskplanner.controller;

import com.levon.taskplanner.dto.LoginRequest;
import com.levon.taskplanner.dto.RegisterRequest;
import com.levon.taskplanner.dto.JwtResponse;
import com.levon.taskplanner.service.AuthService;
import com.levon.taskplanner.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.registerUser(registerRequest);
        return ResponseEntity.ok("User registered successfully");
    }

    /**
     * DEV-ONLY: сброс пароля без аутентификации.
     * Тело: {"username": "...", "newPassword": "..."}
     * Удали этот эндпоинт перед выпуском в продакшн!
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String username    = body.get("username");
        String newPassword = body.get("newPassword");
        if (username == null || newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("Укажи username и newPassword (мин. 6 символов)");
        }
        try {
            Long userId = userService.getUserByUsername(username).getId();
            userService.changePassword(userId, newPassword);
            return ResponseEntity.ok("Пароль сброшен");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Пользователь не найден: " + username);
        }
    }
}