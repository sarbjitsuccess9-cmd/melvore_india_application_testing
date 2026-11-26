<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/includes/db.php';
require_once dirname(__DIR__) . '/includes/response.php';

/**
 * Unified Cart Data Access & Guest-to-User Cart Merge Helper.
 * Authenticated users use `user_cart`; guests use `cart` (`user_id IS NULL`).
 */
final class MelvoreCartHelper
{
    public static function fetchCartItems(PDO $pdo, ?int $userId, string $guestId): array
    {
        if ($userId !== null && $userId > 0) {
            self::consolidateLegacyCartsIfNeeded($pdo, $userId);
            $stmt = $pdo->prepare(
                'SELECT uc.user_cart_id AS cart_id, uc.product_id, uc.variation_id, uc.quantity,
                        uc.color, uc.size,
                        p.name, p.sku, p.img_src, p.regular_price, p.sale_price, p.stock_quantity,
                        p.category_id, p.is_active,
                        pv.regular_price AS var_regular_price, pv.sale_price AS var_sale_price,
                        pv.stock_quantity AS var_stock_quantity, pv.image_url AS var_image_url,
                        pv.is_active AS var_is_active
                 FROM user_cart uc
                 INNER JOIN products p ON p.product_id = uc.product_id
                 LEFT JOIN product_variations pv ON pv.variation_id = uc.variation_id
                 WHERE uc.user_id = ? AND p.is_active = 1
                 ORDER BY uc.added_at DESC, uc.user_cart_id DESC'
            );
            $stmt->execute([$userId]);
            return self::formatRows($stmt->fetchAll());
        }

        if ($guestId === '') {
            return [];
        }

        $stmt = $pdo->prepare(
            'SELECT c.cart_id, c.product_id, c.variation_id, c.quantity,
                    c.color, c.size,
                    p.name, p.sku, p.img_src, p.regular_price, p.sale_price, p.stock_quantity,
                    p.category_id, p.is_active,
                    pv.regular_price AS var_regular_price, pv.sale_price AS var_sale_price,
                    pv.stock_quantity AS var_stock_quantity, pv.image_url AS var_image_url,
                    pv.is_active AS var_is_active
             FROM cart c
             INNER JOIN products p ON p.product_id = c.product_id
             LEFT JOIN product_variations pv ON pv.variation_id = c.variation_id
             WHERE c.user_id IS NULL AND c.session_id = ? AND p.is_active = 1
             ORDER BY c.added_at DESC, c.cart_id DESC'
        );
        $stmt->execute([$guestId]);
        return self::formatRows($stmt->fetchAll());
    }

    public static function mergeGuestCartToUser(PDO $pdo, int $userId, string $guestId): void
    {
        if ($userId <= 0 || $guestId === '' || !MelvoreDB::tableExists('cart') || !MelvoreDB::tableExists('user_cart')) {
            return;
        }
        $gStmt = $pdo->prepare('SELECT cart_id, product_id, variation_id, quantity, color, size FROM cart WHERE user_id IS NULL AND session_id = ?');
        $gStmt->execute([$guestId]);
        $guestRows = $gStmt->fetchAll();
        if (empty($guestRows)) {
            return;
        }

        foreach ($guestRows as $gr) {
            $pid = (int)$gr['product_id'];
            $vid = $gr['variation_id'] !== null ? (int)$gr['variation_id'] : null;
            $qty = max(1, (int)$gr['quantity']);
            $color = $gr['color'] !== null ? trim((string)$gr['color']) : '';
            $size = $gr['size'] !== null ? trim((string)$gr['size']) : '';

            $findStmt = $pdo->prepare(
                'SELECT user_cart_id, quantity FROM user_cart
                 WHERE user_id = ? AND product_id = ?
                   AND COALESCE(variation_id, 0) = ?
                   AND COALESCE(color, "") = ?
                   AND COALESCE(size, "") = ?
                 LIMIT 1'
            );
            $findStmt->execute([$userId, $pid, $vid ?? 0, $color, $size]);
            $existing = $findStmt->fetch();

            if ($existing) {
                $newQty = min(20, ((int)$existing['quantity']) + $qty);
                $upd = $pdo->prepare('UPDATE user_cart SET quantity = ? WHERE user_cart_id = ?');
                $upd->execute([$newQty, (int)$existing['user_cart_id']]);
            } else {
                $ins = $pdo->prepare(
                    'INSERT INTO user_cart (user_id, product_id, variation_id, quantity, color, size)
                     VALUES (?, ?, ?, ?, ?, ?)'
                );
                $ins->execute([
                    $userId,
                    $pid,
                    $vid,
                    min(20, $qty),
                    $color !== '' ? $color : null,
                    $size !== '' ? $size : null,
                ]);
            }
        }

        $del = $pdo->prepare('DELETE FROM cart WHERE user_id IS NULL AND session_id = ?');
        $del->execute([$guestId]);
    }

