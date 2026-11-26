<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';

MelvoreResponse::requireMethod('POST', 'DELETE');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::requireUserId();
    $body = MelvoreResponse::jsonBody();
    $productId = (int)($body['product_id'] ?? ($_GET['product_id'] ?? 0));
    if ($productId > 0) {
        $pdo->prepare('DELETE FROM wishlist WHERE user_id = ? AND product_id = ?')->execute([$userId, $productId]);
    }

    $stmt = $pdo->prepare(
        'SELECT w.wishlist_id, w.product_id, p.sku, p.name, p.slug, p.special_icon,
                p.regular_price, p.sale_price, p.stock_quantity, p.rating_average, p.img_src,
                c.name AS category_name
         FROM wishlist w
         INNER JOIN products p ON p.product_id = w.product_id
         LEFT JOIN categories c ON c.category_id = p.category_id
         WHERE w.user_id = ? AND p.is_active = 1
         ORDER BY w.added_at DESC, w.wishlist_id DESC'
    );
    $stmt->execute([$userId]);
    $items = [];
    $productIds = [];
    foreach ($stmt->fetchAll() as $row) {
        $pid = (int)$row['product_id'];
        $productIds[] = $pid;
        $imgParts = array_values(array_filter(array_map('trim', explode('|', (string)($row['img_src'] ?? '')))));
        $sale = isset($row['sale_price']) && $row['sale_price'] !== null && (float)$row['sale_price'] > 0
            ? (float)$row['sale_price'] : null;
        $stock = (int)($row['stock_quantity'] ?? 0);
        $items[] = [
            'wishlist_id' => (int)$row['wishlist_id'],
            'product_id' => $pid,
            'sku' => (string)($row['sku'] ?? ''),
            'name' => (string)$row['name'],
            'slug' => (string)($row['slug'] ?? ''),
            'category_name' => $row['category_name'] ?? null,
            'badge' => $row['special_icon'] ?: null,
            'regular_price' => (float)$row['regular_price'],
            'sale_price' => $sale,
            'stock_quantity' => $stock,
            'in_stock' => $stock > 0,
            'rating_average' => (float)($row['rating_average'] ?? 0),
            'primary_image' => isset($imgParts[0]) ? MelvoreResponse::mediaUrl($imgParts[0]) : null,
        ];
    }

    MelvoreResponse::okFlat([
        'items' => $items,
        'product_ids' => $productIds,
        'count' => count($items),
    ], 'Removed from wishlist');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to remove item from wishlist.', 'SERVER_ERROR', 500);
}
