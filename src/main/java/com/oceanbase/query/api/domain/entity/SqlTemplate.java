package com.oceanbase.query.api.domain.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sql_template", uniqueConstraints = {
    @UniqueConstraint(name = "uk_template_id", columnNames = "template_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", length = 64, nullable = false)
    private String templateId;

    @Column(name = "scene_code", length = 64, nullable = false)
    private String sceneCode;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "db_type", length = 32, nullable = false)
    private String dbType;

    @Lob // For JSON, TEXT type is appropriate
    @Column(name = "parameters", columnDefinition = "TEXT") // Or use @Convert for better JSON handling
    private String parameters; // Store as JSON string

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
