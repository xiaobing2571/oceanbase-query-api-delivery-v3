package com.oceanbase.query.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 数据源配置数据传输对象
 * 用于在服务层和控制层之间传递数据源配置信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasourceConfigDto {

    /**
     * 数据源ID，唯一标识一个数据源配置
     */
    private String datasourceId;
    
    /**
     * 数据源类型，如OCEANBASE、MYSQL等
     */
    private String datasourceType;

    /**
     * 数据库连接URL
     */
    @NotBlank(message = "数据库连接URL不能为空")
    private String url;

    /**
     * 数据库用户名
     */
    private String username;

    /**
     * 数据库密码
     */
    private String password;
    
    /**
     * 额外配置参数，存储为Map
     */
    private Map<String, Object> extraConfig;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 简化构造函数，用于测试和简单场景
     */
    public DatasourceConfigDto(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }
}
