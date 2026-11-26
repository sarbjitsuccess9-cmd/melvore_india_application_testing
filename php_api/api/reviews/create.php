<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';

MelvoreResponse::requireMethod('POST');
$userId = MelvoreAuth::requireUserId();

try {
    $pdo = MelvoreDB::pdo();
    $body = MelvoreResponse::jsonBody();

    $orderItemId = (int)($body['order_item_id'] ?? 0);
    $otpRequestId = (int)($body['otp_request_id'] ?? 0);
    $rating = (int)($body['rating'] ?? 0);
    $title = trim((string)($body['title'] ?? ''));
    $comment = trim((string)($body['comment'] ?? ''));
    $mediaUrls = is_array($body['media_urls'] ?? null) ? $body['media_urls'] : [];

    if ($orderItemId <= 0 || $rating < 1 || $rating > 5 || strlen($comment) < 5) {
        MelvoreResponse::error('Valid purchased order_item_id, rating (1-5), and comment (min 5 chars) are required.', 'VALIDATION_ERROR', 422);
    }

    $pdo->beginTransaction();

    // Server derives product_id strictly from verified order item owned by authenticated user
    $oStmt = $pdo->prepare(
        "SELECT oi.order_item_id, oi.product_id, o.order_id
         FROM order_items oi
         INNER JOIN orders o ON o.order_id = oi.order_id
         WHERE oi.order_item_id = ? AND o.user_id = ? AND o.order_status <> 'cancelled'
         LIMIT 1"
    );
    $oStmt->execute([$orderItemId, $userId]);
    $item = $oStmt->fetch();
    if (!$item) {
        $pdo->rollBack();
        MelvoreResponse::error('Purchased order item does not belong to your account.', 'FORBIDDEN', 403);
    }
    $productId = (int)$item['product_id'];

    $dupStmt = $pdo->prepare('SELECT review_id FROM reviews WHERE order_item_id = ? AND user_id = ? LIMIT 1');
    $dupStmt->execute([$orderItemId, $userId]);
    if ($dupStmt->fetch()) {
        $pdo->rollBack();
        MelvoreResponse::error('A review has already been submitted for this purchase.', 'DUPLICATE_REVIEW', 409);
    }

    // Verify OTP approval if app_review_otp_requests is active
    if (MelvoreDB::tableExists('app_review_otp_requests')) {
        if ($otpRequestId <= 0) {
            $pdo->rollBack();
            MelvoreResponse::error('Verified OTP request is required before publishing your review.', 'OTP_REQUIRED', 422);
        }
        $vStmt = $pdo->prepare(
            'SELECT otp_request_id FROM app_review_otp_requests
             WHERE otp_request_id = ? AND user_id = ? AND order_item_id = ? AND is_verified = 1
               AND expires_at >= DATE_SUB(NOW(), INTERVAL 30 MINUTE)
             LIMIT 1'
        );
        $vStmt->execute([$otpRequestId, $userId, $orderItemId]);
        if (!$vStmt->fetch()) {
            $pdo->rollBack();
            MelvoreResponse::error('Please complete OTP verification for this purchased item.', 'OTP_UNVERIFIED', 403);
        }
    }

    // Preserve existing moderation/approval workflow (is_approved defaults to 0 unless site_settings auto-approves)
    $autoApprove = 0;
    if (MelvoreDB::tableExists('site_settings')) {
        $sStmt = $pdo->prepare("SELECT setting_value FROM site_settings WHERE setting_key = 'reviews_auto_approve' LIMIT 1");
        $sStmt->execute();
        if ($sStmt->fetchColumn() === '1') {
            $autoApprove = 1;
        }
    }

    $rIns = $pdo->prepare(
        'INSERT INTO reviews
         (product_id, user_id, order_item_id, rating, title, comment, is_approved, is_verified_purchase, helpful_count, created_at, updated_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, 1, 0, NOW(), NOW())'
    );
    $rIns->execute([
        $productId,
        $userId,
        $orderItemId,
        $rating,
        substr($title, 0, 150),
        substr($comment, 0, 2000),
        $autoApprove,
    ]);
    $reviewId = (int)$pdo->lastInsertId();

    if (!empty($mediaUrls) && MelvoreDB::tableExists('app_review_media')) {
        $mIns = $pdo->prepare(
            'INSERT INTO app_review_media (review_id, media_url, media_type, created_at) VALUES (?, ?, "image", NOW())'
        );
        foreach (array_slice($mediaUrls, 0, 3) as $rawUrl) {
            $cleanUrl = trim((string)$rawUrl);
            if ($cleanUrl !== '') {
                $mIns->execute([$reviewId, substr($cleanUrl, 0, 500)]);
            }
        }
    }

    if ($autoApprove === 1) {
        $pdo->prepare(
            'UPDATE products
             SET rating_average = COALESCE((SELECT ROUND(AVG(rating), 2) FROM reviews WHERE product_id = ? AND is_approved = 1), 0),
                 rating_count = (SELECT COUNT(*) FROM reviews WHERE product_id = ? AND is_approved = 1)
             WHERE product_id = ?'
        )->execute([$productId, $productId, $productId]);
    }

    $pdo->commit();

    MelvoreResponse::ok([
        'review_id' => $reviewId,
        'product_id' => $productId,
        'is_approved' => $autoApprove === 1,
        'is_verified_purchase' => true,
    ], $autoApprove === 1 ? 'Review published successfully' : 'Review submitted and awaiting moderation approval', 201);
} catch (Throwable $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    MelvoreResponse::error('Unable to submit review.', 'SERVER_ERROR', 500);
}
