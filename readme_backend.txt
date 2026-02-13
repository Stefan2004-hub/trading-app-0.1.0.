Backend Start:
mvn spring-boot:run
Database Location:
- Uses external PostgreSQL (not Docker)
- Default: localhost:5432/trading_app
- Configured in src/main/resources/application.yml:5
Database Details:
- Database: trading_app
- User: trading_user
- Password: trading_password
- Migrations: Flyway (enabled, files in src/main/resources/db/migration/)
- You need to have PostgreSQL running locally or set DB_URL, DB_USERNAME, DB_PASSWORD environment variables to point to your database.

--how to tun in terminal
mvn -q -DskipTests spring-boot:run -Dspring-boot.run.profiles=local