package com.levon.taskplanner.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long   id;
    private String username;
    private String email;
    private String fullName;      // может быть null
    private String createdAt;     // "dd.MM.yyyy HH:mm"
    private long   taskListsCount;
    private String role;          // "USER" или "ADMIN"
}
