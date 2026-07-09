# Smart Event Ticket System

React + Spring Boot 的高併發事件接收與工單派發平台。這個專案模擬企業在短時間內接收大量系統告警、客服案件、交易異常或監控事件後，如何完成事件接收、Redis 去重、Idempotency 控制、自動建單、工單派發，以及 Dashboard 統計展示的完整流程。

## Preview

### Dashboard Overview

![Dashboard Overview](./Docs/dashboard-live.png)

### Event Submitted State

![Dashboard After Event Submission](./Docs/dashboard-event-submitted.png)

## Highlights

- React Dashboard 提供事件上報、事件流、來源排行、工單派發與統計卡片展示
- Spring Boot REST API 提供事件接收、批次事件、模擬事件、工單流程與 Dashboard Summary
- Redis 用於事件去重、Idempotency Key、Rate Limiting 與 Dashboard 快取
- 支援 Docker Compose，本機可快速完成 App + PostgreSQL + Redis 啟動
- `mvn package` 會自動建置 React 前端並將靜態檔打進 jar

## Current Notes

- GitHub repository slug 已切換為 smart-event-ticket-system，舊網址通常仍可被 GitHub redirect。
- 本地 origin 已指向 https://github.com/funrock99/smart-event-ticket-system.git。
- 目前專案 Java package 為 com.example.smarteventticket。
- Docker Compose 預設整合 Spring Boot、PostgreSQL 與 Redis。
- Dashboard 已包含 idempotent replay 指標，Ticket API 已包含 SLA 與狀態歷程查詢。
## Architecture

```mermaid
graph TD
    A[React Dashboard] -->|HTTP /api| B[Spring Boot API]
    B --> C[(H2 / PostgreSQL)]
    B --> D[(Redis)]
    E[Event Sources] --> B
    B --> F[Ticket Workflow]
    B --> G[Dashboard Summary]
```

## Tech Stack

### Frontend
- React 18
- Vite
- Fetch API
- CSS Dashboard UI

### Backend
- Java 17
- Spring Boot 3.5.x
- Spring Web
- Spring Data JPA
- Spring Data Redis
- Spring Validation
- Spring Boot Actuator
- springdoc OpenAPI / Swagger UI

### Data / Infra
- H2 Database
- PostgreSQL
- Redis
- Docker Compose
- Testcontainers
- Maven
- GitHub Actions

## Core Features

- `POST /api/events` 事件接收入口
- `GET /api/events` / `GET /api/events/{id}` 事件查詢
- `POST /api/events/batch` 批次事件處理
- `POST /api/events/simulate` 模擬大量事件上報
- `GET /api/events/dedup-stats` 去重統計
- 自動建立 Ticket
- 工單指派、狀態流轉與狀態歷程
- SLA 截止時間與 breach 判定
- Dashboard Summary 統計
- Redis Dashboard 快取
- Redis Deduplication，避免重複建單
- Idempotency Key 防止重送請求重複處理
- Rate Limiting 保護單一來源高頻請求
- 全域例外處理與參數驗證

## Why It Stands Out

- 不是單純的工單 CRUD，而是把專案主軸拉成企業後端常見的高頻事件接收平台
- 用事件來源、事件類型與 business key 模擬交易異常、客服案件、監控告警等真實場景
- 前端、後端、Redis、Docker Compose 都已串起來，可直接用 Dashboard 與 Swagger Demo
- 可以從 API 設計、服務保護、狀態流轉、快取策略到 UI 展示一次講完整體設計

## High Concurrency Design

系統在 `POST /api/events` 的入口採用分層保護流程，避免大量重複事件或重送請求直接壓到資料庫：

1. `Rate Limiting`
   - 使用 Redis key `rate:{source}:{minute}` 控制單一來源短時間請求量
   - 超過門檻時直接回應，避免單一來源打爆後端
2. `Idempotency Key`
   - 使用 Redis key `idempotency:{idempotencyKey}` 記錄 `PROCESSING`、`COMPLETED`、`FAILED`
   - 相同請求重送時直接回傳第一次結果，不重複建立 Event / Ticket
3. `Deduplication`
   - 使用 Redis key `alarm:dedup:{source}:{eventType}:{businessKey}`
   - 相同來源、相同事件類型、相同業務鍵值在 dedup window 內視為重複事件，不重複建單
4. `Database as Source of Truth`
   - Redis 是保護層與快取層，正式資料仍落在關聯式資料庫
   - 讓系統同時兼顧高頻請求保護與資料一致性
5. `Dashboard Cache`
   - `dashboard:summary` 使用 Redis 快取，降低高頻查詢對 DB 的壓力

## Project Structure

```text
smart-event-ticket-system
├── .github/workflows/            # CI workflow
├── frontend/                     # React + Vite frontend
├── k6/                           # k6 load test scripts
├── src/
│   ├── main/
│   │   ├── java/com/example/smarteventticket
│   │   └── resources/
│   └── test/
├── Docs/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

## API Docs

- App: `http://localhost:8080/`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Actuator Health: `http://localhost:8080/actuator/health`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- H2 Console: `http://localhost:8080/h2-console` (`dev-h2` profile only)

