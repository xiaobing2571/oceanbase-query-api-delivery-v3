package com.oceanbase.query.api.domain.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "query_scene", uniqueConstraints = {
    @UniqueConstraint(name = "uk_scene_code", columnNames = "scene_code")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryScene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_code", length = 64, nullable = false)
    private String sceneCode;

    @Column(name = "scene_name", length = 128)
    private String sceneName;

    @Lob
    @Column(name = "description")
    private String description;

    @Lob // For JSON, TEXT type is appropriate
    @Column(name = "template_ids", columnDefinition = "TEXT") // Store as JSON string of template IDs
    private String templateIds;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
