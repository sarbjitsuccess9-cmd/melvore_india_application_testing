<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';

MelvoreResponse::requireMethod('GET', 'POST', 'DELETE');

try {
    $pdo = MelvoreDB::pdo();
    MelvoreAuth::requireAdminUserId();
    $method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

    if ($method === 'GET') {
        $rows = $pdo->query('SELECT * FROM app_smart_offers ORDER BY priority DESC, offer_id ASC')->fetchAll();
        MelvoreResponse::ok(['offers' => $rows], 'Smart offers loaded');
    }
    if ($method === 'DELETE') {
        $id = (int)($_GET['id'] ?? (MelvoreResponse::jsonBody()['offer_id'] ?? 0));
        if ($id <= 0) {
            MelvoreResponse::error('Valid offer_id required.', 'VALIDATION_ERROR', 400);
        }
        $pdo->prepare('UPDATE app_smart_offers SET is_active = 0 WHERE offer_id = ?')->execute([$id]);
        MelvoreResponse::ok(['deactivated' => true], 'Offer deactivated');
    }

    $body = MelvoreResponse::jsonBody();
    $key = strtolower(trim((string)($body['offer_key'] ?? '')));
    $title = trim((string)($body['title'] ?? ''));
    if ($key === '' || $title === '') {
        MelvoreResponse::error('offer_key and title are required.', 'VALIDATION_ERROR', 400);
    }

    $couponCode = isset($body['coupon_code']) && trim((string)$body['coupon_code']) !== ''
        ? strtoupper(trim((string)$body['coupon_code'])) : null;
    if ($couponCode !== null) {
        $cStmt = $pdo->prepare('SELECT coupon_id FROM coupons WHERE UPPER(code) = ? AND is_active = 1 LIMIT 1');
        $cStmt->execute([$couponCode]);
        if (!$cStmt->fetch()) {
            MelvoreResponse::error('Referenced coupon_code does not exist or is inactive in coupons table.', 'INVALID_COUPON_REF', 422);
        }
    }

    $stmt = $pdo->prepare(
        'INSERT INTO app_smart_offers
            (offer_key, title, subtitle, badge_text, coupon_code, world_slug, occasion_slug, min_cart_value,
             max_impressions_per_session, cooldown_hours, priority, accent_hex, cta_text, is_active, start_date, end_date)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
         ON DUPLICATE KEY UPDATE
            title = VALUES(title), subtitle = VALUES(subtitle), badge_text = VALUES(badge_text),
            coupon_code = VALUES(coupon_code), world_slug = VALUES(world_slug), occasion_slug = VALUES(occasion_slug),
            min_cart_value = VALUES(min_cart_value), max_impressions_per_session = VALUES(max_impressions_per_session),
            cooldown_hours = VALUES(cooldown_hours), priority = VALUES(priority), accent_hex = VALUES(accent_hex),
            cta_text = VALUES(cta_text), is_active = VALUES(is_active), start_date = VALUES(start_date), end_date = VALUES(end_date)'
    );
    $stmt->execute([
        $key,
        $title,
        (string)($body['subtitle'] ?? ''),
        (string)($body['badge_text'] ?? 'OFFER'),
        $couponCode,
        $body['world_slug'] ?? null,
        $body['occasion_slug'] ?? null,
        max(0.0, (float)($body['min_cart_value'] ?? 0)),
        max(1, (int)($body['max_impressions_per_session'] ?? 1)),
        max(1, (int)($body['cooldown_hours'] ?? 24)),
        (int)($body['priority'] ?? 10),
        (string)($body['accent_hex'] ?? '#3B82F6'),
        (string)($body['cta_text'] ?? 'Use Code'),
        isset($body['is_active']) ? (!empty($body['is_active']) ? 1 : 0) : 1,
        $body['start_date'] ?? null,
        $body['end_date'] ?? null,
    ]);

    MelvoreResponse::ok(['offer_key' => $key], 'Smart offer saved');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to manage smart offers.', 'SERVER_ERROR', 500);
}
