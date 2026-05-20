# USP Portal Backend

## Runtime Baseline

- Java 8
- Maven 3.8+
- MySQL 8.0 compatible instance
- Default HTTP port: `8080`
- Context path: `/usp-portal`

## Verified Local Environment

- Active database: `jdbc:mysql://k8s-bj-test-nodeports.ruijie.com.cn:31563/unify_engine`
- Username: `root`
- Password: `d934ba4dba9c4282803531cda85664c9`
- Module default profile: `dev`
- Host demo effective profile: `uat`

`usp-portal` source now keeps only the development-side datasource defaults in `application-dev.properties`. When the jar is embedded into a host application, the host must provide its own `rj.unify.engine.datasource.*` values in the host's active profile or externalized configuration.

`usp-portal` no longer reads `spring.datasource.*` for its own persistence layer. Portal MyBatis mappers and Flyway migration are now bound to the dedicated `rj.unify.engine.datasource.*` namespace so the module can stay isolated from a host application's business datasource.

## Default Account

- Login name: `admin`
- Password: `Admin@123456`
- Tenant code: `_PLATFORM_`

## Start

From `enterprise-app-platform/`:

```powershell
mvn -pl usp -Pexec-jar -DskipTests package
env=dev java -jar usp/target/usp-portal-1.0.0-SNAPSHOT.jar
```

After startup, backend base URL is:

```text
http://localhost:8080/usp-portal
```

## Build And Test

Run automated backend tests:

```powershell
mvn test
```

Build the executable jar:

```powershell
mvn -Pexec-jar -DskipTests package
```

Generated artifact:

```text
usp-portal/target/usp-portal-1.0.0-SNAPSHOT.jar
```

## Configuration Reference

For a consolidated explanation of the backend, host-side, and frontend runtime settings used by `usp-portal`, see [../doc/usp-portal-config-reference.md](../doc/usp-portal-config-reference.md).

## Verified APIs

The following endpoints were verified against the running service:

- `GET /usp-portal/api/auth/login-options`
- `POST /usp-portal/api/auth/login`
- `GET /usp-portal/api/auth/me`
- `GET /usp-portal/api/tenants`

## Important Runtime Note

If you modify backend classes while an older backend process is still running, the frontend may hit stale-class errors such as `NoSuchMethodError` on `/api/navigation/tree`. In that case, stop the old backend process and start it again before rechecking the page.

## CRM Host Joint Debug

For the combined `crm-server-demo` + `crm-web-demo` local debug flow, prefer the root script instead of starting services manually:

```powershell
./scripts/start-crm-demo.ps1
```

The script clears the fixed local ports, rebuilds the backend jar when needed, starts `crm-server-demo` on `8080`, starts `crm-web-demo` on `5173`, and waits until the host proxy path is reachable.

To stop the processes started by the script:

```powershell
./scripts/stop-crm-demo.ps1
```

This avoids the common local drift caused by stale backend jars, duplicate frontend dev servers, or port conflicts on `8080` and `5173`.

## Unify Engine Datasource Config

For the dedicated Portal datasource property set and host-side configuration guidance, see `doc/usp-unify-engine-datasource-config.html`.
