package com.oceanbase.query.api.controller;

import com.oceanbase.query.api.dto.ApiResponse;
import com.oceanbase.query.api.dto.QueryExecuteRequest;
import com.oceanbase.query.api.dto.QueryResultDto;
import com.oceanbase.query.api.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/query/v1")
@Tag(name = "Query Execution", description = "APIs for executing queries and retrieving results")
@Slf4j
public class QueryController {

    @Autowired
    private QueryService queryService;

    @PostMapping("/scenes/{scene_code}/execute")
    @Operation(summary = "Execute Query Scene",
               description = "Executes all SQL templates associated with a given scene code. Supports synchronous and asynchronous execution.")
    public ResponseEntity<ApiResponse<List<QueryResultDto>>> executeQuery(
            @Parameter(description = "Code of the query scene to execute", required = true) @PathVariable("scene_code") String sceneCode,
            @Valid @RequestBody QueryExecuteRequest request) {
        log.info("Received request to execute query for scene: {}, async: {}", sceneCode, request.getAsync());
        ApiResponse<List<QueryResultDto>> response = queryService.executeQuery(sceneCode, request);
        
        if ("success".equals(response.getStatus())) {
            if (Boolean.TRUE.equals(request.getAsync())) {
                // For async, 202 Accepted might be more appropriate, but APIResponse structure handles it
                return ResponseEntity.ok(response); 
            }
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/result/{task_id}")
    @Operation(summary = "Get Asynchronous Task Result",
               description = "Retrieves the result of an asynchronously executed query task by its task ID.")
    public ResponseEntity<ApiResponse<List<QueryResultDto>>> getAsyncTaskResult(
            @Parameter(description = "ID of the asynchronous task", required = true) @PathVariable("task_id") String taskId) {
        log.info("Received request to get result for async task: {}", taskId);
        ApiResponse<List<QueryResultDto>> response = queryService.getAsyncTaskResult(taskId);
        
        // Check the status within the ApiResponse to determine HTTP status
        if (response.getStatus() != null && (response.getStatus().equalsIgnoreCase("pending") || response.getStatus().equalsIgnoreCase("running"))) {
            // Task is still processing, could return 200 with status or 202 Accepted.
            // Sticking to 200 as the APIResponse clearly indicates the status.
            return ResponseEntity.ok(response);
        } else if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else { // Covers "failed" or other error statuses
            return ResponseEntity.status(400).body(response); // Or a more specific error code like 404 if task not found and that's how it's reported
        }
    }
}

