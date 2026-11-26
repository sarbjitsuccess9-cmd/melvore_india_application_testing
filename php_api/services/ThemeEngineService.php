<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/includes/db.php';
require_once dirname(__DIR__) . '/includes/response.php';

require_once __DIR__ . '/DesignSystemService.php';

/**
 * Dynamic Fashion Worlds Theme, Occasion, Campaign, Vector Asset, World Map & Offer Resolver.
 */
final class MelvoreThemeEngineService
{
    public static function computeConfigVersion(PDO $pdo): string
    {
        $parts = ['v4', 'design:' . MelvoreDesignSystemService::computeManifestVersionToken($pdo)];
        foreach (['app_style_worlds', 'app_campaigns', 'app_occasions', 'app_smart_offers', 'app_world_assets', 'app_brand_origins', 'app_category_editorial_configs', 'app_elite_packs', 'app_design_settings'] as $tbl) {
            if (MelvoreDB::tableExists($tbl)) {
                $cnt = (int)$pdo->query("SELECT COUNT(*) FROM `{$tbl}`")->fetchColumn();
                $parts[] = $tbl . ':' . $cnt;
            }
        }
        return substr(hash('sha256', implode('|', $parts)), 0, 16);
    }

    public static function loadDefaultTheme(?PDO $pdo = null): array
    {
        if ($pdo !== null) {
            $resolved = MelvoreDesignSystemService::resolvePublishedManifest($pdo);
            $c = $resolved['manifest']['tokens']['colors'] ?? [];
            $t = $resolved['manifest']['tokens']['typography'] ?? [];
            $r = $resolved['manifest']['tokens']['radii'] ?? [];
            $m = $resolved['manifest']['tokens']['motion'] ?? [];
            $btn = $resolved['manifest']['components']['button'] ?? [];
            return [
                'theme_key' => (string)($resolved['manifest']['manifest_id'] ?? 'melvore_default'),
                'name' => $resolved['active_design'] === 1 ? 'Melvore Live Storefront' : 'Melvore Default Storefront',
                'active_design' => $resolved['active_design'],
                'slot' => $resolved['resolved_slot'],
                'design_version' => (string)($resolved['manifest']['design_version'] ?? '1.0.0'),
                'mode' => (string)($c['mode'] ?? 'light'),
                'background_color' => (string)($c['background'] ?? '#FFFFFF'),
                'surface_color' => (string)($c['surface'] ?? '#F7F7FA'),
                'surface_elevated' => (string)($c['surface_elevated'] ?? '#EFEFF5'),
                'primary_color' => (string)($c['primary'] ?? '#09090B'),
                'accent_color' => (string)($c['accent'] ?? '#C9A24B'),
                'secondary_accent' => (string)($c['secondary_accent'] ?? '#9CA3AF'),
                'text_primary' => (string)($c['text_primary'] ?? '#09090B'),
                'text_secondary' => (string)($c['text_secondary'] ?? '#52525B'),
                'border_color' => (string)($c['border'] ?? '#E4E4E7'),
                'font_style' => (string)($t['display_font'] ?? 'editorial_serif'),
                'button_style' => (string)($btn['primary_variant'] ?? 'editorial_sharp'),
                'card_radius_dp' => (int)($r['card_dp'] ?? 6),
                'border_width_dp' => (int)($r['border_width_dp'] ?? 1),
                'ambient_preset' => 'none',
                'audio_track_key' => 'none',
                'motion_intensity' => ((string)($m['intensity'] ?? 'subtle')) === 'none' ? 0.0 : (((string)($m['intensity'] ?? 'subtle')) === 'expressive' ? 0.9 : 0.65),
                'motion_preset' => (string)($m['intensity'] ?? 'subtle'),
            ];
        }

        return [
            'theme_key' => 'melvore_default',
            'name' => 'Melvore Storefront',
            'active_design' => 0,
            'slot' => 'default',
            'design_version' => '1.0.0',
            'mode' => 'light',
            'background_color' => '#FFFFFF',
            'surface_color' => '#F7F7FA',
            'surface_elevated' => '#EFEFF5',
            'primary_color' => '#09090B',
            'accent_color' => '#C9A24B',
            'secondary_accent' => '#9CA3AF',
            'text_primary' => '#09090B',
            'text_secondary' => '#52525B',
            'border_color' => '#E4E4E7',
            'font_style' => 'editorial_serif',
            'button_style' => 'editorial_sharp',
            'card_radius_dp' => 6,
            'border_width_dp' => 1,
            'ambient_preset' => 'none',
            'audio_track_key' => 'none',
            'motion_intensity' => 0.65,
            'motion_preset' => 'subtle',
        ];
    }

