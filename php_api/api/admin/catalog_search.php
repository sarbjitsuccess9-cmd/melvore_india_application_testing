<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/product_formatter.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    MelvoreAuth::requireAdminUserId();
    $q = trim((string)($_GET['q'] ?? ''));
    $limit = max(5, min(50, (int)($_GET['limit'] ?? 24)));

    if ($q !== '') {
        $like = '%' . $q . '%';
        $stmt = $pdo->prepare(
            "SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
             FROM products p
             LEFT JOIN categories c ON c.category_id = p.category_id
             LEFT JOIN brands b ON b.brand_id = p.brand_id
             WHERE p.is_active = 1 AND (p.name LIKE ? OR p.sku LIKE ? OR b.name LIKE ? OR c.name LIKE ?)
             ORDER BY p.is_featured DESC, p.product_id DESC
             LIMIT ?"
        );
        $stmt->bindValue(1, $like, PDO::PARAM_STR);
        $stmt->bindValue(2, $like, PDO::PARAM_STR);
        $stmt->bindValue(3, $like, PDO::PARAM_STR);
        $stmt->bindValue(4, $like, PDO::PARAM_STR);
        $stmt->bindValue(5, $limit, PDO::PARAM_INT);
        $stmt->execute();
    } else {
        $stmt = $pdo->prepare(
            "SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
             FROM products p
             LEFT JOIN categories c ON c.category_id = p.category_id
             LEFT JOIN brands b ON b.brand_id = p.brand_id
             WHERE p.is_active = 1
             ORDER BY p.is_featured DESC, p.product_id DESC
             LIMIT ?"
        );
        $stmt->bindValue(1, $limit, PDO::PARAM_INT);
        $stmt->execute();
    }

    $products = MelvoreProductFormatter::formatRows($stmt->fetchAll());
    MelvoreResponse::ok(['products' => $products], 'Catalog products loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to search catalog.', 'SERVER_ERROR', 500);
}
