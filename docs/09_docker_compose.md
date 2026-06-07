# ForgeOps — Docker Compose Configuration

## File: `docker-compose.yml`

```yaml
version: "3.9"

networks:
  forgeops-net:
    driver: bridge

volumes:
  postgres-data:
  redis-data:

services:

  postgres:
    image: postgres:16-alpine
    container_name: forgeops-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: forgeops
      POSTGRES_USER: forgeops
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-forgeops_secret}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - forgeops-net
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U forgeops"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: forgeops-redis
    restart: unless-stopped
    command: redis-server --requirepass ${REDIS_PASSWORD:-redis_secret}
    volumes:
      - redis-data:/data
    networks:
      - forgeops-net
    healthcheck:
      test: ["CMD", "redis-cli", "--pass", "${REDIS_PASSWORD:-redis_secret}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  forgeops-api:
    build:
      context: .
      dockerfile: Dockerfile.api
    container_name: forgeops-api
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/forgeops
      SPRING_DATASOURCE_USERNAME: forgeops
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-forgeops_secret}
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      SPRING_REDIS_PASSWORD: ${REDIS_PASSWORD:-redis_secret}
      FORGEOPS_JWT_SECRET: ${JWT_SECRET:-change_me_in_production_very_long_secret_key}
      FORGEOPS_JWT_EXPIRY: 86400
      FORGEOPS_VAULT_ENCRYPTION_KEY: ${VAULT_ENCRYPTION_KEY:-change_me_vault_key_32chars}
      FORGEOPS_ENGINE_FORK_FACTOR: 10
      FORGEOPS_ENGINE_TASK_TIMEOUT: 300
      FORGEOPS_ENGINE_SSH_CONNECT_TIMEOUT: 10
      FORGEOPS_ENGINE_SSH_STRICT_HOST_CHECK: "false"
      FORGEOPS_DRIFT_SCHEDULER_ENABLED: "true"
      FORGEOPS_CORS_ORIGINS: "http://localhost:3000"
      LOGGING_LEVEL_EU_FORGEOPS: INFO
    ports:
      - "8080:8080"
    networks:
      - forgeops-net

  forgeops-ui:
    build:
      context: ui/
      dockerfile: Dockerfile.ui
    container_name: forgeops-ui
    restart: unless-stopped
    environment:
      VITE_API_URL: http://localhost:8080/api/v1
      VITE_WS_URL: ws://localhost:8080/ws
    ports:
      - "3000:80"
    networks:
      - forgeops-net
    depends_on:
      - forgeops-api
```

---

## File: `Dockerfile.api`

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -q
COPY src src
RUN ./mvnw package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S forgeops && adduser -S forgeops -G forgeops
COPY --from=build /app/target/*.jar app.jar
USER forgeops
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseZGC", "-jar", "app.jar"]
```

---

## File: `ui/Dockerfile.ui`

```dockerfile
# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json .
RUN npm ci --silent
COPY . .
RUN npm run build

# Runtime stage
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

---

## File: `ui/nginx.conf`

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    # SPA routing — all paths serve index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Security headers
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";

    # Cache static assets
    location ~* \.(js|css|png|jpg|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    gzip on;
    gzip_types text/plain text/css application/json application/javascript;
}
```

---

## File: `.env.example`

```env
# Copy to .env and fill in values before running
POSTGRES_PASSWORD=forgeops_secret
REDIS_PASSWORD=redis_secret
JWT_SECRET=change_me_in_production_very_long_secret_key_minimum_32_chars
VAULT_ENCRYPTION_KEY=change_me_vault_32charkey_exactly
```

---

## File: `pom.xml` (key dependencies)

```xml
<dependencies>
  <!-- Spring Boot -->
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-websocket</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-redis</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>

  <!-- Database -->
  <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId></dependency>
  <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>

  <!-- JWT -->
  <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>0.12.3</version></dependency>
  <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>0.12.3</version></dependency>

  <!-- SSH -->
  <dependency><groupId>com.github.mwiede</groupId><artifactId>jsch</artifactId><version>0.2.17</version></dependency>

  <!-- YAML parsing -->
  <dependency><groupId>com.fasterxml.jackson.dataformat</groupId><artifactId>jackson-dataformat-yaml</artifactId></dependency>

  <!-- Utilities -->
  <dependency><groupId>org.mapstruct</groupId><artifactId>mapstruct</artifactId><version>1.5.5.Final</version></dependency>
  <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></dependency>
</dependencies>
```

---

## File: `ui/package.json` (key dependencies)

```json
{
  "dependencies": {
    "react": "^18.3.0",
    "react-dom": "^18.3.0",
    "react-router-dom": "^6.23.0",
    "@tanstack/react-query": "^5.40.0",
    "zustand": "^4.5.0",
    "@stomp/stompjs": "^7.0.0",
    "sockjs-client": "^1.6.1",
    "monaco-editor": "^0.49.0",
    "@monaco-editor/react": "^4.6.0",
    "recharts": "^2.12.0",
    "lucide-react": "^0.383.0",
    "axios": "^1.7.0",
    "date-fns": "^3.6.0",
    "clsx": "^2.1.1"
  },
  "devDependencies": {
    "vite": "^5.3.0",
    "@vitejs/plugin-react": "^4.3.0",
    "tailwindcss": "^3.4.0",
    "autoprefixer": "^10.4.0"
  }
}
```

---

## Startup

```bash
# 1. Clone repo
# 2. Copy env file
cp .env.example .env
# 3. Edit .env with real secrets
# 4. Start everything
docker compose up -d
# 5. Check health
docker compose ps
# 6. View API logs
docker compose logs -f forgeops-api
# 7. Open UI
open http://localhost:3000
# Default login: admin / ForgeOps@Change_Me_Now!
```
