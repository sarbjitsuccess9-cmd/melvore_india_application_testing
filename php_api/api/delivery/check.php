<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $pincode = preg_replace('/[^0-9]/', '', (string)($_GET['pincode'] ?? ''));
    $productId = (int)($_GET['product_id'] ?? 0);

    if (strlen($pincode) !== 6) {
        MelvoreResponse::error('Please enter a valid 6-digit pincode.', 'VALIDATION_ERROR', 422);
    }

    if (MelvoreDB::tableExists('app_delivery_serviceability_cache')) {
        $stmt = $pdo->prepare(
            'SELECT * FROM app_delivery_serviceability_cache
             WHERE pincode = ?
             ORDER BY updated_at DESC
             LIMIT 1'
        );
        $stmt->execute([$pincode]);
        $row = $stmt->fetch();

        if ($row) {
            $serviceable = ((int)($row['is_serviceable'] ?? 1)) === 1;
            $express = $serviceable && ((int)($row['express_available'] ?? 0)) === 1;
            $cod = $serviceable && ((int)($row['cod_available'] ?? 1)) === 1;
            $minDays = (int)($row['estimated_days_min'] ?? 3);
            $maxDays = (int)($row['estimated_days_max'] ?? 6);
            MelvoreResponse::ok([
                'pincode' => $pincode,
                'city' => (string)($row['city'] ?? ''),
                'state' => (string)($row['state'] ?? ''),
                'is_serviceable' => $serviceable,
                'express_available' => $express,
                'cod_available' => $cod,
                'estimated_days_min' => $minDays,
                'estimated_days_max' => $maxDays,
                'delivery_message' => $serviceable
                    ? ($express ? "Express delivery available to $pincode ($minDays-$maxDays days)" : "Standard delivery to $pincode in $minDays-$maxDays business days")
                    : "Currently unserviceable for pincode $pincode",
                'verified_by_cache' => true,
            ], 'Delivery serviceability checked');
        }
    }

    // Conservative uncached pincode response without inventing unverified express promises
    $validPrefix = ((int)$pincode[0]) >= 1 && ((int)$pincode[0]) <= 8;
    MelvoreResponse::ok([
        'pincode' => $pincode,
        'city' => '',
        'state' => '',
        'is_serviceable' => $validPrefix,
        'express_available' => false,
        'cod_available' => $validPrefix,
        'estimated_days_min' => 4,
        'estimated_days_max' => 7,
        'delivery_message' => $validPrefix
            ? "Standard courier delivery available to $pincode (4-7 business days)"
            : "Delivery availability could not be confirmed for $pincode",
        'verified_by_cache' => false,
    ], 'Delivery serviceability checked');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to check pincode serviceability.', 'SERVER_ERROR', 500);
}
