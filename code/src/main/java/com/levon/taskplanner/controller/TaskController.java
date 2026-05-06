package com.levon.taskplanner.controller;

import com.levon.taskplanner.entity.Task;
import com.levon.taskplanner.entity.User;
import com.levon.taskplanner.security.UserDetailsImpl;
import com.levon.taskplanner.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping("/tasklist/{taskListId}")
    public List<Task> getTasksByTaskList(@PathVariable Long taskListId,
                                         @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = User.builder().id(userDetails.getId()).build();
        return taskService.getTasksByTaskList(taskListId, user);
    }

    @GetMapping("/{id}")
    public Task getTask(@PathVariable Long id,
                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = User.builder().id(userDetails.getId()).build();
        return taskService.getTaskById(id, user);
    }

    @PostMapping("/tasklist/{taskListId}")
    public ResponseEntity<Task> createTask(@RequestBody Task task,
                                           @PathVariable Long taskListId,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = User.builder().id(userDetails.getId()).build();
        Task created = taskService.createTask(task, taskListId, user);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public Task updateTask(@PathVariable Long id,
                           @RequestBody Task task,
                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = User.builder().id(userDetails.getId()).build();
        return taskService.updateTask(id, task, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id,
                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = User.builder().id(userDetails.getId()).build();
        taskService.deleteTask(id, user);
        return ResponseEntity.ok("Task deleted");
    }
}