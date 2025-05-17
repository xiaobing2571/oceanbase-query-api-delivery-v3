package com.oceanbase.query.api.controller;

import com.oceanbase.query.api.dto.ApiResponse;
import com.oceanbase.query.api.dto.SceneDto;
import com.oceanbase.query.api.dto.SceneRegisterRequest;
import com.oceanbase.query.api.service.SceneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/query/v1/scenes")
@Tag(name = "Query Scene Management", description = "APIs for managing query scenes")
@Slf4j
public class SceneController {

    @Autowired
    private SceneService sceneService;

    @PostMapping("/register")
    @Operation(summary = "Register Query Scene", description = "Registers a new query scene and associates it with SQL templates.")
    public ResponseEntity<ApiResponse<SceneDto>> registerScene(@Valid @RequestBody SceneRegisterRequest request) {
        log.info("Received request to register query scene: {}", request.getSceneCode());
        ApiResponse<SceneDto> response = sceneService.registerScene(request);
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/{sceneCode}")
    @Operation(summary = "Get Query Scene", description = "Retrieves a specific query scene by code.")
    public ResponseEntity<ApiResponse<SceneDto>> getScene(@PathVariable String sceneCode) {
        ApiResponse<SceneDto> response = sceneService.getScene(sceneCode);
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{sceneCode}")
    @Operation(summary = "Delete Query Scene", description = "Deletes a specific query scene by code.")
    public ResponseEntity<ApiResponse<Void>> deleteScene(@PathVariable String sceneCode) {
        ApiResponse<Void> response = sceneService.deleteScene(sceneCode);
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/list")
    @Operation(summary = "List Query Scenes", description = "Lists all registered query scenes.")
    public ResponseEntity<ApiResponse<List<SceneDto>>> listScenes() {
        ApiResponse<List<SceneDto>> response = sceneService.listScenes();
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PutMapping("/{sceneCode}")
    @Operation(summary = "Update Query Scene", description = "Updates an existing query scene.")
    public ResponseEntity<ApiResponse<SceneDto>> updateScene(@PathVariable String sceneCode, @Valid @RequestBody SceneRegisterRequest request) {
        ApiResponse<SceneDto> response = sceneService.updateScene(sceneCode, request);
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
