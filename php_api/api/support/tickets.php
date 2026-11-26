<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $userId = MelvoreAuth::requireUserId();
    $tickets = [];

    if (MelvoreDB::tableExists('support_tickets')) {
        $stmt = $pdo->prepare('SELECT * FROM support_tickets WHERE user_id = ? ORDER BY created_at DESC, ticket_id DESC LIMIT 30');
        $stmt->execute([$userId]);
        $rows = $stmt->fetchAll();

        $repliesByTicket = [];
        if (!empty($rows) && MelvoreDB::tableExists('ticket_replies')) {
            $tIds = array_map(static fn($r) => (int)$r['ticket_id'], $rows);
            $ph = implode(',', array_fill(0, count($tIds), '?'));
            $rStmt = $pdo->prepare(
                "SELECT r.*, u.name AS sender_name
                 FROM ticket_replies r
                 LEFT JOIN users u ON u.user_id = r.user_id
                 WHERE r.ticket_id IN ($ph)
                 ORDER BY r.created_at ASC, r.reply_id ASC"
            );
            $rStmt->execute($tIds);
            foreach ($rStmt->fetchAll() as $rep) {
                $repliesByTicket[(int)$rep['ticket_id']][] = [
                    'reply_id' => (int)$rep['reply_id'],
                    'sender_type' => (string)($rep['sender_type'] ?? 'user'),
                    'sender_name' => $rep['sender_name'] ?? null,
                    'message' => (string)($rep['message'] ?? ''),
                    'created_at' => (string)($rep['created_at'] ?? ''),
                ];
            }
        }

        foreach ($rows as $row) {
            $tid = (int)$row['ticket_id'];
            $tickets[] = [
                'ticket_id' => $tid,
                'ticket_number' => (string)$row['ticket_number'],
                'order_id' => isset($row['order_id']) && $row['order_id'] !== null ? (int)$row['order_id'] : null,
                'subject' => (string)($row['subject'] ?? ''),
                'category' => (string)($row['category'] ?? 'general'),
                'priority' => (string)($row['priority'] ?? 'medium'),
                'status' => (string)($row['status'] ?? 'open'),
                'message' => (string)($row['message'] ?? ''),
                'created_at' => (string)($row['created_at'] ?? ''),
                'replies' => $repliesByTicket[$tid] ?? [],
            ];
        }
    }

    MelvoreResponse::okFlat(['tickets' => $tickets], 'Support tickets loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load support tickets.', 'SERVER_ERROR', 500);
}
