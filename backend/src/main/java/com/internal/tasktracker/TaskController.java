package com.internal.tasktracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping("/api/tasks")
    public ResponseEntity<?> searchTasks(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {

        // Normalize query input
        String query = q == null ? "" : q.trim();
        String searchTerm = "%" + query.toLowerCase() + "%";

        // Parse status filter
        String normalizedStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                normalizedStatus = TaskStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)).name();
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid status '" + status + "'. Allowed values: OPEN, IN_PROGRESS, DONE"));
            }
        }

        if (page < 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "page must be 1 or greater"));
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "pageSize must be between 1 and " + MAX_PAGE_SIZE));
        }

        log.info("searchTasks q=\"{}\" status={} page={} pageSize={}",
                query, normalizedStatus, page, pageSize);

        List<Task> allResults = taskRepository.searchTasks(searchTerm, normalizedStatus);

        long start = (long) (page - 1) * pageSize;
        int end = (int) Math.min(start + pageSize, allResults.size());
        List<Task> pageResults = (start < allResults.size())
                ? allResults.subList((int) start, end)
                : Collections.emptyList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", pageResults);
        response.put("total", allResults.size());
        response.put("page", page);
        response.put("pageSize", pageSize);

        return ResponseEntity.ok(response);
    }
}
