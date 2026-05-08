package com.levon.taskplanner.repository;

import com.levon.taskplanner.entity.Task;
import com.levon.taskplanner.entity.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByTaskList(TaskList taskList);
}