    public static function formatWorldTheme(array $row): array
    {
        $slug = strtolower(trim((string)($row['slug'] ?? '')));
        $bg = (string)($row['background_color'] ?? '#141418');
        $isDark = self::isDarkHex($bg);

        $cfg = [];
        if (!empty($row['theme_config'])) {
            $decoded = is_string($row['theme_config']) ? json_decode($row['theme_config'], true) : $row['theme_config'];
            if (is_array($decoded)) {
                $cfg = $decoded;
            }
        }
        $runwayCfg = null;
        if (!empty($row['runway_config'])) {
            $decRunway = is_string($row['runway_config']) ? json_decode($row['runway_config'], true) : $row['runway_config'];
            if (is_array($decRunway)) {
                $runwayCfg = $decRunway;
            }
        } elseif (!empty($cfg['runway_config']) && is_array($cfg['runway_config'])) {
            $runwayCfg = $cfg['runway_config'];
        }

        $promoPoster = $row['promo_video_poster_url'] ?? ($row['mobile_image_url'] ?? ($row['cover_image_url'] ?? null));

        return [
            'style_world_id' => (int)($row['style_world_id'] ?? ($row['id'] ?? 0)),
            'slug' => $slug,
            'name' => (string)($row['name'] ?? ''),
            'tagline' => (string)($row['tagline'] ?? ''),
            'description' => (string)($row['description'] ?? ''),
            'theme_key' => (string)($row['theme_key'] ?? ($cfg['theme_key'] ?? $slug)),
            'mode' => (string)($cfg['mode'] ?? ($isDark ? 'dark' : 'light')),
            'animation_key' => (string)($row['animation_key'] ?? 'smooth_reveal'),
            'font_style' => (string)($row['font_style'] ?? 'editorial_serif'),
            'button_style' => (string)(!empty($row['button_style']) ? $row['button_style'] : ($cfg['button_style'] ?? 'editorial_pill')),
            'card_radius_dp' => (int)($cfg['card_radius_dp'] ?? ($row['card_radius_dp'] ?? 16)),
            'border_width_dp' => (int)($cfg['border_width_dp'] ?? 1),
            'primary_color' => (string)($row['primary_color'] ?? '#FFFFFF'),
            'accent_color' => (string)($row['accent_color'] ?? '#D4AF37'),
            'secondary_accent' => (string)($cfg['secondary_accent'] ?? '#9CA3AF'),
            'background_color' => $bg,
            'surface_color' => (string)($cfg['surface_color'] ?? ($isDark ? '#14141D' : '#F8F7FA')),
            'surface_elevated' => (string)($cfg['surface_elevated'] ?? ($isDark ? '#1E1E2B' : '#FFFFFF')),
            'text_primary' => (string)($cfg['text_primary'] ?? ($isDark ? '#FFFFFF' : '#121216')),
            'text_secondary' => (string)($cfg['text_secondary'] ?? ($isDark ? '#D4D4D8' : '#52525B')),
            'border_color' => (string)($cfg['border_color'] ?? ($isDark ? '#2E2E3E' : '#E5E7EB')),
            'ambient_preset' => (string)($row['ambient_preset'] ?? ($cfg['ambient_preset'] ?? $slug)),
            'audio_track_key' => (string)($row['audio_track_key'] ?? ($cfg['audio_track_key'] ?? $slug)),
            'audio_url' => MelvoreResponse::mediaUrl($row['audio_url'] ?? ($cfg['audio_url'] ?? null)),
            'audio_loop' => isset($row['audio_loop']) ? (((int)$row['audio_loop']) === 1) : true,
            'audio_volume' => isset($row['audio_volume']) ? max(0.05, min(1.0, (float)$row['audio_volume'])) : 0.45,
            'sfx_profile' => (string)($cfg['sfx_profile'] ?? $slug),
            'hero_layout' => (string)($cfg['hero_layout'] ?? 'asymmetrical_split'),
            'cover_image_url' => MelvoreResponse::mediaUrl($row['cover_image_url'] ?? ($row['hero_image_url'] ?? null)),
            'mobile_image_url' => MelvoreResponse::mediaUrl($row['mobile_image_url'] ?? ($row['cover_image_url'] ?? null)),
            'video_url' => MelvoreResponse::mediaUrl($row['video_url'] ?? null),
            'promo_video_url' => MelvoreResponse::mediaUrl($row['promo_video_url'] ?? ($row['video_url'] ?? null)),
            'promo_video_poster_url' => MelvoreResponse::mediaUrl($promoPoster),
            'promo_video_is_sponsored' => isset($row['promo_video_is_sponsored']) ? (((int)$row['promo_video_is_sponsored']) === 1) : false,
            'runway_config' => $runwayCfg,
            'display_order' => (int)($row['display_order'] ?? 0),
            'version_tag' => (string)($row['version_tag'] ?? '1.0'),
            'theme_config' => $cfg,
        ];
    }

