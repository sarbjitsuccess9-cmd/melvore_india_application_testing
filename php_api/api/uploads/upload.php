<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/media_upload.php';

MelvoreResponse::requireMethod('POST');

try {
    MelvoreAuth::requireUserId();
    $field = isset($_FILES['file']) ? 'file' : 'image';
    $folder = preg_replace('/[^a-zA-Z0-9_\-]/', '', (string)($_POST['folder'] ?? 'app'));
    if ($folder === '') {
        $folder = 'app';
    }
    $kind = preg_replace('/[^a-zA-Z]/', '', (string)($_POST['kind'] ?? 'image'));
    $relPath = MelvoreMediaUpload::handleMediaUpload($field, $folder, $kind !== '' ? $kind : 'image');
    MelvoreResponse::okFlat([
        'relative_path' => $relPath,
        'media_url' => MelvoreResponse::mediaUrl($relPath),
    ], 'File uploaded', 201);
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to process file upload.', 'UPLOAD_ERROR', 500);
}
