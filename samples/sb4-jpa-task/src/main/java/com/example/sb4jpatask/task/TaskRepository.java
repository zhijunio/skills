package com.example.sb4jpatask.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface TaskRepository extends JpaRepository<Task, UUID> {
}
