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

- `V5__seed_assets_and_exchanges.sql`
  - Seed lookup reference data
  - Canonical `assets` and `exchanges` rows for API lookups

- `V6__fix_price_peaks_trigger_for_multi_tenant.sql`
  - Make `reset_price_peak_on_buy` trigger multi-tenant aware
  - Upsert by `(asset_id, user_id)` for `price_peaks`

- `V7__fix_user_portfolio_performance_pnl.sql`
  - Fix `user_portfolio_performance` balance and cost basis math
  - Use net amount consistently and reduce invested basis on SELL rows
