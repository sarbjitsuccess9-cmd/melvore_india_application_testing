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
    $addressId = (int)($body['address_id'] ?? ($_GET['address_id'] ?? 0));
    if ($addressId <= 0) {
        MelvoreResponse::error('Valid address_id is required.', 'VALIDATION_ERROR', 400);
    }

    $pdo->prepare('DELETE FROM user_addresses WHERE address_id = ? AND user_id = ?')->execute([$addressId, $userId]);
    MelvoreResponse::okFlat([], 'Address removed');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to delete address.', 'SERVER_ERROR', 500);
}
