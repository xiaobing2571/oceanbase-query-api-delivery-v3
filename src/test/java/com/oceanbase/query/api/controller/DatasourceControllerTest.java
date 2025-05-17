package com.oceanbase.query.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.query.api.dto.ApiResponse;
import com.oceanbase.query.api.dto.DatasourceConfigDto;
import com.oceanbase.query.api.dto.DatasourceTestRequest;
import com.oceanbase.query.api.service.DatasourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DatasourceController.class)
public class DatasourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatasourceService datasourceService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testDatasourceConnection_Success() throws Exception {
        DatasourceTestRequest request = new DatasourceTestRequest();
        request.setDatasourceType("direct");
        request.setDatasourceConfig(new DatasourceConfigDto("jdbc:oceanbase://localhost:2881/test", "user", "pass"));

        ApiResponse<Object> serviceResponse = ApiResponse.success("数据源连接成功");
        when(datasourceService.testDatasourceConnection(any(DatasourceTestRequest.class))).thenReturn(serviceResponse);

        mockMvc.perform(post("/api/query/v1/datasource/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").value("数据源连接成功"));
    }

    @Test
    void testDatasourceConnection_Failure() throws Exception {
        DatasourceTestRequest request = new DatasourceTestRequest();
        request.setDatasourceType("direct");
        // Missing config to simulate a failure handled by service

        ApiResponse<Object> serviceResponse = ApiResponse.error("10003", "直接连接时，datasourceConfig 不能为空");
        when(datasourceService.testDatasourceConnection(any(DatasourceTestRequest.class))).thenReturn(serviceResponse);

        mockMvc.perform(post("/api/query/v1/datasource/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // Expecting bad request due to error response
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.errorCode").value("10003"))
                .andExpect(jsonPath("$.message").value("直接连接时，datasourceConfig 不能为空"));
    }
    
    @Test
    void testDatasourceConnection_InvalidRequest() throws Exception {
        // Send an empty body, which should trigger validation if @Valid is effective
        // Or, send a request that doesn't conform to DatasourceTestRequest structure
        // For this test, we'll send a valid structure but expect the service to handle logic errors
        DatasourceTestRequest request = new DatasourceTestRequest(); // Empty request
        // request.setDatasourceType(null); // This would be a validation error if @NotNull is on the field

        // Simulate a validation error or a generic error from the service if validation is not strict at controller for all fields
        ApiResponse<Object> serviceResponse = ApiResponse.error("VALIDATION_ERROR", "Invalid input");
        when(datasourceService.testDatasourceConnection(any(DatasourceTestRequest.class))).thenReturn(serviceResponse);

        mockMvc.perform(post("/api/query/v1/datasource/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}

