-- GOOD — V5__index_orders_shipping_address.sql
-- CREATE INDEX CONCURRENTLY builds the index without blocking writes on PostgreSQL.
-- Recommended migration setup; verify with the resolved Flyway and PostgreSQL versions:
--   1. This statement is ALONE in the file. Flyway runs it outside a transaction;
--      mixing transactional statements is rejected by default.
--   2. spring.flyway.postgresql.transactional-lock=false — otherwise the index
--      build can stall on Flyway's advisory-lock transaction. Verify binding.
-- If a CONCURRENTLY build fails midway it leaves an INVALID index behind:
-- Inspect the invalid index and migration history. With approval, remove the failed
-- artifact and reconcile failed history using Flyway's documented recovery before retry.
-- Do not blindly repair history or use IF NOT EXISTS to hide an invalid index.

CREATE INDEX CONCURRENTLY idx_orders_shipping_address
    ON orders (shipping_address)
    WHERE shipping_address IS NOT NULL;
