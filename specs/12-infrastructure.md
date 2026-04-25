# Infrastructure

## 1. Environment Variables Reference

All configuration is through a `.env` file at the project root. Both Docker Compose files read from this file via `env_file: .env` or by Docker Compose's automatic `.env` loading.

| Key | Description | Default | Required |
|-----|-------------|---------|----------|
| `APP_NAME` | Display name in UI, manifest, push notifications | `Home KM` | No |
| `APP_THEME_COLOR` | PWA theme colour (hex) | `#6366f1` | No |
| `DB_HOST` | PostgreSQL hostname (service name in Docker) | `postgres` | Yes |
| `DB_PORT` | PostgreSQL port | `5432` | No |
| `DB_NAME` | PostgreSQL database name | `homekm` | No |
| `DB_USER` | PostgreSQL username | `homekm` | Yes |
| `DB_PASSWORD` | PostgreSQL password | — | Yes |
| `POSTGRES_PORT` | Host-mapped port for PostgreSQL (infra compose) | `5432` | No |
| `MINIO_ENDPOINT` | MinIO server URL (from app container's perspective) | `http://minio:9000` | Yes |
| `MINIO_ACCESS_KEY` | MinIO root user / access key | — | Yes |
| `MINIO_SECRET_KEY` | MinIO root password / secret key | — | Yes |
| `MINIO_BUCKET_NAME` | Bucket name | `homekm` | No |
| `MINIO_PORT` | Host-mapped port for MinIO API | `9000` | No |
| `MINIO_CONSOLE_PORT` | Host-mapped port for MinIO console | `9001` | No |
| `JWT_SECRET` | JWT signing key (base64, ≥32 chars) | — | Yes |
| `JWT_EXPIRY_HOURS` | JWT token lifetime in hours | `24` | No |
| `BCRYPT_COST` | bcrypt work factor (10–14) | `12` | No |
| `VAPID_PUBLIC_KEY` | VAPID public key (base64url) | — | Yes |
| `VAPID_PRIVATE_KEY` | VAPID private key (base64url) | — | Yes |
| `VAPID_SUBJECT` | VAPID contact URI (`mailto:...`) | — | Yes |
| `PRESIGNED_URL_EXPIRY_MINUTES` | MinIO presigned URL lifetime in minutes | `15` | No |
| `MAX_FILE_UPLOAD_MB` | Max file upload size in MB | `100` | No |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed CORS origins | `http://localhost:3000` | No |
| `API_PORT` | Host-mapped port for Spring Boot API | `8080` | No |
| `FRONTEND_PORT` | Host-mapped port for nginx frontend | `3000` | No |

**.env.example file** must be committed to the repository. The real `.env` file must be in `.gitignore`.

---

## 2. `docker-compose.infra.yml`

```yaml
version: '3.9'

networks:
  infra-net:
    name: homekm-infra-net
    driver: bridge

volumes:
  pgdata:
  miniodata:

services:
  postgres:
    image: pgvector/pgvector:pg15
    environment:
      POSTGRES_DB: ${DB_NAME:-homekm}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./infra/postgres/init.sql:/docker-entrypoint-initdb.d/01-extensions.sql:ro
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    networks:
      - infra-net
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER} -d ${DB_NAME:-homekm}"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  minio:
    image: minio/minio:RELEASE.2024-01-01T00-00-00Z
    command: server /data --console-address :9001
    environment:
      MINIO_ROOT_USER: ${MINIO_ACCESS_KEY}
      MINIO_ROOT_PASSWORD: ${MINIO_SECRET_KEY}
    volumes:
      - miniodata:/data
    ports:
      - "${MINIO_PORT:-9000}:9000"
      - "${MINIO_CONSOLE_PORT:-9001}:9001"
    networks:
      - infra-net
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
```

**Note on PostgreSQL image:** Use the official `pgvector/pgvector:pg15` image which includes pgvector pre-installed. The `init.sql` still runs `CREATE EXTENSION` to ensure both `pgvector` and `pg_trgm` are enabled.

---

## 3. `infra/postgres/init.sql`

```sql
CREATE EXTENSION IF NOT EXISTS pgvector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

This file is mounted as a Docker init script and runs before Flyway migrations.

---

## 4. `docker-compose.app.yml`

```yaml
version: '3.9'

networks:
  infra-net:
    name: homekm-infra-net
    external: true

services:
  api:
    build:
      context: ./backend
      dockerfile: Dockerfile
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://${DB_HOST:-postgres}:${DB_PORT:-5432}/${DB_NAME:-homekm}
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      MINIO_ENDPOINT: ${MINIO_ENDPOINT:-http://minio:9000}
      MINIO_ACCESS_KEY: ${MINIO_ACCESS_KEY}
      MINIO_SECRET_KEY: ${MINIO_SECRET_KEY}
      MINIO_BUCKET_NAME: ${MINIO_BUCKET_NAME:-homekm}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRY_HOURS: ${JWT_EXPIRY_HOURS:-24}
      BCRYPT_COST: ${BCRYPT_COST:-12}
      VAPID_PUBLIC_KEY: ${VAPID_PUBLIC_KEY}
      VAPID_PRIVATE_KEY: ${VAPID_PRIVATE_KEY}
      VAPID_SUBJECT: ${VAPID_SUBJECT}
      PRESIGNED_URL_EXPIRY_MINUTES: ${PRESIGNED_URL_EXPIRY_MINUTES:-15}
      MAX_FILE_UPLOAD_MB: ${MAX_FILE_UPLOAD_MB:-100}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:-http://localhost:3000}
      APP_NAME: ${APP_NAME:-Home KM}
    ports:
      - "${API_PORT:-8080}:8080"
    networks:
      - infra-net
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
      args:
        VITE_APP_NAME: ${APP_NAME:-Home KM}
        VITE_APP_THEME_COLOR: ${APP_THEME_COLOR:-#6366f1}
        MAX_FILE_UPLOAD_MB: ${MAX_FILE_UPLOAD_MB:-100}
    ports:
      - "${FRONTEND_PORT:-3000}:80"
    networks:
      - infra-net
    depends_on:
      api:
        condition: service_healthy
    restart: unless-stopped
```

The `api` service joins the `infra-net` network to reach `postgres` and `minio` services by their service names.

---

## 5. Backend Dockerfile

`backend/Dockerfile`:

```dockerfile
# Build stage
FROM gradle:8-jdk21-alpine AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 6. Frontend Dockerfile

`frontend/Dockerfile`:

```dockerfile
# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_APP_NAME="Home KM"
ARG VITE_APP_THEME_COLOR="#6366f1"
ENV VITE_APP_NAME=$VITE_APP_NAME
ENV VITE_APP_THEME_COLOR=$VITE_APP_THEME_COLOR
RUN npm run build

# Runtime stage
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
ARG MAX_FILE_UPLOAD_MB=100
ENV MAX_FILE_UPLOAD_MB=$MAX_FILE_UPLOAD_MB
EXPOSE 80
```

The `nginx:alpine` image runs `envsubst` on templates in `/etc/nginx/templates/` automatically.

---

## 7. nginx Configuration Template

`frontend/nginx.conf.template`:

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    client_max_body_size ${MAX_FILE_UPLOAD_MB}m;

    location /api/ {
        proxy_pass http://api:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }

    location / {
        try_files $uri $uri/ /index.html;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }

    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

`client_max_body_size` is substituted by `envsubst` using the `MAX_FILE_UPLOAD_MB` variable.

---

## 8. Spring Boot `application.yml`

`backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    hikari:
      maximum-pool-size: 10
  flyway:
    enabled: true
    locations: classpath:db/migration
  servlet:
    multipart:
      max-file-size: ${MAX_FILE_UPLOAD_MB:-100}MB
      max-request-size: ${MAX_FILE_UPLOAD_MB:-100}MB
  jackson:
    serialization:
      write-dates-as-timestamps: false
    time-zone: UTC

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never

app:
  jwt:
    secret: ${JWT_SECRET}
    expiry-hours: ${JWT_EXPIRY_HOURS:24}
  bcrypt:
    cost: ${BCRYPT_COST:12}
  minio:
    endpoint: ${MINIO_ENDPOINT}
    access-key: ${MINIO_ACCESS_KEY}
    secret-key: ${MINIO_SECRET_KEY}
    bucket-name: ${MINIO_BUCKET_NAME:homekm}
  vapid:
    public-key: ${VAPID_PUBLIC_KEY}
    private-key: ${VAPID_PRIVATE_KEY}
    subject: ${VAPID_SUBJECT}
  presigned-url-expiry-minutes: ${PRESIGNED_URL_EXPIRY_MINUTES:15}
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
  name: ${APP_NAME:Home KM}
```

---

## 9. Startup Sequence

```bash
# 1. Copy and configure .env
cp .env.example .env
# Edit .env with real values

# 2. Generate VAPID keys (one-time setup)
npx web-push generate-vapid-keys
# Copy VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY into .env
# Set VAPID_SUBJECT=mailto:yourname@example.com

# 3. Start infrastructure
docker compose -f docker-compose.infra.yml up -d
# Wait for postgres healthcheck to pass

# 4. Start application
docker compose -f docker-compose.app.yml up -d
# Spring Boot runs Flyway migrations on startup
# If migrations fail, the API container will exit
```

---

## 10. Development Setup (Without Docker)

For local development, start only the infra:
```bash
docker compose -f docker-compose.infra.yml up -d
```

Run the backend:
```bash
cd backend
./gradlew bootRun
```

Run the frontend:
```bash
cd frontend
npm run dev
```

Backend listens on `http://localhost:8080`. Frontend dev server proxies `/api` to `http://localhost:8080/api` (configured in `vite.config.ts`).
