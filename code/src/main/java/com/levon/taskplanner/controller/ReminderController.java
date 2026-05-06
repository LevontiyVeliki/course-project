package com.levon.taskplanner.controller;

import com.levon.taskplanner.entity.Reminder;
import com.levon.taskplanner.entity.User;
import com.levon.taskplanner.security.UserDetailsImpl;
import com.levon.taskplanner.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    @Autowired
    private ReminderService reminderService;

    @PostMapping("/task/{taskId}")
    public ResponseEntity<Reminder> createForTask(@RequestBody Reminder reminder,
                                                  @PathVariable Long taskId,
                                                  @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = User.builder().id(userDetails.getId()).build();
        Reminder created = reminderService.createReminderForTask(reminder, taskId, user);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/tasklist/{taskListId}")
    public ResponseEntity<Reminder> createForTaskList(@RequestBody Reminder reminder,
                                                      @PathVariable Long taskListId,
                                                      @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = User.builder().id(userDetails.getId()).build();
        Reminder created = reminderService.createReminderForTaskList(reminder, taskListId, user);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public Reminder updateReminder(@PathVariable Long id,
                                   @RequestBody Reminder reminder,
                                   @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = User.builder().id(userDetails.getId()).build();
        return reminderService.updateReminder(id, reminder, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReminder(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = User.builder().id(userDetails.getId()).build();
        reminderService.deleteReminder(id, user);
        return ResponseEntity.ok("Reminder deleted");
    }
}