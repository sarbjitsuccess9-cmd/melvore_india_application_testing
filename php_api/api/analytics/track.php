<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';

MelvoreResponse::requireMethod('POST');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();
    $body = MelvoreResponse::jsonBody();

    $eventType = trim((string)($body['event_type'] ?? ($body['event'] ?? 'screen_view')));
    $page = trim((string)($body['page'] ?? ($body['screen'] ?? 'app')));
    $productId = isset($body['product_id']) ? (int)$body['product_id'] : null;
    $metaJson = json_encode($body['metadata'] ?? $body, JSON_UNESCAPED_UNICODE);

    if (MelvoreDB::tableExists('analytics_events')) {
        $ins = $pdo->prepare(
            'INSERT INTO analytics_events (user_id, session_id, event_type, event_data, page_url, created_at)
             VALUES (?, ?, ?, ?, ?, NOW())'
        );
        $ins->execute([$userId, $guestId !== '' ? $guestId : null, substr($eventType, 0, 80), $metaJson, substr($page, 0, 255)]);
    }

    if ($productId !== null && $productId > 0 && $eventType === 'product_view') {
        $pdo->prepare('UPDATE products SET view_count = view_count + 1 WHERE product_id = ?')->execute([$productId]);
    }

    MelvoreResponse::ok(['tracked' => true], 'Event recorded');
} catch (Throwable $e) {
    MelvoreResponse::ok(['tracked' => false], 'Skipped');
}
