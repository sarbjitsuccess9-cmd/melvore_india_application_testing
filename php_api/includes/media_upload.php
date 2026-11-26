<?php
declare(strict_types=1);

require_once __DIR__ . '/response.php';

/**
 * Validated image/audio/video upload helper using finfo MIME inspection.
 */
final class MelvoreMediaUpload
{
    private const IMAGE_MIME_MAP = [
        'image/jpeg' => 'jpg',
        'image/png' => 'png',
        'image/webp' => 'webp',
    ];

    private const AUDIO_MIME_MAP = [
        'audio/mpeg' => 'mp3',
        'audio/mp3' => 'mp3',
        'audio/mp4' => 'm4a',
        'audio/x-m4a' => 'm4a',
        'audio/wav' => 'wav',
        'audio/x-wav' => 'wav',
        'audio/ogg' => 'ogg',
    ];

    private const VIDEO_MIME_MAP = [
        'video/mp4' => 'mp4',
        'video/webm' => 'webm',
        'video/quicktime' => 'mov',
    ];

    public static function handleImageUpload(string $fieldName, string $subDir, int $maxBytes = 5_242_880): string
    {
        return self::handleTypedUpload($fieldName, $subDir, self::IMAGE_MIME_MAP, $maxBytes, 'Unsupported image format. Allowed: JPG, PNG, WEBP.');
    }

    public static function handleMediaUpload(string $fieldName, string $subDir, string $kind = 'image'): string
    {
        return match (strtolower($kind)) {
            'audio' => self::handleTypedUpload($fieldName, $subDir, self::AUDIO_MIME_MAP, 15_728_640, 'Unsupported audio format. Allowed: MP3, M4A, WAV, OGG.'),
            'video' => self::handleTypedUpload($fieldName, $subDir, self::VIDEO_MIME_MAP, 31_457_280, 'Unsupported video format. Allowed: MP4, WEBM, MOV.'),
            default => self::handleImageUpload($fieldName, $subDir),
        };
    }

    private static function handleTypedUpload(
        string $fieldName,
        string $subDir,
        array $mimeMap,
        int $maxBytes,
        string $typeErrorMsg
    ): string {
        if (!isset($_FILES[$fieldName]) || !is_array($_FILES[$fieldName])) {
            MelvoreResponse::error('No file uploaded.', 'UPLOAD_MISSING', 400);
        }
        $file = $_FILES[$fieldName];
        if (($file['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK) {
            MelvoreResponse::error('Upload failed with error code ' . (int)($file['error'] ?? -1), 'UPLOAD_ERROR', 400);
        }
        $size = (int)($file['size'] ?? 0);
        if ($size <= 0 || $size > $maxBytes) {
            $maxMb = (int)round($maxBytes / 1_048_576);
            MelvoreResponse::error("File size must be under {$maxMb} MB.", 'FILE_TOO_LARGE', 422);
        }
        $tmpPath = (string)($file['tmp_name'] ?? '');
        if ($tmpPath === '' || !is_uploaded_file($tmpPath)) {
            MelvoreResponse::error('Invalid upload payload.', 'INVALID_UPLOAD', 400);
        }

        $finfo = new finfo(FILEINFO_MIME_TYPE);
        $mime = (string)$finfo->file($tmpPath);
        if (!isset($mimeMap[$mime])) {
            MelvoreResponse::error($typeErrorMsg, 'UNSUPPORTED_MEDIA_TYPE', 415);
        }

        $ext = $mimeMap[$mime];
        $safeSub = preg_replace('/[^a-zA-Z0-9_\-]/', '', $subDir);
        $uploadRoot = MelvoreEnv::get('UPLOAD_ROOT_DIR', dirname(__DIR__, 2) . '/uploads');
        $targetDir = rtrim($uploadRoot, '/') . '/' . $safeSub;
        if (!is_dir($targetDir) && !mkdir($targetDir, 0755, true) && !is_dir($targetDir)) {
            MelvoreResponse::error('Unable to prepare upload storage.', 'STORAGE_ERROR', 500);
        }

        $filename = 'mlv_' . date('Ymd_His') . '_' . bin2hex(random_bytes(8)) . '.' . $ext;
        $destPath = $targetDir . '/' . $filename;
        if (!move_uploaded_file($tmpPath, $destPath)) {
            MelvoreResponse::error('Failed to store uploaded file.', 'STORAGE_ERROR', 500);
        }

        $publicPrefix = rtrim(MelvoreEnv::get('UPLOAD_PUBLIC_PREFIX', '/uploads'), '/');
        return $publicPrefix . '/' . $safeSub . '/' . $filename;
    }
}
