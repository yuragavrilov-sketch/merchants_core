# merchants-core

Domain microservice for merchant lifecycle management.

## Stack

- Java 21
- Spring Boot 4
- Spring Cloud Config client
- Spring Cloud Vault Config
- Maven

## Configuration

`merchants-core` follows the workspace-wide [Java microservice configuration standard](../docs/standards/java-microservice-configuration-standard.md).

- `application.yml` contains common defaults and `spring.config.import`.
- `application-local.yml` contains local datasource defaults and disables Config Server/Vault unless explicitly enabled.
- `application-test.yml` selects the shared test environment and enables Config Server/Vault.
- `application-prod.yml` selects the production environment and enables Config Server/Vault.
- `src/test/resources/application-test.yml` contains the H2 configuration used by automated tests.

Config Server provides non-secret settings. Vault provides secrets such as `spring.datasource.password` and `merchants-core.internal-admin.api-key`.
`pay.environment` drives both the Config Server label and Vault context paths:

```text
pay/{environment}/merchants-core-db-password
pay/{environment}/merchants-core-internal-admin-key
```

For test/prod deployments, imports should be mandatory:

```text
PAY_ENVIRONMENT=test
CONFIG_SERVER_ENABLED=true
VAULT_ENABLED=true
SPRING_CONFIG_IMPORT=configserver:${CONFIG_SERVER_URL},vault://
```

No `bootstrap.yml` is used.

### Oracle NLS character set (corp deployment)

The corporate (TKB) test Oracle runs the **CL8MSWIN1251** (Windows-1251 / Cyrillic)
character set. The thin `ojdbc11` driver bundles only a minimal charset set and
throws `ORA-17056: Non-supported character set` when it reads result metadata
against such a database. The runtime dependency `com.oracle.database.nls:orai18n`
ships the NLS charset data and is therefore required in this contour. The bug is
**runtime-only** and invisible locally (Oracle XE runs `AL32UTF8`). Any new Spring
service that connects to this Oracle must include `orai18n`.

## Run

### Docker Compose contour

The full local contour is owned by [`../infra/docker-compose.yaml`](../infra/docker-compose.yaml).

```powershell
cd ..\infra
docker compose up -d --build merchants-core
```

The container uses `SPRING_PROFILES_ACTIVE=compose`, Config Server label
`compose`, Oracle at `oracle:1521`, and Vault secrets under
`pay/compose/merchants-core-*`.

### Локальный dev-стек

Локальный Oracle, Postgres, Keycloak, Redis и toolbox поднимаются через [`../infra/docker-compose.yaml`](../infra/docker-compose.yaml) (это общий dev-стек на весь NEW_PAY).

```powershell
cd ..\infra
docker compose up -d oracle     # Postgres + Keycloak нужны только для paylimit и live-auth
```

Контейнер `pay-oracle` поднимает Oracle XE 21 в PDB `XEPDB1`, создаёт пользователя `payments`/`dev` и прогоняет `infra/oracle-init/` (schema + seed ~4500 мерчантов).

### Старт сервиса

`merchants-core` слушает **8082** — это порт, который ждёт `payadmin-bff` (см. `merchants-core.base-urls.dev` в его `application.yml`). На 8080 в dev-стеке висит Keycloak.

```powershell
$env:ORACLE_DB_URL='jdbc:oracle:thin:@//localhost:1521/XEPDB1'
$env:ORACLE_DB_USERNAME='payments'
$env:ORACLE_DB_PASSWORD='dev'
mvn spring-boot:run
```

Локальный профиль стартует без Config Server и Vault. Если нужно проверить интеграцию с внешней конфигурацией локально:

```powershell
$env:PAY_ENVIRONMENT='local'
$env:CONFIG_SERVER_ENABLED='true'
$env:VAULT_ENABLED='true'
$env:CONFIG_SERVER_URL='http://pay-payconfig-server:8080'
$env:SPRING_CLOUD_VAULT_TOKEN='dev-vault-token'
mvn spring-boot:run
```

Проверка: `curl http://localhost:8082/internal/v1/admin/merchants?limit=3`.

Если задан `INTERNAL_ADMIN_API_KEY`, все `/internal/**` endpoints требуют этот ключ в заголовке `X-Internal-Admin-Key` (или в имени из `INTERNAL_ADMIN_API_KEY_HEADER`):

```powershell
$env:INTERNAL_ADMIN_API_KEY='dev-secret'
curl -H 'X-Internal-Admin-Key: dev-secret' http://localhost:8082/internal/v1/admin/merchants?limit=3
```

## Swagger / OpenAPI

- Swagger UI: `http://localhost:8082/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8082/v3/api-docs`

## Health / Observability

- Liveness/health: `http://localhost:8082/actuator/health`
- Readiness: `http://localhost:8082/actuator/health/readiness`
- Metrics endpoint is exposed at `/actuator/metrics`.
- Prometheus endpoint is exposed at `/actuator/prometheus`.

## Build and test

```bash
mvn clean verify
```

## Current API

- `GET /api/v1/merchants?limit=100&offset=0&search=alpha&sortBy=name&sortDir=asc`
- `GET /api/v1/merchants/configurations/active-line?limit=100&offset=0&search=inn&sortBy=mercId&sortDir=asc`
- `GET /api/v1/merchants/{merchantId}`
- `GET /api/v1/merchants/{merchantId}/configurations?at=2026-04-24T12:00:00Z&limit=100&offset=0&search=name&sortBy=parameterName&sortDir=asc`
- `GET /api/v1/merchants/{merchantId}/configuration-history?limit=100&offset=0&search=inn&sortBy=dateBegin&sortDir=asc`

## Internal Admin API

Read-only endpoints for Admin BFF:

- `GET /internal/v1/admin/merchants?limit=100&offset=0&search=alpha&sortBy=name&sortDir=asc`
- `GET /internal/v1/admin/merchants/{merchantId}`

The Admin BFF selects the target `merchants-core` instance by environment (`dev`, `test`, `prod`). The selected `merchants-core` instance does not receive an `env` parameter and works only with its own database.

If `at` is omitted, current UTC time is used.

`/configurations/active-line` also uses application UTC clock, not Oracle session-local `CURRENT_TIMESTAMP`.

`/configuration-history` returns an array of full configuration snapshots with validity period (`dateBegin`, `dateEnd`, `configuration`).

`/configurations/active-line` returns merchants with current config as JSON object (`configuration`).

Common query params for list endpoints: `limit`, `offset`, `search`, `sortBy`, `sortDir`.

All successful responses are standardized with envelope:

```json
{
  "data": [],
  "meta": {
    "limit": 100,
    "offset": 0,
    "count": 10,
    "search": "alpha",
    "sortBy": "name",
    "sortDir": "asc",
    "at": null
  },
  "error": null,
  "timestamp": "2026-04-24T18:00:00Z"
}
```
