package com.ruijie.authzengine.application.sdk;

import com.ruijie.authzengine.application.sdk.model.AuthzCheckCommand;
import com.ruijie.authzengine.application.sdk.model.AuthzCheckResult;
import com.ruijie.authzengine.application.sdk.model.PermCodeCheckCommand;

/**
 * 面向业务系统的运行时鉴权服务。
 *
 * <p>对外提供稳定的 Java 注入接口，避免业务侧直接依赖内部应用服务或 Facade 命名。
 */
public interface AuthzRuntimeService {

    /**
     * 执行一次完整鉴权。
     *
     * @param command 鉴权命令
     * @return 鉴权结果
     */
    AuthzCheckResult check(AuthzCheckCommand command);

    /**
     * 按权限编码执行快捷鉴权。
     *
     * <p>适用于业务侧已持有 permCode 的场景，当前主体语义与现有 {@code AuthzFacade.checkByPermCode(...)} 保持一致。
     *
     * @param command 权限编码鉴权命令
     * @return 鉴权结果
     */
    AuthzCheckResult checkByPermissionCode(PermCodeCheckCommand command);
}