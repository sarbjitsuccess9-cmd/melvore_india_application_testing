<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/DesignSystemService.php';

MelvoreResponse::requireMethod('GET', 'POST', 'DELETE');

try {
    $pdo = MelvoreDB::pdo();
    $adminUserId = MelvoreAuth::requireAdminUserId();
    $method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

    if ($method === 'GET') {
        MelvoreResponse::ok(['assets' => MelvoreDesignSystemService::listDesignAssets($pdo)], 'Design assets loaded');
    }

    if ($method === 'DELETE') {
        $id = (int)($_GET['id'] ?? (MelvoreResponse::jsonBody()['asset_id'] ?? 0));
        if ($id <= 0) {
            MelvoreResponse::error('Valid asset_id required.', 'VALIDATION_ERROR', 400);
        }
        $pdo->prepare('UPDATE app_design_assets SET is_active = 0 WHERE asset_id = ?')->execute([$id]);
        MelvoreResponse::ok(['deactivated' => true], 'Design asset deactivated');
    }

    $allowedTypes = ['font' => 'fonts', 'icon' => 'icons', 'image' => 'images', 'animation' => 'animations', 'illustration' => 'illustrations', 'sound' => 'sounds'];

    // Supports either multipart upload OR JSON descriptor upload (SVG/JSON/metadata)
    if (!empty($_FILES['file'])) {
        $type = strtolower(trim((string)($_POST['asset_type'] ?? 'image')));
        if (!isset($allowedTypes[$type])) {
            MelvoreResponse::error('Invalid asset_type.', 'VALIDATION_ERROR', 400);
        }
        $file = $_FILES['file'];
        if (($file['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK) {
            MelvoreResponse::error('Upload failed.', 'UPLOAD_ERROR', 400);
        }
        $ext = strtolower(pathinfo((string)$file['name'], PATHINFO_EXTENSION));
        $allowedExt = ['svg', 'png', 'jpg', 'jpeg', 'webp', 'json', 'ttf', 'otf', 'mp3', 'wav', 'ogg'];
        if (!in_array($ext, $allowedExt, true)) {
            MelvoreResponse::error('Unsupported asset file extension.', 'VALIDATION_ERROR', 400);
        }
        $assetKey = preg_replace('/[^a-z0-9_-]/', '_', strtolower(trim((string)($_POST['asset_key'] ?? pathinfo((string)$file['name'], PATHINFO_FILENAME)))));
        $title = trim((string)($_POST['title'] ?? $assetKey));
        $folder = $allowedTypes[$type];
        $destDir = MelvoreDesignSystemService::assetsDir() . '/' . $folder;
        if (!is_dir($destDir)) {
            mkdir($destDir, 0775, true);
        }
        $filename = $assetKey . '-' . substr(bin2hex(random_bytes(4)), 0, 6) . '.' . $ext;
        $destPath = $destDir . '/' . $filename;
        if (!move_uploaded_file((string)$file['tmp_name'], $destPath)) {
            MelvoreResponse::error('Could not move uploaded asset.', 'UPLOAD_ERROR', 500);
        }
        $relPath = '/design-system/assets/' . $folder . '/' . $filename;
        $mime = mime_content_type($destPath) ?: 'application/octet-stream';
        $size = (int)(filesize($destPath) ?: 0);
    } else {
        $body = MelvoreResponse::jsonBody();
        $type = strtolower(trim((string)($body['asset_type'] ?? 'image')));
        if (!isset($allowedTypes[$type])) {
            MelvoreResponse::error('Invalid asset_type.', 'VALIDATION_ERROR', 400);
        }
        $assetKey = preg_replace('/[^a-z0-9_-]/', '_', strtolower(trim((string)($body['asset_key'] ?? ''))));
        $title = trim((string)($body['title'] ?? $assetKey));
        $relPath = trim((string)($body['relative_path'] ?? ''));
        if ($assetKey === '' || $relPath === '') {
            MelvoreResponse::error('asset_key and relative_path (or multipart file) required.', 'VALIDATION_ERROR', 400);
        }
        $mime = trim((string)($body['mime_type'] ?? 'application/octet-stream'));
        $size = (int)($body['file_size_bytes'] ?? 0);
    }

    if (MelvoreDB::tableExists('app_design_assets')) {
        $stmt = $pdo->prepare(
            'INSERT INTO app_design_assets (asset_key, asset_type, title, relative_path, mime_type, file_size_bytes, is_active, created_by)
             VALUES (?, ?, ?, ?, ?, ?, 1, ?)
             ON DUPLICATE KEY UPDATE
                asset_type = VALUES(asset_type), title = VALUES(title), relative_path = VALUES(relative_path),
                mime_type = VALUES(mime_type), file_size_bytes = VALUES(file_size_bytes), is_active = 1'
        );
        $stmt->execute([$assetKey, $type, $title, $relPath, $mime, $size, $adminUserId]);
    }

    MelvoreResponse::ok([
        'asset_key' => $assetKey,
        'asset_type' => $type,
        'relative_path' => $relPath,
        'url' => MelvoreResponse::mediaUrl($relPath),
    ], 'Design asset registered');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to process design asset: ' . $e->getMessage(), 'SERVER_ERROR', 500);
}
