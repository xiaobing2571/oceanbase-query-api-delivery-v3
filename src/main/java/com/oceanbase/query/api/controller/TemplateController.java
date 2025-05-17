package com.oceanbase.query.api.controller;

import com.oceanbase.query.api.dto.ApiResponse;
import com.oceanbase.query.api.dto.SqlTemplateDto;
import com.oceanbase.query.api.dto.TemplateRegisterRequest;
import com.oceanbase.query.api.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/query/v1/templates")
@Tag(name = "SQL Template Management", description = "APIs for managing SQL templates")
@Slf4j
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    @PostMapping("/register")
    @Operation(summary = "Register SQL Template", description = "Registers a new SQL template with its versions.")
    public ResponseEntity<ApiResponse<SqlTemplateDto>> registerTemplate(@Valid @RequestBody TemplateRegisterRequest request) {
        log.info("Received request to register SQL template: {}", request.getTemplateId());
        ApiResponse<SqlTemplateDto> response = templateService.registerTemplate(request);
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/{templateId}")
    @Operation(summary = "Get SQL Template", description = "Retrieves a specific SQL template by ID.")
    public ResponseEntity<ApiResponse<SqlTemplateDto>> getTemplate(@PathVariable String templateId) {
        ApiResponse<SqlTemplateDto> response = templateService.getTemplate(templateId);
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{templateId}")
    @Operation(summary = "Delete SQL Template", description = "Deletes a specific SQL template by ID.")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable String templateId) {
        ApiResponse<Void> response = templateService.deleteTemplate(templateId);
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/list")
    @Operation(summary = "List SQL Templates", description = "Lists all SQL templates.")
    public ResponseEntity<ApiResponse<List<SqlTemplateDto>>> listTemplates() {
        ApiResponse<List<SqlTemplateDto>> response = templateService.listTemplates();
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/scene/{sceneCode}")
    @Operation(summary = "List SQL Templates by Scene", description = "Lists all SQL templates associated with a specific scene code.")
    public ResponseEntity<ApiResponse<List<SqlTemplateDto>>> listTemplatesByScene(@PathVariable String sceneCode) {
        ApiResponse<List<SqlTemplateDto>> response = templateService.listTemplatesByScene(sceneCode);
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PutMapping("/{templateId}")
    @Operation(summary = "Update SQL Template", description = "Updates an existing SQL template.")
    public ResponseEntity<ApiResponse<SqlTemplateDto>> updateTemplate(@PathVariable String templateId, @Valid @RequestBody TemplateRegisterRequest request) {
        ApiResponse<SqlTemplateDto> response = templateService.updateTemplate(templateId, request);
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
