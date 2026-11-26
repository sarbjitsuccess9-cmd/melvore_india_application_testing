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
    $reviewId = (int)($body['review_id'] ?? 0);

    if ($reviewId <= 0) {
        MelvoreResponse::error('Valid review_id is required.', 'VALIDATION_ERROR', 422);
    }

    $pdo->beginTransaction();

    $chk = $pdo->prepare('SELECT review_id, helpful_count FROM reviews WHERE review_id = ? AND is_approved = 1 FOR UPDATE');
    $chk->execute([$reviewId]);
    $rev = $chk->fetch();
    if (!$rev) {
        $pdo->rollBack();
        MelvoreResponse::error('Review not found.', 'NOT_FOUND', 404);
    }

    $voted = false;
    $count = (int)$rev['helpful_count'];

    if (MelvoreDB::tableExists('app_review_votes')) {
        $vStmt = $pdo->prepare('SELECT vote_id FROM app_review_votes WHERE review_id = ? AND user_id = ? LIMIT 1');
        $vStmt->execute([$reviewId, $userId]);
        $existingVoteId = $vStmt->fetchColumn();
        if ($existingVoteId !== false) {
            $pdo->prepare('DELETE FROM app_review_votes WHERE vote_id = ?')->execute([(int)$existingVoteId]);
            $count = max(0, $count - 1);
            $voted = false;
        } else {
            $pdo->prepare('INSERT INTO app_review_votes (review_id, user_id, created_at) VALUES (?, ?, NOW())')
                ->execute([$reviewId, $userId]);
            $count++;
            $voted = true;
        }
    } else {
        $count++;
        $voted = true;
    }

    $pdo->prepare('UPDATE reviews SET helpful_count = ? WHERE review_id = ?')->execute([$count, $reviewId]);
    $pdo->commit();

    MelvoreResponse::ok([
        'review_id' => $reviewId,
        'helpful_count' => $count,
        'has_voted_helpful' => $voted,
    ], 'Helpful vote updated');
} catch (Throwable $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    MelvoreResponse::error('Unable to update helpful vote.', 'SERVER_ERROR', 500);
}
