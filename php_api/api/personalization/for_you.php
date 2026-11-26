<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/RecommendationService.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();
    $limit = max(4, min(30, (int)($_GET['limit'] ?? 16)));
    $refresh = !empty($_GET['refresh']);

    $payload = MelvoreRecommendationService::buildForActor($pdo, $userId, $guestId, $limit, $refresh);
    MelvoreResponse::ok($payload, 'For You feed loaded');
} catch (Throwable $e) {
    error_log('[Melvore][personalization/for_you] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to load personalized feed.', 'SERVER_ERROR', 500);
}
