# QuarkIoT - How to Run

This guide provides step-by-step instructions for running the QuarkIoT microservices platform.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start](#quick-start)
4. [Accessing Services](#accessing-services)

---

## Prerequisites

Before running the application, ensure you have the following installed:

### Required Software

- **Java 21** or higher
  ```bash
  java -version
  ```

- **Maven 3.9+** (or use the included Maven wrapper `./mvnw`)
  ```bash
  mvn -version
  ```

- **Docker** and **Docker Compose**
  ```bash
  docker --version
  docker compose version
  ```

### System Requirements

- **RAM:** Minimum 8GB (16GB recommended)
- **Disk Space:** At least 5GB free space
- **Ports:** Ensure the following ports are available:
  - `8080` - Adminer (Database UI)
  - `8081` - Device Management Service
  - `8082` - Ingestion Service
  - `8083` - Processing Service
  - `9092` - Kafka Broker
  - `5432` - PostgreSQL
  - `6379` - Redis

---

## Quick Start

The fastest way to run the entire platform:

### 1. Clone the Repository

```bash
git clone <repository-url>
cd quarkiot
```

### 2. Set Up Environment Variables

Copy the example environment file and configure it:

```bash
cp .envexample .env
```

Edit `.env` if needed (default values work for local development):

```env
DB_NAME=iot_platform
DB_USER=admin
DB_PASS=admin
DB_PORT=5432
```

### 3. Run the Application

Use the provided startup script:

```bash
sudo ./scripts/start.sh
```

This script will:
1. Build all services using Maven
2. Build Docker images
3. Start all containers with Docker Compose

**Note:** The script requires `sudo` for Docker operations.

### 4. Wait for Services to Start

The application takes approximately 1-2 minutes to fully start. You can monitor the logs:

```bash
docker compose logs -f
```

Press `Ctrl+C` to stop following logs (services will continue running).

---

## Accessing Services

Once all services are running, you can access them at:

### REST APIs

| Service | URL | Description |
|---------|-----|-------------|
| **Ingestion Service** | http://localhost:8082 | Sensor data ingestion endpoint |
| **Device Management** | http://localhost:8081 | Device CRUD operations |
| **Processing Service** | http://localhost:8083 | Data processing service |
| **Adminer (DB UI)** | http://localhost:8080 | PostgreSQL database interface |

### Swagger UI (API Documentation)

Each service provides interactive API documentation:

- **Ingestion Service:** http://localhost:8082/q/swagger-ui
- **Device Management:** http://localhost:8081/q/swagger-ui
- **Processing Service:** http://localhost:8083/q/swagger-ui

### Database Access (Adminer)

1. Navigate to http://localhost:8080
2. Login with credentials:
   - **System:** PostgreSQL
   - **Server:** postgres
   - **Username:** admin (or value from `.env`)
   - **Password:** admin (or value from `.env`)
   - **Database:** iot_platform

### Kafka Broker

- **Bootstrap Server:** localhost:9092
- **Container Name:** broker

### Redis

- **Host:** localhost
- **Port:** 6379
- **Container Name:** redis
