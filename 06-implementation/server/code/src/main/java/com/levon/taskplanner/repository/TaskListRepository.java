package com.levon.taskplanner.repository;

import com.levon.taskplanner.entity.TaskList;
import com.levon.taskplanner.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskListRepository extends JpaRepository<TaskList, Long> {
    List<TaskList> findByUser(User user);
    long countByUser(User user);
}