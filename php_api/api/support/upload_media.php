<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/media_upload.php';

MelvoreResponse::requireMethod('POST');
$userId = MelvoreAuth::requireUserId();

try {
    $relativePath = MelvoreMediaUpload::handleImageUpload('image', 'support');
    MelvoreResponse::ok([
        'media_url' => MelvoreResponse::mediaUrl($relativePath),
        'relative_path' => $relativePath,
    ], 'Support photo uploaded', 201);
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to upload support evidence photo.', 'SERVER_ERROR', 500);
}
