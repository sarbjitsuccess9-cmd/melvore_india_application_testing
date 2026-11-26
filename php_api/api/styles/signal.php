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
    if ($userId === null && $guestId === '') {
        MelvoreResponse::error('User or guest session identifier required.', 'VALIDATION_ERROR', 422);
    }

    $body = MelvoreResponse::jsonBody();
    $styleWorldId = isset($body['style_world_id']) ? (int)$body['style_world_id'] : null;
    $productId = isset($body['product_id']) ? (int)$body['product_id'] : null;
    $categoryId = isset($body['category_id']) ? (int)$body['category_id'] : null;
    $signalType = trim((string)($body['signal_type'] ?? 'style_tap'));

    $allowedSignals = ['style_tap', 'style_explore', 'product_view', 'wishlist_add', 'category_explore', 'video_engage'];
    if (!in_array($signalType, $allowedSignals, true)) {
        $signalType = 'style_tap';
    }
    $weight = match ($signalType) {
        'wishlist_add' => 5.0,
        'video_engage' => 3.0,
        'style_explore' => 2.5,
        'product_view' => 2.0,
        default => 1.5,
    };

    if (MelvoreDB::tableExists('app_user_style_signals')) {
        $stmt = $pdo->prepare(
            'INSERT INTO app_user_style_signals
             (user_id, guest_id, style_world_id, product_id, category_id, signal_type, signal_weight, created_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, NOW())'
        );
        $stmt->execute([
            $userId,
            $guestId !== '' ? $guestId : null,
            ($styleWorldId !== null && $styleWorldId > 0) ? $styleWorldId : null,
            ($productId !== null && $productId > 0) ? $productId : null,
            ($categoryId !== null && $categoryId > 0) ? $categoryId : null,
            $signalType,
            $weight,
        ]);
        MelvoreRecommendationService::invalidateForActor($pdo, $userId, $guestId);
    }

    MelvoreResponse::ok(['recorded' => true], 'Preference signal recorded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to record style preference.', 'SERVER_ERROR', 500);
}
