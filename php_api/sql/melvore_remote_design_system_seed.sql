-- ============================================================================
-- MELVORE CENTRAL REMOTE DESIGN SYSTEM & OCCASION ENGINE SEED DATA
-- Safe & Idempotent (Re-runnable on live or staging DBs)
-- ============================================================================

INSERT INTO `app_design_settings`
  (`setting_key`, `active_design`, `min_app_schema_version`)
VALUES
  ('global', 1, '1.0.0')
ON DUPLICATE KEY UPDATE
  `active_design` = 1,
  `min_app_schema_version` = VALUES(`min_app_schema_version`);

-- Backfill any existing app_occasions rows missing event_date / activation_start_at / activation_end_at
UPDATE `app_occasions`
SET
  `activation_days_before` = COALESCE(NULLIF(`activation_days_before`, 0), 8),
  `timezone` = COALESCE(NULLIF(`timezone`, ''), 'Asia/Kolkata'),
  `event_date` = COALESCE(`event_date`, DATE(`end_date`), DATE(`start_date`)),
  `activation_start_at` = COALESCE(
      `activation_start_at`,
      CASE
        WHEN COALESCE(`event_date`, DATE(`end_date`)) IS NOT NULL
        THEN TIMESTAMP(DATE_SUB(COALESCE(`event_date`, DATE(`end_date`)), INTERVAL COALESCE(NULLIF(`activation_days_before`, 0), 8) DAY), '00:00:00')
        ELSE `start_date`
      END
  ),
  `activation_end_at` = COALESCE(
      `activation_end_at`,
      CASE
        WHEN COALESCE(`event_date`, DATE(`end_date`)) IS NOT NULL
        THEN TIMESTAMP(COALESCE(`event_date`, DATE(`end_date`)), '23:59:59')
        ELSE `end_date`
      END
  );

-- Seed flagship automatic 8-day-before occasions (Asia/Kolkata IST)
INSERT INTO `app_occasions`
  (`slug`, `name`, `eyebrow`, `headline`, `subtitle`, `occasion_type`, `motif_key`,
   `primary_color`, `accent_color`, `background_color`, `font_style`, `audio_track_key`,
   `cta_text`, `cta_collection_slug`, `region_country`, `region_state`, `region_city`,
   `priority`, `is_active`, `event_date`, `activation_days_before`, `activation_start_at`, `activation_end_at`, `timezone`,
   `start_date`, `end_date`, `theme_config`)
VALUES
  (
    'diwali-2026',
    'Diwali Atelier Lights',
    'FESTIVE EDITION · 8 DAYS OF LIGHT',
    'The Golden Festive Edit',
    'Silk, zardozi metallic shimmer, and luminous evening tailoring.',
    'festival',
    'diya_glow',
    '#FFF8EA',
    '#E6B34B',
    '#1B1224',
    'editorial_serif',
    'festive',
    'Shop Festive Edit',
    'festive-luxe',
    'India',
    '',
    '',
    95,
    1,
    '2026-10-20',
    8,
    '2026-10-12 00:00:00',
    '2026-10-20 23:59:59',
    'Asia/Kolkata',
    '2026-10-12 00:00:00',
    '2026-10-20 23:59:59',
    '{"button_style":"pill_luxe","banner_motif":"diya_glow","banner_badge":"DIWALI EDITION"}'
  ),
  (
    'ganesh-chaturthi-maharashtra-2026',
    'Ganeshotsav Heritage Edit',
    'MAHARASHTRA EXCLUSIVE',
    'Saffron & Handloom Capsule',
    'Regional artisan weaves and ceremonial silhouettes.',
    'regional_festival',
    'floral_garland',
    '#FFFBF5',
    '#E07A2F',
    '#24140E',
    'editorial_serif',
    'festive',
    'Explore Heritage',
    'heritage-edit',
    'India',
    'Maharashtra',
    '',
    88,
    1,
    '2026-09-14',
    8,
    '2026-09-06 00:00:00',
    '2026-09-14 23:59:59',
    'Asia/Kolkata',
    '2026-09-06 00:00:00',
    '2026-09-14 23:59:59',
    '{"button_style":"editorial_sharp","banner_motif":"floral_garland","banner_badge":"MAHARASHTRA"}'
  ),
  (
    'holiday-gala-2026',
    'Winter Solstice Gala',
    'GLOBAL SEASONAL ISSUE',
    'After-Hours Velvet & Chrome',
    'Architectural eveningwear crafted for winter nights.',
    'seasonal',
    'starlight',
    '#F8FAFC',
    '#D4AF37',
    '#0E1118',
    'editorial_serif',
    'luxe_ambient',
    'Shop Gala Edit',
    'party-wear',
    'ALL',
    '',
    '',
    80,
    1,
    '2026-12-25',
    8,
    '2026-12-17 00:00:00',
    '2026-12-25 23:59:59',
    'Asia/Kolkata',
    '2026-12-17 00:00:00',
    '2026-12-25 23:59:59',
    '{"button_style":"pill_luxe","banner_motif":"starlight","banner_badge":"HOLIDAY GALA"}'
  )
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `eyebrow` = VALUES(`eyebrow`),
  `headline` = VALUES(`headline`),
  `subtitle` = VALUES(`subtitle`),
  `event_date` = VALUES(`event_date`),
  `activation_days_before` = VALUES(`activation_days_before`),
  `activation_start_at` = VALUES(`activation_start_at`),
  `activation_end_at` = VALUES(`activation_end_at`),
  `timezone` = VALUES(`timezone`),
  `region_country` = VALUES(`region_country`),
  `region_state` = VALUES(`region_state`),
  `priority` = VALUES(`priority`),
  `is_active` = VALUES(`is_active`);

-- Seed starter asset entries matching php_api/design-system/assets/
INSERT INTO `app_design_assets`
  (`asset_key`, `asset_type`, `title`, `relative_path`, `mime_type`, `file_size_bytes`, `is_active`)
VALUES
  ('default_editorial_hero', 'image', 'Default Editorial Atelier Hero', '/design-system/assets/images/default-editorial-hero.svg', 'image/svg+xml', 890, 1),
  ('icon_registry_pack_v1', 'icon', 'Core Navigation Icon Registry', '/design-system/assets/icons/icon-registry.json', 'application/json', 1420, 1),
  ('motion_presets_v1', 'animation', 'Declarative Editorial Motion Presets', '/design-system/assets/animations/motion-presets.json', 'application/json', 480, 1)
ON DUPLICATE KEY UPDATE
  `relative_path` = VALUES(`relative_path`),
  `is_active` = 1;
