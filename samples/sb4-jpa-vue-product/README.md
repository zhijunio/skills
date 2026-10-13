# Spring Boot + Vue Product Inventory

A full-stack product catalog built with Spring Boot 4, Java 25, Vue 3, PostgreSQL, and Docker Compose.

## Features

- Product create, read, update, and delete API with paging
- Request validation and RFC 9457 Problem Details errors
- PostgreSQL schema managed by Flyway; Hibernate validates the schema at startup
- Vue inventory interface with search, stock summary, and CRUD forms
- Dockerfile builds the Vue assets and Spring Boot application into a small runtime image

## Run locally

Prerequisites: Java 25, Docker, and Node.js 24 or newer.

```sh
cp .env.sample .env
docker compose up -d postgres
./mvnw spring-boot:run
```

In another terminal:

```sh
cd frontend
npm ci
npm run dev
```

Open <http://localhost:5173>. Vite proxies `/api` requests to Spring Boot on port 8080.

## Run the full stack in Docker

```sh
docker compose -f compose.full.yaml up --build
```

Open <http://localhost:8080>. Compose starts PostgreSQL, waits for its health check, then starts the application.

## Verify

```sh
cd frontend && npm run lint:check && npm run test:unit -- --run && npm run build
cd .. && ./mvnw verify
docker compose config
docker compose -f compose.full.yaml config
```

`./mvnw verify` runs Java unit tests, PostgreSQL-backed integration tests (Testcontainers), and the frontend lint, unit tests, and production build.

GitHub Actions runs the same verification on pushes and pull requests, then builds the Docker image without publishing it. Deployment is intentionally left to the target environment's release workflow.

With the optional [Task CLI](https://taskfile.dev/installation/) installed, run `task --list` to see the wrapper-backed shortcuts in `Taskfile.yml`, including `task verify`, `task frontend:dev`, and `task docker:up`.

## API

| Method | Path | Result |
| --- | --- | --- |
| `GET` | `/api/products?page=0&size=20` | Paged products |
| `GET` | `/api/products/{id}` | Product details |
| `POST` | `/api/products` | Create; returns `201` and `Location` |
| `PUT` | `/api/products/{id}` | Replace product details |
| `DELETE` | `/api/products/{id}` | Delete; returns `204` |

Product fields: `name` (1–120 characters), `sku` (2–32 uppercase letters, digits, or hyphens), `price` (non-negative, up to two decimal places), and `quantity` (non-negative integer). SKUs are unique.
