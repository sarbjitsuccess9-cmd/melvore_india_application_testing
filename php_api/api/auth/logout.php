<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';

MelvoreResponse::requireMethod('POST');

try {
    $token = MelvoreAuth::getBearerToken();
    MelvoreAuth::revokeToken($token);
    MelvoreResponse::okFlat([], 'Signed out successfully');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to sign out.', 'SERVER_ERROR', 500);
}
