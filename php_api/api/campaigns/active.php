<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $campaigns = [];
    if (MelvoreDB::tableExists('app_campaigns')) {
        $stmt = $pdo->query(
            'SELECT * FROM app_campaigns
             WHERE is_active = 1
               AND (start_date IS NULL OR start_date <= NOW())
               AND (end_date IS NULL OR end_date >= NOW())
             ORDER BY priority DESC, campaign_id DESC'
        );
        foreach ($stmt->fetchAll() as $c) {
            $campaigns[] = [
                'campaign_id' => (int)($c['campaign_id'] ?? 0),
                'slug' => (string)($c['slug'] ?? ''),
                'title' => (string)($c['title'] ?? ''),
                'subtitle' => (string)($c['subtitle'] ?? ''),
                'festival_key' => (string)($c['festival_key'] ?? 'seasonal'),
                'animation_key' => (string)($c['animation_key'] ?? 'smooth_reveal'),
                'animation_duration_ms' => (int)($c['animation_duration_ms'] ?? 2400),
                'primary_color' => (string)($c['primary_color'] ?? '#D4AF37'),
                'accent_color' => (string)($c['accent_color'] ?? '#FFFFFF'),
                'background_color' => (string)($c['background_color'] ?? '#0B0B0E'),
                'banner_image_url' => MelvoreResponse::mediaUrl($c['banner_image_url'] ?? null),
                'mobile_banner_url' => MelvoreResponse::mediaUrl($c['mobile_banner_url'] ?? null),
                'video_url' => MelvoreResponse::mediaUrl($c['video_url'] ?? null),
                'cta_text' => (string)($c['cta_text'] ?? 'Shop Edit'),
                'cta_link' => (string)($c['cta_link'] ?? ''),
            ];
        }
    }
    MelvoreResponse::ok([
        'active_campaign' => $campaigns[0] ?? null,
        'campaigns' => $campaigns,
    ], 'Active campaigns loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load active campaigns.', 'SERVER_ERROR', 500);
}
