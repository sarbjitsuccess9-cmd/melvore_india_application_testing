<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $postId = (int)($_GET['id'] ?? 0);
    $slug = trim((string)($_GET['slug'] ?? ''));

    if ($postId <= 0 && $slug === '') {
        MelvoreResponse::error('Post id or slug is required.', 'VALIDATION_ERROR', 400);
    }

    $hasCats = MelvoreDB::tableExists('blog_categories');
    $catSelect = $hasCats ? "COALESCE(bc.name, 'Journal') AS category_name" : "'Journal' AS category_name";
    $catJoin = $hasCats ? "LEFT JOIN blog_categories bc ON bc.id = bp.category_id" : "";

    if ($postId > 0) {
        $stmt = $pdo->prepare("SELECT bp.*, $catSelect FROM blog_posts bp $catJoin WHERE bp.id = ? AND bp.status = 'published' LIMIT 1");
        $stmt->execute([$postId]);
    } else {
        $stmt = $pdo->prepare("SELECT bp.*, $catSelect FROM blog_posts bp $catJoin WHERE bp.slug = ? AND bp.status = 'published' LIMIT 1");
        $stmt->execute([$slug]);
    }
    $r = $stmt->fetch();
    if (!$r) {
        MelvoreResponse::error('Journal post not found.', 'NOT_FOUND', 404);
    }

    $pdo->prepare('UPDATE blog_posts SET view_count = view_count + 1 WHERE id = ?')->execute([(int)$r['id']]);

    MelvoreResponse::okFlat([
        'post' => [
            'id' => (int)$r['id'],
            'title' => (string)$r['title'],
            'slug' => (string)$r['slug'],
            'excerpt' => (string)($r['excerpt'] ?? ''),
            'content' => (string)($r['content'] ?? ''),
            'featured_image' => MelvoreResponse::mediaUrl($r['featured_image'] ?? null),
            'category_name' => (string)($r['category_name'] ?? 'Journal'),
            'view_count' => ((int)($r['view_count'] ?? 0)) + 1,
            'created_at' => (string)($r['created_at'] ?? ($r['published_at'] ?? '')),
        ],
    ], 'Post loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load journal post.', 'SERVER_ERROR', 500);
}
