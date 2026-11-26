<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';
require_once dirname(__DIR__, 2) . '/includes/rate_limit.php';

MelvoreResponse::requireMethod('POST');
MelvoreRateLimit::enforce('support_create', 10, 600);

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::requireUserId();
    $body = MelvoreResponse::jsonBody();

    $subject = trim((string)($body['subject'] ?? ''));
    $message = trim((string)($body['message'] ?? ''));
    $category = strtolower(trim((string)($body['category'] ?? 'general')));
    $priority = strtolower(trim((string)($body['priority'] ?? 'medium')));
    $orderId = isset($body['order_id']) && (int)$body['order_id'] > 0 ? (int)$body['order_id'] : null;

    if ($subject === '' || $message === '') {
        MelvoreResponse::error('Subject and message are required.', 'VALIDATION_ERROR', 400);
    }
    if (!in_array($priority, ['low', 'medium', 'high', 'urgent'], true)) {
        $priority = 'medium';
    }

    $ticketNumber = 'TKT' . date('Ymd') . strtoupper(bin2hex(random_bytes(3)));
    $ins = $pdo->prepare(
        "INSERT INTO support_tickets (user_id, order_id, ticket_number, subject, category, priority, status, message, created_at)
         VALUES (?, ?, ?, ?, ?, ?, 'open', ?, NOW())"
    );
    $ins->execute([$userId, $orderId, $ticketNumber, $subject, $category, $priority, $message]);
    $tid = (int)$pdo->lastInsertId();

    MelvoreResponse::okFlat([
        'ticket' => [
            'ticket_id' => $tid,
            'ticket_number' => $ticketNumber,
            'order_id' => $orderId,
            'subject' => $subject,
            'category' => $category,
            'priority' => $priority,
            'status' => 'open',
            'message' => $message,
            'created_at' => date('Y-m-d H:i:s'),
            'replies' => [],
        ],
    ], 'Support ticket opened', 201);
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to create support ticket.', 'SERVER_ERROR', 500);
}
