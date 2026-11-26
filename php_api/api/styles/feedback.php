<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/services/RecommendationService.php';

MelvoreResponse::requireMethod('GET', 'POST');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::getCurrentUserId();
    $guestId = MelvoreAuth::getGuestId();

    if ($userId === null && $guestId === '') {
        MelvoreResponse::error('User or guest session identifier is required.', 'SESSION_REQUIRED', 400);
    }
    if (!MelvoreDB::tableExists('app_style_world_feedback') || !MelvoreDB::tableExists('app_style_worlds')) {
        MelvoreResponse::ok([
            'has_submitted' => false,
            'submitted_world_ids' => [],
            'submitted_world_slugs' => [],
            'feedback' => null,
            'coupon' => null,
        ], 'World feedback module unavailable');
    }

    $actorSql = $userId !== null ? 'user_id = ?' : 'guest_id = ?';
    $actorVal = $userId ?? $guestId;
    $method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

    $fetchCouponPayload = static function (PDO $pdo, ?int $couponId, ?string $couponCode): ?array {
        if (!MelvoreDB::tableExists('coupons')) {
            return null;
        }
        $row = null;
        if ($couponId !== null && $couponId > 0) {
            $s = $pdo->prepare('SELECT * FROM coupons WHERE coupon_id = ? LIMIT 1');
            $s->execute([$couponId]);
            $row = $s->fetch() ?: null;
        } elseif ($couponCode !== null && $couponCode !== '') {
            $s = $pdo->prepare('SELECT * FROM coupons WHERE UPPER(code) = UPPER(?) LIMIT 1');
            $s->execute([$couponCode]);
            $row = $s->fetch() ?: null;
        }
        if (!$row) {
            return null;
        }
        $type = strtolower((string)($row['discount_type'] ?? 'percentage'));
        $val = (float)($row['discount_value'] ?? 10);
        $minOrder = (float)($row['min_order_amount'] ?? 0);
        $maxDisc = !empty($row['max_discount_amount']) ? (float)$row['max_discount_amount'] : null;
        $validTo = $row['valid_to'] ?? null;

        $discSummary = $type === 'percentage'
            ? (int)round($val) . '% OFF'
            : '₹' . (int)round($val) . ' OFF';
        $condParts = [];
        if ($minOrder > 0) {
            $condParts[] = 'Min order ₹' . number_format($minOrder, 0);
        }
        if ($maxDisc !== null && $maxDisc > 0) {
            $condParts[] = 'Up to ₹' . number_format($maxDisc, 0) . ' savings';
        }
        if (!empty($validTo)) {
            $condParts[] = 'Valid until ' . date('d M Y', strtotime((string)$validTo));
        }

        return [
            'coupon_id' => (int)$row['coupon_id'],
            'code' => (string)$row['code'],
            'discount_type' => $type,
            'discount_value' => $val,
            'discount_summary' => $discSummary,
            'min_order_amount' => $minOrder,
            'max_discount_amount' => $maxDisc,
            'valid_to' => $validTo,
            'conditions_text' => implode(' · ', $condParts),
        ];
    };

    if ($method === 'GET') {
        $worldId = (int)($_GET['style_world_id'] ?? ($_GET['id'] ?? 0));
        $worldSlug = trim((string)($_GET['slug'] ?? ($_GET['world_slug'] ?? '')));

        $allStmt = $pdo->prepare(
            "SELECT style_world_id, world_slug, satisfaction_score, feedback_label, coupon_id, coupon_code, submitted_at
             FROM app_style_world_feedback WHERE $actorSql ORDER BY submitted_at DESC"
        );
        $allStmt->execute([$actorVal]);
        $allRows = $allStmt->fetchAll();

        $submittedIds = array_values(array_unique(array_map(static fn($r) => (int)$r['style_world_id'], $allRows)));
        $submittedSlugs = array_values(array_unique(array_map(static fn($r) => (string)$r['world_slug'], $allRows)));

        $matched = null;
        foreach ($allRows as $r) {
            if (($worldId > 0 && (int)$r['style_world_id'] === $worldId) || ($worldSlug !== '' && strcasecmp((string)$r['world_slug'], $worldSlug) === 0)) {
                $matched = $r;
                break;
            }
        }

        MelvoreResponse::ok([
            'has_submitted' => $matched !== null,
            'submitted_world_ids' => $submittedIds,
            'submitted_world_slugs' => $submittedSlugs,
            'feedback' => $matched ? [
                'style_world_id' => (int)$matched['style_world_id'],
                'world_slug' => (string)$matched['world_slug'],
                'satisfaction_score' => (int)$matched['satisfaction_score'],
                'feedback_label' => (string)$matched['feedback_label'],
                'submitted_at' => (string)$matched['submitted_at'],
            ] : null,
            'coupon' => $matched
                ? $fetchCouponPayload($pdo, !empty($matched['coupon_id']) ? (int)$matched['coupon_id'] : null, $matched['coupon_code'] ?? null)
                : null,
        ], 'Feedback status loaded');
    }

    // POST: One-time submission + transactional coupon unlock
    $body = MelvoreResponse::jsonBody();
    $worldId = (int)($body['style_world_id'] ?? 0);
    $worldSlug = trim((string)($body['world_slug'] ?? ($body['slug'] ?? '')));
    $score = (int)($body['satisfaction_score'] ?? 0);

    if ($score < 1 || $score > 4) {
        MelvoreResponse::error('satisfaction_score must be between 1 and 4.', 'VALIDATION_ERROR', 422);
    }
    if ($worldId <= 0 && $worldSlug === '') {
        MelvoreResponse::error('style_world_id or world_slug is required.', 'VALIDATION_ERROR', 422);
    }

    if ($worldId > 0) {
        $wStmt = $pdo->prepare('SELECT * FROM app_style_worlds WHERE style_world_id = ? AND is_active = 1 LIMIT 1');
        $wStmt->execute([$worldId]);
    } else {
        $wStmt = $pdo->prepare('SELECT * FROM app_style_worlds WHERE slug = ? AND is_active = 1 LIMIT 1');
        $wStmt->execute([$worldSlug]);
    }
    $world = $wStmt->fetch();
    if (!$world) {
        MelvoreResponse::error('Style world not found.', 'NOT_FOUND', 404);
    }
    $resolvedWorldId = (int)$world['style_world_id'];
    $resolvedSlug = (string)$world['slug'];

    $label = match ($score) {
        1 => 'Not really',
        2 => "It's okay",
        3 => 'Love it',
        4 => 'Absolutely obsessed!',
        default => 'Love it',
    };

    $pdo->beginTransaction();

    $chkStmt = $pdo->prepare(
        "SELECT * FROM app_style_world_feedback
         WHERE style_world_id = ? AND $actorSql
         LIMIT 1
         FOR UPDATE"
    );
    $chkStmt->execute([$resolvedWorldId, $actorVal]);
    $existing = $chkStmt->fetch();

    if ($existing) {
        $pdo->commit();
        $existingCoupon = $fetchCouponPayload(
            $pdo,
            !empty($existing['coupon_id']) ? (int)$existing['coupon_id'] : null,
            $existing['coupon_code'] ?? null
        );
        MelvoreResponse::ok([
            'has_submitted' => true,
            'already_submitted' => true,
            'feedback' => [
                'feedback_id' => (int)$existing['feedback_id'],
                'style_world_id' => $resolvedWorldId,
                'world_slug' => $resolvedSlug,
                'satisfaction_score' => (int)$existing['satisfaction_score'],
                'feedback_label' => (string)$existing['feedback_label'],
                'submitted_at' => (string)$existing['submitted_at'],
            ],
            'coupon' => $existingCoupon,
        ], 'Feedback already recorded for this fashion world');
    }

    // Create exclusive world satisfaction coupon in existing `coupons` table
    $couponId = null;
    $couponCode = null;
    if (MelvoreDB::tableExists('coupons')) {
        $prefix = strtoupper(substr(preg_replace('/[^A-Za-z0-9]/', '', $resolvedSlug), 0, 4));
        if ($prefix === '') {
            $prefix = 'VIBE';
        }
        $couponCode = 'MLV' . $prefix . strtoupper(bin2hex(random_bytes(3)));
        $discountVal = $score >= 3 ? 12.0 : 10.0;
        $maxDiscount = 600.0;
        $minOrder = 1499.0;

        $cols = ['code', 'discount_type', 'discount_value', 'min_order_amount', 'max_discount_amount', 'usage_limit', 'usage_count', 'per_user_limit', 'valid_from', 'valid_to', 'is_active'];
        $vals = [
            $couponCode,
            'percentage',
            $discountVal,
            $minOrder,
            $maxDiscount,
            1,
            0,
            1,
            date('Y-m-d H:i:s'),
            date('Y-m-d H:i:s', strtotime('+30 days')),
            1,
        ];
        if (MelvoreDB::columnExists('coupons', 'description')) {
            $cols[] = 'description';
            $vals[] = 'Exclusive Melvore Vibe Unlock (' . $world['name'] . ')';
        }
        $colSql = implode(', ', $cols);
        $phSql = implode(', ', array_fill(0, count($cols), '?'));
        $cIns = $pdo->prepare("INSERT INTO coupons ($colSql) VALUES ($phSql)");
        $cIns->execute($vals);
        $couponId = (int)$pdo->lastInsertId();
    }

    $ins = $pdo->prepare(
        'INSERT INTO app_style_world_feedback
            (user_id, guest_id, style_world_id, world_slug, satisfaction_score, feedback_label, coupon_id, coupon_code, submitted_at, created_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())'
    );
    $ins->execute([
        $userId,
        $userId === null ? $guestId : null,
        $resolvedWorldId,
        $resolvedSlug,
        $score,
        $label,
        $couponId,
        $couponCode,
    ]);
    $feedbackId = (int)$pdo->lastInsertId();

    if (MelvoreDB::tableExists('app_user_style_signals')) {
        $sigWeight = match ($score) {
            4 => 10.0,
            3 => 5.5,
            2 => -1.5,
            1 => -7.5,
            default => 0.0,
        };
        $pdo->prepare(
            'INSERT INTO app_user_style_signals (user_id, guest_id, style_world_id, signal_type, signal_weight, created_at)
             VALUES (?, ?, ?, "world_feedback", ?, NOW())'
        )->execute([$userId, $userId === null ? $guestId : null, $resolvedWorldId, $sigWeight]);
    }

    $pdo->commit();

    MelvoreRecommendationService::invalidateForActor($pdo, $userId, $guestId);
    $couponPayload = $fetchCouponPayload($pdo, $couponId, $couponCode);

    MelvoreResponse::ok([
        'has_submitted' => true,
        'already_submitted' => false,
        'feedback' => [
            'feedback_id' => $feedbackId,
            'style_world_id' => $resolvedWorldId,
            'world_slug' => $resolvedSlug,
            'satisfaction_score' => $score,
            'feedback_label' => $label,
            'submitted_at' => gmdate('c'),
        ],
        'coupon' => $couponPayload,
    ], 'Vibe feedback saved and reward unlocked', 201);
} catch (Throwable $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('[Melvore][styles/feedback] Exception: ' . $e->getMessage());
    MelvoreResponse::error('Unable to submit world feedback right now.', 'SERVER_ERROR', 500);
}
