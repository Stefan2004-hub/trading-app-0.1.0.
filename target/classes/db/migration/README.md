# Flyway Migration Map

This folder stores ordered Flyway SQL migrations.

- `V1__domain_schema.sql`
  - Base domain schema from `ddl_scripts.sql`
  - Core tables, views, functions/triggers for trading domain

- `V2__auth_schema.sql`
  - Authentication and identity tables
  - `users`, `oauth_accounts`, `refresh_tokens`

- `V3__multi_tenant_user_fk.sql`
  - Add `user_id` ownership to user-scoped tables
  - Update dependent views for user scoping

- `V4__indexes_and_constraints.sql`
  - Query-path indexes and supporting constraints
  - Auth lookup and tenant-scoped read/write index paths