    /**
     * Computes deterministic activation_start_at / activation_end_at in the occasion timezone
     * (default Asia/Kolkata), applying event_date - activation_days_before (default 8 days).
     */
    public static function computeOccasionWindow(array $row): array
    {
        $tzName = trim((string)($row['timezone'] ?? 'Asia/Kolkata'));
        if ($tzName === '') {
            $tzName = 'Asia/Kolkata';
        }
        try {
            $tz = new DateTimeZone($tzName);
        } catch (Throwable) {
            $tzName = 'Asia/Kolkata';
            $tz = new DateTimeZone($tzName);
        }

        $daysBefore = isset($row['activation_days_before']) && is_numeric($row['activation_days_before'])
            ? max(0, (int)$row['activation_days_before'])
            : 8;
        $eventDate = trim((string)($row['event_date'] ?? ''));
        $rawStart = trim((string)($row['activation_start_at'] ?? ($row['start_date'] ?? '')));
        $rawEnd = trim((string)($row['activation_end_at'] ?? ($row['end_date'] ?? '')));

        $startDt = null;
        $endDt = null;

        if ($rawStart !== '') {
            try {
                $startDt = new DateTimeImmutable($rawStart, $tz);
            } catch (Throwable) {
                $startDt = null;
            }
        }
        if ($startDt === null && $eventDate !== '') {
            try {
                $eventDt = new DateTimeImmutable($eventDate . ' 00:00:00', $tz);
                $startDt = $eventDt->modify("-{$daysBefore} days")->setTime(0, 0, 0);
            } catch (Throwable) {
                $startDt = null;
            }
        }

        if ($rawEnd !== '') {
            try {
                $endDt = new DateTimeImmutable($rawEnd, $tz);
            } catch (Throwable) {
                $endDt = null;
            }
        }
        if ($endDt === null && $eventDate !== '') {
            try {
                $eventDt = new DateTimeImmutable($eventDate . ' 00:00:00', $tz);
                $endDt = $eventDt->setTime(23, 59, 59);
            } catch (Throwable) {
                $endDt = null;
            }
        }

        return [
            'timezone' => $tzName,
            'tz' => $tz,
            'event_date' => $eventDate !== '' ? $eventDate : null,
            'activation_days_before' => $daysBefore,
            'start_dt' => $startDt,
            'end_dt' => $endDt,
        ];
    }

    /**
     * Public occasion resolution — strictly ignores client clock/simulation params
     * and evaluates server NOW() in each occasion's timezone (Asia/Kolkata default).
     */
    public static function fetchActiveOccasions(PDO $pdo, string $country = 'India', string $state = '', string $city = ''): array
    {
        return self::evaluateOccasionsAtTimestamp($pdo, null, $country, $state, $city, false);
    }

