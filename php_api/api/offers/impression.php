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

    $offerId = (int)($body['offer_id'] ?? 0);
    $actionType = strtolower(trim((string)($body['action_type'] ?? 'shown')));
    if ($offerId <= 0 || !in_array($actionType, ['shown', 'dismissed', 'applied'], true)) {
        MelvoreResponse::error('Valid offer_id and action_type are required.', 'VALIDATION_ERROR', 400);
    }

    if (MelvoreDB::tableExists('app_offer_impressions')) {
        $ins = $pdo->prepare(
            'INSERT INTO app_offer_impressions (offer_id, user_id, guest_id, action_type, created_at)
             VALUES (?, ?, ?, ?, NOW())'
        );
        $ins->execute([$offerId, $userId, $guestId !== '' ? $guestId : null, $actionType]);
    }

    MelvoreResponse::ok(['recorded' => true], 'Offer interaction recorded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to record offer impression.', 'SERVER_ERROR', 500);
}
