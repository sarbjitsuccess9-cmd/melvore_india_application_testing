<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/CartHelper.php';
require_once dirname(__DIR__, 2) . '/services/PricingEngine.php';

MelvoreResponse::requireMethod('POST', 'DELETE');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();
    $body = MelvoreResponse::jsonBody();
    $cartId = (int)($body['cart_id'] ?? ($_GET['cart_id'] ?? 0));

    if ($cartId > 0) {
        if ($userId !== null && $userId > 0) {
            $pdo->prepare('DELETE FROM user_cart WHERE user_cart_id = ? AND user_id = ?')->execute([$cartId, $userId]);
        } elseif ($guestId !== '') {
            $pdo->prepare('DELETE FROM cart WHERE cart_id = ? AND user_id IS NULL AND session_id = ?')->execute([$cartId, $guestId]);
        }
    }

    $items = MelvoreCartHelper::fetchCartItems($pdo, $userId, $guestId);
    $summary = MelvorePricingEngine::calculateTotals($pdo, $items, '', 'online', $userId);
    $totalQty = array_sum(array_map(static fn($it) => (int)$it['quantity'], $items));

    MelvoreResponse::okFlat([
        'items' => $items,
        'count' => $totalQty,
        'summary' => $summary,
    ], 'Item removed');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to remove item from bag.', 'SERVER_ERROR', 500);
}
