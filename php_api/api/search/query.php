<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/rate_limit.php';
require_once dirname(__DIR__, 2) . '/includes/product_formatter.php';

MelvoreResponse::requireMethod('GET');
MelvoreRateLimit::enforce('search_query', 90, 60);

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();

    $rawQuery = trim((string)($_GET['q'] ?? ($_GET['search'] ?? '')));
    $page = max(1, (int)($_GET['page'] ?? 1));
    $limit = max(1, min(60, (int)($_GET['limit'] ?? 24)));
    $brandId = (int)($_GET['brand_id'] ?? 0);
    $category = trim((string)($_GET['category'] ?? ''));
    $size = trim((string)($_GET['size'] ?? ''));
    $color = trim((string)($_GET['color'] ?? ''));
    $minPrice = isset($_GET['min_price']) && $_GET['min_price'] !== '' ? (float)$_GET['min_price'] : null;
    $maxPrice = isset($_GET['max_price']) && $_GET['max_price'] !== '' ? (float)$_GET['max_price'] : null;
    $sort = trim((string)($_GET['sort'] ?? 'relevance'));

    // Normalize and tokenize multi-word queries
    $normalized = strtolower((string)preg_replace('/[^a-zA-Z0-9\s\-]/', ' ', $rawQuery));
    $tokens = array_values(array_unique(array_filter(
        preg_split('/[\s\-]+/', $normalized) ?: [],
        static fn($t) => strlen($t) >= 2 && !in_array($t, ['the', 'and', 'for', 'with', 'in', 'of'], true)
    )));

    // Expand synonyms via app_search_synonyms
    $synonymGroups = [];
    if (!empty($tokens) && MelvoreDB::tableExists('app_search_synonyms')) {
        $synRows = $pdo->query('SELECT term, synonyms FROM app_search_synonyms WHERE is_active = 1')->fetchAll();
        foreach ($tokens as $tok) {
            $group = [$tok];
            foreach ($synRows as $sr) {
                $base = strtolower(trim((string)$sr['term']));
                $syns = array_filter(array_map(static fn($x) => strtolower(trim($x)), explode(',', (string)$sr['synonyms'])));
                $allTerms = array_merge([$base], $syns);
                if (in_array($tok, $allTerms, true)) {
                    $group = array_values(array_unique(array_merge($group, $allTerms)));
                }
            }
            $synonymGroups[$tok] = $group;
        }
    } else {
        foreach ($tokens as $tok) {
            $synonymGroups[$tok] = [$tok];
        }
    }

    // Filter base catalog on SQL level before precision ranking
    $where = ['p.is_active = 1'];
    $params = [];
    if ($brandId > 0) {
        $where[] = 'p.brand_id = ?';
        $params[] = $brandId;
    }
    if ($category !== '' && $category !== 'all') {
        $where[] = '(c.slug = ? OR c.name = ?)';
        $params[] = $category;
        $params[] = $category;
    }
    if ($minPrice !== null) {
        $where[] = 'COALESCE(NULLIF(p.sale_price, 0), p.regular_price) >= ?';
        $params[] = $minPrice;
    }
    if ($maxPrice !== null) {
        $where[] = 'COALESCE(NULLIF(p.sale_price, 0), p.regular_price) <= ?';
        $params[] = $maxPrice;
    }
    if ($size !== '') {
        $where[] = 'p.available_sizes LIKE ?';
        $params[] = '%"' . $size . '"%';
    }
    if ($color !== '') {
        $where[] = 'p.available_colors LIKE ?';
        $params[] = '%' . $color . '%';
    }

    $hasTags = MelvoreDB::tableExists('app_tags') && MelvoreDB::tableExists('app_product_tag_map');
    $tagJoin = $hasTags
        ? 'LEFT JOIN app_product_tag_map ptm ON ptm.product_id = p.product_id
           LEFT JOIN app_tags t ON t.tag_id = ptm.tag_id'
        : '';
    $tagSelect = $hasTags ? ", GROUP_CONCAT(DISTINCT t.name SEPARATOR ' ') AS tag_names" : ", '' AS tag_names";

    $sql = "SELECT p.*, c.name AS category_name, c.slug AS category_slug,
                   b.name AS brand_name, b.slug AS brand_slug $tagSelect
            FROM products p
            LEFT JOIN categories c ON c.category_id = p.category_id
            LEFT JOIN brands b ON b.brand_id = p.brand_id
            $tagJoin
            WHERE " . implode(' AND ', $where) . "
            GROUP BY p.product_id
            LIMIT 400";

    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $candidates = $stmt->fetchAll();

    // Score candidates with exact phrase, token/synonym coverage, tag match, and Levenshtein typo tolerance
    $scored = [];
    foreach ($candidates as $row) {
        if (empty($tokens)) {
            $row['_score'] = ((int)$row['is_featured'] * 10) + (float)$row['rating_average'];
            $scored[] = $row;
            continue;
        }

        $hayName = strtolower((string)$row['name']);
        $hayBrand = strtolower((string)($row['brand_name'] ?? ''));
        $hayCat = strtolower((string)($row['category_name'] ?? ''));
        $hayTags = strtolower((string)($row['tag_names'] ?? ''));
        $hayDesc = strtolower((string)($row['description'] ?? '') . ' ' . ($row['short_description'] ?? '') . ' ' . ($row['available_colors'] ?? '') . ' ' . ($row['available_sizes'] ?? ''));
        $allWords = array_values(array_unique(array_filter(preg_split('/[^a-z0-9]+/', "$hayName $hayBrand $hayCat $hayTags") ?: [])));

        $score = 0.0;
        $matchedTokens = 0;

        if ($normalized !== '' && str_contains($hayName, $normalized)) {
            $score += 60.0;
        }

        foreach ($synonymGroups as $tok => $variants) {
            $tokenHit = false;
            foreach ($variants as $v) {
                $weight = ($v === $tok) ? 1.0 : 0.78;
                if (str_contains($hayName, $v)) {
                    $score += 24.0 * $weight;
                    $tokenHit = true;
                } elseif (str_contains($hayTags, $v)) {
                    $score += 20.0 * $weight;
                    $tokenHit = true;
                } elseif (str_contains($hayBrand, $v) || str_contains($hayCat, $v)) {
                    $score += 18.0 * $weight;
                    $tokenHit = true;
                } elseif (str_contains($hayDesc, $v)) {
                    $score += 10.0 * $weight;
                    $tokenHit = true;
                }
            }

            // Typo-tolerant Levenshtein match for tokens >= 4 chars
            if (!$tokenHit && strlen($tok) >= 4) {
                foreach ($allWords as $w) {
                    if (strlen($w) >= 4 && abs(strlen($w) - strlen($tok)) <= 2 && levenshtein($tok, $w) <= 1) {
                        $score += 11.0;
                        $tokenHit = true;
                        break;
                    }
                }
            }

            if ($tokenHit) {
                $matchedTokens++;
            }
        }

        if ($matchedTokens > 0) {
            $coverageBonus = ($matchedTokens / count($tokens)) * 35.0;
            $popularityBonus = min(15.0, ((float)$row['rating_average'] * 2.0) + log10(max(1, (int)$row['purchase_count'] + 1)));
            $row['_score'] = $score + $coverageBonus + $popularityBonus;
            $row['_matched_tokens'] = $matchedTokens;
            $scored[] = $row;
        }
    }

    // Apply requested sort
    usort($scored, static function (array $a, array $b) use ($sort): int {
        return match ($sort) {
            'price_asc' => (float)($a['sale_price'] ?: $a['regular_price']) <=> (float)($b['sale_price'] ?: $b['regular_price']),
            'price_desc' => (float)($b['sale_price'] ?: $b['regular_price']) <=> (float)($a['sale_price'] ?: $a['regular_price']),
            'rating' => (float)$b['rating_average'] <=> (float)$a['rating_average'],
            'newest' => (int)$b['product_id'] <=> (int)$a['product_id'],
            default => (($b['_matched_tokens'] ?? 0) <=> ($a['_matched_tokens'] ?? 0))
                ?: (($b['_score'] ?? 0) <=> ($a['_score'] ?? 0)),
        };
    });

    $total = count($scored);
    $offset = ($page - 1) * $limit;
    $slice = array_slice($scored, $offset, $limit);
    $formatted = MelvoreProductFormatter::formatRows($slice);

    // Log search query to app_search_logs
    if ($rawQuery !== '' && MelvoreDB::tableExists('app_search_logs')) {
        $lStmt = $pdo->prepare(
            'INSERT INTO app_search_logs (user_id, guest_id, query_text, normalized_query, results_count, created_at)
             VALUES (?, ?, ?, ?, ?, NOW())'
        );
        $lStmt->execute([$userId, $guestId !== '' ? $guestId : null, substr($rawQuery, 0, 160), substr($normalized, 0, 160), $total]);
    }

    MelvoreResponse::ok([
        'query' => $rawQuery,
        'products' => $formatted,
        'pagination' => [
            'page' => $page,
            'limit' => $limit,
            'total' => $total,
            'total_pages' => max(1, (int)ceil($total / $limit)),
        ],
    ], 'Search results loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Search request failed.', 'SERVER_ERROR', 500);
}
