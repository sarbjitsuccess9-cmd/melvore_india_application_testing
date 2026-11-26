<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $productId = (int)($_GET['product_id'] ?? 0);
    $page = max(1, (int)($_GET['page'] ?? 1));
    $limit = max(1, min(40, (int)($_GET['limit'] ?? 15)));
    $offset = ($page - 1) * $limit;

    if ($productId <= 0) {
        MelvoreResponse::error('Valid product_id is required.', 'VALIDATION_ERROR', 422);
    }

    $cntStmt = $pdo->prepare('SELECT COUNT(*) FROM reviews WHERE product_id = ? AND is_approved = 1');
    $cntStmt->execute([$productId]);
    $total = (int)$cntStmt->fetchColumn();

    $rStmt = $pdo->prepare(
        'SELECT r.*, COALESCE(NULLIF(u.name, ""), "Verified Customer") AS user_name
         FROM reviews r
         LEFT JOIN users u ON u.user_id = r.user_id
         WHERE r.product_id = ? AND r.is_approved = 1
         ORDER BY r.is_verified_purchase DESC, r.helpful_count DESC, r.created_at DESC
         LIMIT ? OFFSET ?'
    );
    $rStmt->bindValue(1, $productId, PDO::PARAM_INT);
    $rStmt->bindValue(2, $limit, PDO::PARAM_INT);
    $rStmt->bindValue(3, $offset, PDO::PARAM_INT);
    $rStmt->execute();
    $rows = $rStmt->fetchAll();

    $reviewIds = array_map(static fn($r) => (int)$r['review_id'], $rows);
    $mediaByReview = [];
    if (!empty($reviewIds) && MelvoreDB::tableExists('app_review_media')) {
        $ph = implode(',', array_fill(0, count($reviewIds), '?'));
        $mStmt = $pdo->prepare("SELECT review_id, media_url, media_type FROM app_review_media WHERE review_id IN ($ph) ORDER BY media_id ASC");
        $mStmt->execute($reviewIds);
        foreach ($mStmt->fetchAll() as $m) {
            $url = MelvoreResponse::mediaUrl($m['media_url'] ?? null);
            if ($url !== null) {
                $mediaByReview[(int)$m['review_id']][] = [
                    'url' => $url,
                    'type' => (string)($m['media_type'] ?? 'image'),
                ];
            }
        }
    }

    $votedSet = [];
    if ($userId !== null && !empty($reviewIds) && MelvoreDB::tableExists('app_review_votes')) {
        $ph = implode(',', array_fill(0, count($reviewIds), '?'));
        $vStmt = $pdo->prepare("SELECT review_id FROM app_review_votes WHERE user_id = ? AND review_id IN ($ph)");
        $vStmt->execute(array_merge([$userId], $reviewIds));
        foreach ($vStmt->fetchAll() as $vr) {
            $votedSet[(int)$vr['review_id']] = true;
        }
    }

    $reviews = [];
    foreach ($rows as $r) {
        $rid = (int)$r['review_id'];
        $reviews[] = [
            'review_id' => $rid,
            'product_id' => (int)$r['product_id'],
            'user_name' => (string)$r['user_name'],
            'rating' => (int)$r['rating'],
            'title' => (string)($r['title'] ?? ''),
            'comment' => (string)($r['comment'] ?? ''),
            'is_verified_purchase' => ((int)($r['is_verified_purchase'] ?? 0)) === 1,
            'helpful_count' => (int)($r['helpful_count'] ?? 0),
            'has_voted_helpful' => isset($votedSet[$rid]),
            'media' => $mediaByReview[$rid] ?? [],
            'created_at' => (string)($r['created_at'] ?? ''),
        ];
    }

    // Check which of the user's delivered/valid order items for this product are eligible for review
    $eligibleItems = [];
    if ($userId !== null) {
        $elStmt = $pdo->prepare(
            "SELECT oi.order_item_id, oi.order_id, o.order_number, oi.product_name, o.created_at
             FROM order_items oi
             INNER JOIN orders o ON o.order_id = oi.order_id
             LEFT JOIN reviews rv ON rv.order_item_id = oi.order_item_id AND rv.user_id = ?
             WHERE o.user_id = ? AND oi.product_id = ?
               AND o.order_status IN ('delivered', 'shipped', 'processing')
               AND o.order_status <> 'cancelled'
               AND rv.review_id IS NULL
             ORDER BY o.created_at DESC
             LIMIT 10"
        );
        $elStmt->execute([$userId, $userId, $productId]);
        foreach ($elStmt->fetchAll() as $el) {
            $eligibleItems[] = [
                'order_item_id' => (int)$el['order_item_id'],
                'order_id' => (int)$el['order_id'],
                'order_number' => (string)$el['order_number'],
                'product_name' => (string)$el['product_name'],
                'ordered_at' => (string)$el['created_at'],
            ];
        }
    }

    MelvoreResponse::ok([
        'reviews' => $reviews,
        'eligible_order_items' => $eligibleItems,
        'pagination' => [
            'page' => $page,
            'limit' => $limit,
            'total' => $total,
            'total_pages' => max(1, (int)ceil($total / $limit)),
        ],
    ], 'Product reviews loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load product reviews.', 'SERVER_ERROR', 500);
}
