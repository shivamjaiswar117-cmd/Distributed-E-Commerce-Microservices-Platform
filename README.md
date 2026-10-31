# Distributed E-Commerce Microservices Platform

A distributed e-commerce backend built using Java, Spring Boot, REST APIs, Apache Kafka, Redis, MySQL, Docker, Kubernetes, Terraform, and AWS.

## Architecture

```text
                         Client
                            |
                            v
                   API Gateway :9090
                            |
             +--------------+--------------+
             |              |              |
             v              v              v
       User Service   Product Service   Order Service
          :8081           :8082            :8083
             |              |               |
           MySQL          MySQL            MySQL
                            |
                           Redis
                         (Caching)

                     Order Service
                            |
                            v
                     Kafka: order-created
                            |
             +--------------+--------------+
             |              |              |
             v              v              v
       Payment Service Inventory Service Notification Service
           :8084           :8085                :8086
             |
             v
       Kafka: payment-result
             |
             v
       Order status updated
```

## Technology Stack

- Language: Java 21
- Framework: Spring Boot
- API Gateway: Spring Cloud Gateway
- Database: MySQL
- Messaging: Apache Kafka and Zookeeper
- Caching: Redis
- Testing: JUnit and Mockito
- Build: Maven
- Containers: Docker and Docker Compose
- Orchestration: Kubernetes and Amazon EKS
- Infrastructure as Code: Terraform
- Cloud: AWS, including ECR and RDS
- Version Control: Git and GitHub
- CI: GitHub Actions workflow configured

## Microservices

| Service | Port | Responsibility |
|---|---:|---|
| API Gateway | 9090 | Routes client requests |
| User Service | 8081 | User management |
| Product Service | 8082 | Product management and caching |
| Order Service | 8083 | Order creation and status |
| Payment Service | 8084 | Payment event processing |
| Inventory Service | 8085 | Inventory validation and stock updates |
| Notification Service | 8086 | Order event notifications |

## Prerequisites

- Java 21
- Docker Desktop with Docker Compose
- Git
- Maven Wrapper included in each service
- MySQL configured according to the project settings

## Run Locally

1. Clone the repository:

   ```bash
   git clone https://github.com/shivamjaiswar117-cmd/Distributed-E-Commerce-Microservices-Platform.git
   cd Distributed-E-Commerce-Microservices-Platform
   ```

2. Create a local `.env` file with the required environment variables. Use your own credentials; never commit this file.

3. Start the application:

   ```powershell
   docker compose --env-file .env -f .\infra\docker-compose.yml up -d --build
   ```

4. Check container status:

   ```powershell
   docker compose --env-file .env -f .\infra\docker-compose.yml ps
   ```

5. Stop the application:

   ```powershell
   docker compose --env-file .env -f .\infra\docker-compose.yml down
   ```

## API Endpoints

Base URL: `http://localhost:9090`

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/users` | List users |
| GET | `/products` | List products |
| GET | `/orders` | List orders |
| GET | `/orders/{id}` | Retrieve an order |
| POST | `/orders` | Create an order |

Example order request:

```json
{
  "userId": 1,
  "productId": 2,
  "quantity": 1,
  "totalPrice": 999.00
}
```

Create an order using PowerShell:

```powershell
$body = @{
    userId = 1
    productId = 2
    quantity = 1
    totalPrice = 999.00
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri "http://localhost:9090/orders" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

Use valid user and product IDs from your running database. Confirm the actual endpoint mappings in the source code if your configuration differs.

## Event-Driven Workflow

1. The Order Service creates an order and publishes an `order-created` event.
2. Payment, Inventory, and Notification services consume the event independently.
3. The Payment Service publishes a `payment-result` event.
4. The Order Service consumes the result and updates the order status.
5. Inventory processing validates available stock and records processed orders to help prevent duplicate deductions.

## Run Tests

Run tests for each service from PowerShell:

```powershell
$services = @(
    ".\user-service\user-service",
    ".\product-service\product-service",
    ".\order-service\order-service",
    ".\payment-service\payment-service",
    ".\inventory-service\inventory-service",
    ".\notification-service\notification-service",
    ".\api-gateway\api-gateway"
)

foreach ($service in $services) {
    Push-Location $service
    try {
        .\mvnw.cmd test
        if ($LASTEXITCODE -ne 0) {
            throw "Tests failed in $service"
        }
    }
    finally {
        Pop-Location
    }
}
```

## Infrastructure

The repository also includes Kubernetes manifests and Terraform configuration for AWS infrastructure. The Terraform and Kubernetes files were written for AWS (EKS, RDS, ECR) but are not currently deployed; the project runs locally with Docker Compose. Review environment variables, IAM permissions, networking, costs, and database connectivity before deploying to AWS.

## Security

- Keep `.env` and all credentials out of Git.
- Use least-privilege IAM permissions.
- Never commit access keys, passwords, tokens, or private endpoints containing credentials.
- Rotate any credentials accidentally exposed in source control or logs.

## Monitoring

Prometheus and Grafana run as part of the Docker Compose stack.

| Tool | URL | Notes |
|---|---|---|
| Prometheus | http://localhost:9091 | Scrapes `/actuator/prometheus` on all 7 services every 15s |
| Grafana | http://localhost:3000 | Login as `admin` with `GRAFANA_ADMIN_PASSWORD` from your local `.env` |

Grafana uses the Prometheus data source `http://prometheus:9090`. Import dashboard ID `4701` (JVM Micrometer) for JVM memory, CPU, and HTTP metrics.

Only the Actuator `health`, `info` and `prometheus` endpoints are public; all other endpoints on secured services still require authentication.

## Project Status

- All seven services build, pass their unit tests, and run in Docker Compose.
- Order flow verified end to end: Order Service -> Kafka `order-created` -> Payment and Inventory -> result events -> order status `CONFIRMED`.
- Payment is simulated: orders with a missing user or a non-positive amount are declined (`FAILED`) and the order is cancelled. Inventory rejects missing stock, insufficient stock, and invalid quantities, and handles duplicate events.
- Prometheus and Grafana monitoring are working locally (7 of 7 targets up).
- AWS deployment (Terraform/EKS/RDS) is configured but not deployed.