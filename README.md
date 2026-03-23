# Trading App

A full-stack trading operations platform for tracking transactions, defining buy/sell strategies, monitoring portfolio performance, and generating market/strategy alerts.

This repository contains:
- A Java/Spring Boot backend API with JWT and Google OAuth support
- A React + TypeScript frontend (Vite) for authenticated workflows
- PostgreSQL schema migrations managed with Flyway

## Key Capabilities
- Authentication: email/password login, JWT access/refresh flow, Google OAuth entry/callback
- Portfolio operations: buy/sell transaction tracking, open-position summaries, portfolio performance
- Strategy management: buy/sell strategy CRUD and strategy alert generation/acknowledgement
- Market intelligence: market alert scan/view/delete flows
- Reference data: assets and exchanges lookup management
- Historical and spot price support: historical refresh endpoints and live spot-price queries
- Operational tooling: SQL backup endpoint and maintenance ping toggles

## Tech Stack
- Backend: Java 17, Spring Boot 3.4, Spring Security, Spring Data JPA, Flyway
- Database: PostgreSQL
- Auth/Security: JWT (`jjwt`), OAuth2 client (Google)
- Frontend: React 18, TypeScript, Redux Toolkit, React Router, Vite
- Deployment artifacts: Dockerfile (backend), Render config (`render.yaml`), Vercel config (`frontend/vercel.json`)

## Architecture Overview
- Frontend (`frontend/`): SPA for authenticated user workflows
- Backend (`src/main/java`): REST API, domain services, persistence, security
- Database (`src/main/resources/db/migration`): versioned schema migrations
- External market data integrations:
  - CoinGecko (historical sync)
  - Coinbase and Gate.io (spot prices)

## Repository Structure
```text
.
├── src/
│   ├── main/java/com/trading        # API controllers, services, domain, security
│   └── main/resources/db/migration  # Flyway SQL migrations (V1...V14)
├── frontend/                        # React + TypeScript + Vite client app
├── Dockerfile                       # Backend container build
├── render.yaml                      # Render deployment configuration
├── ENVIRONMENT_VARIABLES.md         # Backend/frontend environment variable matrix
└── pom.xml                          # Backend build and dependencies
```

## Prerequisites
- Java 17+
- Maven 3.9+
- Node.js 18+ and npm
- PostgreSQL 14+ (local or remote)

## Local Development
### 1) Backend configuration
Use the provided template:

```bash
cp .env.backend.example .env.backend.local
```

Then set values in your shell or env loader (at minimum DB credentials and secure JWT values for non-sample usage).

### 2) Frontend configuration
```bash
cd frontend
cp .env.example .env.local
```

Adjust `VITE_API_BASE_URL` if your backend is not running on `http://localhost:8080`.

### 3) Run backend
From repository root:

```bash
mvn -q -DskipTests spring-boot:run -Dspring-boot.run.profiles=local
```

Default API URL: `http://localhost:8080`

### 4) Run frontend
From `frontend/`:

```bash
npm run dev
```

Default frontend URL: `http://localhost:5173`

## Environment Variables
See the complete variable matrix in [`ENVIRONMENT_VARIABLES.md`](./ENVIRONMENT_VARIABLES.md).

Primary groups:
- Backend runtime and DB (`APP_NAME`, `SERVER_PORT`, `DB_*`, `FLYWAY_ENABLED`)
- Security and auth (`JWT_*`, `GOOGLE_CLIENT_*`, `CORS_ALLOWED_ORIGIN`)
- External providers (`APP_COINGECKO_*`, spot-price provider URLs/timeouts)
- Frontend runtime (`VITE_API_BASE_URL`, `VITE_GOOGLE_AUTH_START_URL`)

## API Overview
Base URL (local): `http://localhost:8080`

- Health/Public
  - `GET /`
  - `GET /health`
  - `GET /api/public/ping`

- Authentication (`/api/auth`)
  - `POST /register`, `POST /login`, `POST /refresh`, `POST /logout`, `GET /me`
  - `GET /oauth2/google`, `GET /oauth2/callback`

- Transactions (`/api/transactions`)
  - `GET /`, `POST /buy`, `POST /sell`, `PUT /{transactionId}`, `DELETE /{transactionId}`
  - `POST /clean-history` (Excel file response)

- Accumulation Trades (`/api/accumulation-trades`)
  - `GET /`, `POST /open`, `POST /close`

- Strategies (`/api/strategies`)
  - Sell: `GET /sell`, `POST /sell`, `DELETE /sell/{id}`
  - Buy: `GET /buy`, `POST /buy`, `DELETE /buy/{id}`
  - Alerts: `GET /alerts`, `POST /alerts/generate`, `POST /alerts/{id}/acknowledge`, `DELETE /alerts/{id}`

- Portfolio (`/api/portfolio`)
  - `GET /summary`, `GET /performance`, `GET /asset-summary`

- Market Alerts (`/api/market-alerts`)
  - `GET /`, `GET /summary`, `POST /scan`, `POST /{id}/view`, `DELETE /`

- Lookup / Reference Data (`/api`)
  - Assets: `GET/POST /assets`, `GET/PUT/DELETE /assets/{assetId}`
  - Exchanges: `GET/POST /exchanges`, `GET/PUT/DELETE /exchanges/{exchangeId}`

- Price Peaks (`/api/price-peaks`)
  - `GET /`, `PUT /{id}`, `DELETE /{id}`

- Spot Prices (`/api/prices`)
  - `GET /spot`

- Historical Data (`/api/historical-data`)
  - `GET /`, `GET /missing-today`, `POST /refresh`, `POST /clean-reset`

- User Preferences (`/api/user-preferences`)
  - `GET /`, `PUT /`

- System/Admin
  - `GET /api/system/sql-backup`
  - `GET /api/admin/ping-status`, `POST /api/admin/toggle-ping`

## Database and Migrations
- Flyway migrations are located in `src/main/resources/db/migration`.
- The app expects a PostgreSQL datasource (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
- Default connection in `application.yml` points to `trading_app` with `sslmode=require`.

## Testing and Build Commands
Backend:
```bash
mvn test
```

Frontend:
```bash
cd frontend
npm run build
```

## Deployment Notes
- Backend Docker image is built via multi-stage `Dockerfile` (Maven build + JRE runtime).
- `render.yaml` defines a Render web service using the Dockerfile and `/health` check.
- Frontend contains `frontend/vercel.json` for Vercel hosting configuration.

## Security Notes
- Replace default/sample JWT and OAuth secrets before any shared/staging/production deployment.
- Restrict `CORS_ALLOWED_ORIGIN` to trusted frontend origins.
- Keep `.env` files out of version control.

## Contributing
Contributions are welcome through pull requests with clear descriptions and reproducible validation steps.

## License
TBD
