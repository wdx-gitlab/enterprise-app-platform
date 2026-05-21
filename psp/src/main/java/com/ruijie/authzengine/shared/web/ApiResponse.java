package com.ruijie.authzengine.shared.web;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应包装。
 *
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ApiResponse", description = "统一接口响应")
public class ApiResponse<T> {

    @Schema(description = "响应码", example = "0")
    private String code;

    @Schema(description = "响应消息", example = "成功")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    /**
     * 构造成功响应。
     *
     * @param data 业务数据
     * @param <T> 数据类型
     * @return 统一成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("0", "成功", data);
    }

    /**
     * 构造失败响应。
     *
     * @param code 业务编码
     * @param message 可读消息
     * @param <T> 数据类型
     * @return 统一失败响应
     */
    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}