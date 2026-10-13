package com.example.sb4jpatask.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface TaskStatusHistoryRepository extends JpaRepository<TaskStatusHistory, Long> {

    List<TaskStatusHistory> findAllByTaskIdOrderByIdAsc(UUID taskId);
}
