-- GOOD — V4__add_shipping_address_to_orders.sql
-- Correct double-underscore naming, nullable column, multi-step rename.
-- The index for shipping_address lives in its own file:
-- concurrent-index-migration.sql (V5). CREATE INDEX CONCURRENTLY must not
-- share a migration with transactional statements under Flyway's default mixed=false policy.

-- Step 1: Add nullable column (usually no rewrite, but still requires a table lock)
ALTER TABLE orders ADD COLUMN shipping_address TEXT;

-- Step 2: Multi-step column rename (safe approach)
-- Never use ALTER TABLE RENAME COLUMN directly — it breaks code mid-deploy
-- Instead: add new → deploy compatible writers → backfill/reconcile → switch readers → drop old
-- (assumes a legacy full_name column from an earlier migration)
ALTER TABLE orders ADD COLUMN customer_name VARCHAR(255);

-- Step 3: Execute separately AFTER compatible writers are deployed.
-- Repeat bounded batches with a stable cursor and verify concurrent updates.
-- UPDATE orders SET customer_name = full_name WHERE customer_name IS NULL
--   AND id IN (SELECT id FROM orders WHERE customer_name IS NULL ORDER BY id LIMIT 1000);

-- Note: The DROP COLUMN for 'full_name' goes in a LATER migration
-- after all application code reads from 'customer_name'
