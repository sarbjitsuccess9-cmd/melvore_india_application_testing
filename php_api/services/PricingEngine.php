<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/includes/db.php';

/**
 * Server-side Checkout & Coupon Pricing Engine for Melvore.
 * Strictly validates `coupons` using verified production column names:
 * `usage_count`, `valid_to`, `valid_from`, `min_order_amount`, `max_discount_amount`,
 * `usage_limit`, `per_user_limit`, `payment_methods`, `item_count_min`, `item_count_max`, `categories`.
 */
final class MelvorePricingEngine
{
    public static function loadSitePricingConfig(PDO $pdo): array
    {
        $cfg = [
            'shipping_charge' => 0.0,
            'free_shipping_minimum' => 0.0,
            'tax_rate_percent' => 0.0,
            'cod_fee' => 0.0,
            'enable_cod' => true,
            'enable_online_payment' => true,
            'currency' => '₹',
        ];
        if (!MelvoreDB::tableExists('site_settings')) {
            return $cfg;
        }
        $keys = ['shipping_charge', 'free_shipping_minimum', 'tax_rate', 'cod_fee', 'enable_cod', 'enable_online_payment', 'currency'];
        $ph = implode(',', array_fill(0, count($keys), '?'));
        $stmt = $pdo->prepare("SELECT setting_key, setting_value FROM site_settings WHERE setting_key IN ($ph)");
        $stmt->execute($keys);
        foreach ($stmt->fetchAll() as $row) {
            $k = (string)$row['setting_key'];
            $v = (string)($row['setting_value'] ?? '');
            if ($k === 'shipping_charge') {
                $cfg['shipping_charge'] = max(0.0, (float)$v);
            } elseif ($k === 'free_shipping_minimum') {
                $cfg['free_shipping_minimum'] = max(0.0, (float)$v);
            } elseif ($k === 'tax_rate') {
                $cfg['tax_rate_percent'] = max(0.0, (float)$v);
            } elseif ($k === 'cod_fee') {
                $cfg['cod_fee'] = max(0.0, (float)$v);
            } elseif ($k === 'enable_cod') {
                $cfg['enable_cod'] = ((int)$v === 1 || strtolower($v) === 'true');
            } elseif ($k === 'enable_online_payment') {
                $cfg['enable_online_payment'] = ((int)$v === 1 || strtolower($v) === 'true');
            } elseif ($k === 'currency' && trim($v) !== '') {
                $cfg['currency'] = trim($v) === 'INR' ? '₹' : trim($v);
            }
        }
        return $cfg;
    }

    public static function calculateTotals(
        PDO $pdo,
        array $items,
        string $couponCode = '',
        string $paymentMethod = 'online',
        ?int $userId = null
    ): array {
        $cfg = self::loadSitePricingConfig($pdo);
        $subtotal = 0.0;
        $totalItems = 0;
        $categoryIds = [];

        foreach ($items as $it) {
            $subtotal += (float)($it['line_total'] ?? 0.0);
            $totalItems += max(1, (int)($it['quantity'] ?? 1));
            if (!empty($it['category_id'])) {
                $categoryIds[] = (int)$it['category_id'];
            }
        }
        $subtotal = round($subtotal, 2);

        $shippingCharge = ($cfg['free_shipping_minimum'] > 0 && $subtotal >= $cfg['free_shipping_minimum']) || $subtotal <= 0
            ? 0.0
            : round($cfg['shipping_charge'], 2);

        $codFee = (strtolower($paymentMethod) === 'cod' && $subtotal > 0)
            ? round($cfg['cod_fee'], 2)
            : 0.0;

        $discountAmount = 0.0;
        $appliedCouponId = null;
        $appliedCode = '';
        $couponMessage = null;

        $cleanCode = strtoupper(trim($couponCode));
        if ($cleanCode !== '' && MelvoreDB::tableExists('coupons')) {
            $cStmt = $pdo->prepare('SELECT * FROM coupons WHERE UPPER(code) = ? LIMIT 1');
            $cStmt->execute([$cleanCode]);
            $coupon = $cStmt->fetch();

            if (!$coupon) {
                $couponMessage = 'Coupon code was not found.';
            } elseif (((int)($coupon['is_active'] ?? 0)) !== 1) {
                $couponMessage = 'This coupon is currently inactive.';
            } elseif (!empty($coupon['valid_from']) && strtotime((string)$coupon['valid_from']) > time()) {
                $couponMessage = 'This coupon promotion has not started yet.';
            } elseif (!empty($coupon['valid_to']) && strtotime((string)$coupon['valid_to']) < time()) {
                $couponMessage = 'This coupon has expired.';
            } elseif (
                isset($coupon['usage_limit']) && $coupon['usage_limit'] !== null && (int)$coupon['usage_limit'] > 0
                && (int)($coupon['usage_count'] ?? 0) >= (int)$coupon['usage_limit']
            ) {
                $couponMessage = 'This coupon usage limit has been reached.';
            } elseif ($subtotal < (float)($coupon['min_order_amount'] ?? 0)) {
                $minAmt = number_format((float)$coupon['min_order_amount'], 0);
                $couponMessage = "Minimum order amount of ₹{$minAmt} required for this code.";
            } else {
                $error = self::checkEligibilityRules($pdo, $coupon, $totalItems, $categoryIds, $paymentMethod, $userId);
                if ($error !== null) {
                    $couponMessage = $error;
                } else {
                    $type = strtolower((string)($coupon['discount_type'] ?? 'fixed'));
                    $val = (float)($coupon['discount_value'] ?? 0);
                    if ($type === 'percentage') {
                        $discountAmount = round(($subtotal * $val) / 100.0, 2);
                        if (!empty($coupon['max_discount_amount']) && (float)$coupon['max_discount_amount'] > 0) {
                            $discountAmount = min($discountAmount, (float)$coupon['max_discount_amount']);
                        }
                    } elseif ($type === 'free_shipping') {
                        $discountAmount = $shippingCharge;
                        $shippingCharge = 0.0;
                    } else {
                        $discountAmount = min($subtotal, round($val, 2));
                    }
                    $discountAmount = max(0.0, min($subtotal + $shippingCharge, round($discountAmount, 2)));
                    $appliedCouponId = (int)$coupon['coupon_id'];
                    $appliedCode = (string)$coupon['code'];
                    $couponMessage = "Coupon {$appliedCode} applied!";
                }
            }
        }

        $taxableBase = max(0.0, $subtotal - $discountAmount);
        $taxAmount = $cfg['tax_rate_percent'] > 0
            ? round(($taxableBase * $cfg['tax_rate_percent']) / 100.0, 2)
            : 0.0;

        $totalAmount = max(0.0, round($subtotal + $shippingCharge + $taxAmount + $codFee - $discountAmount, 2));

        return [
            'subtotal' => $subtotal,
            'shipping_charge' => $shippingCharge,
            'free_ship_min' => round($cfg['free_shipping_minimum'], 2),
            'tax_amount' => $taxAmount,
            'discount_amount' => $discountAmount,
            'cod_fee' => $codFee,
            'total_amount' => $totalAmount,
            'coupon_id' => $appliedCouponId,
            'coupon_code' => $appliedCode,
            'coupon_message' => $couponMessage,
            'currency' => $cfg['currency'],
        ];
    }

