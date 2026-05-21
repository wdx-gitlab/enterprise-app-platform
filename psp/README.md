# PSP 权限引擎 (authz-engine-spring-boot-starter)

## 模块说明

PSP（Permission Service Platform）是企业应用平台的权限引擎模块，提供以下核心能力：

- 权限元模型（Action / 策略模板 / BO 元模型）管理
- 主体目录（用户 / 组织 / 岗位 / 角色 / 用户组 / 主体关系）管理
- 资源目录（菜单 / 页面 / 组件 / API）管理
- 权限项与授权分配管理
- 派生权限与委托授权
- 行/列数据过滤（PEP 切面）
- 鉴权决策 API
- 审计日志

## Runtime Baseline

- Java 8
- Spring Boot 2.3.x
- MySQL 8.0 compatible instance
- MyBatis-Plus 3.3.2
- Flyway 6.x

## Auto-configuration Entry

```
com.ruijie.authzengine.autoconfigure.AuthzEngineAutoConfiguration
```

## 专属数据源配置（宿主应用 application.properties）

```properties
authz.engine.datasource.url=jdbc:mysql://localhost:3306/authz_engine?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
authz.engine.datasource.username=root
authz.engine.datasource.password=your_password
authz.engine.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

## Build

从 `enterprise-app-platform/` 执行：

```powershell
mvn -pl psp -DskipTests package
```
