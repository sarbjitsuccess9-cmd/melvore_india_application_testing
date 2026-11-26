<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';

MelvoreResponse::requireMethod('POST');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::requireUserId();
    $body = MelvoreResponse::jsonBody();

    $ticketId = (int)($body['ticket_id'] ?? 0);
    $message = trim((string)($body['message'] ?? ''));
    if ($ticketId <= 0 || $message === '') {
        MelvoreResponse::error('ticket_id and message are required.', 'VALIDATION_ERROR', 400);
    }

    $chk = $pdo->prepare('SELECT ticket_id FROM support_tickets WHERE ticket_id = ? AND user_id = ? LIMIT 1');
    $chk->execute([$ticketId, $userId]);
    if (!$chk->fetch()) {
        MelvoreResponse::error('Support ticket not found.', 'NOT_FOUND', 404);
    }

    $ins = $pdo->prepare(
        "INSERT INTO ticket_replies (ticket_id, user_id, sender_type, message, created_at)
         VALUES (?, ?, 'user', ?, NOW())"
    );
    $ins->execute([$ticketId, $userId, $message]);
    $replyId = (int)$pdo->lastInsertId();

    $pdo->prepare("UPDATE support_tickets SET status = 'open' WHERE ticket_id = ? AND status = 'closed'")
        ->execute([$ticketId]);

    MelvoreResponse::okFlat([
        'reply_id' => $replyId,
    ], 'Reply posted', 201);
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to post reply.', 'SERVER_ERROR', 500);
}
