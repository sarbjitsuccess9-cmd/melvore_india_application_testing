<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';

MelvoreResponse::requireMethod('POST');

try {
    $body = MelvoreResponse::jsonBody();
    $brandId = (int)($body['brand_id'] ?? 0);
    if ($brandId <= 0) {
        MelvoreResponse::error('Valid brand_id is required.', 'VALIDATION_ERROR', 422);
    }

    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();
    if ($userId === null && $guestId === '') {
        MelvoreResponse::ok(['recorded' => false], 'Skipped anonymous tracking without session');
    }

    if (MelvoreDB::tableExists('app_user_brand_activity')) {
        $stmt = MelvoreDB::pdo()->prepare(
            'INSERT INTO app_user_brand_activity (user_id, guest_id, brand_id, activity_type, created_at)
             VALUES (?, ?, ?, ?, NOW())'
        );
        $stmt->execute([$userId, $guestId !== '' ? $guestId : null, $brandId, 'view']);
    }

    MelvoreResponse::ok(['recorded' => true, 'brand_id' => $brandId], 'Brand view recorded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to record brand activity.', 'SERVER_ERROR', 500);
}
