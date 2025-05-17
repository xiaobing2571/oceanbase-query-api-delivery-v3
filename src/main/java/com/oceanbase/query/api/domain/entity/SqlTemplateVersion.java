package com.oceanbase.query.api.domain.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import javax.persistence.*;

@Entity
@Table(name = "sql_template_version", indexes = {
    @Index(name = "idx_template_id_version", columnList = "template_id, version_condition") // Composite index for faster lookups
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlTemplateVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", length = 64, nullable = false)
    private String templateId; // This will be linked via service logic, not a direct FK in JPA to avoid complexities with templateId string

    @Column(name = "version_condition", length = 64, nullable = false)
    private String versionCondition;

    @Lob
    @Column(name = "sql_text", nullable = false, columnDefinition = "TEXT")
    private String sqlText;

    // No direct @ManyToOne to SqlTemplate here to keep it simple as per document's DDL (FOREIGN KEY (template_id) REFERENCES sql_template(template_id))
    // The linkage will be managed at the service layer or by ensuring template_id consistency.
}

