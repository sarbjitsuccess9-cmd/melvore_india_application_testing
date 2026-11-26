<?php
declare(strict_types=1);

require_once __DIR__ . '/db.php';
require_once __DIR__ . '/response.php';

/**
 * Formats product rows consistently from `products`, `categories`, `brands`, and `app_product_media`.
 */
final class MelvoreProductFormatter
{
    public static function formatRows(array $rows, bool $includeMedia = false): array
    {
        if (empty($rows)) {
            return [];
        }
        $pdo = MelvoreDB::pdo();
        $productIds = array_values(array_unique(array_map(static fn($r) => (int)$r['product_id'], $rows)));
        $mediaByProduct = [];

        if ($includeMedia && MelvoreDB::tableExists('app_product_media') && !empty($productIds)) {
            $placeholders = implode(',', array_fill(0, count($productIds), '?'));
            $mStmt = $pdo->prepare(
                "SELECT product_id, media_type, media_url, thumbnail_url, alt_text, is_primary, display_order
                 FROM app_product_media
                 WHERE product_id IN ($placeholders) AND is_active = 1
                 ORDER BY is_primary DESC, display_order ASC"
            );
            $mStmt->execute($productIds);
            foreach ($mStmt->fetchAll() as $m) {
                $pid = (int)$m['product_id'];
                $mediaByProduct[$pid][] = [
                    'type' => (string)($m['media_type'] ?? 'image'),
                    'url' => MelvoreResponse::mediaUrl((string)($m['media_url'] ?? '')),
                    'thumbnail_url' => MelvoreResponse::mediaUrl($m['thumbnail_url'] ?? null),
                    'alt_text' => (string)($m['alt_text'] ?? ''),
                    'is_primary' => ((int)($m['is_primary'] ?? 0)) === 1,
                ];
            }
        }

        $out = [];
        foreach ($rows as $row) {
            $pid = (int)$row['product_id'];
            $imgParts = array_values(array_filter(array_map('trim', explode('|', (string)($row['img_src'] ?? '')))));
            $gallery = [];
            foreach ($imgParts as $part) {
                $resolved = MelvoreResponse::mediaUrl($part);
                if ($resolved !== null) {
                    $gallery[] = $resolved;
                }
            }
            $videos = [];
            if (!empty($mediaByProduct[$pid])) {
                foreach ($mediaByProduct[$pid] as $mItem) {
                    if ($mItem['type'] === 'video' && !empty($mItem['url'])) {
                        $videos[] = $mItem;
                    } elseif (!empty($mItem['url']) && !in_array($mItem['url'], $gallery, true)) {
                        $gallery[] = $mItem['url'];
                    }
                }
            }

            $regPrice = (float)($row['regular_price'] ?? 0);
            $salePrice = isset($row['sale_price']) && $row['sale_price'] !== null && (float)$row['sale_price'] > 0
                ? (float)$row['sale_price']
                : null;
            $effective = $salePrice ?? $regPrice;
            $discountPercent = ($salePrice !== null && $salePrice < $regPrice && $regPrice > 0)
                ? (int)floor((($regPrice - $salePrice) / $regPrice) * 100)
                : 0;

            $colors = [];
            if (!empty($row['available_colors'])) {
                $decodedColors = is_string($row['available_colors'])
                    ? json_decode($row['available_colors'], true)
                    : $row['available_colors'];
                if (is_array($decodedColors)) {
                    foreach ($decodedColors as $c) {
                        if (is_array($c)) {
                            $colors[] = [
                                'name' => (string)($c['name'] ?? ($c['label'] ?? 'Default')),
                                'value' => (string)($c['value'] ?? ($c['hex'] ?? '#141414')),
                            ];
                        } elseif (is_string($c) && $c !== '') {
                            $colors[] = ['name' => $c, 'value' => '#141414'];
                        }
                    }
                }
            }

            $sizes = [];
            if (!empty($row['available_sizes'])) {
                $decodedSizes = is_string($row['available_sizes'])
                    ? json_decode($row['available_sizes'], true)
                    : $row['available_sizes'];
                if (is_array($decodedSizes)) {
                    $sizes = array_values(array_filter(array_map('strval', $decodedSizes)));
                }
            }

            $stockQty = (int)($row['stock_quantity'] ?? 0);
            $origin = (string)($row['origin_country'] ?? '');
            $isSponsored = isset($row['is_sponsored']) ? ((int)$row['is_sponsored'] === 1) : false;
            $expressEligible = isset($row['express_delivery']) ? ((int)$row['express_delivery'] === 1) : false;

            $out[] = [
                'product_id' => $pid,
                'sku' => (string)($row['sku'] ?? ''),
                'name' => (string)($row['name'] ?? ''),
                'slug' => (string)($row['slug'] ?? ''),
                'badge' => $row['special_icon'] ?: ($row['badge'] ?? null),
                'short_description' => $row['short_description'] ?? null,
                'description' => $row['description'] ?? null,
                'category_id' => (int)($row['category_id'] ?? 0),
                'category_name' => $row['category_name'] ?? null,
                'category_slug' => $row['category_slug'] ?? null,
                'brand_id' => isset($row['brand_id']) ? (int)$row['brand_id'] : null,
                'brand_name' => $row['brand_name'] ?? null,
                'brand_slug' => $row['brand_slug'] ?? null,
                'origin_country' => $origin !== '' ? $origin : null,
                'is_imported' => $origin !== '' || !empty($row['is_imported']),
                'regular_price' => $regPrice,
                'sale_price' => $salePrice,
                'effective_price' => $effective,
                'discount_percent' => $discountPercent,
                'stock_quantity' => $stockQty,
                'in_stock' => $stockQty > 0,
                'is_featured' => ((int)($row['is_featured'] ?? 0)) === 1,
                'is_sponsored' => $isSponsored,
                'express_delivery' => $expressEligible,
                'rating_average' => (float)($row['rating_average'] ?? 0),
                'rating_count' => (int)($row['rating_count'] ?? 0),
                'primary_image' => $gallery[0] ?? null,
                'images' => $gallery,
                'videos' => $videos,
                'available_colors' => $colors,
                'available_sizes' => $sizes,
            ];
        }
        return $out;
    }
}
