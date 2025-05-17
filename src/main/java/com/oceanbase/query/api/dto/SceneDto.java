package com.oceanbase.query.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 查询场景数据传输对象
 * 用于在服务层和控制层之间传递查询场景相关信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SceneDto {
    
    /**
     * 场景代码，唯一标识一个查询场景
     */
    private String sceneCode;
    
    /**
     * 场景名称
     */
    private String sceneName;
    
    /**
     * 场景描述信息
     */
    private String description;
    
    /**
     * 关联的模板ID列表
     */
    private List<String> templateIds;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
