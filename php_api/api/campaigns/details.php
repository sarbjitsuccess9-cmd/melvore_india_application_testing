<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $cid = (int)($_GET['id'] ?? 0);
    $slug = trim((string)($_GET['slug'] ?? ''));
    if ($cid <= 0 && $slug === '') {
        MelvoreResponse::error('Campaign id or slug is required.', 'VALIDATION_ERROR', 400);
    }

    if (!MelvoreDB::tableExists('app_campaigns')) {
        MelvoreResponse::error('Campaign not found.', 'NOT_FOUND', 404);
    }

    $stmt = $cid > 0
        ? $pdo->prepare('SELECT * FROM app_campaigns WHERE campaign_id = ? LIMIT 1')
        : $pdo->prepare('SELECT * FROM app_campaigns WHERE slug = ? LIMIT 1');
    $stmt->execute([$cid > 0 ? $cid : $slug]);
    $c = $stmt->fetch();
    if (!$c) {
        MelvoreResponse::error('Campaign not found.', 'NOT_FOUND', 404);
    }

    $id = (int)$c['campaign_id'];
    $assets = [];
    if (MelvoreDB::tableExists('app_campaign_assets')) {
        $aStmt = $pdo->prepare('SELECT * FROM app_campaign_assets WHERE campaign_id = ? ORDER BY display_order ASC');
        $aStmt->execute([$id]);
        foreach ($aStmt->fetchAll() as $ast) {
            $assets[] = [
                'asset_id' => (int)($ast['asset_id'] ?? 0),
                'asset_type' => (string)($ast['asset_type'] ?? 'image'),
                'media_url' => MelvoreResponse::mediaUrl($ast['media_url'] ?? null),
                'mobile_media_url' => MelvoreResponse::mediaUrl($ast['mobile_media_url'] ?? null),
                'animation_key' => (string)($ast['animation_key'] ?? 'none'),
                'title' => (string)($ast['title'] ?? ''),
                'subtitle' => (string)($ast['subtitle'] ?? ''),
            ];
        }
    }

    MelvoreResponse::ok([
        'campaign' => [
            'campaign_id' => $id,
            'slug' => (string)$c['slug'],
            'title' => (string)$c['title'],
            'subtitle' => (string)($c['subtitle'] ?? ''),
            'festival_key' => (string)($c['festival_key'] ?? 'seasonal'),
            'animation_key' => (string)($c['animation_key'] ?? 'smooth_reveal'),
            'animation_duration_ms' => (int)($c['animation_duration_ms'] ?? 2400),
            'primary_color' => (string)$c['primary_color'],
            'accent_color' => (string)$c['accent_color'],
            'background_color' => (string)$c['background_color'],
            'banner_image_url' => MelvoreResponse::mediaUrl($c['banner_image_url'] ?? null),
            'mobile_banner_url' => MelvoreResponse::mediaUrl($c['mobile_banner_url'] ?? null),
            'video_url' => MelvoreResponse::mediaUrl($c['video_url'] ?? null),
            'cta_text' => (string)($c['cta_text'] ?? 'Shop Edit'),
            'cta_link' => (string)($c['cta_link'] ?? ''),
            'assets' => $assets,
        ],
    ], 'Campaign details loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load campaign details.', 'SERVER_ERROR', 500);
}
