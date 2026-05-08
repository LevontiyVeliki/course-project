package com.levon.taskplanner.service;

import com.levon.taskplanner.dto.UserProfileDto;
import com.levon.taskplanner.entity.User;
import com.levon.taskplanner.repository.TaskListRepository;
import com.levon.taskplanner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskListRepository taskListRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);
        user.setFullName(userDetails.getFullName());
        // update other fields as needed (except password/role)
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public void updateFullName(Long userId, String fullName) {
        String cleanName = fullName == null ? "" : fullName.trim();
        // Используем JPQL-запрос: обновляем ТОЛЬКО fullName,
        // не загружаем сущность в JPA-кэш → passwordHash не затрагивается
        userRepository.updateFullNameById(userId, cleanName);
    }

    public void changePassword(Long userId, String newPassword) {
        String hash = passwordEncoder.encode(newPassword);
        userRepository.updatePasswordById(userId, hash);
    }

    /** Возвращает профиль пользователя без пароля и без ленивых коллекций. */
    public UserProfileDto getProfile(Long userId) {
        User user = getUserById(userId);
        String formattedDate = user.getCreatedAt() != null
                ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                : "Неизвестно";
        long taskCount = taskListRepository.countByUser(user);
        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                formattedDate,
                taskCount,
                user.getRole().name()
        );
    }
}