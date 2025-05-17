package com.oceanbase.query.api.domain.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "datasource_config", uniqueConstraints = {
    @UniqueConstraint(name = "uk_datasource_id", columnNames = "datasource_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasourceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "datasource_id", length = 64, nullable = false)
    private String datasourceId;

    @Column(name = "db_type", length = 32, nullable = false)
    private String dbType;

    @Column(name = "url", length = 255, nullable = false)
    private String url;

    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "password", length = 128) // Consider encrypting this field
    private String password;

    @Lob
    @Column(name = "extra_config", columnDefinition = "TEXT") // Store as JSON string
    private String extraConfig;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
