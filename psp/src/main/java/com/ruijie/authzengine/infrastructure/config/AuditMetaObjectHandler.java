package com.ruijie.authzengine.infrastructure.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;

/**
 * 审计字段自动填充器。
 * <p>不注册为 @Component，由 authz-engine starter 以具名 bean {@code authzMetaObjectHandler} 注入，
 * 避免与宿主项目的 MetaObjectHandler 产生 bean 冲突。
 */
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // BaseEntity 标准字段
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createdBy", String.class, "system");
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedBy", String.class, "system");
        strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
        // usp_menu_item 独立审计字段（不继承 BaseEntity）
        strictInsertFill(metaObject, "createdTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedTime", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);
        strictUpdateFill(metaObject, "updatedBy", String.class, "system");
        // usp_menu_item 独立审计字段
        strictUpdateFill(metaObject, "updatedTime", LocalDateTime.class, now);
    }
}