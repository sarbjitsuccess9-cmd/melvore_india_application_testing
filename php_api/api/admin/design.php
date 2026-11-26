<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/DesignSystemService.php';

MelvoreResponse::requireMethod('GET', 'POST', 'PUT');

try {
    $pdo = MelvoreDB::pdo();
    $adminUserId = MelvoreAuth::requireAdminUserId();
    $method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

    if ($method === 'GET') {
        $action = trim((string)($_GET['action'] ?? 'workspace'));
        if ($action === 'compare') {
            $left = (int)($_GET['left_id'] ?? 0);
            $right = (int)($_GET['right_id'] ?? 0);
            $cmp = MelvoreDesignSystemService::compareReleases($pdo, $left, $right);
            if (empty($cmp['ok'])) {
                MelvoreResponse::error(implode(' ', $cmp['errors'] ?? ['Compare failed']), 'VALIDATION_ERROR', 400);
            }
            MelvoreResponse::ok($cmp, 'Release comparison generated');
        }
        MelvoreResponse::ok(MelvoreDesignSystemService::getAdminWorkspace($pdo), 'Design studio workspace loaded');
    }

    $body = MelvoreResponse::jsonBody();
    $action = strtolower(trim((string)($body['action'] ?? '')));

    switch ($action) {
        case 'save_draft': {
            $slot = (string)($body['slot'] ?? 'live');
            $manifest = $body['manifest'] ?? null;
            if (!is_array($manifest)) {
                MelvoreResponse::error('manifest object is required.', 'VALIDATION_ERROR', 400);
            }
            $res = MelvoreDesignSystemService::saveDraft($pdo, $slot, $manifest, $adminUserId, (string)($body['notes'] ?? ''));
            if (empty($res['ok'])) {
                MelvoreResponse::error(implode(' ', $res['errors'] ?? ['Validation failed']), 'SCHEMA_INVALID', 422, $res);
            }
            MelvoreResponse::ok($res, 'Draft manifest saved');
        }
        case 'publish': {
            $slot = (string)($body['slot'] ?? 'live');
            $manifest = isset($body['manifest']) && is_array($body['manifest']) ? $body['manifest'] : null;
            $versionTag = isset($body['version_tag']) ? (string)$body['version_tag'] : null;
            $res = MelvoreDesignSystemService::publishSlot($pdo, $slot, $manifest, $adminUserId, $versionTag, (string)($body['notes'] ?? ''));
            if (empty($res['ok'])) {
                MelvoreResponse::error(implode(' ', $res['errors'] ?? ['Publish failed']), 'SCHEMA_INVALID', 422, $res);
            }
            MelvoreResponse::ok($res, "Published {$slot} manifest");
        }
        case 'switch_active': {
            $activeDesign = (int)($body['active_design'] ?? 0);
            $res = MelvoreDesignSystemService::switchActiveDesign($pdo, $activeDesign, $adminUserId, (string)($body['notes'] ?? ''));
            if (empty($res['ok'])) {
                MelvoreResponse::error(implode(' ', $res['errors'] ?? ['Switch failed']), 'VALIDATION_ERROR', 400, $res);
            }
            MelvoreResponse::ok($res, 'Active storefront design switched');
        }
        case 'rollback': {
            $releaseId = (int)($body['release_id'] ?? 0);
            if ($releaseId > 0) {
                $res = MelvoreDesignSystemService::rollbackToRelease($pdo, $releaseId, $adminUserId, (string)($body['notes'] ?? ''));
            } else {
                // Quick rollback to Default (0)
                $res = MelvoreDesignSystemService::switchActiveDesign($pdo, 0, $adminUserId, (string)($body['notes'] ?? 'Emergency rollback to Default (0)'));
            }
            if (empty($res['ok'])) {
                MelvoreResponse::error(implode(' ', $res['errors'] ?? ['Rollback failed']), 'VALIDATION_ERROR', 400, $res);
            }
            MelvoreResponse::ok($res, 'Rollback completed');
        }
        case 'duplicate': {
            $releaseId = (int)($body['release_id'] ?? 0);
            $targetSlot = (string)($body['target_slot'] ?? 'live');
            $res = MelvoreDesignSystemService::duplicateRelease($pdo, $releaseId, $targetSlot, $adminUserId);
            if (empty($res['ok'])) {
                MelvoreResponse::error(implode(' ', $res['errors'] ?? ['Duplicate failed']), 'VALIDATION_ERROR', 400, $res);
            }
            MelvoreResponse::ok($res, 'Release duplicated into draft');
        }
        default:
            MelvoreResponse::error("Unknown design studio action '{$action}'.", 'VALIDATION_ERROR', 400);
    }
} catch (Throwable $e) {
    MelvoreResponse::error('Design Studio error: ' . $e->getMessage(), 'SERVER_ERROR', 500);
}