    private static function checkEligibilityRules(
        PDO $pdo,
        array $coupon,
        int $totalItems,
        array $categoryIds,
        string $paymentMethod,
        ?int $userId
    ): ?string {
        $minItems = (int)($coupon['item_count_min'] ?? 0);
        if ($minItems > 0 && $totalItems < $minItems) {
            return "Requires at least {$minItems} items in bag.";
        }
        if (isset($coupon['item_count_max']) && $coupon['item_count_max'] !== null && (int)$coupon['item_count_max'] > 0) {
            $maxItems = (int)$coupon['item_count_max'];
            if ($totalItems > $maxItems) {
                return "Applicable on up to {$maxItems} items only.";
            }
        }
        if (!empty($coupon['payment_methods'])) {
            $pm = is_string($coupon['payment_methods']) ? json_decode($coupon['payment_methods'], true) : $coupon['payment_methods'];
            if (is_array($pm) && !empty($pm)) {
                $allowed = array_map('strtolower', array_map('strval', $pm));
                if (!in_array(strtolower($paymentMethod), $allowed, true)) {
                    return 'This coupon is not valid for the selected payment method.';
                }
            }
        }
        if (!empty($coupon['categories'])) {
            $cats = is_string($coupon['categories']) ? json_decode($coupon['categories'], true) : $coupon['categories'];
            if (is_array($cats) && !empty($cats)) {
                $allowedCats = array_map('intval', $cats);
                if (empty(array_intersect($allowedCats, $categoryIds))) {
                    return 'This coupon does not apply to items currently in your bag.';
                }
            }
        }
        if ($userId !== null && $userId > 0) {
            $perUserLimit = (int)($coupon['per_user_limit'] ?? 0);
            if ($perUserLimit > 0 && MelvoreDB::tableExists('orders')) {
                $uStmt = $pdo->prepare(
                    "SELECT COUNT(*) FROM orders
                     WHERE user_id = ? AND coupon_id = ? AND order_status <> 'cancelled'"
                );
                $uStmt->execute([$userId, (int)$coupon['coupon_id']]);
                if ((int)$uStmt->fetchColumn() >= $perUserLimit) {
                    return 'You have already redeemed this coupon the maximum number of times.';
                }
            }
            if (!empty($coupon['conditions'])) {
                $cond = is_string($coupon['conditions']) ? json_decode($coupon['conditions'], true) : $coupon['conditions'];
                if (is_array($cond) && !empty($cond['first_order_only'])) {
                    $oStmt = $pdo->prepare("SELECT COUNT(*) FROM orders WHERE user_id = ? AND order_status <> 'cancelled'");
                    $oStmt->execute([$userId]);
                    if ((int)$oStmt->fetchColumn() > 0) {
                        return 'This offer is valid on your first Melvore order only.';
                    }
                }
            }
        }
        return null;
    }
}
