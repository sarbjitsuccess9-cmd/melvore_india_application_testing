<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/CartHelper.php';
require_once dirname(__DIR__, 2) . '/services/PricingEngine.php';

MelvoreResponse::requireMethod('POST');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();
    $body = MelvoreResponse::jsonBody();
    $cartId = (int)($body['cart_id'] ?? 0);
    if ($cartId <= 0) {
        MelvoreResponse::error('Valid cart_id is required.', 'VALIDATION_ERROR', 400);
    }

    $delta = isset($body['delta']) ? (int)$body['delta'] : null;
    $explicitQty = isset($body['quantity']) ? (int)$body['quantity'] : null;

    if ($userId !== null && $userId > 0) {
        $stmt = $pdo->prepare(
            'SELECT uc.user_cart_id, uc.quantity, uc.product_id, uc.variation_id, p.stock_quantity, pv.stock_quantity AS var_stock
             FROM user_cart uc
             INNER JOIN products p ON p.product_id = uc.product_id
             LEFT JOIN product_variations pv ON pv.variation_id = uc.variation_id
             WHERE uc.user_cart_id = ? AND uc.user_id = ? LIMIT 1'
        );
        $stmt->execute([$cartId, $userId]);
        $row = $stmt->fetch();
        if ($row) {
            $maxStock = ($row['variation_id'] !== null && $row['var_stock'] !== null)
                ? (int)$row['var_stock']
                : (int)$row['stock_quantity'];
            $targetQty = $explicitQty ?? (((int)$row['quantity']) + ($delta ?? 0));
            if ($targetQty <= 0) {
                $pdo->prepare('DELETE FROM user_cart WHERE user_cart_id = ? AND user_id = ?')->execute([$cartId, $userId]);
            } else {
                $clamped = min(max(1, $targetQty), max(1, min(20, $maxStock)));
                $pdo->prepare('UPDATE user_cart SET quantity = ? WHERE user_cart_id = ? AND user_id = ?')->execute([$clamped, $cartId, $userId]);
            }
        }
    } elseif ($guestId !== '') {
        $stmt = $pdo->prepare(
            'SELECT c.cart_id, c.quantity, c.product_id, c.variation_id, p.stock_quantity, pv.stock_quantity AS var_stock
             FROM cart c
             INNER JOIN products p ON p.product_id = c.product_id
             LEFT JOIN product_variations pv ON pv.variation_id = c.variation_id
             WHERE c.cart_id = ? AND c.user_id IS NULL AND c.session_id = ? LIMIT 1'
        );
        $stmt->execute([$cartId, $guestId]);
        $row = $stmt->fetch();
        if ($row) {
            $maxStock = ($row['variation_id'] !== null && $row['var_stock'] !== null)
                ? (int)$row['var_stock']
                : (int)$row['stock_quantity'];
            $targetQty = $explicitQty ?? (((int)$row['quantity']) + ($delta ?? 0));
            if ($targetQty <= 0) {
                $pdo->prepare('DELETE FROM cart WHERE cart_id = ? AND user_id IS NULL AND session_id = ?')->execute([$cartId, $guestId]);
            } else {
                $clamped = min(max(1, $targetQty), max(1, min(20, $maxStock)));
                $pdo->prepare('UPDATE cart SET quantity = ? WHERE cart_id = ? AND user_id IS NULL AND session_id = ?')->execute([$clamped, $cartId, $guestId]);
            }
        }
    }

    $items = MelvoreCartHelper::fetchCartItems($pdo, $userId, $guestId);
    $summary = MelvorePricingEngine::calculateTotals($pdo, $items, '', 'online', $userId);
    $totalQty = array_sum(array_map(static fn($it) => (int)$it['quantity'], $items));

    MelvoreResponse::okFlat([
        'items' => $items,
        'count' => $totalQty,
        'summary' => $summary,
    ], 'Bag updated');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to update bag.', 'SERVER_ERROR', 500);
}
