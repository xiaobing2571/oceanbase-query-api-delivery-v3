package com.oceanbase.query.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.query.api.domain.entity.DatasourceConfig;
import com.oceanbase.query.api.domain.repository.DatasourceConfigRepository;
import com.oceanbase.query.api.dto.ApiResponse;
import com.oceanbase.query.api.dto.DatasourceConfigDto;
import com.oceanbase.query.api.dto.DatasourceTestRequest;
import com.oceanbase.query.api.service.DatasourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DatasourceServiceImpl implements DatasourceService {

    @Autowired
    private DatasourceConfigRepository datasourceConfigRepository;

    @Autowired
    private ObjectMapper objectMapper; // For handling JSON in extraConfig

    @Override
    public ApiResponse<Object> testDatasourceConnection(DatasourceTestRequest request) {
        String url = null;
        String username = null;
        String password = null;
        String extraConfigJson = null;

        try {
            switch (request.getDatasourceType().toLowerCase()) {
                case "direct":
                    if (request.getDatasourceConfig() == null) {
                        return ApiResponse.error("10003", "直接连接时，datasourceConfig 不能为空");
                    }
                    url = request.getDatasourceConfig().getUrl();
                    username = request.getDatasourceConfig().getUsername();
                    password = request.getDatasourceConfig().getPassword();
                    if (request.getExtraConfig() != null) {
                        extraConfigJson = objectMapper.writeValueAsString(request.getExtraConfig());
                    }
                    break;
                case "registered":
                    if (request.getDatasourceId() == null) {
                        return ApiResponse.error("10004", "注册数据源连接时，datasourceId 不能为空");
                    }
                    DatasourceConfig registeredDs = datasourceConfigRepository.findByDatasourceId(request.getDatasourceId())
                            .orElse(null);
                    if (registeredDs == null) {
                        return ApiResponse.error("10005", "未找到注册的数据源: " + request.getDatasourceId());
                    }
                    url = registeredDs.getUrl();
                    username = registeredDs.getUsername();
                    password = registeredDs.getPassword(); // Assuming password is not encrypted in DB for now
                    extraConfigJson = registeredDs.getExtraConfig();
                    break;
                case "external":
                    log.warn("External datasource type is not yet implemented.");
                    return ApiResponse.error("10006", "外部数据源类型暂未实现");
                default:
                    return ApiResponse.error("10007", "不支持的数据源类型: " + request.getDatasourceType());
            }

            if (url == null) {
                 return ApiResponse.error("10008", "无法获取数据库连接URL");
            }

            Properties connectionProps = new Properties();
            if (username != null) {
                connectionProps.put("user", username);
            }
            if (password != null) {
                connectionProps.put("password", password);
            }

            if (extraConfigJson != null && !extraConfigJson.isEmpty() && !"null".equalsIgnoreCase(extraConfigJson)) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> extraProps = objectMapper.readValue(extraConfigJson, Map.class);
                    extraProps.forEach(connectionProps::setProperty);
                } catch (JsonProcessingException e) {
                    log.error("解析 extra_config 失败: {}", extraConfigJson, e);
                    return ApiResponse.error("10009", "解析 extra_config 失败: " + e.getMessage());
                }
            }
            
            try {
                // TODO: Make driver configurable or detect based on URL prefix
                Class.forName("com.oceanbase.jdbc.Driver"); 
            } catch (ClassNotFoundException e) {
                log.error("JDBC 驱动未找到", e);
                return ApiResponse.error("10010", "JDBC 驱动未找到: " + e.getMessage());
            }

            try (Connection connection = DriverManager.getConnection(url, connectionProps)) {
                if (connection.isValid(2)) { 
                    log.info("数据源连接测试成功: {}", url);
                    return ApiResponse.success("数据源连接成功");
                }
            } catch (SQLException e) {
                log.error("数据源连接失败: {}", url, e);
                return ApiResponse.error("10001", "数据源连接失败: " + e.getMessage());
            }

        } catch (JsonProcessingException e) {
            log.error("处理请求时发生JSON处理错误", e);
            return ApiResponse.error("40001", "请求处理错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("测试数据源连接时发生未知错误", e);
            return ApiResponse.error("40001", "测试数据源连接时发生未知错误: " + e.getMessage());
        }
        return ApiResponse.error("40001", "数据源连接测试失败 (未知原因)");
    }

    @Override
    @Transactional
    public ApiResponse<DatasourceConfigDto> registerDatasource(DatasourceConfigDto requestDto) {
        if (requestDto.getUrl() == null || requestDto.getUrl().isEmpty()) {
            return ApiResponse.error("20001", "数据源URL不能为空");
        }
        // Potentially add more validation for username/password based on requirements

        DatasourceConfig datasourceConfig = new DatasourceConfig();
        BeanUtils.copyProperties(requestDto, datasourceConfig);
        if (datasourceConfig.getDatasourceId() == null || datasourceConfig.getDatasourceId().isEmpty()) {
            datasourceConfig.setDatasourceId(UUID.randomUUID().toString()); // Generate ID if not provided
        }
        // Handle extraConfig (assuming it's a Map<String, Object> in DTO and needs to be JSON string in Entity)
        if (requestDto.getExtraConfig() != null) {
            try {
                datasourceConfig.setExtraConfig(objectMapper.writeValueAsString(requestDto.getExtraConfig()));
            } catch (JsonProcessingException e) {
                log.error("序列化 extraConfig 失败 for datasourceId {}: {}", datasourceConfig.getDatasourceId(), requestDto.getExtraConfig(), e);
                return ApiResponse.error("20002", "序列化 extraConfig 失败: " + e.getMessage());
            }
        }

        try {
            DatasourceConfig savedConfig = datasourceConfigRepository.save(datasourceConfig);
            DatasourceConfigDto responseDto = new DatasourceConfigDto();
            BeanUtils.copyProperties(savedConfig, responseDto);
            if (savedConfig.getExtraConfig() != null) {
                try {
                    responseDto.setExtraConfig(objectMapper.readValue(savedConfig.getExtraConfig(), Map.class));
                } catch (JsonProcessingException e) {
                    log.warn("反序列化 extraConfig 失败 for datasourceId {}: {}", savedConfig.getDatasourceId(), savedConfig.getExtraConfig(), e);
                    // Decide if this is a critical error or if DTO can proceed with null/empty extraConfig
                }
            }
            log.info("数据源注册成功: {}", responseDto.getDatasourceId());
            return ApiResponse.success(responseDto);
        } catch (Exception e) {
            log.error("注册数据源失败: {}", datasourceConfig.getDatasourceId(), e);
            return ApiResponse.error("20003", "注册数据源时发生内部错误: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteDatasource(String datasourceId) {
        if (!datasourceConfigRepository.existsByDatasourceId(datasourceId)) {
            log.warn("尝试删除不存在的数据源: {}", datasourceId);
            return ApiResponse.error("20004", "未找到要删除的数据源: " + datasourceId);
        }
        try {
            datasourceConfigRepository.deleteByDatasourceId(datasourceId);
            log.info("数据源删除成功: {}", datasourceId);
            return ApiResponse.success(null); // Or success with a message
        } catch (Exception e) {
            log.error("删除数据源失败: {}", datasourceId, e);
            return ApiResponse.error("20005", "删除数据源时发生内部错误: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<DatasourceConfigDto> getDatasource(String datasourceId) {
        return datasourceConfigRepository.findByDatasourceId(datasourceId)
                .map(config -> {
                    DatasourceConfigDto dto = new DatasourceConfigDto();
                    BeanUtils.copyProperties(config, dto);
                    if (config.getExtraConfig() != null) {
                        try {
                            dto.setExtraConfig(objectMapper.readValue(config.getExtraConfig(), Map.class));
                        } catch (JsonProcessingException e) {
                            log.warn("反序列化 extraConfig 失败 for datasourceId {}: {}", config.getDatasourceId(), config.getExtraConfig(), e);
                        }
                    }
                    log.debug("查询数据源成功: {}", datasourceId);
                    return ApiResponse.success(dto);
                })
                .orElseGet(() -> {
                    log.warn("查询数据源未找到: {}", datasourceId);
                    return ApiResponse.error("20006", "未找到数据源: " + datasourceId);
                });
    }

    @Override
    public ApiResponse<List<DatasourceConfigDto>> listDatasources() {
        try {
            List<DatasourceConfigDto> dtos = datasourceConfigRepository.findAll().stream()
                    .map(config -> {
                        DatasourceConfigDto dto = new DatasourceConfigDto();
                        BeanUtils.copyProperties(config, dto);
                        if (config.getExtraConfig() != null) {
                            try {
                                dto.setExtraConfig(objectMapper.readValue(config.getExtraConfig(), Map.class));
                            } catch (JsonProcessingException e) {
                                log.warn("反序列化 extraConfig 失败 for datasourceId {}: {}", config.getDatasourceId(), config.getExtraConfig(), e);
                            }
                        }
                        return dto;
                    })
                    .collect(Collectors.toList());
            log.debug("列出所有数据源成功，数量: {}", dtos.size());
            return ApiResponse.<List<DatasourceConfigDto>>builder()
                    .status("success")
                    .data(dtos)
                    .build();
        } catch (Exception e) {
            log.error("列出数据源时发生内部错误", e);
            return ApiResponse.error("20007", "列出数据源时发生内部错误: " + e.getMessage());
        }
    }
}
