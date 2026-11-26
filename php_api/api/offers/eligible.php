<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/CartHelper.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();
    $worldSlug = strtolower(trim((string)($_GET['world_slug'] ?? '')));
    $cartTotal = isset($_GET['cart_subtotal']) ? (float)$_GET['cart_subtotal'] : null;

    if ($cartTotal === null) {
        $items = MelvoreCartHelper::fetchCartItems($pdo, $userId, $guestId);
        $cartTotal = 0.0;
        foreach ($items as $it) {
            $cartTotal += (float)($it['line_total'] ?? 0.0);
        }
    }

    $offers = [];
    if (MelvoreDB::tableExists('app_smart_offers')) {
        $stmt = $pdo->query(
            'SELECT o.*, c.discount_type, c.discount_value, c.valid_to, c.is_active AS coupon_active
             FROM app_smart_offers o
             LEFT JOIN coupons c ON UPPER(c.code) = UPPER(o.coupon_code)
             WHERE o.is_active = 1
               AND (o.start_date IS NULL OR o.start_date <= NOW())
               AND (o.end_date IS NULL OR o.end_date >= NOW())
             ORDER BY o.priority DESC, o.offer_id ASC'
        );
        foreach ($stmt->fetchAll() as $row) {
            // Never return an offer whose linked coupon is inactive or expired
            if (!empty($row['coupon_code'])) {
                if (((int)($row['coupon_active'] ?? 0)) !== 1) {
                    continue;
                }
                if (!empty($row['valid_to']) && strtotime((string)$row['valid_to']) < time()) {
                    continue;
                }
            }
            $rowWorld = strtolower(trim((string)($row['world_slug'] ?? '')));
            if ($worldSlug !== '' && $rowWorld !== '' && $rowWorld !== $worldSlug) {
                continue;
            }
            $minVal = (float)($row['min_cart_value'] ?? 0);
            if ($minVal > 0 && $cartTotal < ($minVal * 0.5)) {
                // Only surface contextual threshold offers when user is reasonably close or eligible
                // Wait: let user see upcoming threshold hint if cart > 0 or general editorial offers
                if ($cartTotal <= 0) {
                    continue;
                }
            }

            $offerId = (int)$row['offer_id'];
            $cooldownHours = max(1, (int)($row['cooldown_hours'] ?? 24));

            // Check backend dismissal/frequency cooldown if table exists and actor known
            if (($userId !== null || $guestId !== '') && MelvoreDB::tableExists('app_offer_impressions')) {
                $actorSql = $userId !== null ? 'user_id = ?' : 'guest_id = ?';
                $actorVal = $userId ?? $guestId;
                $cStmt = $pdo->prepare(
                    "SELECT COUNT(*) FROM app_offer_impressions
                     WHERE offer_id = ? AND {$actorSql}
                       AND action_type IN ('dismissed', 'applied')
                       AND created_at >= DATE_SUB(NOW(), INTERVAL ? HOUR)"
                );
                $cStmt->execute([$offerId, $actorVal, $cooldownHours]);
                if ((int)$cStmt->fetchColumn() > 0) {
                    continue;
                }
            }

            $offers[] = [
                'offer_id' => $offerId,
                'offer_key' => (string)$row['offer_key'],
                'title' => (string)$row['title'],
                'subtitle' => (string)($row['subtitle'] ?? ''),
                'badge_text' => (string)($row['badge_text'] ?? 'OFFER'),
                'coupon_code' => $row['coupon_code'] ?? null,
                'world_slug' => $row['world_slug'] ?? null,
                'occasion_slug' => $row['occasion_slug'] ?? null,
                'min_cart_value' => $minVal,
                'max_impressions_per_session' => max(1, (int)($row['max_impressions_per_session'] ?? 1)),
                'cooldown_hours' => $cooldownHours,
                'accent_hex' => (string)($row['accent_hex'] ?? '#3B82F6'),
                'cta_text' => (string)($row['cta_text'] ?? 'Use Code'),
            ];
        }
    }

    MelvoreResponse::ok([
        'offers' => $offers,
        'cart_subtotal' => round($cartTotal, 2),
    ], 'Eligible contextual offers loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to evaluate eligible offers.', 'SERVER_ERROR', 500);
}
