# 📦 Kafka Order Processing System

A production-aware, event-driven microservices system built with **Apache Kafka**, **Spring Boot 3**, and **Docker**. Simulates a real e-commerce order flow where placing an order triggers asynchronous email confirmation and payment processing across independent services.

---

## 🏗️ Architecture

```
┌─────────────────┐         ┌─────────────────────┐
│   REST Client   │──POST──▶│     Order API        │
│  (Postman/curl) │         │   (Spring Boot)      │
└─────────────────┘         └────────┬────────────┘
                                      │ publishes
                                      ▼
                            ┌─────────────────────┐
                            │    Kafka Broker      │
                            │   [orders topic]     │
                            │   3 partitions       │
                            └──────┬──────┬───────┘
                                   │      │
                     ┌─────────────┘      └──────────────┐
                     ▼                                    ▼
          ┌──────────────────┐               ┌──────────────────────┐
          │  Email Service   │               │   Payment Service    │
          │  (Spring Boot)   │               │   (Spring Boot)      │
          │                  │               │                      │
          │ • Sends HTML     │               │ • Persists to        │
          │   confirmation   │               │   PostgreSQL         │
          │   via Mailtrap   │               │ • Idempotency check  │
          └──────────────────┘               │ • REST API to query  │
                                             └──────────────────────┘
                                                        │
                                             ┌──────────▼──────────┐
                                             │     PostgreSQL       │
                                             │   (payments table)  │
                                             └─────────────────────┘
```

**On failure:** Both services retry 3 times with 2-second backoff. After exhausting retries, failed messages are routed to `orders.DLT` (Dead Letter Topic) for investigation — no messages are lost and no queues are blocked.

---

## ✨ Features

- **Event-driven architecture** — services communicate exclusively through Kafka, with zero direct coupling between them
- **Concurrent consumers** — each service runs 3 consumer threads, one per partition, for parallel processing
- **Dead Letter Topic handling** — failed messages automatically routed to `orders.DLT` after retries, with full metadata headers (partition, offset, exception)
- **Idempotent payment processing** — database-level unique constraint on `orderId` prevents duplicate charges if Kafka redelivers a message
- **HTML email notifications** — formatted order confirmation emails via Mailtrap SMTP
- **ProblemDetail error responses** — RFC 9457 compliant error format across all REST endpoints
- **Payment status API** — `GET /payments/{orderId}` and `GET /payments` endpoints with proper HTTP semantics
- **Global exception handling** — `@RestControllerAdvice` with structured logging (WARN for expected errors, ERROR with full stack trace for unexpected)
- **Full test suite** — unit tests (Mockito), web layer tests (MockMvc), and integration tests (Testcontainers with real Kafka + Postgres)

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Messaging | Apache Kafka (KRaft mode — no Zookeeper) |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Email | Spring Mail + Mailtrap SMTP |
| Containerization | Docker + Docker Compose |
| Testing | JUnit 5, Mockito, MockMvc, Testcontainers, Awaitility |
| Build tool | Maven (multi-module) |
| Observability | Kafka UI (topic browser + consumer group monitor) |

---

## 📁 Project Structure

```
first-kafka-order-project/
│
├── docker-compose.yml          # Orchestrates all services
│
├── service/                    # Order API (producer)
│   ├── controller/             # REST endpoint (POST /orders)
│   ├── service/                # Business logic
│   ├── producer/               # KafkaTemplate publisher
│   ├── model/                  # OrderEvent + OrderItem
│   ├── config/                 # Topic configuration (3 partitions)
│   └── dtos/                   # OrderRecord request DTO
│
├── email-service/              # Email consumer
│   ├── consumer/               # @KafkaListener (concurrency=3)
│   ├── service/                # HTML email builder + JavaMailSender
│   ├── config/                 # Error handler + DLT publisher
│   └── model/                  # OrderEvent (local copy)
│
└── payment-service/            # Payment consumer + REST API
    ├── consumer/               # @KafkaListener (concurrency=3)
    ├── service/                # Payment processing + idempotency
    ├── controller/             # GET /payments endpoints
    ├── repository/             # Spring Data JPA
    ├── model/                  # Payment entity + OrderEvent
    ├── dto/                    # PaymentResponse record
    ├── exception/              # GlobalExceptionHandler + ProblemDetail
    └── config/                 # Error handler + DLT publisher
```

---

## 🚀 Running Locally

### Prerequisites

- Docker Engine 24+ and Docker Compose
- Java 21
- Maven 3.9+