## Quick Start

### Option 1: Docker Compose

```bash
docker compose up --build
```

啟動後會以 `docker-postgres` profile 啟動，資料會落到 PostgreSQL。可直接開啟：
- App: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### Option 2: Local Development

先啟動 Redis 與 Spring Boot：

```bash
docker compose up -d redis
mvn spring-boot:run
```

再啟動 React 前端：

```bash
cd frontend
npm install
npm run dev
```

開發模式位址：
- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080`

Vite 會自動 proxy `/api` 到 Spring Boot。

## Profiles

- `dev-h2`: 預設本機 profile，使用 H2 與 `/h2-console`
- `docker-postgres`: Docker Compose profile，使用 PostgreSQL 作為持久化資料庫

## Build

### Package Backend + Frontend Together

```bash
mvn package
```

這條命令會自動執行：
1. 安裝 Maven 所需 Node.js / npm toolchain
2. 在 `frontend/` 執行 `npm ci`
3. 在 `frontend/` 執行 `npm run build`
4. 將 React build 產物打進 Spring Boot jar

輸出檔案：
- `target/smart-event-ticket-system-0.0.1-SNAPSHOT.jar`

## Test

### Full Test Suite

```bash
mvn test
```

### Frontend Build Check

```bash
cd frontend
npm run build
```

### Redis Container Integration Test Only

```bash
mvn -Dtest=AlarmRecentRedisContainerIntegrationTest test
```

### PostgreSQL + Redis Integration Test Only

```bash
mvn -Dtest=PostgresRedisIntegrationTest test
```

### k6 Load Test

```bash
k6 run k6/event-ingestion-test.js
```

這個腳本會同時驗證事件接收、Idempotency replay、Dedup 與 Rate Limiting 路徑。

## API Overview

### Event APIs

```http
POST /api/events
GET /api/events?page=0&size=20&sort=occurredAt,desc
GET /api/events/{id}
POST /api/events/batch
POST /api/events/simulate
GET /api/events/dedup-stats
```

### Ticket APIs

```http
GET /api/tickets?page=0&size=20&sort=createdAt,desc
GET /api/tickets/{id}
GET /api/tickets/{id}/history
PUT /api/tickets/{id}/assign
PUT /api/tickets/{id}/status
```

### Dashboard API

```http
GET /api/dashboard/summary
```

## Example Request

```http
POST /api/events
Idempotency-Key: IDEMP-20260709-0001
Content-Type: application/json
```

```json
{
  "source": "payment-system",
  "eventType": "TRANSACTION_ERROR",
  "businessKey": "TXN-10001",
  "severity": "HIGH",
  "message": "Transaction failed due to account validation error",
  "payload": "{\"transactionId\":\"TXN-10001\"}"
}
```

## Example Response

```json
{
  "success": true,
  "eventId": 1,
  "ticketId": 1,
  "duplicated": false,
  "rateLimited": false,
  "message": "Event accepted and ticket created"
}
```

## Demo Flow

1. 開啟 React Dashboard
2. 送出一筆 `POST /api/events` 事件
3. 觀察事件流新增資料
4. 觀察系統自動建立工單
5. 指派工單處理人員
6. 更新工單狀態為 `PROCESSING`、`RESOLVED`、`CLOSED`
7. 重新查看 Dashboard Summary、來源排行與去重統計
8. 重複送出相同 `source + eventType + businessKey` 觀察 duplicated 結果

## Redis Keys

- `dashboard:summary`
- `alarm:dedup:{source}:{eventType}:{businessKey}`
- `idempotency:{idempotencyKey}`
- `rate:{source}:{minute}`
- `metrics:duplicate-events`
- `metrics:rate-limited-events`
- `metrics:idempotent-replayed-events`

## System Highlights

This project demonstrates a high-frequency event ingestion and automatic ticket dispatching platform.

- Redis-based Idempotency Key with `PROCESSING`, `COMPLETED`, and `FAILED` states.
- Redis-based Deduplication Window to suppress duplicated business events.
- Per-source Rate Limiting to protect API and database from traffic bursts.
- Idempotent replay metrics surfaced in the dashboard summary.
- PostgreSQL as the source of truth in Docker deployments.
- Repository-level pagination with stable DTO responses.
- Ticket SLA deadlines and status history for enterprise-style ticket lifecycle tracking.
- Spring Boot Actuator health endpoint, Docker healthchecks, and GitHub Actions CI.
- k6 load testing script to validate concurrent ingestion behavior.

## Domain Rules

- 工單狀態流程：`OPEN -> PROCESSING -> RESOLVED -> CLOSED`
- SLA 規則：`URGENT=1h`、`HIGH=4h`、`MEDIUM=8h`、`LOW=24h`
- 相同 `source + eventType + businessKey` 在 dedup window 內會被視為重複事件
- 相同 `Idempotency-Key` 搭配相同 request payload 會回傳第一次結果，不重複建立 Event / Ticket
- 單一來源在短時間高頻請求時會觸發 Rate Limiting
- Redis 不可用時，核心事件寫入流程仍會嘗試回退，但去重與保護能力會下降



