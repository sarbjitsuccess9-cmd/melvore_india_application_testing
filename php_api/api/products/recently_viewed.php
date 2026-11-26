<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/product_formatter.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();
    $limit = max(2, min(20, (int)($_GET['limit'] ?? 10)));
    $products = [];

    if (MelvoreDB::tableExists('app_user_style_signals') && ($userId !== null || $guestId !== '')) {
        if ($userId !== null) {
            $stmt = $pdo->prepare(
                'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug,
                        MAX(s.created_at) AS last_viewed_at
                 FROM app_user_style_signals s
                 INNER JOIN products p ON p.product_id = s.product_id AND p.is_active = 1
                 LEFT JOIN categories c ON c.category_id = p.category_id
                 LEFT JOIN brands b ON b.brand_id = p.brand_id
                 WHERE s.user_id = ? AND s.product_id IS NOT NULL
                 GROUP BY p.product_id
                 ORDER BY last_viewed_at DESC
                 LIMIT ?'
            );
            $stmt->bindValue(1, $userId, PDO::PARAM_INT);
            $stmt->bindValue(2, $limit, PDO::PARAM_INT);
        } else {
            $stmt = $pdo->prepare(
                'SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug,
                        MAX(s.created_at) AS last_viewed_at
                 FROM app_user_style_signals s
                 INNER JOIN products p ON p.product_id = s.product_id AND p.is_active = 1
                 LEFT JOIN categories c ON c.category_id = p.category_id
                 LEFT JOIN brands b ON b.brand_id = p.brand_id
                 WHERE s.guest_id = ? AND s.product_id IS NOT NULL
                 GROUP BY p.product_id
                 ORDER BY last_viewed_at DESC
                 LIMIT ?'
            );
            $stmt->bindValue(1, $guestId, PDO::PARAM_STR);
            $stmt->bindValue(2, $limit, PDO::PARAM_INT);
        }
        $stmt->execute();
        $products = MelvoreProductFormatter::formatRows($stmt->fetchAll());
    }

    MelvoreResponse::ok(['products' => $products], 'Recently viewed products loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load recently viewed products.', 'SERVER_ERROR', 500);
}
