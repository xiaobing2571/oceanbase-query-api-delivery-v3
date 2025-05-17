package com.oceanbase.query.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.query.api.dto.ApiResponse;
import com.oceanbase.query.api.dto.DatasourceConfigDto;
import com.oceanbase.query.api.dto.QueryExecuteRequest;
import com.oceanbase.query.api.dto.QueryResultDto;
import com.oceanbase.query.api.service.QueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueryController.class)
public class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryService queryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void executeQuery_Sync_Success() throws Exception {
        // 准备同步查询请求
        QueryExecuteRequest request = new QueryExecuteRequest();
        request.setDatasourceType("direct");
        request.setDatasourceConfig(new DatasourceConfigDto("url", "user", "pass"));
        request.setAsync(false);
        request.setExtraParams(Map.of("param1", "value1"));

        // 准备模拟服务返回结果
        List<QueryResultDto> results = Collections.singletonList(
                QueryResultDto.builder().templateId("tpl1").status("success").data(Collections.singletonList(Map.of("col1", "val1"))).build()
        );
        ApiResponse<List<QueryResultDto>> serviceResponse = ApiResponse.<List<QueryResultDto>>success(results);

        when(queryService.executeQuery(eq("scene1"), any(QueryExecuteRequest.class))).thenReturn(serviceResponse);

        // 执行测试并验证结果
        mockMvc.perform(post("/api/query/v1/scenes/scene1/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].templateId").value("tpl1"));
    }

    @Test
    void executeQuery_Async_Success() throws Exception {
        // 准备异步查询请求
        QueryExecuteRequest request = new QueryExecuteRequest();
        request.setDatasourceType("direct");
        request.setDatasourceConfig(new DatasourceConfigDto("url", "user", "pass"));
        request.setAsync(true);

        // 准备模拟服务返回结果
        ApiResponse<List<QueryResultDto>> serviceResponse = ApiResponse.asyncSuccess("task-123", "异步查询任务已提交");
        when(queryService.executeQuery(eq("scene1"), any(QueryExecuteRequest.class))).thenReturn(serviceResponse);

        // 执行测试并验证结果
        mockMvc.perform(post("/api/query/v1/scenes/scene1/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // 或 isAccepted() 如果控制器返回 202 表示异步提交
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.taskId").value("task-123"))
                .andExpect(jsonPath("$.message").value("异步查询任务已提交"));
    }

    @Test
    void executeQuery_Failure_SceneNotFound() throws Exception {
        // 准备查询请求
        QueryExecuteRequest request = new QueryExecuteRequest();
        request.setAsync(false);

        // 准备模拟服务返回结果
        ApiResponse<List<QueryResultDto>> serviceResponse = ApiResponse.error("20002", "场景代码未找到: scene-not-found");
        when(queryService.executeQuery(eq("scene-not-found"), any(QueryExecuteRequest.class))).thenReturn(serviceResponse);

        // 执行测试并验证结果
        mockMvc.perform(post("/api/query/v1/scenes/scene-not-found/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.errorCode").value("20002"));
    }

    @Test
    void getAsyncTaskResult_Success() throws Exception {
        // 准备模拟服务返回结果
        List<QueryResultDto> results = Collections.singletonList(
                QueryResultDto.builder().templateId("tpl1").status("success").data(Collections.singletonList(Map.of("col1", "val1"))).build()
        );
        ApiResponse<List<QueryResultDto>> serviceResponse = ApiResponse.<List<QueryResultDto>>success(results);
        when(queryService.getAsyncTaskResult("task-123")).thenReturn(serviceResponse);

        // 执行测试并验证结果
        mockMvc.perform(get("/api/query/v1/result/task-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].templateId").value("tpl1"));
    }

    @Test
    void getAsyncTaskResult_Pending() throws Exception {
        // 准备模拟服务返回结果
        ApiResponse<List<QueryResultDto>> serviceResponse = ApiResponse.<List<QueryResultDto>>builder()
                .status("pending")
                .message("任务正在处理中")
                .taskId("task-456")
                .build();
        when(queryService.getAsyncTaskResult("task-456")).thenReturn(serviceResponse);

        // 执行测试并验证结果
        mockMvc.perform(get("/api/query/v1/result/task-456"))
                .andExpect(status().isOk()) // 控制器返回 200 OK，状态在响应体中
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.taskId").value("task-456"))
                .andExpect(jsonPath("$.message").value("任务正在处理中"));
    }

    @Test
    void getAsyncTaskResult_Failed() throws Exception {
        // 准备模拟服务返回结果
        ApiResponse<List<QueryResultDto>> serviceResponse = ApiResponse.error("40001", "任务执行失败");
        when(queryService.getAsyncTaskResult("task-789")).thenReturn(serviceResponse);

        // 执行测试并验证结果
        mockMvc.perform(get("/api/query/v1/result/task-789"))
                .andExpect(status().isBadRequest()) // 控制器返回 400 表示任务失败
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.errorCode").value("40001"))
                .andExpect(jsonPath("$.message").value("任务执行失败"));
    }
    
    @Test
    void executeQuery_InvalidRequest_BodyMissing() throws Exception {
        // Spring Boot 如果缺少必需的 @RequestBody 会返回 400
        // 这里不需要模拟服务，因为控制器/框架会处理这种情况
        mockMvc.perform(post("/api/query/v1/scenes/scene1/execute")
                .contentType(MediaType.APPLICATION_JSON))
                // .content() 被省略
                .andExpect(status().isBadRequest());
    }
}
