<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/user_formatter.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::requireUserId();
    $stmt = $pdo->prepare('SELECT * FROM user_addresses WHERE user_id = ? ORDER BY is_default DESC, address_id DESC');
    $stmt->execute([$userId]);
    $addresses = array_map([MelvoreUserFormatter::class, 'formatAddress'], $stmt->fetchAll());

    MelvoreResponse::okFlat(['addresses' => $addresses], 'Addresses loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load addresses.', 'SERVER_ERROR', 500);
}
