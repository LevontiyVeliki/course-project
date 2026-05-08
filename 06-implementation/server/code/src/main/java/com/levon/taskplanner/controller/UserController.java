package com.levon.taskplanner.controller;

import com.levon.taskplanner.dto.UserProfileDto;
import com.levon.taskplanner.entity.User;
import com.levon.taskplanner.security.UserDetailsImpl;
import com.levon.taskplanner.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /** Обновить полное имя текущего пользователя. */
    @PatchMapping("/me")
    public ResponseEntity<?> updateMyProfile(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String fullName = body.get("fullName");
        userService.updateFullName(userDetails.getId(), fullName);
        return ResponseEntity.ok("updated");
    }

    /** Сменить пароль текущего пользователя. */
    @PatchMapping("/me/password")
    public ResponseEntity<?> changeMyPassword(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.trim().length() < 6) {
            return ResponseEntity.badRequest().body("Пароль должен быть не менее 6 символов");
        }
        userService.changePassword(userDetails.getId(), newPassword.trim());
        return ResponseEntity.ok("password updated");
    }

    /** Текущий авторизованный пользователь — профиль без пароля. */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMyProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(userService.getProfile(userDetails.getId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted");
    }
}