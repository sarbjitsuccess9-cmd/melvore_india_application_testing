<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/ThemeEngineService.php';

MelvoreResponse::requireMethod('GET', 'POST', 'PUT', 'DELETE');

try {
    $pdo = MelvoreDB::pdo();
    MelvoreAuth::requireAdminUserId();
    $method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

    if ($method === 'GET') {
        $stmt = $pdo->query('SELECT * FROM app_style_worlds ORDER BY display_order ASC, style_world_id ASC');
        $worlds = array_map([MelvoreThemeEngineService::class, 'formatWorldTheme'], $stmt->fetchAll());
        MelvoreResponse::ok(['worlds' => $worlds], 'Admin worlds loaded');
    }

    if ($method === 'DELETE') {
        $id = (int)($_GET['id'] ?? (MelvoreResponse::jsonBody()['style_world_id'] ?? 0));
        if ($id <= 0) {
            MelvoreResponse::error('Valid style_world_id required.', 'VALIDATION_ERROR', 400);
        }
        $pdo->prepare('UPDATE app_style_worlds SET is_active = 0 WHERE style_world_id = ?')->execute([$id]);
        MelvoreResponse::ok(['deactivated' => true], 'World deactivated');
    }

    $body = MelvoreResponse::jsonBody();
    $slug = strtolower(trim((string)($body['slug'] ?? '')));
    $name = trim((string)($body['name'] ?? ''));
    if ($slug === '' || $name === '') {
        MelvoreResponse::error('slug and name are required.', 'VALIDATION_ERROR', 400);
    }

    $themeCfg = isset($body['theme_config']) ? json_encode($body['theme_config'], JSON_UNESCAPED_UNICODE) : null;
    $runwayCfg = isset($body['runway_config']) ? json_encode($body['runway_config'], JSON_UNESCAPED_UNICODE) : null;

    $hasRichCols = MelvoreDB::columnExists('app_style_worlds', 'audio_url')
        && MelvoreDB::columnExists('app_style_worlds', 'promo_video_url')
        && MelvoreDB::columnExists('app_style_worlds', 'runway_config')
        && MelvoreDB::columnExists('app_style_worlds', 'button_style');

    if ($hasRichCols) {
        $stmt = $pdo->prepare(
            'INSERT INTO app_style_worlds
                (slug, name, tagline, description, theme_key, animation_key, button_style, font_style, ambient_preset,
                 audio_track_key, audio_url, audio_loop, audio_volume,
                 primary_color, accent_color, background_color, cover_image_url, mobile_image_url,
                 video_url, promo_video_url, promo_video_poster_url, promo_video_is_sponsored,
                 display_order, version_tag, is_active, theme_config, runway_config)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
             ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                tagline = VALUES(tagline),
                description = VALUES(description),
                theme_key = VALUES(theme_key),
                animation_key = VALUES(animation_key),
                button_style = VALUES(button_style),
                font_style = VALUES(font_style),
                ambient_preset = VALUES(ambient_preset),
                audio_track_key = VALUES(audio_track_key),
                audio_url = VALUES(audio_url),
                audio_loop = VALUES(audio_loop),
                audio_volume = VALUES(audio_volume),
                primary_color = VALUES(primary_color),
                accent_color = VALUES(accent_color),
                background_color = VALUES(background_color),
                cover_image_url = VALUES(cover_image_url),
                mobile_image_url = VALUES(mobile_image_url),
                video_url = VALUES(video_url),
                promo_video_url = VALUES(promo_video_url),
                promo_video_poster_url = VALUES(promo_video_poster_url),
                promo_video_is_sponsored = VALUES(promo_video_is_sponsored),
                display_order = VALUES(display_order),
                version_tag = VALUES(version_tag),
                is_active = VALUES(is_active),
                theme_config = VALUES(theme_config),
                runway_config = VALUES(runway_config)'
        );
        $stmt->execute([
            $slug,
            $name,
            (string)($body['tagline'] ?? ''),
            (string)($body['description'] ?? ''),
            (string)($body['theme_key'] ?? $slug),
            (string)($body['animation_key'] ?? 'smooth_reveal'),
            isset($body['button_style']) && $body['button_style'] !== '' ? (string)$body['button_style'] : null,
            (string)($body['font_style'] ?? 'editorial_serif'),
            (string)($body['ambient_preset'] ?? $slug),
            (string)($body['audio_track_key'] ?? $slug),
            !empty($body['audio_url']) ? trim((string)$body['audio_url']) : null,
            array_key_exists('audio_loop', $body) ? (!empty($body['audio_loop']) ? 1 : 0) : 1,
            max(0.05, min(1.0, (float)($body['audio_volume'] ?? 0.45))),
            (string)($body['primary_color'] ?? '#FFFFFF'),
            (string)($body['accent_color'] ?? '#D4AF37'),
            (string)($body['background_color'] ?? '#0B0B10'),
            !empty($body['cover_image_url']) ? trim((string)$body['cover_image_url']) : null,
            !empty($body['mobile_image_url']) ? trim((string)$body['mobile_image_url']) : null,
            !empty($body['video_url']) ? trim((string)$body['video_url']) : null,
            !empty($body['promo_video_url']) ? trim((string)$body['promo_video_url']) : null,
            !empty($body['promo_video_poster_url']) ? trim((string)$body['promo_video_poster_url']) : null,
            !empty($body['promo_video_is_sponsored']) ? 1 : 0,
            (int)($body['display_order'] ?? 0),
            (string)($body['version_tag'] ?? '3.0'),
            isset($body['is_active']) ? (!empty($body['is_active']) ? 1 : 0) : 1,
            $themeCfg,
            $runwayCfg,
        ]);
    } else {
        $stmt = $pdo->prepare(
            'INSERT INTO app_style_worlds
                (slug, name, tagline, description, theme_key, animation_key, font_style, ambient_preset, audio_track_key,
                 primary_color, accent_color, background_color, cover_image_url, mobile_image_url, video_url, display_order, version_tag, is_active, theme_config)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
             ON DUPLICATE KEY UPDATE
                name = VALUES(name), tagline = VALUES(tagline), description = VALUES(description),
                theme_key = VALUES(theme_key), animation_key = VALUES(animation_key), font_style = VALUES(font_style),
                ambient_preset = VALUES(ambient_preset), audio_track_key = VALUES(audio_track_key),
                primary_color = VALUES(primary_color), accent_color = VALUES(accent_color), background_color = VALUES(background_color),
                cover_image_url = VALUES(cover_image_url), mobile_image_url = VALUES(mobile_image_url), video_url = VALUES(video_url),
                display_order = VALUES(display_order), version_tag = VALUES(version_tag), is_active = VALUES(is_active), theme_config = VALUES(theme_config)'
        );
        $stmt->execute([
            $slug, $name, (string)($body['tagline'] ?? ''), (string)($body['description'] ?? ''),
            (string)($body['theme_key'] ?? $slug), (string)($body['animation_key'] ?? 'smooth_reveal'),
            (string)($body['font_style'] ?? 'editorial_serif'), (string)($body['ambient_preset'] ?? $slug),
            (string)($body['audio_track_key'] ?? $slug), (string)($body['primary_color'] ?? '#FFFFFF'),
            (string)($body['accent_color'] ?? '#D4AF37'), (string)($body['background_color'] ?? '#0B0B10'),
            $body['cover_image_url'] ?? null, $body['mobile_image_url'] ?? null, $body['video_url'] ?? null,
            (int)($body['display_order'] ?? 0), (string)($body['version_tag'] ?? '2.0'),
            isset($body['is_active']) ? (!empty($body['is_active']) ? 1 : 0) : 1, $themeCfg,
        ]);
    }

    MelvoreResponse::ok(['slug' => $slug], 'Fashion world saved');
} catch (Throwable $e) {
    error_log('[Melvore][admin/worlds] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to manage fashion worlds.', 'SERVER_ERROR', 500);
}
