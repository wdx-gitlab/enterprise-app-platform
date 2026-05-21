package com.ruijie.authzengine.shared.exception;

import com.ruijie.authzengine.shared.web.ApiResponse;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice(basePackages = "com.ruijie.authzengine.api")
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        return ApiResponse.failure(ErrorCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 处理缺少请求参数异常。
     *
     * @param exception 缺少请求参数异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException exception) {
        return ApiResponse.failure(ErrorCode.BAD_REQUEST.getCode(), exception.getParameterName() + " 不能为空");
    }

    /**
     * 处理方法参数约束校验异常。
     *
     * @param exception 参数约束异常
     * @return 统一错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining("; "));
        return ApiResponse.failure(ErrorCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 处理不支持的请求方法，返回真实 HTTP 405。
     *
     * @param exception 请求方法异常
     * @return 统一错误响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
        HttpRequestMethodNotSupportedException exception
    ) {
        log.warn("请求方法不支持 method={}, message={}", exception.getMethod(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiResponse.failure(ErrorCode.METHOD_NOT_ALLOWED.getCode(), ErrorCode.METHOD_NOT_ALLOWED.getMessage()));
    }

    /**
     * 处理请求参数类型转换异常。
     *
     * @param exception 参数类型转换异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception) {
        return ApiResponse.failure(ErrorCode.BAD_REQUEST.getCode(), exception.getName() + " 参数格式不合法");
    }

    /**
     * 处理业务异常。
     * <p>鉴权类异常（AUTHZ-401 / AUTHZ-403）返回对应真实 HTTP 状态码，其余业务异常仍返回 HTTP 200。
     *
     * @param exception 业务异常
     * @return 统一错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        log.warn("业务异常: code={}, message={}", exception.getCode(), exception.getMessage());
        ApiResponse<Void> body = ApiResponse.failure(exception.getCode(), exception.getMessage());
        HttpStatus httpStatus = resolveHttpStatus(exception.getCode());
        return ResponseEntity.status(httpStatus).body(body);
    }

    /**
     * 将业务错误码映射到 HTTP 状态码。
     * 鉴权相关错误映射到真实 4xx，其余业务错误统一返回 200。
     */
    private HttpStatus resolveHttpStatus(String code) {
        if (ErrorCode.UNAUTHENTICATED.getCode().equals(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (ErrorCode.FORBIDDEN.getCode().equals(code)) {
            return HttpStatus.FORBIDDEN;
        }
        if (code != null && code.startsWith("AUTHZ-409")) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.OK;
    }

    /**
     * 处理外部集成异常，HTTP 始终返回 200。
     *
     * @param exception 外部集成异常
     * @return 统一错误响应
     */
    @ExceptionHandler(AuthzIntegrationException.class)
    public ApiResponse<Void> handleAuthzIntegrationException(AuthzIntegrationException exception) {
        log.error("外部集成异常: {}", exception.getMessage(), exception);
        return ApiResponse.failure(ErrorCode.INTEGRATION_ERROR.getCode(), exception.getMessage());
    }

    /**
     * 处理系统异常，HTTP 始终返回 200。
     *
     * @param exception 系统异常
     * @return 统一错误响应
     */
    @ExceptionHandler(SystemException.class)
    public ApiResponse<Void> handleSystemException(SystemException exception) {
        log.error("系统异常: {}", exception.getMessage(), exception);
        return ApiResponse.failure(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理未显式收口的异常，HTTP 始终返回 200。
     *
     * @param exception 未知异常
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        log.error("未处理异常", exception);
        return ApiResponse.failure(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
    }
}
