<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';

MelvoreResponse::requireMethod('GET', 'POST', 'PUT');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();

    if ($userId === null && $guestId === '') {
        MelvoreResponse::error('User or guest session identifier is required.', 'SESSION_REQUIRED', 400);
    }
    if (!MelvoreDB::tableExists('app_user_preferences')) {
        MelvoreResponse::ok([
            'ambient_audio_enabled' => false,
            'ui_sfx_enabled' => false,
            'reduced_motion' => false,
            'audio_consent_answered' => false,
            'preferred_world_slug' => null,
        ], 'Default preferences loaded');
    }

    $method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');
    $hasVideoAuto = MelvoreDB::columnExists('app_user_preferences', 'video_autoplay_muted');
    $hasDataSaver = MelvoreDB::columnExists('app_user_preferences', 'data_saver_mode');

    if ($method === 'POST' || $method === 'PUT') {
        $body = MelvoreResponse::jsonBody();
        $ambient = !empty($body['ambient_audio_enabled']) ? 1 : 0;
        $sfx = !empty($body['ui_sfx_enabled']) ? 1 : 0;
        $reduced = !empty($body['reduced_motion']) ? 1 : 0;
        $videoAuto = array_key_exists('video_autoplay_muted', $body) ? (!empty($body['video_autoplay_muted']) ? 1 : 0) : 1;
        $dataSaver = !empty($body['data_saver_mode']) ? 1 : 0;
        $consent = !empty($body['audio_consent_answered']) ? 1 : 0;
        $worldSlug = isset($body['preferred_world_slug']) && trim((string)$body['preferred_world_slug']) !== ''
            ? trim((string)$body['preferred_world_slug']) : null;

        if ($userId !== null) {
            if ($hasVideoAuto && $hasDataSaver) {
                $pdo->prepare(
                    'INSERT INTO app_user_preferences
                        (user_id, guest_id, ambient_audio_enabled, ui_sfx_enabled, reduced_motion, video_autoplay_muted, data_saver_mode, audio_consent_answered, preferred_world_slug)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                     ON DUPLICATE KEY UPDATE
                        ambient_audio_enabled = VALUES(ambient_audio_enabled),
                        ui_sfx_enabled = VALUES(ui_sfx_enabled),
                        reduced_motion = VALUES(reduced_motion),
                        video_autoplay_muted = VALUES(video_autoplay_muted),
                        data_saver_mode = VALUES(data_saver_mode),
                        audio_consent_answered = VALUES(audio_consent_answered),
                        preferred_world_slug = VALUES(preferred_world_slug)'
                )->execute([$userId, $guestId !== '' ? $guestId : null, $ambient, $sfx, $reduced, $videoAuto, $dataSaver, $consent, $worldSlug]);
            } else {
                $pdo->prepare(
                    'INSERT INTO app_user_preferences
                        (user_id, guest_id, ambient_audio_enabled, ui_sfx_enabled, reduced_motion, audio_consent_answered, preferred_world_slug)
                     VALUES (?, ?, ?, ?, ?, ?, ?)
                     ON DUPLICATE KEY UPDATE
                        ambient_audio_enabled = VALUES(ambient_audio_enabled),
                        ui_sfx_enabled = VALUES(ui_sfx_enabled),
                        reduced_motion = VALUES(reduced_motion),
                        audio_consent_answered = VALUES(audio_consent_answered),
                        preferred_world_slug = VALUES(preferred_world_slug)'
                )->execute([$userId, $guestId !== '' ? $guestId : null, $ambient, $sfx, $reduced, $consent, $worldSlug]);
            }
        } else {
            $chk = $pdo->prepare('SELECT pref_id FROM app_user_preferences WHERE guest_id = ? LIMIT 1');
            $chk->execute([$guestId]);
            $pid = $chk->fetchColumn();
            if ($pid) {
                if ($hasVideoAuto && $hasDataSaver) {
                    $pdo->prepare(
                        'UPDATE app_user_preferences
                         SET ambient_audio_enabled = ?, ui_sfx_enabled = ?, reduced_motion = ?, video_autoplay_muted = ?, data_saver_mode = ?, audio_consent_answered = ?, preferred_world_slug = ?
                         WHERE pref_id = ?'
                    )->execute([$ambient, $sfx, $reduced, $videoAuto, $dataSaver, $consent, $worldSlug, (int)$pid]);
                } else {
                    $pdo->prepare(
                        'UPDATE app_user_preferences
                         SET ambient_audio_enabled = ?, ui_sfx_enabled = ?, reduced_motion = ?, audio_consent_answered = ?, preferred_world_slug = ?
                         WHERE pref_id = ?'
                    )->execute([$ambient, $sfx, $reduced, $consent, $worldSlug, (int)$pid]);
                }
            } else {
                if ($hasVideoAuto && $hasDataSaver) {
                    $pdo->prepare(
                        'INSERT INTO app_user_preferences
                            (user_id, guest_id, ambient_audio_enabled, ui_sfx_enabled, reduced_motion, video_autoplay_muted, data_saver_mode, audio_consent_answered, preferred_world_slug)
                         VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?)'
                    )->execute([$guestId, $ambient, $sfx, $reduced, $videoAuto, $dataSaver, $consent, $worldSlug]);
                } else {
                    $pdo->prepare(
                        'INSERT INTO app_user_preferences
                            (user_id, guest_id, ambient_audio_enabled, ui_sfx_enabled, reduced_motion, audio_consent_answered, preferred_world_slug)
                         VALUES (NULL, ?, ?, ?, ?, ?, ?)'
                    )->execute([$guestId, $ambient, $sfx, $reduced, $consent, $worldSlug]);
                }
            }
        }
    }

    if ($userId !== null) {
        $stmt = $pdo->prepare('SELECT * FROM app_user_preferences WHERE user_id = ? LIMIT 1');
        $stmt->execute([$userId]);
    } else {
        $stmt = $pdo->prepare('SELECT * FROM app_user_preferences WHERE guest_id = ? LIMIT 1');
        $stmt->execute([$guestId]);
    }
    $row = $stmt->fetch();

    MelvoreResponse::ok([
        'ambient_audio_enabled' => $row ? (((int)$row['ambient_audio_enabled']) === 1) : false,
        'ui_sfx_enabled' => $row ? (((int)$row['ui_sfx_enabled']) === 1) : false,
        'reduced_motion' => $row ? (((int)$row['reduced_motion']) === 1) : false,
        'video_autoplay_muted' => ($row && isset($row['video_autoplay_muted'])) ? (((int)$row['video_autoplay_muted']) === 1) : true,
        'data_saver_mode' => ($row && isset($row['data_saver_mode'])) ? (((int)$row['data_saver_mode']) === 1) : false,
        'audio_consent_answered' => $row ? (((int)$row['audio_consent_answered']) === 1) : false,
        'preferred_world_slug' => $row['preferred_world_slug'] ?? null,
    ], 'Preferences synchronized');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to sync preferences.', 'SERVER_ERROR', 500);
}