    /**
     * Admin-only occasion scheduler simulator. Never modifies live DB.
     */
    public static function simulateOccasionsForAdmin(
        PDO $pdo,
        string $simulatedDatetime,
        string $country = 'India',
        string $state = '',
        string $city = ''
    ): array {
        return self::evaluateOccasionsAtTimestamp($pdo, $simulatedDatetime, $country, $state, $city, true);
    }

    private static function evaluateOccasionsAtTimestamp(
        PDO $pdo,
        ?string $simulatedDatetime,
        string $country,
        string $state,
        string $city,
        bool $includeDiagnostics
    ): array {
        if (!MelvoreDB::tableExists('app_occasions')) {
            return $includeDiagnostics ? ['active_occasion' => null, 'active_occasions' => [], 'evaluated' => []] : [];
        }
        $rows = $pdo->query('SELECT * FROM app_occasions ORDER BY priority DESC, occasion_id ASC')->fetchAll() ?: [];
        $activeCandidates = [];
        $diagnostics = [];

        foreach ($rows as $row) {
            $win = self::computeOccasionWindow($row);
            $tz = $win['tz'];
            if ($simulatedDatetime !== null && trim($simulatedDatetime) !== '') {
                try {
                    $now = new DateTimeImmutable(trim($simulatedDatetime), $tz);
                } catch (Throwable) {
                    $now = new DateTimeImmutable('now', $tz);
                }
            } else {
                $now = new DateTimeImmutable('now', $tz);
            }

            $isEnabled = ((int)($row['is_active'] ?? 0)) === 1;
            $inWindow = true;
            if ($win['start_dt'] instanceof DateTimeImmutable && $now < $win['start_dt']) {
                $inWindow = false;
            }
            if ($win['end_dt'] instanceof DateTimeImmutable && $now > $win['end_dt']) {
                $inWindow = false;
            }

            $rCountry = trim((string)($row['region_country'] ?? ''));
            $rState = trim((string)($row['region_state'] ?? ''));
            $rCity = trim((string)($row['region_city'] ?? ''));

            $regionMatch = true;
            $specificityScore = 0;
            if ($rCountry !== '' && strcasecmp($rCountry, 'ALL') !== 0) {
                if ($country === '' || strcasecmp($rCountry, $country) !== 0) {
                    $regionMatch = false;
                } else {
                    $specificityScore += 1;
                }
            }
            if ($rState !== '') {
                if ($state === '' || strcasecmp($rState, $state) !== 0) {
                    $regionMatch = false;
                } else {
                    $specificityScore += 2;
                }
            }
            if ($rCity !== '') {
                if ($city === '' || strcasecmp($rCity, $city) !== 0) {
                    $regionMatch = false;
                } else {
                    $specificityScore += 4;
                }
            }

            $matched = $isEnabled && $inWindow && $regionMatch;
            $formatted = self::formatOccasionRow($row, $win);

            if ($includeDiagnostics) {
                $reason = match (true) {
                    !$isEnabled => 'Disabled (is_active = 0)',
                    !$inWindow => 'Outside activation window (' . ($win['start_dt']?->format('Y-m-d H:i:s') ?? 'any') . ' to ' . ($win['end_dt']?->format('Y-m-d H:i:s') ?? 'any') . ')',
                    !$regionMatch => "Region mismatch (requires {$rCountry}/{$rState}/{$rCity})",
                    default => 'ACTIVE IN WINDOW',
                };
                $diagnostics[] = [
                    'occasion_id' => $formatted['occasion_id'],
                    'slug' => $formatted['slug'],
                    'name' => $formatted['name'],
                    'priority' => $formatted['priority'],
                    'specificity' => $specificityScore,
                    'matched' => $matched,
                    'reason' => $reason,
                    'evaluated_at' => $now->format('Y-m-d H:i:s P'),
                    'window_start' => $win['start_dt']?->format('Y-m-d H:i:s'),
                    'window_end' => $win['end_dt']?->format('Y-m-d H:i:s'),
                ];
            }

            if ($matched) {
                $activeCandidates[] = [
                    'priority' => (int)($row['priority'] ?? 0),
                    'specificity' => $specificityScore,
                    'occasion_id' => (int)($row['occasion_id'] ?? 0),
                    'data' => $formatted,
                ];
            }
        }

        usort($activeCandidates, static function (array $a, array $b): int {
            if ($a['priority'] !== $b['priority']) {
                return $b['priority'] <=> $a['priority'];
            }
            if ($a['specificity'] !== $b['specificity']) {
                return $b['specificity'] <=> $a['specificity'];
            }
            return $a['occasion_id'] <=> $b['occasion_id'];
        });

        $list = array_map(static fn (array $c): array => $c['data'], $activeCandidates);
        if ($includeDiagnostics) {
            return [
                'active_occasion' => $list[0] ?? null,
                'active_occasions' => $list,
                'evaluated' => $diagnostics,
            ];
        }
        return $list;
    }

