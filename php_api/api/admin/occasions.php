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
        $rows = $pdo->query('SELECT * FROM app_occasions ORDER BY priority DESC, occasion_id ASC')->fetchAll() ?: [];
        foreach ($rows as &$r) {
            $win = MelvoreThemeEngineService::computeOccasionWindow($r);
            $r['computed_window_start'] = $win['start_dt']?->format('Y-m-d H:i:s');
            $r['computed_window_end'] = $win['end_dt']?->format('Y-m-d H:i:s');
            $r['computed_timezone'] = $win['timezone'];
        }
        MelvoreResponse::ok(['occasions' => $rows], 'Admin occasions loaded');
    }

    if ($method === 'DELETE') {
        $id = (int)($_GET['id'] ?? (MelvoreResponse::jsonBody()['occasion_id'] ?? 0));
        if ($id <= 0) {
            MelvoreResponse::error('Valid occasion_id required.', 'VALIDATION_ERROR', 400);
        }
        $pdo->prepare('UPDATE app_occasions SET is_active = 0 WHERE occasion_id = ?')->execute([$id]);
        MelvoreResponse::ok(['deactivated' => true], 'Occasion deactivated');
    }

    $body = MelvoreResponse::jsonBody();
    $slug = strtolower(trim((string)($body['slug'] ?? '')));
    $name = trim((string)($body['name'] ?? ''));
    $headline = trim((string)($body['headline'] ?? ''));
    if ($slug === '' || $name === '' || $headline === '') {
        MelvoreResponse::error('slug, name, and headline are required.', 'VALIDATION_ERROR', 400);
    }

    $tzName = trim((string)($body['timezone'] ?? 'Asia/Kolkata'));
    if ($tzName === '') {
        $tzName = 'Asia/Kolkata';
    }
    try {
        $tz = new DateTimeZone($tzName);
    } catch (Throwable) {
        $tzName = 'Asia/Kolkata';
        $tz = new DateTimeZone($tzName);
    }

    $eventDate = !empty($body['event_date']) ? trim((string)$body['event_date']) : null;
    $daysBefore = isset($body['activation_days_before']) && is_numeric($body['activation_days_before'])
        ? max(0, (int)$body['activation_days_before'])
        : 8;

    $activationStartAt = !empty($body['activation_start_at']) ? trim((string)$body['activation_start_at']) : ($body['start_date'] ?? null);
    $activationEndAt = !empty($body['activation_end_at']) ? trim((string)$body['activation_end_at']) : ($body['end_date'] ?? null);

    // Auto-compute 8-day-before IST activation window whenever event_date is provided
    if ($eventDate !== null && $eventDate !== '') {
        $ev = new DateTimeImmutable($eventDate . ' 00:00:00', $tz);
        if (empty($activationStartAt)) {
            $activationStartAt = $ev->modify("-{$daysBefore} days")->setTime(0, 0, 0)->format('Y-m-d H:i:s');
        }
        if (empty($activationEndAt)) {
            $activationEndAt = $ev->setTime(23, 59, 59)->format('Y-m-d H:i:s');
        }
    }

    $cfg = isset($body['theme_config']) ? json_encode($body['theme_config'], JSON_UNESCAPED_UNICODE) : null;
    $designOvr = isset($body['design_override']) ? json_encode($body['design_override'], JSON_UNESCAPED_UNICODE) : null;

    $hasSchedulerCols = MelvoreDB::columnExists('app_occasions', 'event_date');

    if ($hasSchedulerCols) {
        $stmt = $pdo->prepare(
            'INSERT INTO app_occasions
                (slug, name, eyebrow, headline, subtitle, occasion_type, motif_key, primary_color, accent_color, background_color,
                 font_style, audio_track_key, cta_text, cta_collection_slug, region_country, region_state, region_city,
                 priority, is_active, event_date, activation_days_before, activation_start_at, activation_end_at, timezone,
                 start_date, end_date, theme_config, design_override_json)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
             ON DUPLICATE KEY UPDATE
                name = VALUES(name), eyebrow = VALUES(eyebrow), headline = VALUES(headline), subtitle = VALUES(subtitle),
                occasion_type = VALUES(occasion_type), motif_key = VALUES(motif_key), primary_color = VALUES(primary_color),
                accent_color = VALUES(accent_color), background_color = VALUES(background_color), font_style = VALUES(font_style),
                audio_track_key = VALUES(audio_track_key), cta_text = VALUES(cta_text), cta_collection_slug = VALUES(cta_collection_slug),
                region_country = VALUES(region_country), region_state = VALUES(region_state), region_city = VALUES(region_city),
                priority = VALUES(priority), is_active = VALUES(is_active),
                event_date = VALUES(event_date), activation_days_before = VALUES(activation_days_before),
                activation_start_at = VALUES(activation_start_at), activation_end_at = VALUES(activation_end_at), timezone = VALUES(timezone),
                start_date = VALUES(start_date), end_date = VALUES(end_date),
                theme_config = VALUES(theme_config), design_override_json = VALUES(design_override_json)'
        );
        $stmt->execute([
            $slug,
            $name,
            (string)($body['eyebrow'] ?? ''),
            $headline,
            (string)($body['subtitle'] ?? ''),
            (string)($body['occasion_type'] ?? 'festival'),
            (string)($body['motif_key'] ?? 'confetti'),
            (string)($body['primary_color'] ?? '#FFFFFF'),
            (string)($body['accent_color'] ?? '#D4AF37'),
            (string)($body['background_color'] ?? '#140F1C'),
            (string)($body['font_style'] ?? 'editorial_serif'),
            (string)($body['audio_track_key'] ?? 'festive'),
            (string)($body['cta_text'] ?? 'Explore Occasion'),
            (string)($body['cta_collection_slug'] ?? ''),
            (string)($body['region_country'] ?? 'ALL'),
            (string)($body['region_state'] ?? ''),
            (string)($body['region_city'] ?? ''),
            (int)($body['priority'] ?? 50),
            isset($body['is_active']) ? (!empty($body['is_active']) ? 1 : 0) : 1,
            $eventDate,
            $daysBefore,
            $activationStartAt,
            $activationEndAt,
            $tzName,
            $activationStartAt,
            $activationEndAt,
            $cfg,
            $designOvr,
        ]);
    } else {
        $stmt = $pdo->prepare(
            'INSERT INTO app_occasions
                (slug, name, eyebrow, headline, subtitle, occasion_type, motif_key, primary_color, accent_color, background_color,
                 font_style, audio_track_key, cta_text, cta_collection_slug, region_country, priority, is_active, start_date, end_date, theme_config)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
             ON DUPLICATE KEY UPDATE
                name = VALUES(name), eyebrow = VALUES(eyebrow), headline = VALUES(headline), subtitle = VALUES(subtitle),
                occasion_type = VALUES(occasion_type), motif_key = VALUES(motif_key), primary_color = VALUES(primary_color),
                accent_color = VALUES(accent_color), background_color = VALUES(background_color), font_style = VALUES(font_style),
                audio_track_key = VALUES(audio_track_key), cta_text = VALUES(cta_text), cta_collection_slug = VALUES(cta_collection_slug),
                region_country = VALUES(region_country), priority = VALUES(priority), is_active = VALUES(is_active),
                start_date = VALUES(start_date), end_date = VALUES(end_date), theme_config = VALUES(theme_config)'
        );
        $stmt->execute([
            $slug, $name, (string)($body['eyebrow'] ?? ''), $headline, (string)($body['subtitle'] ?? ''),
            (string)($body['occasion_type'] ?? 'festival'), (string)($body['motif_key'] ?? 'confetti'),
            (string)($body['primary_color'] ?? '#FFFFFF'), (string)($body['accent_color'] ?? '#D4AF37'),
            (string)($body['background_color'] ?? '#140F1C'), (string)($body['font_style'] ?? 'editorial_serif'),
            (string)($body['audio_track_key'] ?? 'festive'), (string)($body['cta_text'] ?? 'Explore Occasion'),
            (string)($body['cta_collection_slug'] ?? ''), (string)($body['region_country'] ?? 'ALL'),
            (int)($body['priority'] ?? 50), isset($body['is_active']) ? (!empty($body['is_active']) ? 1 : 0) : 1,
            $activationStartAt, $activationEndAt, $cfg,
        ]);
    }

    MelvoreResponse::ok([
        'slug' => $slug,
        'event_date' => $eventDate,
        'activation_days_before' => $daysBefore,
        'activation_start_at' => $activationStartAt,
        'activation_end_at' => $activationEndAt,
        'timezone' => $tzName,
    ], 'Occasion saved with computed activation schedule');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to manage occasions: ' . $e->getMessage(), 'SERVER_ERROR', 500);
}
