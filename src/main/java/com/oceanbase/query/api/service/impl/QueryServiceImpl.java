package com.oceanbase.query.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.query.api.domain.entity.*;
import com.oceanbase.query.api.domain.repository.*;
import com.oceanbase.query.api.dto.*;
import com.oceanbase.query.api.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class QueryServiceImpl implements QueryService {

    @Autowired
    private QuerySceneRepository querySceneRepository;

    @Autowired
    private SqlTemplateRepository sqlTemplateRepository;

    @Autowired
    private SqlTemplateVersionRepository sqlTemplateVersionRepository;

    @Autowired
    private DatasourceConfigRepository datasourceConfigRepository;

    @Autowired
    private AsyncTaskRepository asyncTaskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public ApiResponse<List<QueryResultDto>> executeQuery(String sceneCode, QueryExecuteRequest request) {
        Optional<QueryScene> sceneOpt = querySceneRepository.findBySceneCode(sceneCode);
        if (sceneOpt.isEmpty()) {
            return ApiResponse.<List<QueryResultDto>>error("20002", "场景代码未找到: " + sceneCode);
        }
        QueryScene scene = sceneOpt.get();

        if (Boolean.TRUE.equals(request.getAsync())) {
            String taskId = UUID.randomUUID().toString();
            AsyncTask asyncTask = new AsyncTask();
            asyncTask.setTaskId(taskId);
            asyncTask.setSceneCode(sceneCode);
            try {
                asyncTask.setRequestPayload(objectMapper.writeValueAsString(request));
            } catch (JsonProcessingException e) {
                log.error("序列化异步任务请求失败 for scene {}: {}", sceneCode, e.getMessage());
            }
            asyncTask.setStatus("PENDING");
            asyncTask.setCreateTime(LocalDateTime.now());
            asyncTask.setUpdateTime(LocalDateTime.now());
            asyncTask.setCallbackUrl(request.getCallbackUrl());
            asyncTaskRepository.save(asyncTask);

            executeSceneQueryAsync(taskId, scene, request);
            return ApiResponse.<List<QueryResultDto>>asyncSuccess(taskId, "异步查询任务已提交");
        }

        return executeSceneQuerySync(scene, request);
    }

    @Async
    public void executeSceneQueryAsync(String taskId, QueryScene scene, QueryExecuteRequest request) {
        AsyncTask asyncTask = asyncTaskRepository.findByTaskId(taskId).orElse(null);
        if (asyncTask == null) {
            log.error("无法找到异步任务进行执行: {}", taskId);
            return;
        }

        asyncTask.setStatus("RUNNING");
        asyncTask.setUpdateTime(LocalDateTime.now());
        asyncTaskRepository.save(asyncTask);

        ApiResponse<List<QueryResultDto>> response = executeSceneQuerySync(scene, request);

        asyncTask.setUpdateTime(LocalDateTime.now());
        if ("success".equals(response.getStatus())) {
            asyncTask.setStatus("SUCCESS");
            try {
                asyncTask.setResults(objectMapper.writeValueAsString(response.getResults()));
            } catch (JsonProcessingException e) {
                log.error("序列化异步任务结果失败 for task {}: {}", taskId, e.getMessage());
                asyncTask.setStatus("FAILED");
                asyncTask.setErrorMessage("结果序列化失败: " + e.getMessage());
            }
        } else {
            asyncTask.setStatus("FAILED");
            asyncTask.setErrorMessage(response.getMessage() != null ? response.getMessage() : "执行失败，未知错误");
        }
        asyncTaskRepository.save(asyncTask);

        if (asyncTask.getCallbackUrl() != null && !asyncTask.getCallbackUrl().isEmpty()) {
            log.info("准备回调: {} for task {}", asyncTask.getCallbackUrl(), taskId);
            // TODO: Implement callback logic (e.g., using RestTemplate or WebClient)
        }
    }

    private ApiResponse<List<QueryResultDto>> executeSceneQuerySync(QueryScene scene, QueryExecuteRequest request) {
        List<String> templateIds;
        try {
            templateIds = objectMapper.readValue(scene.getTemplateIds(), new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析场景模板ID列表失败 for scene {}: {}", scene.getSceneCode(), e.getMessage());
            return ApiResponse.<List<QueryResultDto>>error("20003", "解析场景模板ID列表失败: " + e.getMessage());
        }

        if (templateIds == null || templateIds.isEmpty()) {
            return ApiResponse.<List<QueryResultDto>>error("20004", "场景 " + scene.getSceneCode() + " 未关联任何SQL模板");
        }

        DataSource targetDataSource;
        try {
            targetDataSource = getTargetDataSource(request);
        } catch (IllegalArgumentException | SQLException e) {
            return ApiResponse.<List<QueryResultDto>>error("10001", "获取数据源失败: " + e.getMessage());
        }
        if (targetDataSource == null) {
             return ApiResponse.<List<QueryResultDto>>error("10001", "无法创建目标数据源连接");
        }

        List<QueryResultDto> allResults = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(templateIds.size(), 5));
        List<Future<QueryResultDto>> futures = new ArrayList<>();
        long overallTimeout = request.getExecutionTimeoutMs() != null ? request.getExecutionTimeoutMs() : 30000L;

        for (String templateId : templateIds) {
            Callable<QueryResultDto> task = () -> executeSingleTemplate(templateId, request, targetDataSource);
            futures.add(executor.submit(task));
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(overallTimeout, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
                log.warn("场景 {} 执行超时 ({}ms)，部分模板可能未完成", scene.getSceneCode(), overallTimeout);
                for (Future<QueryResultDto> future : futures) {
                    if (future.isDone() && !future.isCancelled()) {
                        try {
                            allResults.add(future.get());
                        } catch (InterruptedException | ExecutionException e) {
                            log.error("获取模板执行结果时出错 (超时后): {}", e.getMessage());
                        }
                    } else {
                        // Add a placeholder for timed-out/cancelled tasks if identifiable
                    }
                }
                return ApiResponse.<List<QueryResultDto>>error("10002", "场景查询执行超时");
            }

            for (Future<QueryResultDto> future : futures) {
                try {
                    allResults.add(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    log.error("获取模板执行结果时出错: {}", e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            log.error("场景 {} 执行被中断", scene.getSceneCode(), e);
            return ApiResponse.<List<QueryResultDto>>error("40001", "查询执行被中断");
        }
        
        boolean allIndividualSuccess = allResults.stream().allMatch(r -> "success".equals(r.getStatus()));
        if (allIndividualSuccess && allResults.size() == templateIds.size()) {
             return ApiResponse.<List<QueryResultDto>>success(allResults);
        } else {
            return ApiResponse.<List<QueryResultDto>>success(allResults);
        }
    }

    private QueryResultDto executeSingleTemplate(String templateId, QueryExecuteRequest queryRequest, DataSource targetDataSource) {
        long startTimeMs = System.currentTimeMillis();
        Optional<SqlTemplate> templateOpt = sqlTemplateRepository.findByTemplateId(templateId);
        if (templateOpt.isEmpty()) {
            return QueryResultDto.builder().templateId(templateId).status("failed").errorMessage("模板未找到").executionTimeMs(System.currentTimeMillis() - startTimeMs).build();
        }
        SqlTemplate template = templateOpt.get();

        List<SqlTemplateVersion> versions = sqlTemplateVersionRepository.findByTemplateId(templateId);
        if (versions.isEmpty()) {
            return QueryResultDto.builder().templateId(templateId).status("failed").errorMessage("模板无可用版本").executionTimeMs(System.currentTimeMillis() - startTimeMs).build();
        }

        String targetObVersion = "";
        if (queryRequest.getExtraParams() != null && queryRequest.getExtraParams().containsKey("targetObVersion")) {
            targetObVersion = queryRequest.getExtraParams().get("targetObVersion").toString();
        }

        SqlTemplateVersion matchedVersion = findMatchingSqlVersion(versions, targetObVersion);
        if (matchedVersion == null) {
            return QueryResultDto.builder().templateId(templateId).status("failed").errorMessage("无匹配当前OB版本的SQL模板: " + targetObVersion).executionTimeMs(System.currentTimeMillis() - startTimeMs).build();
        }

        String sql = matchedVersion.getSqlText();
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (queryRequest.getStartTime() != null) params.addValue("start_time", queryRequest.getStartTime());
        if (queryRequest.getEndTime() != null) params.addValue("end_time", queryRequest.getEndTime());
        if (queryRequest.getTenant() != null) params.addValue("tenant", queryRequest.getTenant());

        if (template.getParameters() != null && !template.getParameters().isEmpty()) {
            try {
                List<TemplateParameterDto> paramDefs = objectMapper.readValue(template.getParameters(), new TypeReference<List<TemplateParameterDto>>() {});
                if (queryRequest.getExtraParams() != null) {
                    for (TemplateParameterDto paramDef : paramDefs) {
                        if (queryRequest.getExtraParams().containsKey(paramDef.getName())) {
                            params.addValue(paramDef.getName(), queryRequest.getExtraParams().get(paramDef.getName()));
                        }
                    }
                }
            } catch (JsonProcessingException e) {
                log.error("解析模板参数定义失败 for template {}: {}", templateId, e.getMessage());
                return QueryResultDto.builder().templateId(templateId).status("failed").errorMessage("解析模板参数定义失败").executionTimeMs(System.currentTimeMillis() - startTimeMs).build();
            }
        }

        try {
            NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(targetDataSource);
            List<Map<String, Object>> resultData = namedJdbcTemplate.queryForList(sql, params);
            return QueryResultDto.builder()
                    .templateId(templateId)
                    .version(matchedVersion.getVersionCondition())
                    .status("success")
                    .data(resultData)
                    .executionTimeMs(System.currentTimeMillis() - startTimeMs)
                    .build();
        } catch (Exception e) {
            log.error("执行SQL模板 {} 失败: {}", templateId, e.getMessage(), e);
            return QueryResultDto.builder()
                    .templateId(templateId)
                    .version(matchedVersion.getVersionCondition())
                    .status("failed")
                    .errorMessage("SQL执行失败: " + e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTimeMs)
                    .build();
        }
    }

    private SqlTemplateVersion findMatchingSqlVersion(List<SqlTemplateVersion> versions, String targetObVersion) {
        if (targetObVersion == null || targetObVersion.isEmpty()) {
            return versions.stream().filter(v -> v.getVersionCondition().equals(">=0.0.0") || (!v.getVersionCondition().startsWith("<") && !v.getVersionCondition().startsWith(">"))).findFirst().orElse(versions.isEmpty() ? null : versions.get(0));
        }
        for (SqlTemplateVersion v : versions) {
            String condition = v.getVersionCondition();
            if (condition.startsWith(">=")) {
                if (compareVersions(targetObVersion, condition.substring(2)) >= 0) return v;
            } else if (condition.startsWith("<=")) {
                if (compareVersions(targetObVersion, condition.substring(2)) <= 0) return v;
            } else if (condition.startsWith(">")) {
                if (compareVersions(targetObVersion, condition.substring(1)) > 0) return v;
            } else if (condition.startsWith("<")) {
                if (compareVersions(targetObVersion, condition.substring(1)) < 0) return v;
            } else if (condition.equals(targetObVersion)) {
                return v;
            }
        }
        return versions.stream().filter(v -> v.getVersionCondition().equalsIgnoreCase("all") || v.getVersionCondition().equals(">=0.0.0")).findFirst().orElse(null);
    }

    private int compareVersions(String v1, String v2) {
        try {
            String[] parts1 = v1.split("\\.");
            String[] parts2 = v2.split("\\.");
            int length = Math.max(parts1.length, parts2.length);
            for (int i = 0; i < length; i++) {
                int p1 = i < parts1.length ? Integer.parseInt(parts1[i].replaceAll("[^0-9]", "")) : 0;
                int p2 = i < parts2.length ? Integer.parseInt(parts2[i].replaceAll("[^0-9]", "")) : 0;
                if (p1 < p2) return -1;
                if (p1 > p2) return 1;
            }
            return 0;
        } catch (NumberFormatException e) {
            log.warn("无法比较版本号: {} vs {}", v1, v2);
            return v1.compareTo(v2);
        }
    }

    private DataSource getTargetDataSource(QueryExecuteRequest request) throws SQLException {
        String url, username, password, extraConfigJson = null;
        String cacheKey;

        switch (request.getDatasourceType().toLowerCase()) {
            case "direct":
                if (request.getDatasourceConfig() == null) throw new IllegalArgumentException("直接连接时，datasourceConfig 不能为空");
                url = request.getDatasourceConfig().getUrl();
                username = request.getDatasourceConfig().getUsername();
                password = request.getDatasourceConfig().getPassword();
                if (request.getExtraParams() != null) { 
                    try {
                        extraConfigJson = objectMapper.writeValueAsString(request.getExtraParams());
                    } catch (JsonProcessingException e) { 
                        log.warn("序列化直接连接的extraParams失败: {}", e.getMessage());
                    }
                }
                cacheKey = "direct:" + url + ":" + username; 
                break;
            case "registered":
                if (request.getDatasourceId() == null) throw new IllegalArgumentException("注册数据源连接时，datasourceId 不能为空");
                DatasourceConfig registeredDs = datasourceConfigRepository.findByDatasourceId(request.getDatasourceId())
                        .orElseThrow(() -> new IllegalArgumentException("未找到注册的数据源: " + request.getDatasourceId()));
                url = registeredDs.getUrl();
                username = registeredDs.getUsername();
                password = registeredDs.getPassword();
                extraConfigJson = registeredDs.getExtraConfig();
                cacheKey = "registered:" + request.getDatasourceId();
                break;
            case "external":
                throw new IllegalArgumentException("外部数据源类型暂未实现");
            default:
                throw new IllegalArgumentException("不支持的数据源类型: " + request.getDatasourceType());
        }

        if (url == null) {
            throw new IllegalArgumentException("无法获取数据库连接URL");
        }

        if (dataSourceCache.containsKey(cacheKey)) {
            try (Connection conn = dataSourceCache.get(cacheKey).getConnection()) {
                if (conn.isValid(1)) {
                    return dataSourceCache.get(cacheKey);
                }
            } catch (SQLException e) {
                log.warn("缓存的数据源 {} 不再有效，将重新创建", cacheKey, e);
                dataSourceCache.remove(cacheKey);
            }
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        if (username != null) dataSource.setUsername(username);
        if (password != null) dataSource.setPassword(password);
        
        if (extraConfigJson != null && !extraConfigJson.isEmpty() && !"null".equalsIgnoreCase(extraConfigJson)) {
            try {
                Properties connProps = new Properties();
                Map<String, String> extraPropsMap = objectMapper.readValue(extraConfigJson, new TypeReference<Map<String, String>>() {});
                extraPropsMap.forEach(connProps::setProperty);
                dataSource.setConnectionProperties(connProps);
            } catch (JsonProcessingException e) {
                log.error("解析 extra_config 失败 for datasource {}: {}", cacheKey, e.getMessage());
            }
        }

        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(2)) {
                throw new SQLException("创建的数据源连接无效: " + url);
            }
        } catch (SQLException e) {
            log.error("创建数据源连接失败: {}", url, e);
            throw e;
        }
        
        dataSourceCache.put(cacheKey, dataSource);
        return dataSource;
    }

    @Override
    public ApiResponse<List<QueryResultDto>> getAsyncTaskResult(String taskId) {
        Optional<AsyncTask> taskOpt = asyncTaskRepository.findByTaskId(taskId);
        if (taskOpt.isEmpty()) {
            return ApiResponse.<List<QueryResultDto>>error("40004", "异步任务未找到: " + taskId);
        }
        AsyncTask task = taskOpt.get();
        if ("PENDING".equals(task.getStatus()) || "RUNNING".equals(task.getStatus())) {
            return ApiResponse.<List<QueryResultDto>>builder()
                    .status(task.getStatus().toLowerCase())
                    .message("任务正在处理中")
                    .taskId(taskId)
                    .build();
        }
        if ("SUCCESS".equals(task.getStatus())) {
            try {
                List<QueryResultDto> results = objectMapper.readValue(task.getResults(), new TypeReference<List<QueryResultDto>>() {});
                return ApiResponse.<List<QueryResultDto>>success(results);
            } catch (JsonProcessingException e) {
                log.error("解析异步任务结果失败 for task {}: {}", taskId, e.getMessage());
                return ApiResponse.<List<QueryResultDto>>error("40001", "解析任务结果失败: " + e.getMessage());
            }
        }
        return ApiResponse.<List<QueryResultDto>>error("40001", task.getErrorMessage() != null ? task.getErrorMessage() : "任务执行失败");
    }
}

