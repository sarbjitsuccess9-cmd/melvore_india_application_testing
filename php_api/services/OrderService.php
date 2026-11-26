<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/includes/db.php';
require_once dirname(__DIR__) . '/includes/response.php';

/**
 * Shared order item formatter, atomic inventory restoration, and timeline builder.
 */
final class MelvoreOrderService
{
    public static function restoreStockAndCoupon(PDO $pdo, array $orderRow): void
    {
        $orderId = (int)$orderRow['order_id'];
        $itemsStmt = $pdo->prepare('SELECT product_id, variation_id, quantity FROM order_items WHERE order_id = ?');
        $itemsStmt->execute([$orderId]);
        foreach ($itemsStmt->fetchAll() as $it) {
            $qty = max(1, (int)$it['quantity']);
            $pid = (int)$it['product_id'];
            $vid = $it['variation_id'] !== null ? (int)$it['variation_id'] : null;

            $pdo->prepare('UPDATE products SET stock_quantity = stock_quantity + ?, purchase_count = GREATEST(0, purchase_count - ?) WHERE product_id = ?')
                ->execute([$qty, $qty, $pid]);
            if ($vid !== null && $vid > 0) {
                $pdo->prepare('UPDATE product_variations SET stock_quantity = stock_quantity + ? WHERE variation_id = ?')
                    ->execute([$qty, $vid]);
            }
        }

        if (!empty($orderRow['coupon_id']) && MelvoreDB::tableExists('coupons')) {
            $pdo->prepare('UPDATE coupons SET usage_count = GREATEST(0, usage_count - 1) WHERE coupon_id = ?')
                ->execute([(int)$orderRow['coupon_id']]);
        }
    }

    public static function formatOrderItems(PDO $pdo, int $orderId): array
    {
        $stmt = $pdo->prepare(
            'SELECT oi.*, p.img_src
             FROM order_items oi
             LEFT JOIN products p ON p.product_id = oi.product_id
             WHERE oi.order_id = ?
             ORDER BY oi.order_item_id ASC'
        );
        $stmt->execute([$orderId]);
        $out = [];
        foreach ($stmt->fetchAll() as $row) {
            $attrs = [];
            if (!empty($row['attributes'])) {
                $decoded = is_string($row['attributes']) ? json_decode($row['attributes'], true) : $row['attributes'];
                if (is_array($decoded)) {
                    $attrs = $decoded;
                }
            }
            $parts = array_values(array_filter(array_map('trim', explode('|', (string)($row['img_src'] ?? '')))));
            $out[] = [
                'order_item_id' => (int)$row['order_item_id'],
                'product_id' => (int)$row['product_id'],
                'product_name' => (string)$row['product_name'],
                'sku' => (string)($row['sku'] ?? ''),
                'quantity' => (int)$row['quantity'],
                'unit_price' => (float)$row['unit_price'],
                'total_price' => (float)$row['total_price'],
                'color' => $attrs['color'] ?? null,
                'size' => $attrs['size'] ?? null,
                'attributes' => [
                    'color' => $attrs['color'] ?? null,
                    'size' => $attrs['size'] ?? null,
                ],
                'image_url' => isset($parts[0]) ? MelvoreResponse::mediaUrl($parts[0]) : null,
            ];
        }
        return $out;
    }

    public static function buildTimeline(array $order): array
    {
        $status = strtolower((string)($order['order_status'] ?? 'pending'));
        $created = (string)($order['created_at'] ?? '');
        if ($status === 'cancelled' || $status === 'refunded') {
            return [
                ['key' => 'placed', 'title' => 'Order Placed', 'description' => 'Order received by Melvore', 'timestamp' => $created, 'completed' => true, 'current' => false],
                ['key' => $status, 'title' => ucfirst($status), 'description' => (string)($order['cancellation_reason'] ?? 'Order cancelled'), 'timestamp' => null, 'completed' => true, 'current' => true],
            ];
        }

        $stages = [
            'pending' => ['Order Placed', 'Waiting for confirmation'],
            'processing' => ['Curating & Packing', 'Prepared in studio quality check'],
            'shipped' => ['Dispatched', !empty($order['courier_name']) ? ('With ' . $order['courier_name']) : 'In transit to your address'],
            'delivered' => ['Delivered', 'Order delivered'],
        ];
        $orderRank = ['pending' => 0, 'processing' => 1, 'shipped' => 2, 'delivered' => 3];
        $curRank = $orderRank[$status] ?? 0;

        $timeline = [];
        $idx = 0;
        foreach ($stages as $k => [$title, $desc]) {
            $timeline[] = [
                'key' => $k,
                'title' => $title,
                'description' => $desc,
                'timestamp' => $idx === 0 ? $created : null,
                'completed' => $idx <= $curRank,
                'current' => $idx === $curRank,
            ];
            $idx++;
        }
        return $timeline;
    }
}
