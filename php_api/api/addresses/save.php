<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/user_formatter.php';

MelvoreResponse::requireMethod('POST');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::requireUserId();
    $body = MelvoreResponse::jsonBody();

    $addressId = isset($body['address_id']) && (int)$body['address_id'] > 0 ? (int)$body['address_id'] : null;
    $fullName = trim((string)($body['full_name'] ?? ''));
    $mobile = preg_replace('/[^0-9+\- ]/', '', trim((string)($body['mobile'] ?? '')));
    $line1 = trim((string)($body['address_line1'] ?? ''));
    $line2 = trim((string)($body['address_line2'] ?? ''));
    $city = trim((string)($body['city'] ?? ''));
    $state = trim((string)($body['state'] ?? ''));
    $pincode = preg_replace('/[^0-9A-Za-z\- ]/', '', trim((string)($body['pincode'] ?? '')));
    $country = trim((string)($body['country'] ?? 'India')) ?: 'India';
    $type = strtolower(trim((string)($body['address_type'] ?? 'home')));
    if (!in_array($type, ['home', 'work', 'other'], true)) {
        $type = 'home';
    }
    $isDefault = !empty($body['is_default']) ? 1 : 0;

    if ($fullName === '' || $mobile === '' || $line1 === '' || $city === '' || $state === '' || $pincode === '') {
        MelvoreResponse::error('Please complete all required address fields.', 'VALIDATION_ERROR', 400);
    }

    $pdo->beginTransaction();
    if ($isDefault === 1) {
        $pdo->prepare('UPDATE user_addresses SET is_default = 0 WHERE user_id = ?')->execute([$userId]);
    }

    if ($addressId !== null) {
        $chk = $pdo->prepare('SELECT address_id FROM user_addresses WHERE address_id = ? AND user_id = ? LIMIT 1');
        $chk->execute([$addressId, $userId]);
        if (!$chk->fetch()) {
            $pdo->rollBack();
            MelvoreResponse::error('Address not found.', 'NOT_FOUND', 404);
        }
        $upd = $pdo->prepare(
            'UPDATE user_addresses
             SET full_name = ?, address_line1 = ?, address_line2 = ?, city = ?, state = ?, pincode = ?, country = ?, mobile = ?, is_default = ?, address_type = ?
             WHERE address_id = ? AND user_id = ?'
        );
        $upd->execute([$fullName, $line1, $line2 !== '' ? $line2 : null, $city, $state, $pincode, $country, $mobile, $isDefault, $type, $addressId, $userId]);
        $savedId = $addressId;
    } else {
        $ins = $pdo->prepare(
            'INSERT INTO user_addresses (user_id, full_name, address_line1, address_line2, city, state, pincode, country, mobile, is_default, address_type)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $ins->execute([$userId, $fullName, $line1, $line2 !== '' ? $line2 : null, $city, $state, $pincode, $country, $mobile, $isDefault, $type]);
        $savedId = (int)$pdo->lastInsertId();
    }
    $pdo->commit();

    $rowStmt = $pdo->prepare('SELECT * FROM user_addresses WHERE address_id = ? LIMIT 1');
    $rowStmt->execute([$savedId]);
    $row = $rowStmt->fetch();

    MelvoreResponse::okFlat([
        'address' => $row ? MelvoreUserFormatter::formatAddress($row) : null,
    ], 'Address saved');
} catch (Throwable $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    MelvoreResponse::error('Unable to save address.', 'SERVER_ERROR', 500);
}
