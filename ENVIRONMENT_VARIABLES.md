# Environment Variable Matrix

## Backend (.env.backend.example)
| Variable | Required | Default | Purpose |
|---|---|---|---|
| `APP_NAME` | No | `trading-app` | Spring app name |
| `SERVER_PORT` | No | `8080` | HTTP server port |
| `DB_URL` | Yes (non-sample) | `jdbc:postgresql://localhost:5432/trading_app` | PostgreSQL JDBC URL |
| `DB_USERNAME` | Yes (non-sample) | `trading_user` | DB username |
| `DB_PASSWORD` | Yes (non-sample) | `trading_password` | DB password |
| `JPA_DDL_AUTO` | No | `validate` | Hibernate DDL mode |
| `FLYWAY_ENABLED` | No | `true` | Flyway migration toggle |
| `JWT_SECRET` | Yes (production) | sample value in `application.yml` | JWT signing secret |
| `JWT_ACCESS_TOKEN_TTL_MINUTES` | No | `15` | Access token TTL in minutes |
| `JWT_REFRESH_TOKEN_TTL_DAYS` | No | `30` | Refresh token TTL in days |
| `GOOGLE_CLIENT_ID` | Yes (Google login) | sample value in `application.yml` | Google OAuth client id |
| `GOOGLE_CLIENT_SECRET` | Yes (Google login) | sample value in `application.yml` | Google OAuth client secret |
| `CORS_ALLOWED_ORIGIN` | No | `http://localhost:5173` | Allowed frontend origin |

## Frontend (frontend/.env.example)
| Variable | Required | Default | Purpose |
|---|---|---|---|
| `VITE_API_BASE_URL` | Yes | `http://localhost:8080` | Backend API base URL |
| `VITE_GOOGLE_AUTH_START_URL` | No | `http://localhost:8080/api/auth/oauth2/google` | OAuth login entry URL |

## Config Validation Command
Use this command to validate that sample config loads without missing keys:

`mvn -q -DskipTests spring-boot:run -Dspring-boot.run.profiles=sample`
