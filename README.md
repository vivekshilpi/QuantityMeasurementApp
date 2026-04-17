# Quantity Measurement Platform

This repository now runs the Quantity Measurement App as a Spring Boot microservices system without changing the original business behavior.

## Services

- `service-registry` runs Eureka on `8761`
- `api-gateway` exposes the public API on `8080`
- `auth-service` handles register, login, Google login, JWT, and `/api/v1/auth/**`
- `quantity-service` handles quantity operations and history under `/api/v1/quantities/**`
- `quantity-common` contains shared DTOs, exceptions, enums, and measurement logic

## Legacy Archive

The original monolith source has been moved to `legacy/monolith-archive/src`.

## Local Run Guide

1. Start MySQL and create two databases:
   - `quantitymeasurement_auth`
   - `quantitymeasurement_quantity`
2. Start the service registry:

```bash
mvn -pl service-registry spring-boot:run
```

3. Start the auth service in a new terminal:

```bash
mvn -pl auth-service -am spring-boot:run
```

4. Start the quantity service in another terminal:

```bash
mvn -pl quantity-service -am spring-boot:run
```

5. Start the API gateway in another terminal:

```bash
mvn -pl api-gateway -am spring-boot:run
```

6. Open the system:
   - Eureka dashboard: `http://localhost:8761`
   - Gateway entry point: `http://localhost:8080`
   - Auth service docs: `http://localhost:8081/swagger-ui/index.html`
   - Quantity service docs: `http://localhost:8082/swagger-ui/index.html`

## Docker Run Guide

Start the full stack with Docker Compose:

```bash
docker compose up --build
```

Useful URLs after startup:
- Eureka dashboard: `http://localhost:8761`
- Gateway entry point: `http://localhost:8080`
- Auth service docs: `http://localhost:8081/swagger-ui/index.html`
- Quantity service docs: `http://localhost:8082/swagger-ui/index.html`

Stop the stack:

```bash
docker compose down
```

Remove containers and database volume:

```bash
docker compose down -v
```

## Build

Compile all modules:

```bash
mvn -DskipTests compile
```

Package all services:

```bash
mvn -DskipTests package
```

## Notes

- The gateway preserves the public API paths from the monolith.
- `auth-service` and `quantity-service` each use their own database schema.
- Google OAuth environment variables are optional unless you want Google login enabled.