    public static function consolidateLegacyCartsIfNeeded(PDO $pdo, int $userId): void
    {
        if ($userId <= 0 || !MelvoreDB::tableExists('carts') || !MelvoreDB::tableExists('user_cart')) {
            return;
        }
        $cntStmt = $pdo->prepare('SELECT COUNT(*) FROM user_cart WHERE user_id = ?');
        $cntStmt->execute([$userId]);
        if ((int)$cntStmt->fetchColumn() > 0) {
            return;
        }
        $legStmt = $pdo->prepare('SELECT product_id, variation_id, quantity, color, size FROM carts WHERE user_id = ?');
        $legStmt->execute([$userId]);
        $rows = $legStmt->fetchAll();
        if (empty($rows)) {
            return;
        }
        foreach ($rows as $r) {
            $pid = (int)$r['product_id'];
            $vid = $r['variation_id'] !== null ? (int)$r['variation_id'] : null;
            $qty = max(1, (int)$r['quantity']);
            $color = $r['color'] ?? null;
            $size = $r['size'] ?? null;

            $chk = $pdo->prepare(
                'SELECT user_cart_id FROM user_cart
                 WHERE user_id = ? AND product_id = ? AND COALESCE(variation_id, 0) = ? AND COALESCE(color, "") = ? AND COALESCE(size, "") = ?
                 LIMIT 1'
            );
            $chk->execute([$userId, $pid, $vid ?? 0, (string)($color ?? ''), (string)($size ?? '')]);
            if (!$chk->fetch()) {
                $ins = $pdo->prepare(
                    'INSERT INTO user_cart (user_id, product_id, variation_id, quantity, color, size) VALUES (?, ?, ?, ?, ?, ?)'
                );
                $ins->execute([$userId, $pid, $vid, min(20, $qty), $color, $size]);
            }
        }
    }

    private static function formatRows(array $rows): array
    {
        $items = [];
        foreach ($rows as $row) {
            $regPrice = ($row['var_regular_price'] !== null && (float)$row['var_regular_price'] > 0)
                ? (float)$row['var_regular_price']
                : (float)$row['regular_price'];
            $salePrice = null;
            if ($row['var_sale_price'] !== null && (float)$row['var_sale_price'] > 0) {
                $salePrice = (float)$row['var_sale_price'];
            } elseif ($row['sale_price'] !== null && (float)$row['sale_price'] > 0) {
                $salePrice = (float)$row['sale_price'];
            }
            $unitPrice = ($salePrice !== null && $salePrice < $regPrice) ? $salePrice : $regPrice;

            $stock = ($row['variation_id'] !== null && $row['var_stock_quantity'] !== null)
                ? (int)$row['var_stock_quantity']
                : (int)$row['stock_quantity'];
            $qty = max(1, (int)$row['quantity']);

            $img = $row['var_image_url'] ?? null;
            if ($img === null || trim((string)$img) === '') {
                $parts = array_values(array_filter(array_map('trim', explode('|', (string)($row['img_src'] ?? '')))));
                $img = $parts[0] ?? null;
            }

            $color = isset($row['color']) && trim((string)$row['color']) !== '' ? trim((string)$row['color']) : null;
            $size = isset($row['size']) && trim((string)$row['size']) !== '' ? trim((string)$row['size']) : null;

            $items[] = [
                'cart_id' => (int)$row['cart_id'],
                'product_id' => (int)$row['product_id'],
                'variation_id' => $row['variation_id'] !== null ? (int)$row['variation_id'] : null,
                'category_id' => (int)($row['category_id'] ?? 0),
                'name' => (string)$row['name'],
                'sku' => (string)($row['sku'] ?? ''),
                'color' => $color,
                'size' => $size,
                'attributes' => [
                    'color' => $color,
                    'size' => $size,
                ],
                'image_url' => MelvoreResponse::mediaUrl($img !== null ? (string)$img : null),
                'unit_price' => round($unitPrice, 2),
                'regular_price' => round($regPrice, 2),
                'quantity' => $qty,
                'line_total' => round($unitPrice * $qty, 2),
                'available_stock' => max(0, $stock),
                'in_stock' => $stock >= $qty && $stock > 0,
            ];
        }
        return $items;
    }
}
