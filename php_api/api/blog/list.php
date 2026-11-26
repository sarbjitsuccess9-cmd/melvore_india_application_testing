<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $posts = [];
    if (MelvoreDB::tableExists('blog_posts')) {
        $hasCats = MelvoreDB::tableExists('blog_categories');
        $sql = $hasCats
            ? "SELECT bp.*, COALESCE(bc.name, 'Journal') AS category_name
               FROM blog_posts bp
               LEFT JOIN blog_categories bc ON bc.id = bp.category_id
               WHERE bp.status = 'published'
               ORDER BY bp.published_at DESC, bp.id DESC
               LIMIT 30"
            : "SELECT bp.*, 'Journal' AS category_name
               FROM blog_posts bp
               WHERE bp.status = 'published'
               ORDER BY bp.published_at DESC, bp.id DESC
               LIMIT 30";
        foreach ($pdo->query($sql)->fetchAll() as $r) {
            $posts[] = [
                'id' => (int)$r['id'],
                'title' => (string)$r['title'],
                'slug' => (string)$r['slug'],
                'excerpt' => (string)($r['excerpt'] ?? ''),
                'content' => (string)($r['content'] ?? ''),
                'featured_image' => MelvoreResponse::mediaUrl($r['featured_image'] ?? null),
                'category_name' => (string)($r['category_name'] ?? 'Journal'),
                'view_count' => (int)($r['view_count'] ?? 0),
                'created_at' => (string)($r['created_at'] ?? ($r['published_at'] ?? '')),
            ];
        }
    }
    MelvoreResponse::okFlat(['posts' => $posts], 'Journal posts loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load blog posts.', 'SERVER_ERROR', 500);
}
