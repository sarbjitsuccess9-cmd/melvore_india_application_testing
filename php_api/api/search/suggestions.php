<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/rate_limit.php';

MelvoreResponse::requireMethod('GET');
MelvoreRateLimit::enforce('search_suggest', 120, 60);

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();
    $prefix = trim((string)($_GET['q'] ?? ''));

    $suggestions = [];
    if ($prefix !== '') {
        $like = '%' . $prefix . '%';
        $pStmt = $pdo->prepare("SELECT name FROM products WHERE is_active = 1 AND name LIKE ? ORDER BY purchase_count DESC LIMIT 6");
        $pStmt->execute([$like]);
        foreach ($pStmt->fetchAll() as $r) {
            $suggestions[] = ['text' => (string)$r['name'], 'type' => 'product'];
        }
        $bStmt = $pdo->prepare("SELECT name FROM brands WHERE status = 'active' AND name LIKE ? LIMIT 4");
        $bStmt->execute([$like]);
        foreach ($bStmt->fetchAll() as $r) {
            $suggestions[] = ['text' => (string)$r['name'], 'type' => 'brand'];
        }
        if (MelvoreDB::tableExists('app_tags')) {
            $tStmt = $pdo->prepare("SELECT name FROM app_tags WHERE name LIKE ? LIMIT 4");
            $tStmt->execute([$like]);
            foreach ($tStmt->fetchAll() as $r) {
                $suggestions[] = ['text' => (string)$r['name'], 'type' => 'style'];
            }
        }
    }

    $recentSearches = [];
    $trendingSearches = [];
    if (MelvoreDB::tableExists('app_search_logs')) {
        if ($userId !== null) {
            $rStmt = $pdo->prepare(
                "SELECT query_text, MAX(created_at) AS last_at FROM app_search_logs
                 WHERE user_id = ? AND query_text <> '' GROUP BY query_text ORDER BY last_at DESC LIMIT 8"
            );
            $rStmt->execute([$userId]);
            $recentSearches = array_map(static fn($r) => (string)$r['query_text'], $rStmt->fetchAll());
        } elseif ($guestId !== '') {
            $rStmt = $pdo->prepare(
                "SELECT query_text, MAX(created_at) AS last_at FROM app_search_logs
                 WHERE guest_id = ? AND query_text <> '' GROUP BY query_text ORDER BY last_at DESC LIMIT 8"
            );
            $rStmt->execute([$guestId]);
            $recentSearches = array_map(static fn($r) => (string)$r['query_text'], $rStmt->fetchAll());
        }

        $trStmt = $pdo->query(
            "SELECT query_text, COUNT(*) AS hits FROM app_search_logs
             WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) AND results_count > 0 AND query_text <> ''
             GROUP BY query_text ORDER BY hits DESC LIMIT 10"
        );
        $trendingSearches = array_map(static fn($r) => (string)$r['query_text'], $trStmt->fetchAll());
    }

    if (empty($trendingSearches) && MelvoreDB::tableExists('trending_tags')) {
        $ttStmt = $pdo->query('SELECT name FROM trending_tags ORDER BY tag_id ASC LIMIT 10');
        $trendingSearches = array_map(static fn($r) => (string)$r['name'], $ttStmt->fetchAll());
    }

    MelvoreResponse::ok([
        'suggestions' => $suggestions,
        'recent_searches' => $recentSearches,
        'trending_searches' => $trendingSearches,
    ], 'Search suggestions loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load search suggestions.', 'SERVER_ERROR', 500);
}
