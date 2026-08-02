# Payment System

A microservices backend for user authentication and Stripe payment processing, built with Spring Boot 4, PostgreSQL, and RabbitMQ.

## Services

| Service | Port | Responsibility |
|---|---|---|
| `auth-payment-service` | 8081 | Register/login, JWT, Stripe payments |
| `notification-service` | 8082 | Sends email notifications via RabbitMQ events |

## Tech Stack

- Java 21 / Spring Boot 4
- PostgreSQL 16
- RabbitMQ 3
- Stripe Java SDK
- Docker / Docker Compose

## Local Setup

### 1. Prerequisites

- Docker Desktop running
- A Stripe account (test keys from [dashboard.stripe.com](https://dashboard.stripe.com))

### 2. Configure environment

```bash
cp .env.example .env
```

Edit `.env` with your values — DB credentials, RabbitMQ credentials, JWT secret, Stripe keys, and mail settings. See `.env.example` for all required variables.

```bash
# Generate a secure JWT secret
openssl rand -base64 64
```

### 3. Configure Spring Boot

```bash
cp auth-payment-service/src/main/resources/application.yaml.example \
   auth-payment-service/src/main/resources/application.yaml

cp notification-service/src/main/resources/application.yaml.example \
   notification-service/src/main/resources/application.yaml
```

### 4. Start all services

```bash
docker-compose up --build
```

Services start in dependency order. First build takes ~5 minutes (Maven downloads dependencies inside Docker).

### 5. Verify

```bash
docker-compose ps                                    # all 4 containers should be Up
curl http://localhost:8081/api/auth/register         # should return 405 (wrong method = it's alive)
open http://localhost:15672                          # RabbitMQ UI (use your RABBITMQ credentials)
```

## API Reference

Full spec: [`openapi.yml`](./openapi.yml) — import into [Postman](https://www.postman.com) or view at [editor.swagger.io](https://editor.swagger.io).

### Auth

#### Register
```http
POST http://localhost:8081/api/auth/register
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "secret123",
  "firstName": "John",
  "lastName": "Doe"
}
```

#### Login
```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "secret123"
}
```

Both return a `token` — use it as `Authorization: Bearer <token>` on protected endpoints.

### Payments

#### Create Payment Intent
```http
POST http://localhost:8081/api/payments/checkout
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 2000,
  "currency": "usd",
  "description": "Order #1234"
}
```

Amount is in **cents** — `2000` = $20.00. Minimum is `50`.

The response includes a `clientSecret` which your frontend passes to the Stripe SDK to confirm the payment.

#### Stripe Webhook

`POST /api/payments/webhook` is called by Stripe — not by your frontend. To test webhooks locally:

```bash
# Install Stripe CLI, then forward events to your local server
stripe listen --forward-to http://localhost:8081/api/payments/webhook
```

Handled events: `payment_intent.succeeded`, `payment_intent.payment_failed`, `payment_intent.canceled`.

## Project Structure

```
payment-system/
├── auth-payment-service/       # Auth + payment service (port 8081)
│   ├── src/
│   ├── Dockerfile
│   └── docker-entrypoint.sh
├── notification-service/       # Email notification service (port 8082)
│   ├── src/
│   ├── Dockerfile
│   └── docker-entrypoint.sh
├── docker-compose.yml          # Local development
├── docker-stack.yml            # Production (Docker Swarm)
├── openapi.yml                 # API spec
└── .env.example                # Environment variable template
```

## Files Never Committed

| File | Why |
|---|---|
| `.env` | Contains your real secrets |
| `*/src/main/resources/application.yaml` | Contains your real secrets as fallback defaults |

Copy from the `.example` versions and fill in your own values.
