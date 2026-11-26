-- ============================================================================
-- MELVORE — FIXED DELIVERY SERVICEABILITY CACHE SEED
-- Prerequisite: Run `melvore_live_schema_compatibility_repair.sql` first.
--
-- Idempotent seed across key Indian metro pincodes using INSERT IGNORE
-- so existing verified cache entries remain intact.
-- ============================================================================

SET NAMES utf8mb4;

INSERT IGNORE INTO `app_delivery_serviceability_cache`
  (`pincode`, `city`, `state`, `is_serviceable`, `cod_available`, `express_available`, `estimated_days_min`, `estimated_days_max`)
VALUES
  ('110001', 'New Delhi', 'Delhi', 1, 1, 1, 2, 4),
  ('110033', 'New Delhi', 'Delhi', 1, 1, 1, 1, 3),
  ('400001', 'Mumbai', 'Maharashtra', 1, 1, 1, 2, 4),
  ('560001', 'Bengaluru', 'Karnataka', 1, 1, 1, 2, 4),
  ('500001', 'Hyderabad', 'Telangana', 1, 1, 1, 2, 5),
  ('600001', 'Chennai', 'Tamil Nadu', 1, 1, 0, 3, 5),
  ('700001', 'Kolkata', 'West Bengal', 1, 1, 0, 3, 6),
  ('411001', 'Pune', 'Maharashtra', 1, 1, 1, 2, 4),
  ('302001', 'Jaipur', 'Rajasthan', 1, 1, 0, 3, 5),
  ('380001', 'Ahmedabad', 'Gujarat', 1, 1, 1, 2, 4);

SELECT COUNT(*) AS delivery_cache_rows FROM `app_delivery_serviceability_cache`;
