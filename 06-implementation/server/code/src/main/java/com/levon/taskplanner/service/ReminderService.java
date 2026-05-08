package com.levon.taskplanner.service;

import com.levon.taskplanner.entity.Reminder;
import com.levon.taskplanner.entity.Task;
import com.levon.taskplanner.entity.TaskList;
import com.levon.taskplanner.entity.User;
import com.levon.taskplanner.repository.ReminderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReminderService {

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskListService taskListService;

    public Reminder createReminderForTask(Reminder reminder, Long taskId, User user) {
        Task task = taskService.getTaskById(taskId, user);
        reminder.setTask(task);
        return reminderRepository.save(reminder);
    }

    public Reminder createReminderForTaskList(Reminder reminder, Long taskListId, User user) {
        TaskList taskList = taskListService.getTaskListById(taskListId, user);
        reminder.setTaskList(taskList);
        return reminderRepository.save(reminder);
    }

    public Reminder updateReminder(Long id, Reminder updated, User user) {
        Reminder existing = reminderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reminder not found"));
        // Check ownership indirectly through task or taskList
        if (existing.getTask() != null && !existing.getTask().getTaskList().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        if (existing.getTaskList() != null && !existing.getTaskList().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        existing.setTriggerTime(updated.getTriggerTime());
        existing.setMessage(updated.getMessage());
        existing.setSent(updated.isSent());
        return reminderRepository.save(existing);
    }

    public void deleteReminder(Long id, User user) {
        Reminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reminder not found"));
        // Access check same as update
        if (reminder.getTask() != null && !reminder.getTask().getTaskList().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        if (reminder.getTaskList() != null && !reminder.getTaskList().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        reminderRepository.delete(reminder);
    }
}