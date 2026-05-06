package com.levon.taskplanner.controller;

import com.levon.taskplanner.entity.TaskList;
import com.levon.taskplanner.entity.User;
import com.levon.taskplanner.security.UserDetailsImpl;
import com.levon.taskplanner.service.TaskListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasklists")
public class TaskListController {

    @Autowired
    private TaskListService taskListService;

    @GetMapping
    public List<TaskList> getUserTaskLists(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = User.builder().id(userDetails.getId()).build();
        return taskListService.getTaskListsByUser(user);
    }

    @GetMapping("/{id}")
    public TaskList getTaskList(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = User.builder().id(userDetails.getId()).build();
        return taskListService.getTaskListById(id, user);
    }

    @PostMapping
    public ResponseEntity<TaskList> createTaskList(@RequestBody TaskList taskList,
                                                   @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = User.builder().id(userDetails.getId()).build();
        taskList.setUser(user);
        TaskList created = taskListService.createTaskList(taskList);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public TaskList updateTaskList(@PathVariable Long id,
                                   @RequestBody TaskList taskList,
                                   @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = User.builder().id(userDetails.getId()).build();
        return taskListService.updateTaskList(id, taskList, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTaskList(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = User.builder().id(userDetails.getId()).build();
        taskListService.deleteTaskList(id, user);
        return ResponseEntity.ok("TaskList deleted");
    }
}