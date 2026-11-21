# Banking Transaction System

Backend y frontend de un sistema bancario **simulado**, construido como proyecto educativo para aprender Java, Spring Boot, Angular, seguridad, persistencia, transacciones, concurrencia y Docker.

> **Aviso:** este proyecto no procesa dinero real, no se conecta con bancos reales y no utiliza datos financieros reales.

## Contenido

- [Características](#características)
- [Arquitectura](#arquitectura)
- [Stack tecnológico](#stack-tecnológico)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Requisitos](#requisitos)
- [Configuración](#configuración)
- [Ejecutar todo con Docker](#ejecutar-todo-con-docker)
- [Ejecutar en desarrollo](#ejecutar-en-desarrollo)
- [Usar la aplicación](#usar-la-aplicación)
- [API principal](#api-principal)
- [Testing](#testing)
- [Seguridad](#seguridad)
- [Decisiones técnicas](#decisiones-técnicas)
- [Solución de problemas](#solución-de-problemas)

## Características

### Backend

- Registro e inicio de sesión.
- Contraseñas protegidas con BCrypt.
- JWT stateless con HS256.
- Roles `CUSTOMER` y `ADMIN`.
- Cuentas en USD, EUR y GBP.
- Saldos con `BigDecimal` y PostgreSQL `NUMERIC(19,4)`.
- Depósitos simulados exclusivos para administradores.
- Transferencias atómicas entre cuentas.
- Validación de saldo, moneda, propietario y cuentas iguales.
- Idempotencia mediante `Idempotency-Key`.
- Ledger de créditos y débitos.
- Historial paginado de movimientos.
- Bloqueos pesimistas para evitar sobregiros bajo concurrencia.
- Migraciones de base de datos con Flyway.
- Manejo uniforme de errores HTTP.
- Health check con Spring Actuator.
- Documentación interactiva con OpenAPI y Swagger UI.

### Frontend

- Angular standalone con TypeScript.
- Registro e inicio de sesión.
- Persistencia del JWT en el navegador.
- Interceptor que añade automáticamente `Authorization: Bearer ...`.
- Protección de rutas autenticadas.
- Dashboard de cuentas y saldos.
- Creación de cuentas.
- Formulario de depósitos simulados para administradores.
- Formulario de transferencias.
- Visualización del ledger.
- Diseño responsive.
- Nginx para servir la aplicación en Docker.

## Arquitectura

El repositorio es un monorepo con dos aplicaciones independientes:

```text
                    ┌────────────────────┐
                    │ Angular + Nginx    │
                    │ :4200              │
                    └─────────┬──────────┘
                              │ /api proxy
                    ┌─────────▼──────────┐
                    │ Spring Boot       │
                    │ :8080              │
                    └─────────┬──────────┘
                              │ JDBC
                    ┌─────────▼──────────┐
                    │ PostgreSQL 17      │
                    │ :5432              │
                    └────────────────────┘
```

El backend está organizado por funcionalidad:

```text
backend/src/main/java/com/example/banking/
├── auth/
├── accounts/
├── transfers/
├── ledger/
└── shared/
```

Cada módulo separa responsabilidades:

```text
api            Controladores y DTOs HTTP
application    Casos de uso y servicios
 domain        Entidades y reglas del dominio
infrastructure Repositorios y acceso a datos
```

## Stack tecnológico

| Área | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 4.2 |
| Seguridad | Spring Security 7, JWT HS256, BCrypt |
| Persistencia | Spring Data JPA, Hibernate 7 |
| Base de datos | PostgreSQL 17 |
| Migraciones | Flyway |
| API | REST, OpenAPI, Swagger UI |
| Frontend | Angular 21, TypeScript |
| Servidor frontend | Nginx |
| Testing backend | JUnit, Spring Boot Test, H2, Testcontainers |
| Automatización | Maven Wrapper, npm, GitHub Actions |
| Contenedores | Docker Compose |

## Estructura del repositorio

```text
banking-transaction-system/
├── backend/
│   ├── pom.xml
│   ├── mvnw
│   ├── Dockerfile
│   ├── .dockerignore
│   └── src/
├── frontend/
│   ├── package.json
│   ├── package-lock.json
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── proxy.conf.json
│   ├── tsconfig*.json
│   └── src/
├── compose.yaml
├── .env.example
├── .github/workflows/ci.yml
└── README.md
```

## Requisitos

Para ejecutar el proyecto localmente necesitas:

- Java 21.
- Node.js 22 o superior.
- npm.
- Docker y Docker Compose.
- Maven no es necesario: el backend incluye Maven Wrapper.

Comprobar versiones:

```bash
java -version
node --version
npm --version
docker --version
```

## Configuración

Copia el archivo de ejemplo:

```bash
cp .env.example .env
```

Variables principales:

```dotenv
POSTGRES_DB=banking
POSTGRES_USER=banking
POSTGRES_PASSWORD=banking
JWT_SECRET=replace-this-with-a-long-random-secret
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=change-this-development-password
```

Para desarrollo usa un secreto JWT largo y aleatorio. Los valores del ejemplo son únicamente para desarrollo local.

## Ejecutar todo con Docker

Desde la raíz del repositorio:

```bash
docker compose up --build
```

También puedes ejecutarlo en segundo plano:

```bash
docker compose up --build -d
```

Servicios:

| Servicio | URL |
|---|---|
| Frontend | http://localhost:4200 |
| Backend | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| PostgreSQL | localhost:5432 |

Ver estado:

```bash
docker compose ps
```

Ver logs:

```bash
docker compose logs -f app
docker compose logs -f frontend
docker compose logs -f postgres
```

Detener los servicios:

```bash
docker compose down
```

> `docker compose down` no elimina el volumen de PostgreSQL. Para eliminar también los datos simulados usa `docker compose down -v`.

Dentro de Compose, el backend se conecta a PostgreSQL usando:

```text
jdbc:postgresql://postgres:5432/banking
```

El frontend se conecta al backend a través del proxy Nginx usando `/api`.

## Ejecutar en desarrollo

### Backend

Desde la raíz:

```bash
cd backend
./mvnw spring-boot:run
```

Por defecto, el backend local usa:

```text
jdbc:postgresql://localhost:5432/banking
```

### Frontend

Desde la raíz:

```bash
cd frontend
npm install
npm start
```

Abre:

```text
http://localhost:4200
```

En desarrollo, `frontend/proxy.conf.json` redirige las peticiones `/api` al backend local en `http://localhost:8080`.

## Usar la aplicación

### 1. Registrar un cliente

En el frontend abre:

```text
http://localhost:4200
```

Selecciona **Regístrate** y crea un usuario con una contraseña de al menos 8 caracteres.

También puedes usar la API:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"cliente@example.com","password":"password123"}'
```

### 2. Crear una cuenta

Desde el dashboard selecciona una moneda y pulsa **Crear cuenta**.

Por API:

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"currency":"USD"}'
```

### 3. Crear administrador de desarrollo

Configura las variables antes de iniciar Compose:

```bash
export ADMIN_EMAIL=admin-demo@example.com
export ADMIN_PASSWORD=AdminDemo123!
docker compose up --build -d app frontend
```

El administrador se crea únicamente si el email todavía no existe. Cambiar las variables después no modifica un administrador existente.

Inicia sesión con esas credenciales para ver la herramienta **Depósito simulado**.

### 4. Depositar fondos simulados

Los depósitos no representan dinero real y requieren rol `ADMIN`:

```bash
curl -X POST http://localhost:8080/api/v1/simulations/deposits \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"accountId":1,"amount":100.00}'
```

### 5. Transferir

La transferencia requiere que el usuario autenticado sea propietario de la cuenta origen:

```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: transfer-001' \
  -d '{"sourceAccountId":1,"destinationAccountId":2,"amount":25.00,"currency":"USD"}'
```

Repetir la misma petición con la misma clave no vuelve a mover el dinero.

### 6. Consultar el ledger

```bash
curl 'http://localhost:8080/api/v1/accounts/1/transactions?page=0&size=20&sort=createdAt,desc' \
  -H "Authorization: Bearer $TOKEN"
```

Los parámetros son de consulta; no se envía un JSON en el body.

## API principal

| Método | Endpoint | Acceso |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Público |
| `POST` | `/api/v1/auth/login` | Público |
| `GET` | `/api/v1/accounts` | Autenticado |
| `POST` | `/api/v1/accounts` | Autenticado |
| `GET` | `/api/v1/accounts/{id}` | Autenticado |
| `POST` | `/api/v1/simulations/deposits` | `ADMIN` |
| `POST` | `/api/v1/transfers` | Propietario de origen |
| `GET` | `/api/v1/accounts/{id}/transactions` | Propietario o `ADMIN` |
| `GET` | `/actuator/health` | Público |

La especificación completa está disponible en:

```text
http://localhost:8080/v3/api-docs
```

La interfaz interactiva está disponible en:

```text
http://localhost:8080/swagger-ui.html
```

## Testing

### Tests rápidos del backend

```bash
cd backend
./mvnw test
```

Estos tests usan H2 y comprueban:

- Contexto de Spring.
- Persistencia de cuentas.
- Registro, login y JWT.
- Autorización HTTP.
- Transferencias.
- Idempotencia.
- Concurrencia y ausencia de sobregiro.

### Test de PostgreSQL real

```bash
cd backend
./mvnw verify -Pintegration
```

Este perfil ejecuta `PostgresMigrationIT` con Testcontainers, Flyway y PostgreSQL real.

Testcontainers necesita que la JVM tenga acceso directo a Docker. Un Docker Context SSH puede funcionar para el CLI, pero no ser detectado automáticamente por Testcontainers.

### Build del frontend

```bash
cd frontend
npm run build
```

### Auditoría de dependencias de producción

```bash
cd frontend
npm audit --omit=dev --audit-level=high
```

### Integración continua

GitHub Actions está definido en:

```text
.github/workflows/ci.yml
```

El workflow ejecuta:

1. Tests Maven del backend.
2. Tests de integración PostgreSQL.
3. Instalación y build del frontend.

## Seguridad

- Las sesiones HTTP son stateless.
- El JWT se firma con HS256.
- Las contraseñas nunca se almacenan en texto plano.
- El claim JWT `role` se convierte en autoridades `ROLE_CUSTOMER` y `ROLE_ADMIN`.
- Las rutas de simulación requieren `ADMIN`.
- Las transferencias verifican la propiedad de la cuenta origen.
- Los balances no se almacenan en el frontend como fuente autoritativa.
- El frontend adjunta el JWT mediante un interceptor HTTP.
- El secreto JWT debe cambiarse en cualquier entorno que no sea desarrollo.

## Decisiones técnicas

### Dinero

Se usa `BigDecimal` en Java y `NUMERIC(19,4)` en PostgreSQL. No se usa `double` para balances.

### Transferencias

Las transferencias se ejecutan dentro de una transacción. Las cuentas origen y destino se bloquean con `PESSIMISTIC_WRITE` y se bloquean en orden estable por ID para reducir deadlocks.

### Idempotencia

Cada transferencia requiere `Idempotency-Key`. El backend guarda un hash del payload junto al actor y la operación:

- Misma clave y mismo payload: devuelve el resultado original.
- Misma clave y payload diferente: responde `409 Conflict`.

### Ledger

Cada operación crea entradas de ledger:

- `CREDIT`: fondos recibidos.
- `DEBIT`: fondos enviados.

El balance y el ledger se persisten en PostgreSQL dentro de la misma transacción.

### Migraciones

Flyway controla el esquema mediante migraciones inmutables ubicadas en:

```text
backend/src/main/resources/db/migration/
```

## Solución de problemas

### Swagger devuelve `401`

Inicia sesión nuevamente y usa el `accessToken` actual. En Swagger, si el esquema Bearer ya está configurado, pega solamente el token cuando el formulario lo indique.

### El depósito devuelve `403` o `insufficient_scope`

Debes iniciar sesión con un usuario cuyo token tenga:

```json
"role": "ADMIN"
```

Un usuario `CUSTOMER` puede ver sus cuentas, pero no puede hacer depósitos simulados.

### La transferencia devuelve saldo insuficiente

Primero aplica un depósito simulado con el administrador. Después vuelve a autorizar el frontend con el token del cliente propietario de la cuenta origen.

### El ledger devuelve `500`

Usa este formato:

```text
/api/v1/accounts/1/transactions?page=0&size=20&sort=createdAt,desc
```

No envíes:

```text
sort=["createdAt"]
```

### El frontend no carga datos

Comprueba:

```bash
curl http://localhost:8080/actuator/health
```

Si usas desarrollo local, verifica que `frontend/proxy.conf.json` apunta al backend correcto. Si usas Docker, Nginx redirige `/api` al servicio `app` automáticamente.

### El frontend muestra una versión antigua

Haz una recarga completa del navegador:

```text
macOS: Cmd + Shift + R
Windows/Linux: Ctrl + Shift + R
```

### Reiniciar completamente la base de datos simulada

> Esto elimina todos los usuarios, cuentas, transferencias y movimientos.

```bash
docker compose down -v
docker compose up --build
```

## Estado del proyecto

El proyecto cubre el flujo completo de aprendizaje:

- Backend REST seguro.
- Persistencia PostgreSQL.
- Transacciones e idempotencia.
- Concurrencia.
- Frontend Angular.
- Docker Compose.
- CI automatizada.
- Documentación OpenAPI.

Las siguientes mejoras serían extensiones futuras, no requisitos del flujo actual:

- Refresh tokens.
- Recuperación de contraseña.
- Verificación de email.
- Rate limiting.
- Auditoría administrativa avanzada.
- Observabilidad con métricas y trazas.
- Despliegue HTTPS con dominio.
