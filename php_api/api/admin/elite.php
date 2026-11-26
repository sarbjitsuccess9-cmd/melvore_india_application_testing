<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/product_formatter.php';

MelvoreResponse::requireMethod('GET', 'POST', 'PUT', 'DELETE');

try {
    $pdo = MelvoreDB::pdo();
    MelvoreAuth::requireAdminUserId();
    $method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

    if (!MelvoreDB::tableExists('app_elite_packs')) {
        MelvoreResponse::error('app_elite_packs table is missing.', 'SCHEMA_ERROR', 500);
    }

    $hasStatusCol = MelvoreDB::columnExists('app_elite_packs', 'status');
    $hasLayoutCol = MelvoreDB::columnExists('app_elite_packs', 'layout_type');
    $hasFeaturedCol = MelvoreDB::columnExists('app_elite_packs', 'featured_product_id');
    $hasSchedCol = MelvoreDB::columnExists('app_elite_packs', 'scheduled_publish_at');

    $loadPacksWithItems = static function (PDO $pdo, ?int $filterPackId = null) use ($hasStatusCol, $hasLayoutCol, $hasFeaturedCol, $hasSchedCol): array {
        $sql = 'SELECT * FROM app_elite_packs';
        $params = [];
        if ($filterPackId !== null && $filterPackId > 0) {
            $sql .= ' WHERE pack_id = ?';
            $params[] = $filterPackId;
        }
        $sql .= ' ORDER BY is_hero DESC, display_order ASC, pack_id DESC';
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $packs = $stmt->fetchAll();

        $itemsByPack = [];
        if (!empty($packs) && MelvoreDB::tableExists('app_elite_pack_items')) {
            $packIds = array_map(static fn($p) => (int)$p['pack_id'], $packs);
            $ph = implode(',', array_fill(0, count($packIds), '?'));
            $iStmt = $pdo->prepare(
                "SELECT * FROM app_elite_pack_items WHERE pack_id IN ($ph) ORDER BY display_order ASC, item_id ASC"
            );
            $iStmt->execute($packIds);
            $rawItems = $iStmt->fetchAll();

            $prodIds = array_values(array_unique(array_filter(array_map(static fn($r) => (int)($r['product_id'] ?? 0), $rawItems))));
            $productsMap = [];
            if (!empty($prodIds)) {
                $pPh = implode(',', array_fill(0, count($prodIds), '?'));
                $pStmt = $pdo->prepare(
                    "SELECT p.*, c.name AS category_name, c.slug AS category_slug, b.name AS brand_name, b.slug AS brand_slug
                     FROM products p
                     LEFT JOIN categories c ON c.category_id = p.category_id
                     LEFT JOIN brands b ON b.brand_id = p.brand_id
                     WHERE p.product_id IN ($pPh)"
                );
                $pStmt->execute($prodIds);
                foreach (MelvoreProductFormatter::formatRows($pStmt->fetchAll()) as $fp) {
                    $productsMap[(int)$fp['product_id']] = $fp;
                }
            }

            foreach ($rawItems as $it) {
                $epid = !empty($it['product_id']) ? (int)$it['product_id'] : 0;
                $prod = $epid > 0 ? ($productsMap[$epid] ?? null) : null;
                $itemType = (string)($it['item_type'] ?? 'product');
                $itemsByPack[(int)$it['pack_id']][] = [
                    'item_id' => (int)$it['item_id'],
                    'item_type' => $itemType,
                    'product_id' => $epid > 0 ? $epid : null,
                    'brand_id' => !empty($it['brand_id']) ? (int)$it['brand_id'] : null,
                    'title' => (string)(!empty($it['title']) ? $it['title'] : ($prod['name'] ?? '')),
                    'subtitle' => (string)($it['subtitle'] ?? ($prod['brand_name'] ?? '')),
                    'badge_text' => (string)($it['badge_text'] ?? ''),
                    // Strictly show product catalog primary_image for product items
                    'image_url' => $itemType === 'product'
                        ? ($prod['primary_image'] ?? null)
                        : MelvoreResponse::mediaUrl($it['image_url'] ?? null),
                    'video_url' => MelvoreResponse::mediaUrl($it['video_url'] ?? null),
                    'is_sponsored' => ((int)($it['is_sponsored'] ?? 0)) === 1,
                    'display_order' => (int)($it['display_order'] ?? 0),
                    'is_active' => ((int)($it['is_active'] ?? 1)) === 1,
                    'product' => $prod,
                ];
            }
        }

        $out = [];
        foreach ($packs as $p) {
            $pid = (int)$p['pack_id'];
            $out[] = [
                'pack_id' => $pid,
                'slug' => (string)$p['slug'],
                'section_key' => (string)($p['section_key'] ?? 'featured_luxury'),
                'brand_name' => (string)($p['brand_name'] ?? 'MELVORE LUXE'),
                'eyebrow' => (string)($p['eyebrow'] ?? ''),
                'title' => (string)($p['title'] ?? ''),
                'subtitle' => (string)($p['subtitle'] ?? ''),
                'story' => (string)($p['story'] ?? ''),
                'cta_text' => (string)($p['cta_text'] ?? 'Enter Collection'),
                'cta_link' => (string)($p['cta_link'] ?? ''),
                'hero_image_url' => MelvoreResponse::mediaUrl($p['hero_image_url'] ?? null),
                'mobile_image_url' => MelvoreResponse::mediaUrl($p['mobile_image_url'] ?? null),
                'video_url' => MelvoreResponse::mediaUrl($p['video_url'] ?? null),
                'animation_key' => (string)($p['animation_key'] ?? 'metallic_shine'),
                'accent_color' => (string)($p['accent_color'] ?? '#D4AF37'),
                'background_color' => (string)($p['background_color'] ?? '#000000'),
                'layout_type' => $hasLayoutCol ? (string)($p['layout_type'] ?? 'editorial_grid') : 'editorial_grid',
                'featured_product_id' => ($hasFeaturedCol && !empty($p['featured_product_id'])) ? (int)$p['featured_product_id'] : null,
                'status' => $hasStatusCol ? (string)($p['status'] ?? 'published') : (((int)$p['is_active']) === 1 ? 'published' : 'archived'),
                'is_hero' => ((int)($p['is_hero'] ?? 0)) === 1,
                'is_sponsored' => ((int)($p['is_sponsored'] ?? 0)) === 1,
                'is_active' => ((int)($p['is_active'] ?? 1)) === 1,
                'display_order' => (int)($p['display_order'] ?? 0),
                'scheduled_publish_at' => $hasSchedCol ? ($p['scheduled_publish_at'] ?? null) : null,
                'start_date' => $p['start_date'] ?? null,
                'end_date' => $p['end_date'] ?? null,
                'items' => $itemsByPack[$pid] ?? [],
            ];
        }
        return $out;
    };

    if ($method === 'GET') {
        $packId = isset($_GET['pack_id']) ? (int)$_GET['pack_id'] : null;
        $packs = $loadPacksWithItems($pdo, $packId);
        MelvoreResponse::ok(['packs' => $packs], 'Luxe collections loaded');
    }

    if ($method === 'DELETE') {
        $packId = (int)($_GET['pack_id'] ?? ($_GET['id'] ?? 0));
        $itemId = (int)($_GET['item_id'] ?? 0);
        if ($itemId > 0 && MelvoreDB::tableExists('app_elite_pack_items')) {
            $pdo->prepare('DELETE FROM app_elite_pack_items WHERE item_id = ?')->execute([$itemId]);
            MelvoreResponse::ok(['deleted_item_id' => $itemId], 'Luxe item removed');
        }
        if ($packId <= 0) {
            MelvoreResponse::error('Valid pack_id or item_id is required.', 'VALIDATION_ERROR', 400);
        }
        $hard = !empty($_GET['hard']);
        if ($hard) {
            if (MelvoreDB::tableExists('app_elite_pack_items')) {
                $pdo->prepare('DELETE FROM app_elite_pack_items WHERE pack_id = ?')->execute([$packId]);
            }
            $pdo->prepare('DELETE FROM app_elite_packs WHERE pack_id = ?')->execute([$packId]);
            MelvoreResponse::ok(['deleted_pack_id' => $packId], 'Luxe collection deleted');
        } else {
            $sql = $hasStatusCol
                ? "UPDATE app_elite_packs SET is_active = 0, status = 'archived' WHERE pack_id = ?"
                : 'UPDATE app_elite_packs SET is_active = 0 WHERE pack_id = ?';
            $pdo->prepare($sql)->execute([$packId]);
            MelvoreResponse::ok(['archived_pack_id' => $packId], 'Luxe collection archived');
        }
    }

    $body = MelvoreResponse::jsonBody();
    $action = trim((string)($body['action'] ?? 'save_pack'));

    if ($action === 'reorder_packs' && !empty($body['order']) && is_array($body['order'])) {
        $pdo->beginTransaction();
        foreach ($body['order'] as $idx => $pid) {
            $pdo->prepare('UPDATE app_elite_packs SET display_order = ? WHERE pack_id = ?')
                ->execute([(int)$idx, (int)$pid]);
        }
        $pdo->commit();
        MelvoreResponse::ok(['reordered' => true], 'Luxe collections reordered');
    }

    if ($action === 'duplicate_pack' && !empty($body['pack_id'])) {
        $srcId = (int)$body['pack_id'];
        $sStmt = $pdo->prepare('SELECT * FROM app_elite_packs WHERE pack_id = ? LIMIT 1');
        $sStmt->execute([$srcId]);
        $src = $sStmt->fetch();
        if (!$src) {
            MelvoreResponse::error('Source collection not found.', 'NOT_FOUND', 404);
        }
        $newSlug = (string)$src['slug'] . '-copy-' . substr(bin2hex(random_bytes(3)), 0, 4);
        $pdo->beginTransaction();
        $pdo->prepare(
            'INSERT INTO app_elite_packs
                (slug, section_key, brand_name, eyebrow, title, subtitle, story, cta_text, cta_link,
                 hero_image_url, mobile_image_url, video_url, animation_key, accent_color, background_color,
                 is_hero, is_sponsored, display_order, is_active)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)'
        )->execute([
            $newSlug,
            $src['section_key'],
            $src['brand_name'],
            $src['eyebrow'],
            $src['title'] . ' (Copy)',
            $src['subtitle'],
            $src['story'],
            $src['cta_text'],
            $src['cta_link'],
            $src['hero_image_url'],
            $src['mobile_image_url'],
            $src['video_url'],
            $src['animation_key'],
            $src['accent_color'],
            $src['background_color'],
            (int)$src['is_hero'],
            (int)$src['is_sponsored'],
            ((int)$src['display_order']) + 1,
        ]);
        $newPackId = (int)$pdo->lastInsertId();
        if ($hasStatusCol) {
            $pdo->prepare("UPDATE app_elite_packs SET status = 'draft' WHERE pack_id = ?")->execute([$newPackId]);
        }
        if (MelvoreDB::tableExists('app_elite_pack_items')) {
            $itStmt = $pdo->prepare('SELECT * FROM app_elite_pack_items WHERE pack_id = ? ORDER BY display_order ASC');
            $itStmt->execute([$srcId]);
            foreach ($itStmt->fetchAll() as $it) {
                $pdo->prepare(
                    'INSERT INTO app_elite_pack_items (pack_id, item_type, product_id, brand_id, title, subtitle, badge_text, image_url, video_url, is_sponsored, display_order, is_active)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
                )->execute([
                    $newPackId, $it['item_type'], $it['product_id'], $it['brand_id'], $it['title'],
                    $it['subtitle'], $it['badge_text'], $it['image_url'], $it['video_url'],
                    (int)$it['is_sponsored'], (int)$it['display_order'], (int)$it['is_active'],
                ]);
            }
        }
        $pdo->commit();
        MelvoreResponse::ok(['pack' => $loadPacksWithItems($pdo, $newPackId)[0] ?? null], 'Collection duplicated as draft', 201);
    }

    // Create / Update Pack + Sync Curated Items
    $packId = !empty($body['pack_id']) ? (int)$body['pack_id'] : 0;
    $slug = strtolower(trim((string)($body['slug'] ?? '')));
    $title = trim((string)($body['title'] ?? ''));
    if ($slug === '' || $title === '') {
        MelvoreResponse::error('slug and title are required.', 'VALIDATION_ERROR', 422);
    }

    $status = strtolower(trim((string)($body['status'] ?? 'published')));
    if (!in_array($status, ['draft', 'published', 'archived'], true)) {
        $status = 'published';
    }
    $isActive = array_key_exists('is_active', $body)
        ? (!empty($body['is_active']) ? 1 : 0)
        : ($status === 'published' ? 1 : 0);

    $pdo->beginTransaction();

    if ($packId > 0) {
        $upd = $pdo->prepare(
            'UPDATE app_elite_packs SET
                slug = ?, section_key = ?, brand_name = ?, eyebrow = ?, title = ?, subtitle = ?, story = ?,
                cta_text = ?, cta_link = ?, hero_image_url = ?, mobile_image_url = ?, video_url = ?,
                animation_key = ?, accent_color = ?, background_color = ?, is_hero = ?, is_sponsored = ?,
                display_order = ?, is_active = ?, start_date = ?, end_date = ?
             WHERE pack_id = ?'
        );
        $upd->execute([
            $slug,
            (string)($body['section_key'] ?? 'featured_luxury'),
            (string)($body['brand_name'] ?? 'MELVORE LUXE'),
            (string)($body['eyebrow'] ?? ''),
            $title,
            (string)($body['subtitle'] ?? ''),
            (string)($body['story'] ?? ''),
            (string)($body['cta_text'] ?? 'Enter Collection'),
            (string)($body['cta_link'] ?? ''),
            !empty($body['hero_image_url']) ? trim((string)$body['hero_image_url']) : null,
            !empty($body['mobile_image_url']) ? trim((string)$body['mobile_image_url']) : null,
            !empty($body['video_url']) ? trim((string)$body['video_url']) : null,
            (string)($body['animation_key'] ?? 'metallic_shine'),
            (string)($body['accent_color'] ?? '#D4AF37'),
            (string)($body['background_color'] ?? '#000000'),
            !empty($body['is_hero']) ? 1 : 0,
            !empty($body['is_sponsored']) ? 1 : 0,
            (int)($body['display_order'] ?? 0),
            $isActive,
            !empty($body['start_date']) ? trim((string)$body['start_date']) : null,
            !empty($body['end_date']) ? trim((string)$body['end_date']) : null,
            $packId,
        ]);
    } else {
        $ins = $pdo->prepare(
            'INSERT INTO app_elite_packs
                (slug, section_key, brand_name, eyebrow, title, subtitle, story, cta_text, cta_link,
                 hero_image_url, mobile_image_url, video_url, animation_key, accent_color, background_color,
                 is_hero, is_sponsored, display_order, is_active, start_date, end_date)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
             ON DUPLICATE KEY UPDATE
                title = VALUES(title), subtitle = VALUES(subtitle), story = VALUES(story),
                hero_image_url = VALUES(hero_image_url), mobile_image_url = VALUES(mobile_image_url),
                accent_color = VALUES(accent_color), background_color = VALUES(background_color),
                is_hero = VALUES(is_hero), is_sponsored = VALUES(is_sponsored),
                display_order = VALUES(display_order), is_active = VALUES(is_active)'
        );
        $ins->execute([
            $slug,
            (string)($body['section_key'] ?? 'featured_luxury'),
            (string)($body['brand_name'] ?? 'MELVORE LUXE'),
            (string)($body['eyebrow'] ?? ''),
            $title,
            (string)($body['subtitle'] ?? ''),
            (string)($body['story'] ?? ''),
            (string)($body['cta_text'] ?? 'Enter Collection'),
            (string)($body['cta_link'] ?? ''),
            !empty($body['hero_image_url']) ? trim((string)$body['hero_image_url']) : null,
            !empty($body['mobile_image_url']) ? trim((string)$body['mobile_image_url']) : null,
            !empty($body['video_url']) ? trim((string)$body['video_url']) : null,
            (string)($body['animation_key'] ?? 'metallic_shine'),
            (string)($body['accent_color'] ?? '#D4AF37'),
            (string)($body['background_color'] ?? '#000000'),
            !empty($body['is_hero']) ? 1 : 0,
            !empty($body['is_sponsored']) ? 1 : 0,
            (int)($body['display_order'] ?? 0),
            $isActive,
            !empty($body['start_date']) ? trim((string)$body['start_date']) : null,
            !empty($body['end_date']) ? trim((string)$body['end_date']) : null,
        ]);
        $packId = (int)$pdo->lastInsertId();
        if ($packId <= 0) {
            $idStmt = $pdo->prepare('SELECT pack_id FROM app_elite_packs WHERE slug = ? LIMIT 1');
            $idStmt->execute([$slug]);
            $packId = (int)$idStmt->fetchColumn();
        }
    }

    if ($packId > 0 && ($hasStatusCol || $hasLayoutCol || $hasFeaturedCol || $hasSchedCol)) {
        $sets = [];
        $vals = [];
        if ($hasStatusCol) {
            $sets[] = 'status = ?';
            $vals[] = $status;
        }
        if ($hasLayoutCol) {
            $sets[] = 'layout_type = ?';
            $vals[] = (string)($body['layout_type'] ?? 'editorial_grid');
        }
        if ($hasFeaturedCol) {
            $sets[] = 'featured_product_id = ?';
            $vals[] = !empty($body['featured_product_id']) ? (int)$body['featured_product_id'] : null;
        }
        if ($hasSchedCol) {
            $sets[] = 'scheduled_publish_at = ?';
            $vals[] = !empty($body['scheduled_publish_at']) ? trim((string)$body['scheduled_publish_at']) : null;
        }
        if (!empty($sets)) {
            $vals[] = $packId;
            $pdo->prepare('UPDATE app_elite_packs SET ' . implode(', ', $sets) . ' WHERE pack_id = ?')->execute($vals);
        }
    }

    // Sync items with strict deduplication & active-product validation
    if (isset($body['items']) && is_array($body['items']) && MelvoreDB::tableExists('app_elite_pack_items')) {
        $pdo->prepare('DELETE FROM app_elite_pack_items WHERE pack_id = ?')->execute([$packId]);
        $seenProdIds = [];
        $orderIdx = 0;

        foreach ($body['items'] as $it) {
            if (!is_array($it)) {
                continue;
            }
            $itemType = (string)($it['item_type'] ?? 'product');
            $epid = !empty($it['product_id']) ? (int)$it['product_id'] : null;
            $ebid = !empty($it['brand_id']) ? (int)$it['brand_id'] : null;

            if ($itemType === 'product') {
                if ($epid === null || $epid <= 0) {
                    continue;
                }
                if (isset($seenProdIds[$epid])) {
                    continue; // prevent duplicate product inside same Luxe collection
                }
                $pChk = $pdo->prepare('SELECT name, is_active FROM products WHERE product_id = ? LIMIT 1');
                $pChk->execute([$epid]);
                $pRow = $pChk->fetch();
                if (!$pRow || ((int)$pRow['is_active']) !== 1) {
                    continue; // reject inactive or non-existent catalog products
                }
                $seenProdIds[$epid] = true;
                // Strictly leave image_url NULL for product items so runtime displays product's own catalog img_src
                $itemImg = null;
                $itemTitle = !empty($it['title']) ? trim((string)$it['title']) : (string)$pRow['name'];
            } else {
                $itemImg = !empty($it['image_url']) ? trim((string)$it['image_url']) : null;
                $itemTitle = trim((string)($it['title'] ?? ''));
            }

            $pdo->prepare(
                'INSERT INTO app_elite_pack_items
                    (pack_id, item_type, product_id, brand_id, title, subtitle, badge_text, image_url, video_url, is_sponsored, display_order, is_active)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
            )->execute([
                $packId,
                $itemType,
                $epid,
                $ebid,
                $itemTitle,
                (string)($it['subtitle'] ?? ''),
                (string)($it['badge_text'] ?? ''),
                $itemImg,
                !empty($it['video_url']) ? trim((string)$it['video_url']) : null,
                !empty($it['is_sponsored']) ? 1 : 0,
                $orderIdx++,
                array_key_exists('is_active', $it) ? (!empty($it['is_active']) ? 1 : 0) : 1,
            ]);
        }
    }

    $pdo->commit();
    $saved = $loadPacksWithItems($pdo, $packId)[0] ?? null;
    MelvoreResponse::ok(['pack' => $saved], 'Luxe collection saved');
} catch (Throwable $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('[Melvore][admin/elite] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to manage Luxe collection.', 'SERVER_ERROR', 500);
}
