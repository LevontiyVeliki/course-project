package com.levon.taskplanner.service;

import com.levon.taskplanner.entity.TaskList;
import com.levon.taskplanner.entity.User;
import com.levon.taskplanner.repository.TaskListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskListService {

    @Autowired
    private TaskListRepository taskListRepository;

    public List<TaskList> getTaskListsByUser(User user) {
        return taskListRepository.findByUser(user);
    }

    public TaskList getTaskListById(Long id, User user) {
        TaskList taskList = taskListRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TaskList not found"));
        if (!taskList.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        return taskList;
    }

    public TaskList createTaskList(TaskList taskList) {
        return taskListRepository.save(taskList);
    }

    public TaskList updateTaskList(Long id, TaskList updatedTaskList, User user) {
        TaskList existing = getTaskListById(id, user);
        existing.setName(updatedTaskList.getName());
        existing.setTargetDate(updatedTaskList.getTargetDate());
        existing.setStatus(updatedTaskList.getStatus());
        return taskListRepository.save(existing);
    }

    public void deleteTaskList(Long id, User user) {
        TaskList taskList = getTaskListById(id, user);
        taskListRepository.delete(taskList);
    }
}