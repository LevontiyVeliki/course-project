package com.levon.taskplanner.service;

import com.levon.taskplanner.entity.Task;
import com.levon.taskplanner.entity.TaskList;
import com.levon.taskplanner.entity.User;
import com.levon.taskplanner.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskListService taskListService;

    public List<Task> getTasksByTaskList(Long taskListId, User user) {
        TaskList taskList = taskListService.getTaskListById(taskListId, user);
        return taskRepository.findByTaskList(taskList);
    }

    public Task getTaskById(Long id, User user) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        // Check that user owns the parent TaskList
        if (!task.getTaskList().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        return task;
    }

    public Task createTask(Task task, Long taskListId, User user) {
        TaskList taskList = taskListService.getTaskListById(taskListId, user);
        task.setTaskList(taskList);
        return taskRepository.save(task);
    }

    public Task updateTask(Long id, Task updatedTask, User user) {
        Task existing = getTaskById(id, user);
        existing.setDescription(updatedTask.getDescription());
        existing.setStatus(updatedTask.getStatus());
        existing.setPriority(updatedTask.getPriority());
        existing.setOrderIndex(updatedTask.getOrderIndex());
        return taskRepository.save(existing);
    }

    public void deleteTask(Long id, User user) {
        Task task = getTaskById(id, user);
        taskRepository.delete(task);
    }
}