    private static function formatOccasionRow(array $row, array $win): array
    {
        $cfg = [];
        if (!empty($row['theme_config'])) {
            $decoded = is_string($row['theme_config']) ? json_decode($row['theme_config'], true) : $row['theme_config'];
            if (is_array($decoded)) {
                $cfg = $decoded;
            }
        }
        $designOverride = null;
        if (!empty($row['design_override_json'])) {
            $decOvr = is_string($row['design_override_json']) ? json_decode($row['design_override_json'], true) : $row['design_override_json'];
            if (is_array($decOvr)) {
                $designOverride = $decOvr;
            }
        }

        return [
            'occasion_id' => (int)$row['occasion_id'],
            'slug' => (string)$row['slug'],
            'name' => (string)$row['name'],
            'eyebrow' => (string)($row['eyebrow'] ?? ''),
            'headline' => (string)($row['headline'] ?? ''),
            'subtitle' => (string)($row['subtitle'] ?? ''),
            'occasion_type' => (string)($row['occasion_type'] ?? 'festival'),
            'motif_key' => (string)($row['motif_key'] ?? 'confetti'),
            'button_style' => (string)(!empty($row['button_style']) ? $row['button_style'] : ($cfg['button_style'] ?? '')),
            'primary_color' => (string)($row['primary_color'] ?? '#D4AF37'),
            'accent_color' => (string)($row['accent_color'] ?? '#FFFFFF'),
            'background_color' => (string)($row['background_color'] ?? '#140F1C'),
            'font_style' => (string)($row['font_style'] ?? 'editorial_serif'),
            'audio_track_key' => (string)($row['audio_track_key'] ?? 'festive'),
            'audio_url' => MelvoreResponse::mediaUrl($row['audio_url'] ?? ($cfg['audio_url'] ?? null)),
            'audio_loop' => isset($row['audio_loop']) ? (((int)$row['audio_loop']) === 1) : true,
            'cta_text' => (string)($row['cta_text'] ?? 'Shop Edit'),
            'cta_collection_slug' => (string)($row['cta_collection_slug'] ?? ''),
            'banner_image_url' => MelvoreResponse::mediaUrl($row['banner_image_url'] ?? null),
            'priority' => (int)($row['priority'] ?? 0),
            'event_date' => $win['event_date'],
            'activation_days_before' => $win['activation_days_before'],
            'activation_start_at' => $win['start_dt']?->format('Y-m-d H:i:s'),
            'activation_end_at' => $win['end_dt']?->format('Y-m-d H:i:s'),
            'timezone' => $win['timezone'],
            'region_country' => (string)($row['region_country'] ?? 'ALL'),
            'region_state' => (string)($row['region_state'] ?? ''),
            'region_city' => (string)($row['region_city'] ?? ''),
            'start_date' => $row['start_date'] ?? ($win['start_dt']?->format('Y-m-d H:i:s')),
            'end_date' => $row['end_date'] ?? ($win['end_dt']?->format('Y-m-d H:i:s')),
            'theme_config' => $cfg,
            'design_override' => $designOverride,
        ];
    }

