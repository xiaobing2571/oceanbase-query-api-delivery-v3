package com.oceanbase.query.api.controller;

import com.oceanbase.query.api.dto.ApiResponse;
import com.oceanbase.query.api.dto.DatasourceConfigDto;
import com.oceanbase.query.api.dto.DatasourceTestRequest;
import com.oceanbase.query.api.service.DatasourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/query/v1/datasource")
@Tag(name = "Datasource Management", description = "APIs for managing and testing datasources")
@Slf4j
public class DatasourceController {

    @Autowired
    private DatasourceService datasourceService;

    @PostMapping("/test")
    @Operation(summary = "Test Datasource Connection", description = "Tests the connectivity to a given datasource configuration.")
    public ResponseEntity<ApiResponse<Object>> testDatasourceConnection(@Valid @RequestBody DatasourceTestRequest request) {
        log.info("Received request to test datasource connection: {}", request.getDatasourceType());
        ApiResponse<Object> response = datasourceService.testDatasourceConnection(request);
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            // Consider mapping error codes to HTTP status codes if appropriate
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/register")
    @Operation(summary = "Register Datasource", description = "Registers a new datasource configuration.")
    public ResponseEntity<ApiResponse<DatasourceConfigDto>> registerDatasource(@Valid @RequestBody DatasourceConfigDto request) {
        ApiResponse<DatasourceConfigDto> response = datasourceService.registerDatasource(request);
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{datasourceId}")
    @Operation(summary = "Get Datasource Configuration", description = "Retrieves a specific datasource configuration by ID.")
    public ResponseEntity<ApiResponse<DatasourceConfigDto>> getDatasource(@PathVariable String datasourceId) {
        ApiResponse<DatasourceConfigDto> response = datasourceService.getDatasource(datasourceId);
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{datasourceId}")
    @Operation(summary = "Delete Datasource Configuration", description = "Deletes a specific datasource configuration by ID.")
    public ResponseEntity<ApiResponse<Void>> deleteDatasource(@PathVariable String datasourceId) {
        ApiResponse<Void> response = datasourceService.deleteDatasource(datasourceId);
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping
    @Operation(summary = "List Datasource Configurations", description = "Lists all registered datasource configurations.")
    public ResponseEntity<ApiResponse<List<DatasourceConfigDto>>> listDatasources() {
        ApiResponse<List<DatasourceConfigDto>> response = datasourceService.listDatasources();
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/list")
    @Operation(summary = "List Datasource Configurations (Legacy)", description = "Lists all registered datasource configurations (Legacy endpoint).")
    public ResponseEntity<ApiResponse<List<DatasourceConfigDto>>> listDatasourcesLegacy() {
        // 调用相同的服务方法，保持向后兼容
        return listDatasources();
    }
}
