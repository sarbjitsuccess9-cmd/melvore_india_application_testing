<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $slug = strtolower(trim((string)($_GET['slug'] ?? 'about')));
    $aboutContent = [];

    if ($slug === 'about' && MelvoreDB::tableExists('about_content')) {
        foreach ($pdo->query('SELECT * FROM about_content ORDER BY id ASC')->fetchAll() as $r) {
            $aboutContent[] = [
                'id' => (int)$r['id'],
                'section_key' => (string)($r['section_key'] ?? ''),
                'title' => (string)($r['title'] ?? ''),
                'content' => (string)($r['content'] ?? ''),
            ];
        }
    } elseif (MelvoreDB::tableExists('cms_pages')) {
        $stmt = $pdo->prepare('SELECT * FROM cms_pages WHERE slug = ? LIMIT 1');
        $stmt->execute([$slug]);
        $page = $stmt->fetch();
        if ($page) {
            $aboutContent[] = [
                'id' => (int)($page['id'] ?? 0),
                'section_key' => $slug,
                'title' => (string)($page['title'] ?? ucfirst($slug)),
                'content' => (string)($page['content'] ?? ''),
            ];
        }
    }

    MelvoreResponse::okFlat(['about_content' => $aboutContent], 'CMS page loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load CMS page.', 'SERVER_ERROR', 500);
}
