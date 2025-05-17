package com.oceanbase.query.api.domain.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "async_task") // Table for managing asynchronous tasks
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsyncTask {

    @Id
    @Column(name = "task_id", length = 64, nullable = false, unique = true)
    private String taskId; // Usually a UUID

    @Column(name = "status", length = 32, nullable = false)
    private String status; // e.g., PENDING, RUNNING, SUCCESS, FAILED

    @Lob
    @Column(name = "results", columnDefinition = "TEXT") // Store results as JSON string
    private String results;

    @Column(name = "callback_url", length = 512)
    private String callbackUrl;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "scene_code", length = 64)
    private String sceneCode;

    @Lob
    @Column(name = "request_payload", columnDefinition = "TEXT") // Store original request payload for retries or debugging
    private String requestPayload;
}

