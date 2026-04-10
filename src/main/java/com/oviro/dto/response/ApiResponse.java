package com.oviro.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private String errorCode;
    private T data;
    private Map<String, String> validationErrors;
    private LocalDateTime timestamp = LocalDateTime.now();

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Succès", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        ApiResponse<T> r = new ApiResponse<>(false, message, null);
        r.errorCode = errorCode;
        return r;
    }

    public static <T> ApiResponse<Map<String, String>> validationError(Map<String, String> errors) {
        ApiResponse<Map<String, String>> r = new ApiResponse<>(false, "Erreur de validation", errors);
        r.errorCode = "VALIDATION_ERROR";
        return r;
    }
}
