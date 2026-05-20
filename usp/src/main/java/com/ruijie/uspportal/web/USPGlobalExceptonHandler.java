package com.ruijie.uspportal.web;

import com.ruijie.uspportal.common.ApiResponse;
import com.ruijie.uspportal.common.BusinessException;
import com.ruijie.framework.sso.base.exception.SSOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * <p>统一拦截业务异常、参数校验异常与未预期异常，并转换为门户通用响应结构。</p>
 */
@RestControllerAdvice
public class USPGlobalExceptonHandler {

    /**
     * 处理业务异常。
     *
     * @param ex 业务异常
     * @return 失败响应
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        return ApiResponse.failure(ex.getMessage());
    }

    /**
     * 处理参数校验异常。
     *
     * @param ex 参数校验异常
     * @return 失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "请求参数不合法"
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ApiResponse.failure(message);
    }

    /**
     * 处理上游 SSO 登录校验异常。
     *
     * @param ex SSO 登录异常
     * @return 失败响应
     */
    @ExceptionHandler(SSOException.class)
    @ResponseStatus(HttpStatus.PRECONDITION_REQUIRED)
    public ApiResponse<Void> handleSsoException(SSOException ex) {
        return ApiResponse.failure(ex.getMessage());
    }

    /**
     * 处理未预期异常。
     *
     * @param ex 未预期异常
     * @return 失败响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception ex) {
        return ApiResponse.failure(ex.getMessage());
    }
}
