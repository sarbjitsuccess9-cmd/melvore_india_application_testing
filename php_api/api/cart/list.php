<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/CartHelper.php';
require_once dirname(__DIR__, 2) . '/services/PricingEngine.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();
    $couponCode = trim((string)($_GET['coupon_code'] ?? ''));
    $paymentMethod = trim((string)($_GET['payment_method'] ?? 'online'));

    $items = MelvoreCartHelper::fetchCartItems($pdo, $userId, $guestId);
    $summary = MelvorePricingEngine::calculateTotals($pdo, $items, $couponCode, $paymentMethod, $userId);
    $totalQty = array_sum(array_map(static fn($it) => (int)$it['quantity'], $items));

    MelvoreResponse::okFlat([
        'items' => $items,
        'count' => $totalQty,
        'summary' => $summary,
    ], 'Cart loaded');
} catch (Throwable $e) {
    error_log('[Melvore][cart/list] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to load shopping bag.', 'SERVER_ERROR', 500);
}
