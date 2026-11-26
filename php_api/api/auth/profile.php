<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/user_formatter.php';

MelvoreResponse::requireMethod('GET', 'PUT');

try {
    $pdo = MelvoreDB::pdo();
    $uid = MelvoreAuth::requireUserId();
    $method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

    if ($method === 'PUT') {
        $body = MelvoreResponse::jsonBody();
        $name = trim((string)($body['name'] ?? ''));
        $mobilePrimary = preg_replace('/[^0-9+\- ]/', '', trim((string)($body['mobile_primary'] ?? '')));
        $mobileAlt = isset($body['mobile_alternate']) && trim((string)$body['mobile_alternate']) !== ''
            ? preg_replace('/[^0-9+\- ]/', '', trim((string)$body['mobile_alternate']))
            : null;

        if (strlen($name) < 2) {
            MelvoreResponse::error('Name must be at least 2 characters.', 'VALIDATION_ERROR', 400);
        }
        $pdo->prepare('UPDATE users SET name = ?, mobile_primary = ?, mobile_alternate = ? WHERE user_id = ?')
            ->execute([$name, $mobilePrimary, $mobileAlt, $uid]);
    }

    $uStmt = $pdo->prepare('SELECT * FROM users WHERE user_id = ? LIMIT 1');
    $uStmt->execute([$uid]);
    $user = $uStmt->fetch();
    if (!$user) {
        MelvoreResponse::error('User not found.', 'NOT_FOUND', 404);
    }

    $cartCount = 0;
    if (MelvoreDB::tableExists('user_cart')) {
        $cStmt = $pdo->prepare('SELECT COALESCE(SUM(quantity), 0) FROM user_cart WHERE user_id = ?');
        $cStmt->execute([$uid]);
        $cartCount = (int)$cStmt->fetchColumn();
    }
    $wishCount = 0;
    if (MelvoreDB::tableExists('wishlist')) {
        $wStmt = $pdo->prepare('SELECT COUNT(*) FROM wishlist WHERE user_id = ?');
        $wStmt->execute([$uid]);
        $wishCount = (int)$wStmt->fetchColumn();
    }
    $pendingOrders = 0;
    if (MelvoreDB::tableExists('orders')) {
        $oStmt = $pdo->prepare("SELECT COUNT(*) FROM orders WHERE user_id = ? AND order_status IN ('pending', 'processing', 'shipped')");
        $oStmt->execute([$uid]);
        $pendingOrders = (int)$oStmt->fetchColumn();
    }

    $addresses = [];
    if (MelvoreDB::tableExists('user_addresses')) {
        $aStmt = $pdo->prepare('SELECT * FROM user_addresses WHERE user_id = ? ORDER BY is_default DESC, address_id DESC');
        $aStmt->execute([$uid]);
        foreach ($aStmt->fetchAll() as $addr) {
            $addresses[] = MelvoreUserFormatter::formatAddress($addr);
        }
    }

    MelvoreResponse::okFlat([
        'user' => MelvoreUserFormatter::formatUser($user),
        'counts' => [
            'cart' => $cartCount,
            'wishlist' => $wishCount,
            'pending_orders' => $pendingOrders,
        ],
        'addresses' => $addresses,
    ], $method === 'PUT' ? 'Profile updated' : 'Profile loaded');
} catch (Throwable $e) {
    error_log('[Melvore][auth/profile] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to load profile.', 'SERVER_ERROR', 500);
}
