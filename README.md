# 🐳 DevOps Training — Lab 1: Java Spring Boot + Maven

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9-red?logo=apachemaven)](https://maven.apache.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?logo=spring)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)](https://www.docker.com/)

> **DevOps Training Series · Lab 1 of 5**
> 3-Tier web application — Frontend + Spring Boot Backend + PostgreSQL — fully Dockerized.

---

## 🏗 Architecture

```
Browser (Port 8080)
        │
        ▼
┌─────────────────────────┐
│   Spring Boot App        │  ← Tier 1+2 (Frontend + Backend)
│   Static HTML + REST API │
│   Port: 8080             │
└────────────┬────────────┘
             │ JDBC / JPA
             ▼
┌─────────────────────────┐
│   PostgreSQL 16          │  ← Tier 3 (Database)
│   Port: 5432 (internal)  │
│   Volume: lab1_pgdata    │
└─────────────────────────┘
```

---

## 📁 Project Structure

```
lab1-java-maven/
├── src/
│   └── main/
│       ├── java/com/devops/lab1/
│       │   ├── Lab1Application.java       # Spring Boot entry point
│       │   └── controller/
│       │       └── StatusController.java  # REST API endpoints
│       └── resources/
│           ├── application.properties     # App configuration
│           └── static/
│               └── index.html             # Frontend (served by Spring Boot)
├── docker/
│   └── init.sql                           # DB initialization script
├── pom.xml                                # Maven build definition
├── Dockerfile                             # Multi-stage build
├── docker-compose.yml                     # Full stack orchestration
└── .dockerignore
```

---

## 🚀 Quick Start

### Option A — Docker Compose (Recommended)

```bash
# 1. Clone / navigate to project
cd lab1-java-maven

# 2. Start all services (builds + runs)
docker compose up -d --build

# 3. Open browser
open http://localhost:8080

# 4. View logs
docker compose logs -f app

# 5. Stop everything
docker compose down
```

### Option B — Build & Run Manually

```bash
# Build the Docker image
docker build -t lab1-java-maven:1.0 .

# Start PostgreSQL first
docker run -d \
  --name lab1-db \
  -e POSTGRES_DB=devopslab \
  -e POSTGRES_USER=labuser \
  -e POSTGRES_PASSWORD=labpass123 \
  -v lab1_pgdata:/var/lib/postgresql/data \
  postgres:16-alpine

# Run the app (linking to DB container)
docker run -d \
  --name lab1-app \
  -p 8080:8080 \
  --link lab1-db:db \
  -e DB_URL=jdbc:postgresql://db:5432/devopslab \
  -e DB_USER=labuser \
  -e DB_PASSWORD=labpass123 \
  lab1-java-maven:1.0
```

---

## 🔍 API Endpoints

| Method | Endpoint       | Description                        |
|--------|----------------|------------------------------------|
| GET    | `/`            | Frontend HTML page                 |
| GET    | `/api/status`  | Backend + DB connection status     |
| GET    | `/api/health`  | Health check (used by Docker)      |
| GET    | `/actuator/health` | Spring Boot Actuator health    |

**Test the API:**
```bash
curl http://localhost:8080/api/status | python3 -m json.tool
```

---

## 🌱 Environment Variables

| Variable      | Default                                    | Description          |
|---------------|--------------------------------------------|----------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/devopslab` | JDBC connection URL  |
| `DB_USER`     | `labuser`                                  | Database username    |
| `DB_PASSWORD` | `labpass`                                  | Database password    |

---

## 🐳 Docker Deep Dive

### Multi-Stage Build Explained

```dockerfile
# Stage 1: Build with full Maven + JDK (heavy image ~600MB)
FROM maven:3.9-eclipse-temurin-17 AS builder
RUN mvn package -DskipTests

# Stage 2: Run with JRE only (lean image ~110MB)
FROM eclipse-temurin:17-jre-alpine
COPY --from=builder /build/target/*.jar app.jar
```

**Result:** Final image is ~82% smaller than single-stage!

### Layer Cache Strategy

```
Layer 1: FROM eclipse-temurin (rarely changes)
Layer 2: COPY pom.xml (changes when deps change)
Layer 3: RUN mvn dependency:go-offline (cached until pom.xml changes)
Layer 4: COPY src (changes on code changes → only this re-runs)
Layer 5: RUN mvn package
```

### Volume Persistence

```yaml
volumes:
  lab1_pgdata:    # Named volume — data survives container deletion
```

```bash
# Check where Docker stores the data
docker volume inspect lab1_pgdata

# Verify data persists after restart
docker compose down && docker compose up -d
# Your database still has all data!
```

---

## 🔧 Useful Commands

```bash
# Shell into running app container
docker compose exec app sh

# Shell into database
docker compose exec db psql -U labuser -d devopslab

# View real-time logs
docker compose logs -f

# Check container resource usage
docker stats

# Inspect network
docker network inspect lab1_backend

# Rebuild without cache
docker compose build --no-cache app
```

---

## 🏭 Real-World Production Notes

1. **Never hardcode secrets** — use `.env` files or secret managers (AWS Secrets Manager, HashiCorp Vault)
2. **JVM flags are critical** — `-XX:+UseContainerSupport` prevents OOMKill in Kubernetes
3. **Health checks matter** — `depends_on: condition: service_healthy` prevents startup race conditions
4. **Non-root user** — always run as a non-root user in production containers
5. **Internal networks** — databases should never be exposed to the public internet

---

## 📊 Comparison: Maven vs Gradle (see Lab 2)

| Feature            | Maven (Lab 1)       | Gradle (Lab 2)         |
|--------------------|---------------------|------------------------|
| Config format      | `pom.xml` (XML)     | `build.gradle` (Groovy/Kotlin) |
| Build command      | `mvn package`       | `gradle bootJar`       |
| Cache              | `~/.m2`             | `~/.gradle`            |
| Speed              | Moderate            | Faster (incremental)   |
| Industry adoption  | Very High           | High (especially Android) |

---

*DevOps Training Series — Lab 1 of 5 · Java Spring Boot + Maven + PostgreSQL + Docker*