    public static function fetchWorldAssets(PDO $pdo): array
    {
        if (!MelvoreDB::tableExists('app_world_assets')) {
            return [];
        }
        $stmt = $pdo->query(
            'SELECT * FROM app_world_assets
             WHERE is_active = 1
             ORDER BY world_slug ASC, display_order ASC, asset_id ASC'
        );
        $assets = [];
        foreach ($stmt->fetchAll() as $row) {
            $meta = [];
            if (!empty($row['metadata_json'])) {
                $decoded = is_string($row['metadata_json']) ? json_decode($row['metadata_json'], true) : $row['metadata_json'];
                if (is_array($decoded)) {
                    $meta = $decoded;
                }
            }
            $assets[] = [
                'asset_id' => (int)$row['asset_id'],
                'asset_key' => (string)$row['asset_key'],
                'world_slug' => (string)$row['world_slug'],
                'occasion_slug' => $row['occasion_slug'] ?? null,
                'vector_type' => (string)$row['vector_type'],
                'layer_role' => (string)($row['layer_role'] ?? 'ambient_floating'),
                'primary_hex' => (string)($row['primary_hex'] ?? '#FFFFFF'),
                'secondary_hex' => (string)($row['secondary_hex'] ?? '#D4AF37'),
                'opacity' => (float)($row['opacity'] ?? 0.35),
                'motion_pattern' => (string)($row['motion_pattern'] ?? 'float_drift'),
                'image_url' => MelvoreResponse::mediaUrl($row['image_url'] ?? null),
                'metadata' => $meta,
            ];
        }
        return $assets;
    }

    public static function fetchWorldMapOrigins(PDO $pdo): array
    {
        $pins = [];
        if (MelvoreDB::tableExists('app_brand_origins')) {
            $hasProfiles = MelvoreDB::tableExists('app_brand_profiles');
            $sql = "SELECT o.*, b.name AS brand_name, b.slug AS brand_slug, b.logo AS brand_logo, b.description AS brand_description,
                           " . ($hasProfiles ? "bp.tagline AS brand_tagline, bp.story AS brand_story, bp.editorial_cover_url" : "NULL AS brand_tagline, NULL AS brand_story, NULL AS editorial_cover_url") . ",
                           (SELECT COUNT(*) FROM products p WHERE p.brand_id = b.brand_id AND p.is_active = 1) AS product_count
                    FROM app_brand_origins o
                    INNER JOIN brands b ON b.brand_id = o.brand_id AND b.status = 'active'
                    " . ($hasProfiles ? "LEFT JOIN app_brand_profiles bp ON bp.brand_id = b.brand_id" : "") . "
                    WHERE o.is_active = 1 AND o.is_verified = 1
                    ORDER BY o.display_order ASC, o.origin_id ASC";
            foreach ($pdo->query($sql)->fetchAll() as $row) {
                $pins[] = [
                    'origin_id' => (int)$row['origin_id'],
                    'brand_id' => (int)$row['brand_id'],
                    'brand_name' => (string)$row['brand_name'],
                    'brand_slug' => (string)$row['brand_slug'],
                    'brand_logo' => MelvoreResponse::mediaUrl($row['brand_logo'] ?? null),
                    'cover_image_url' => MelvoreResponse::mediaUrl($row['editorial_cover_url'] ?? null),
                    'tagline' => (string)($row['brand_tagline'] ?? ''),
                    'story' => (string)($row['origin_story'] ?? ($row['brand_story'] ?? ($row['brand_description'] ?? ''))),
                    'city' => (string)$row['city'],
                    'country' => (string)$row['country'],
                    'country_code' => (string)($row['country_code'] ?? ''),
                    'latitude' => (float)$row['latitude'],
                    'longitude' => (float)$row['longitude'],
                    'world_slug' => (string)($row['world_slug'] ?? ''),
                    'product_count' => (int)($row['product_count'] ?? 0),
                    'is_verified' => true,
                ];
            }
        }
        return $pins;
    }

    private static function isDarkHex(string $hex): bool
    {
        $clean = ltrim(trim($hex), '#');
        if (strlen($clean) === 8) {
            $clean = substr($clean, 2);
        }
        if (strlen($clean) !== 6 || !ctype_xdigit($clean)) {
            return true;
        }
        $r = hexdec(substr($clean, 0, 2));
        $g = hexdec(substr($clean, 2, 2));
        $b = hexdec(substr($clean, 4, 2));
        $luminance = (0.299 * $r + 0.587 * $g + 0.114 * $b) / 255.0;
        return $luminance < 0.52;
    }
}
