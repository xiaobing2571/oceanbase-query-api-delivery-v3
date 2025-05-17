package com.oceanbase.query.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SQL模板数据传输对象
 * 用于在服务层和控制层之间传递SQL模板相关信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SqlTemplateDto {
    
    /**
     * 模板ID，唯一标识一个SQL模板
     */
    private String templateId;
    
    /**
     * 关联的场景代码
     */
    private String sceneCode;
    
    /**
     * 模板描述信息
     */
    private String description;
    
    /**
     * 数据库类型，如OCEANBASE、MYSQL等
     */
    private String dbType;
    
    /**
     * 模板参数列表
     */
    private List<TemplateParameterDto> parameters;
    
    /**
     * 版本映射列表，包含不同OB版本对应的SQL语句
     */
    private List<VersionMappingDto> versionMapping;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
