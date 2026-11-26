<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/RecommendationService.php';

MelvoreResponse::requireMethod('POST');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();
    $body = MelvoreResponse::jsonBody();

    $videoId = (int)($body['video_id'] ?? 0);
    $eventType = trim((string)($body['event_type'] ?? ''));
    $watchSeconds = max(0, (int)($body['watch_seconds'] ?? 0));

    $allowed = ['view', 'like', 'unlike', 'save', 'unsave', 'share', 'product_click'];
    if ($videoId <= 0 || !in_array($eventType, $allowed, true)) {
        MelvoreResponse::error('Valid video_id and event_type required.', 'VALIDATION_ERROR', 422);
    }
    if (!MelvoreDB::tableExists('app_product_videos') || !MelvoreDB::tableExists('app_video_events')) {
        MelvoreResponse::error('Video tracking unavailable.', 'NOT_FOUND', 404);
    }

    $pdo->beginTransaction();

    $vStmt = $pdo->prepare('SELECT video_id, product_id, brand_id, views_count, likes_count, saves_count, shares_count FROM app_product_videos WHERE video_id = ? FOR UPDATE');
    $vStmt->execute([$videoId]);
    $video = $vStmt->fetch();
    if (!$video) {
        $pdo->rollBack();
        MelvoreResponse::error('Video not found.', 'NOT_FOUND', 404);
    }

    $ins = $pdo->prepare(
        'INSERT INTO app_video_events (video_id, user_id, guest_id, event_type, watch_seconds, created_at)
         VALUES (?, ?, ?, ?, ?, NOW())'
    );
    $ins->execute([$videoId, $userId, $guestId !== '' ? $guestId : null, $eventType, $watchSeconds]);

    $views = (int)$video['views_count'];
    $likes = (int)$video['likes_count'];
    $saves = (int)$video['saves_count'];
    $shares = (int)$video['shares_count'];

    switch ($eventType) {
        case 'view':
            $views++;
            break;
        case 'like':
            $likes++;
            break;
        case 'unlike':
            $likes = max(0, $likes - 1);
            break;
        case 'save':
            $saves++;
            break;
        case 'unsave':
            $saves = max(0, $saves - 1);
            break;
        case 'share':
            $shares++;
            break;
    }

    $upd = $pdo->prepare(
        'UPDATE app_product_videos SET views_count = ?, likes_count = ?, saves_count = ?, shares_count = ? WHERE video_id = ?'
    );
    $upd->execute([$views, $likes, $saves, $shares, $videoId]);

    // Propagate engagement signal for personalization
    if (($userId !== null || $guestId !== '') && in_array($eventType, ['like', 'save', 'product_click'], true) && MelvoreDB::tableExists('app_user_style_signals')) {
        $sig = $pdo->prepare(
            'INSERT INTO app_user_style_signals (user_id, guest_id, product_id, signal_type, signal_weight, created_at)
             VALUES (?, ?, ?, "video_engage", 3.0, NOW())'
        );
        $sig->execute([$userId, $guestId !== '' ? $guestId : null, $video['product_id'] ? (int)$video['product_id'] : null]);
    }

    $pdo->commit();
    if ($userId !== null || $guestId !== '') {
        MelvoreRecommendationService::invalidateForActor($pdo, $userId, $guestId);
    }

    MelvoreResponse::ok([
        'video_id' => $videoId,
        'event_type' => $eventType,
        'views_count' => $views,
        'likes_count' => $likes,
        'saves_count' => $saves,
        'shares_count' => $shares,
    ], 'Video event recorded');
} catch (Throwable $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('[Melvore][videos/event] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to record video event.', 'SERVER_ERROR', 500);
}
