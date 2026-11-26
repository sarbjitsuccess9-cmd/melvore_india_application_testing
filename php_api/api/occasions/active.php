<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/services/ThemeEngineService.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $occasions = MelvoreThemeEngineService::fetchActiveOccasions(
        $pdo,
        trim((string)($_GET['country'] ?? 'India')),
        trim((string)($_GET['state'] ?? '')),
        trim((string)($_GET['city'] ?? ''))
    );
    MelvoreResponse::ok([
        'active_occasion' => $occasions[0] ?? null,
        'occasions' => $occasions,
    ], 'Active occasions loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load active occasions.', 'SERVER_ERROR', 500);
}
