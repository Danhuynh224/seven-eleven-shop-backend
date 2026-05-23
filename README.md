# 7-Eleven Vietnam — Shop Backend

REST API backend for the 7-Eleven Vietnam Fresher Java Engineer technical test.
Covers product management, customer ordering, and real-time event streaming.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security 6 + JWT (jjwt 0.12) |
| Database | PostgreSQL 16 + Flyway migrations |
| Cache | Redis 7 (`@Cacheable` / `@CacheEvict`) |
| Messaging | Apache Kafka 3.7 (KRaft — no Zookeeper) |
| ORM | Spring Data JPA + Hibernate |
| Docs | SpringDoc OpenAPI 2.5 / Swagger UI |
| Mapping | MapStruct 1.5.5 + Lombok |
| Build | Maven 3.9, multi-stage Docker |
| CI | GitHub Actions |
| Testing | JUnit 5 + Mockito (10 unit tests) |

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Client / Swagger UI                  │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTP
                        ▼
┌─────────────────────────────────────────────────────────┐
│            Spring Security — JWT Filter                  │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│                  REST Controllers                         │
│  /api/auth  /api/products  /api/orders                   │
│  /api/admin/products  /api/admin/orders  /api/categories │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│              Service Layer (@Transactional)               │
│         @Cacheable / @CacheEvict via Redis               │
└──────────┬──────────────────────────┬────────────────────┘
           │                          │
           ▼                          ▼
┌──────────────────┐       ┌─────────────────────┐
│  PostgreSQL 16   │       │     Redis 7          │
│  (Flyway DDL)    │       │  products: 10 min    │
│  Pessimistic lock│       │  categories: 30 min  │
│  on order create │       └─────────────────────┘
└──────────────────┘
           │
           │ after order saved
           ▼
┌──────────────────────────────────────┐
│   Kafka — topic: order.created        │
│   KRaft mode, 3 partitions            │
│   Fire-and-forget (order TX safe)     │
└──────────────────────────────────────┘
```

---

## Quick Start

### Prerequisites

- Docker Desktop

### Run with Docker Compose

```bash
# 1. Clone the repository
git clone <repo-url>
cd seven-eleven-shop-backend

# 2. Copy env file (defaults work out of the box)
cp .env.example .env

# 3. Start all services (PostgreSQL, Redis, Kafka, Backend)
docker compose up --build
```

Services started:

| Service | URL |
|---------|-----|
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |
| Kafka | localhost:9092 |

### Default Accounts

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | ADMIN |
| `customer1` | `customer123` | CUSTOMER |
| `customer2` | `customer123` | CUSTOMER |

> Passwords are BCrypt-hashed at runtime via `DataInitializerConfig` — no plaintext in SQL or source code.

---

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Login — returns JWT token |
| POST | `/api/auth/register` | Register a new customer |

### Products (Public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/products` | List products (paginated, search, filter by category, sort) |
| GET | `/api/products/{id}` | Get product detail |

### Admin — Products

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/admin/products` | Create product |
| PUT | `/api/admin/products/{id}` | Update product |
| DELETE | `/api/admin/products/{id}` | Soft-delete product |

### Categories

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/categories` | List all categories |

### Orders (Customer)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders` | Place a new order |
| GET | `/api/orders/my` | View my orders (paginated) |

### Admin — Orders

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/orders` | List all orders (filter by status, date range) |
| GET | `/api/admin/orders/{id}` | Get order detail |
| PUT | `/api/admin/orders/{id}/status` | Update order status |

> Full interactive docs at `/swagger-ui.html` — all endpoints are testable directly in the browser.

---

## Example: Place an Order

```bash
# 1. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"customer1","password":"customer123"}'
# Response: { "token": "eyJ..." }

# 2. Place order
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"productId": 1, "quantity": 2},
      {"productId": 3, "quantity": 1}
    ],
    "note": "no ice"
  }'
```

---

## Design Highlights

### Oversell Prevention
Product stock is locked with `PESSIMISTIC_WRITE` before each order, preventing race conditions under concurrent requests.

### Price Snapshot
Unit price is captured from the database at order time — never trusted from the client. Ensures historical order accuracy even when product prices change later.

### Kafka — Fire-and-Forget
An `OrderCreatedEvent` is published to Kafka after each successful order. The publish is wrapped in a try-catch so a Kafka outage never rolls back the order transaction.

### Redis Cache
`GET /api/products/{id}` and `GET /api/categories` are cached. Cache is evicted on any product write (create / update / delete). TTL: products 10 min, categories 30 min.

### Soft Delete
Products are soft-deleted via a `deleted_at` timestamp. All public queries filter `WHERE deleted_at IS NULL`, preserving referential integrity on existing orders.

### Security
Stateless JWT authentication (no server-side session). Role-based access control: `CUSTOMER` vs `ADMIN`. Non-root Docker user.

---

## Running Tests

```bash
mvn test
```

10 unit tests covering `OrderService` and `ProductService`: stock decrement, oversell guard, multi-item total calculation, cache eviction. Uses Mockito — no external dependencies required.

---

## Project Structure

```
src/
├── main/java/vn/sevenleven/shop/
│   ├── config/       # Security, Redis, Kafka, JPA auditing, OpenAPI
│   ├── controller/   # REST controllers (public + admin)
│   ├── dto/          # Request/Response records (no entity exposure)
│   ├── entity/       # JPA entities with BaseEntity auditing
│   ├── enums/        # Role, OrderStatus
│   ├── event/        # Kafka event records
│   ├── exception/    # GlobalExceptionHandler, custom exceptions
│   ├── kafka/        # OrderEventProducer, OrderEventConsumer
│   ├── mapper/       # MapStruct mappers
│   ├── repository/   # Spring Data JPA repositories
│   └── service/      # Service interfaces + implementations
├── main/resources/
│   ├── application.yml         # Base config
│   ├── application-dev.yml     # Local dev overrides
│   ├── application-docker.yml  # Docker network hostnames
│   └── db/migration/           # Flyway SQL migrations (V1 DDL, V2 seed)
└── test/java/vn/sevenleven/shop/
    └── service/                # Unit tests (Mockito)
```

---

## CI/CD

GitHub Actions runs on every push to `main` / `develop`:

1. **Build & Test** — `mvn test` (JDK 17 Temurin, Maven cache)
2. **Build Docker Image** — validates the multi-stage Dockerfile builds successfully
