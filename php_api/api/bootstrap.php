<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/ThemeEngineService.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();
    $clientVersion = trim((string)($_GET['config_version'] ?? ''));
    $country = trim((string)($_GET['country'] ?? 'India'));
    $state = trim((string)($_GET['state'] ?? ''));
    $city = trim((string)($_GET['city'] ?? ''));

    $version = MelvoreThemeEngineService::computeConfigVersion($pdo);
    $worlds = [];
    if (MelvoreDB::tableExists('app_style_worlds')) {
        $wStmt = $pdo->query(
            'SELECT * FROM app_style_worlds
             WHERE is_active = 1
               AND (start_date IS NULL OR start_date <= NOW())
               AND (end_date IS NULL OR end_date >= NOW())
             ORDER BY display_order ASC, style_world_id ASC'
        );
        foreach ($wStmt->fetchAll() as $w) {
            $worlds[] = MelvoreThemeEngineService::formatWorldTheme($w);
        }
    }

    $occasions = MelvoreThemeEngineService::fetchActiveOccasions($pdo, $country, $state, $city);
    $assets = MelvoreThemeEngineService::fetchWorldAssets($pdo);
    $origins = MelvoreThemeEngineService::fetchWorldMapOrigins($pdo);

    $smartOffers = [];
    if (MelvoreDB::tableExists('app_smart_offers')) {
        $oStmt = $pdo->query(
            'SELECT * FROM app_smart_offers
             WHERE is_active = 1
               AND (start_date IS NULL OR start_date <= NOW())
               AND (end_date IS NULL OR end_date >= NOW())
             ORDER BY priority DESC, offer_id ASC LIMIT 12'
        );
        foreach ($oStmt->fetchAll() as $of) {
            $smartOffers[] = [
                'offer_id' => (int)$of['offer_id'],
                'offer_key' => (string)$of['offer_key'],
                'title' => (string)$of['title'],
                'subtitle' => (string)($of['subtitle'] ?? ''),
                'badge_text' => (string)($of['badge_text'] ?? 'OFFER'),
                'coupon_code' => $of['coupon_code'] ?? null,
                'world_slug' => $of['world_slug'] ?? null,
                'occasion_slug' => $of['occasion_slug'] ?? null,
                'min_cart_value' => (float)($of['min_cart_value'] ?? 0),
                'max_impressions_per_session' => max(1, (int)($of['max_impressions_per_session'] ?? 1)),
                'cooldown_hours' => max(1, (int)($of['cooldown_hours'] ?? 24)),
                'priority' => (int)($of['priority'] ?? 10),
                'accent_hex' => (string)($of['accent_hex'] ?? '#3B82F6'),
                'cta_text' => (string)($of['cta_text'] ?? 'Apply Offer'),
            ];
        }
    }

    $submittedWorldIds = [];
    $submittedWorldSlugs = [];
    if (($userId !== null || $guestId !== '') && MelvoreDB::tableExists('app_style_world_feedback')) {
        $fSql = $userId !== null
            ? 'SELECT style_world_id, world_slug FROM app_style_world_feedback WHERE user_id = ?'
            : 'SELECT style_world_id, world_slug FROM app_style_world_feedback WHERE guest_id = ?';
        $fStmt = $pdo->prepare($fSql);
        $fStmt->execute([$userId ?? $guestId]);
        foreach ($fStmt->fetchAll() as $fr) {
            $submittedWorldIds[] = (int)$fr['style_world_id'];
            $submittedWorldSlugs[] = (string)$fr['world_slug'];
        }
    }
    $submittedWorldIds = array_values(array_unique($submittedWorldIds));
    $submittedWorldSlugs = array_values(array_unique($submittedWorldSlugs));
    foreach ($worlds as &$wRef) {
        $wid = (int)($wRef['style_world_id'] ?? 0);
        $wRef['has_submitted_feedback'] = in_array($wid, $submittedWorldIds, true);
    }
    unset($wRef);

    $userPrefs = null;
    if (($userId !== null || $guestId !== '') && MelvoreDB::tableExists('app_user_preferences')) {
        if ($userId !== null) {
            $pStmt = $pdo->prepare('SELECT * FROM app_user_preferences WHERE user_id = ? LIMIT 1');
            $pStmt->execute([$userId]);
        } else {
            $pStmt = $pdo->prepare('SELECT * FROM app_user_preferences WHERE guest_id = ? ORDER BY updated_at DESC LIMIT 1');
            $pStmt->execute([$guestId]);
        }
        $pr = $pStmt->fetch();
        if ($pr) {
            $userPrefs = [
                'ambient_audio_enabled' => ((int)($pr['ambient_audio_enabled'] ?? 0)) === 1,
                'ui_sfx_enabled' => ((int)($pr['ui_sfx_enabled'] ?? 0)) === 1,
                'reduced_motion' => ((int)($pr['reduced_motion'] ?? 0)) === 1,
                'video_autoplay_muted' => isset($pr['video_autoplay_muted']) ? (((int)$pr['video_autoplay_muted']) === 1) : true,
                'data_saver_mode' => isset($pr['data_saver_mode']) ? (((int)$pr['data_saver_mode']) === 1) : false,
                'audio_consent_answered' => ((int)($pr['audio_consent_answered'] ?? 0)) === 1,
                'preferred_world_slug' => $pr['preferred_world_slug'] ?? null,
            ];
        }
    }

    $resolvedDesign = MelvoreDesignSystemService::resolvePublishedManifest(
        $pdo,
        null,
        trim((string)($_GET['schema_version'] ?? MelvoreDesignSystemService::SUPPORTED_SCHEMA_VERSION))
    );

    // Ensure every world defined in the active design manifest (e.g. pixel, grunge, old-money, party-wear)
    // is surfaced even if the DB table has not yet been re-seeded, while DB rows always take priority.
    $manifestWorlds = $resolvedDesign['manifest']['worlds'] ?? [];
    if (is_array($manifestWorlds) && !empty($manifestWorlds)) {
        $existingSlugs = [];
        foreach ($worlds as $existingWorld) {
            $s = strtolower(trim((string)($existingWorld['slug'] ?? '')));
            if ($s !== '') {
                $existingSlugs[$s] = true;
            }
        }
        $nextOrder = count($worlds) + 1;
        foreach ($manifestWorlds as $wSlug => $wSpec) {
            $cleanSlug = strtolower(trim((string)$wSlug));
            if ($cleanSlug === '' || isset($existingSlugs[$cleanSlug]) || !is_array($wSpec)) {
                continue;
            }
            $kit = is_array($wSpec['kit'] ?? null) ? $wSpec['kit'] : [];
            $pal = is_array($kit['palette'] ?? null) ? $kit['palette'] : [];
            $typo = is_array($kit['typography'] ?? null) ? $kit['typography'] : [];
            $label = ucwords(str_replace('-', ' ', $cleanSlug));
            $heroRel = (string)($kit['hero_image'] ?? '');
            $heroUrl = $heroRel !== '' ? MelvoreResponse::mediaUrl($heroRel) : null;
            $mode = strtolower((string)($pal['mode'] ?? 'dark'));
            $worlds[] = [
                'style_world_id' => 0,
                'slug' => $cleanSlug,
                'name' => $label,
                'tagline' => ucfirst(str_replace('_', ' ', (string)($wSpec['template'] ?? 'Editorial World'))),
                'description' => '',
                'theme_key' => (string)($wSpec['template'] ?? $cleanSlug),
                'mode' => ($mode === 'light') ? 'light' : 'dark',
                'animation_key' => (string)($kit['motion_preset'] ?? 'smooth_reveal'),
                'font_style' => (string)($typo['display_font'] ?? ($wSpec['font_style'] ?? 'editorial_serif')),
                'button_style' => (string)($kit['button_skin'] ?? ($wSpec['button_style'] ?? 'editorial_sharp')),
                'card_radius_dp' => (int)($wSpec['card_radius_dp'] ?? 8),
                'primary_color' => (string)($pal['text_primary'] ?? '#FFFFFF'),
                'accent_color' => (string)($pal['accent'] ?? '#D4AF37'),
                'secondary_accent' => (string)($pal['secondary_accent'] ?? '#94A3B8'),
                'background_color' => (string)($pal['background'] ?? '#0E0D1B'),
                'surface_color' => (string)($pal['surface'] ?? '#181632'),
                'surface_elevated' => (string)($pal['surface'] ?? '#181632'),
                'text_primary' => (string)($pal['text_primary'] ?? '#FFFFFF'),
                'text_secondary' => (string)($pal['text_secondary'] ?? '#A1A1AA'),
                'border_color' => (string)($pal['border'] ?? '#27272A'),
                'ambient_preset' => $cleanSlug,
                'audio_track_key' => null,
                'cover_image_url' => $heroUrl,
                'mobile_image_url' => $heroUrl,
                'video_url' => null,
                'display_order' => $nextOrder++,
                'has_submitted_feedback' => in_array($cleanSlug, $submittedWorldSlugs, true),
            ];
            $existingSlugs[$cleanSlug] = true;
        }
    }

    MelvoreResponse::ok([
        'config_version' => $version,
        'unchanged' => ($clientVersion !== '' && hash_equals($version, $clientVersion)),
        'server_time_utc' => gmdate('c'),
        'server_time_ist' => (new DateTimeImmutable('now', new DateTimeZone('Asia/Kolkata')))->format('c'),
        'active_design' => $resolvedDesign['active_design'],
        'design_slot' => $resolvedDesign['resolved_slot'],
        'design_version' => (string)($resolvedDesign['manifest']['design_version'] ?? '1.0.0'),
        'design_checksum' => $resolvedDesign['checksum_sha256'],
        'design_manifest' => $resolvedDesign['manifest'],
        'default_theme' => MelvoreThemeEngineService::loadDefaultTheme($pdo),
        'active_occasion' => $occasions[0] ?? null,
        'occasions' => $occasions,
        'fashion_worlds' => $worlds,
        'submitted_world_ids' => $submittedWorldIds,
        'submitted_world_slugs' => $submittedWorldSlugs,
        'world_assets' => $assets,
        'world_map_origins' => $origins,
        'smart_offers' => $smartOffers,
        'user_preferences' => $userPrefs,
    ], 'Bootstrap configuration loaded');
} catch (Throwable $e) {
    error_log('[Melvore][bootstrap] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to load dynamic theme configuration.', 'SERVER_ERROR', 500);
}