### 1. Clone the repository

```bash
git clone https://github.com/yourusername/first-kafka-order-project.git
cd first-kafka-order-project
```

### 2. Configure email credentials

In `docker-compose.yml`, replace the Mailtrap placeholders under the `email-service` block:

```yaml
environment:
  MAIL_USERNAME: your-mailtrap-username
  MAIL_PASSWORD: your-mailtrap-password
```

Sign up free at [mailtrap.io](https://mailtrap.io) to get credentials. Emails are caught in a sandbox inbox — nothing is delivered to real addresses.

### 3. Start everything

```bash
docker compose up --build
```

This starts: Kafka (KRaft), Kafka UI, PostgreSQL, Order API, Email Service, Payment Service.

### 4. Verify services are running

| Service | URL |
|---|---|
| Order API | http://localhost:8080 |
| Payment API | http://localhost:8082 |
| Kafka UI | http://localhost:8090 |
| PostgreSQL | localhost:5433 |

---

## 📬 API Usage

### Place an order

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerEmail": "user@example.com",
    "totalAmount": 99.90,
    "items": [
      {
        "productId": "p1",
        "productName": "Clean Code",
        "quantity": 1,
        "unitPrice": 99.90
      }
    ]
  }'
```

**Response:** `202 Accepted`
```
Order received: abc-123-uuid
```

### Query payment status

```bash
# Single payment
curl http://localhost:8082/payments/{orderId}

# All payments
curl http://localhost:8082/payments
```

**Response:** `200 OK`
```json
{
  "paymentId": "ef0c8222-58f5-46a2-b302-aaf81d46ec91",
  "orderId": "abc-123-uuid",
  "customerEmail": "user@example.com",
  "amount": 99.90,
  "status": "SUCCESS",
  "processedAt": "2026-03-19T20:35:40"
}
```

### Trigger a DLT failure (for testing)

Send an order with `"fail"` in the email to simulate payment failure:

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerEmail": "fail@test.com",
    "totalAmount": 50.00,
    "items": []
  }'
```

Watch the logs — you'll see 3 retries, then the message routed to `orders.DLT`.

---

## 🧪 Running Tests

```bash
# All tests (requires Docker for integration tests)
mvn clean test

# Unit tests only (fast, no Docker needed)
mvn test -Dtest="PaymentServiceTest,PaymentControllerTest"

# Integration tests only
mvn test -Dtest="PaymentFlowIntegrationTest"
```

### Test coverage

| Test class | Type | What it covers |
|---|---|---|
| `PaymentServiceTest` | Unit (Mockito) | Business logic, idempotency, exception paths |
| `PaymentControllerTest` | Web layer (MockMvc) | HTTP responses, ProblemDetail format, status codes |
| `PaymentFlowIntegrationTest` | Integration (Testcontainers) | Full Kafka → Consumer → PostgreSQL flow |

---

## 🔍 Monitoring with Kafka UI

Open [http://localhost:8090](http://localhost:8090) to:

- Browse all topics (`orders`, `orders.DLT`)
- Inspect individual messages with their JSON payload
- Monitor consumer groups (`email-service-group`, `payment-service-group`) and their offsets
- Verify partition assignment across the 3 consumer threads

---

## 🔑 Key Design Decisions

**Why KRaft instead of Zookeeper?**
KRaft is Kafka's built-in consensus mechanism, stabilized in Kafka 3.3 and the official replacement for Zookeeper. It removes an entire container from the stack and is what production clusters are migrating to.

**Why `concurrency = 3` on consumers?**
The `orders` topic has 3 partitions. Kafka assigns one partition per consumer thread within a group — having 3 threads means all partitions are consumed in parallel. Without this, 2 partitions would sit idle.

**Why idempotency check before saving?**
Kafka guarantees *at-least-once* delivery — in failure scenarios a message can be delivered more than once. The `findByOrderId` check before inserting, combined with a unique database constraint on `orderId`, ensures that even if the same message is processed twice, only one payment record is created.

**Why `ResponseEntity.accepted()` (202) instead of 200?**
The order isn't processed synchronously — it's published to Kafka and processed asynchronously by downstream services. HTTP 202 means "received and queued for processing", which is semantically accurate. HTTP 200 would imply the operation completed, which it hasn't yet.

**Why ProblemDetail (RFC 9457)?**
Instead of inventing a custom error format, ProblemDetail is the industry standard for HTTP error responses. Spring Boot 3 supports it natively and it's what frontend teams expect in modern APIs.

---

## 📄 License

MIT
