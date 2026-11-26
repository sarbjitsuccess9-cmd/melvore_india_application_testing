<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/ThemeEngineService.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();

    if (!MelvoreDB::tableExists('app_style_worlds')) {
        MelvoreResponse::ok(['style_worlds' => []], 'Style worlds unavailable');
    }

    $stmt = $pdo->query(
        'SELECT * FROM app_style_worlds
         WHERE is_active = 1
           AND (start_date IS NULL OR start_date <= NOW())
           AND (end_date IS NULL OR end_date >= NOW())
         ORDER BY display_order ASC, style_world_id ASC'
    );
    $rows = $stmt->fetchAll();

    $counts = [];
    if (MelvoreDB::tableExists('app_style_world_products') && !empty($rows)) {
        $ids = array_map(static fn($r) => (int)($r['style_world_id'] ?? $r['id']), $rows);
        $ph = implode(',', array_fill(0, count($ids), '?'));
        $cStmt = $pdo->prepare("SELECT style_world_id, COUNT(*) AS cnt FROM app_style_world_products WHERE style_world_id IN ($ph) GROUP BY style_world_id");
        $cStmt->execute($ids);
        foreach ($cStmt->fetchAll() as $cr) {
            $counts[(int)$cr['style_world_id']] = (int)$cr['cnt'];
        }
    }

    $submittedWorldIds = [];
    if (($userId !== null || $guestId !== '') && MelvoreDB::tableExists('app_style_world_feedback')) {
        $fSql = $userId !== null
            ? 'SELECT style_world_id FROM app_style_world_feedback WHERE user_id = ?'
            : 'SELECT style_world_id FROM app_style_world_feedback WHERE guest_id = ?';
        $fStmt = $pdo->prepare($fSql);
        $fStmt->execute([$userId ?? $guestId]);
        $submittedWorldIds = array_map('intval', $fStmt->fetchAll(PDO::FETCH_COLUMN));
    }

    $worlds = [];
    foreach ($rows as $row) {
        $wid = (int)($row['style_world_id'] ?? ($row['id'] ?? 0));
        $formatted = MelvoreThemeEngineService::formatWorldTheme($row);
        $formatted['product_count'] = $counts[$wid] ?? 0;
        $formatted['has_submitted_feedback'] = in_array($wid, $submittedWorldIds, true);
        $worlds[] = $formatted;
    }

    MelvoreResponse::ok(['style_worlds' => $worlds], 'Style worlds loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load style worlds.', 'SERVER_ERROR', 500);
}
