package com.example.springbootskillvalidation.task;

import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("title", "createdAt", "updatedAt", "completed");

    private final TaskRepository taskRepository;
    private final Clock clock;
    private final TaskApiProperties taskApiProperties;

    TaskService(TaskRepository taskRepository, Clock clock, TaskApiProperties taskApiProperties) {
        this.taskRepository = taskRepository;
        this.clock = clock;
        this.taskApiProperties = taskApiProperties;
    }

    @Transactional
    TaskResponse create(CreateTaskRequest request) {
        var task = Task.create(request.title().trim(), normalizeDescription(request.description()), clock.instant());
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    Page<TaskResponse> findAll(Boolean completed, String query, Pageable pageable) {
        Specification<Task> spec = (root, criteriaQuery, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (completed != null) predicates.add(builder.equal(root.get("completed"), completed));
            if (query != null && !query.isBlank()) {
                String escapedQuery = query.trim()
                        .toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_");
                predicates.add(builder.like(builder.lower(root.get("title")), "%" + escapedQuery + "%", '\\'));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        var sort = pageable.getSort().isUnsorted() ? Sort.by(Sort.Order.desc("createdAt")) : pageable.getSort();
        if (sort.stream().anyMatch(order -> !SORTABLE_FIELDS.contains(order.getProperty()))) {
            throw new InvalidTaskSortException();
        }
        var safePageable = PageRequest.of(
                pageable.getPageNumber(), Math.min(pageable.getPageSize(), taskApiProperties.maxPageSize()), sort);
        return taskRepository.findAll(spec, safePageable).map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    TaskResponse findById(UUID id) {
        return TaskResponse.from(getTask(id));
    }

    @Transactional
    TaskResponse update(UUID id, UpdateTaskRequest request) {
        var task = getTask(id);
        if (task.version() != request.version()) throw new TaskVersionConflictException();
        task.update(
                request.title().trim(),
                normalizeDescription(request.description()),
                request.completed(),
                clock.instant());
        try {
            return TaskResponse.from(taskRepository.saveAndFlush(task));
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new TaskVersionConflictException();
        }
    }

    @Transactional
    void delete(UUID id) {
        taskRepository.delete(getTask(id));
    }

    private Task getTask(UUID id) {
        return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }
}
