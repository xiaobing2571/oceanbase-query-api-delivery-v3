package com.oceanbase.query.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private String status; // e.g., "success", "failed"
    private T data; // For single object responses or list of results in query execution
    private List<T> results; // Specifically for query execution multiple results
    private String errorCode;
    private String message;
    private String taskId; // For async responses

    // Success response for single data object
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status("success")
                .data(data)
                .build();
    }

    // Success response for list of results (e.g., query execution)
    public static <T> ApiResponse<T> success(List<T> results) {
        return ApiResponse.<T>builder()
                .status("success")
                .results(results)
                .build();
    }
    
    // Success response for async task submission
    public static <T> ApiResponse<T> asyncSuccess(String taskId, String message) {
        return ApiResponse.<T>builder()
                .status("success")
                .taskId(taskId)
                .message(message)
                .build();
    }

    // Failure response
    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return ApiResponse.<T>builder()
                .status("failed")
                .errorCode(errorCode)
                .message(message)
                .build();
    }
}

