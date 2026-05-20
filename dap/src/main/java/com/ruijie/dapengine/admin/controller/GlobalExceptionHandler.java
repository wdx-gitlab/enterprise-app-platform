package com.ruijie.dapengine.admin.controller;

import com.ruijie.dapengine.common.exception.DapEngineException;
import com.ruijie.dapengine.common.exception.DapValidationException;
import com.ruijie.dapengine.common.model.Result;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * DAP Engine 控制器专用异常处理器。
 * 仅处理 DAP 自身暴露的 Admin Controller，避免影响宿主应用或其它 SDK 的控制器。
 */
@RestControllerAdvice(basePackageClasses = {
    MetadataConfigController.class,
    SyncConfigController.class,
    MasterDataBrowseController.class
})
public class GlobalExceptionHandler {

    @ExceptionHandler(DapValidationException.class)
    public Result<Void> handleValidation(DapValidationException ex) {
        return Result.fail(4001, ex.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException ex) {
        return Result.fail(4090, "[DAP Engine] 数据已存在，请检查唯一键约束");
    }

    @ExceptionHandler(DapEngineException.class)
    public Result<Void> handleDapEngine(DapEngineException ex) {
        return Result.fail(5000, ex.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    public Result<Void> handleDataAccess(DataAccessException ex) {
        return Result.fail(5000, "[DAP Engine] 数据库操作异常：" + ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleGeneral(Exception ex) {
        return Result.fail(5000, "[DAP Engine] 服务器内部错误：" + ex.getMessage());
    }
}
