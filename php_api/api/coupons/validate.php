<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/CartHelper.php';
require_once dirname(__DIR__, 2) . '/services/PricingEngine.php';

MelvoreResponse::requireMethod('GET', 'POST');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();
    $method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

    if ($method === 'GET') {
        $coupons = [];
        if (MelvoreDB::tableExists('coupons')) {
            $stmt = $pdo->query(
                'SELECT coupon_id, code, description, discount_type, discount_value, min_order_amount, max_discount_amount
                 FROM coupons
                 WHERE is_active = 1
                   AND (valid_from IS NULL OR valid_from <= NOW())
                   AND (valid_to IS NULL OR valid_to >= NOW())
                   AND (usage_limit IS NULL OR usage_limit = 0 OR usage_count < usage_limit)
                 ORDER BY min_order_amount ASC, coupon_id DESC'
            );
            foreach ($stmt->fetchAll() as $c) {
                $coupons[] = [
                    'coupon_id' => (int)$c['coupon_id'],
                    'code' => (string)$c['code'],
                    'description' => (string)($c['description'] ?? ''),
                    'discount_type' => (string)($c['discount_type'] ?? 'fixed'),
                    'discount_value' => (float)($c['discount_value'] ?? 0),
                    'min_order_amount' => (float)($c['min_order_amount'] ?? 0),
                    'max_discount_amount' => $c['max_discount_amount'] !== null ? (float)$c['max_discount_amount'] : null,
                ];
            }
        }
        MelvoreResponse::okFlat(['coupons' => $coupons], 'Active coupons loaded');
    }

    $body = MelvoreResponse::jsonBody();
    $code = strtoupper(trim((string)($body['coupon_code'] ?? '')));
    $paymentMethod = trim((string)($body['payment_method'] ?? 'online'));
    if ($code === '') {
        MelvoreResponse::error('Please enter a coupon code.', 'COUPON_REQUIRED', 400);
    }

    $items = MelvoreCartHelper::fetchCartItems($pdo, $userId, $guestId);
    $summary = MelvorePricingEngine::calculateTotals($pdo, $items, $code, $paymentMethod, $userId);
    $valid = $summary['coupon_id'] !== null;

    MelvoreResponse::okFlat([
        'valid' => $valid,
        'code' => $valid ? $summary['coupon_code'] : $code,
        'discount_amount' => (float)$summary['discount_amount'],
        'summary' => $summary,
    ], (string)($summary['coupon_message'] ?? ($valid ? 'Coupon applied' : 'Invalid coupon')));
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to validate coupon.', 'SERVER_ERROR', 500);
}
