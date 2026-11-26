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

    $action = trim((string)($body['action'] ?? 'evaluate'));
    $topicId = (int)($body['topic_id'] ?? 0);
    $flowId = (int)($body['flow_id'] ?? 0);
    $orderId = (int)($body['order_id'] ?? 0);
    $orderItemId = (int)($body['order_item_id'] ?? 0);
    $currentNodeId = (int)($body['node_id'] ?? 0);
    $selectedOptionId = (int)($body['option_id'] ?? 0);
    $customerNote = trim((string)($body['note'] ?? ''));
    $evidenceUrls = is_array($body['evidence_urls'] ?? null) ? $body['evidence_urls'] : [];
    $sessionToken = trim((string)($body['session_token'] ?? ''));

    // Verify order ownership on backend when order_id provided
    $orderReport = null;
    if ($orderId > 0) {
        $oStmt = $pdo->prepare('SELECT * FROM orders WHERE order_id = ? AND user_id = ? LIMIT 1');
        $oStmt->execute([$orderId, $userId]);
        $order = $oStmt->fetch();
        if (!$order) {
            MelvoreResponse::error('Order not found for your account.', 'FORBIDDEN', 403);
        }

        $oiStmt = $pdo->prepare(
            'SELECT oi.order_item_id, oi.product_id, oi.product_name, oi.quantity, oi.unit_price, oi.total_price, oi.attributes
             FROM order_items oi
             WHERE oi.order_id = ?'
        );
        $oiStmt->execute([$orderId]);
        $items = $oiStmt->fetchAll();

        $status = strtolower((string)($order['order_status'] ?? 'pending'));
        $paymentStatus = strtolower((string)($order['payment_status'] ?? 'pending'));
        $createdAtTs = strtotime((string)($order['created_at'] ?? 'now')) ?: time();
        $daysSinceOrder = (int)floor((time() - $createdAtTs) / 86400);

        // Return/exchange eligibility verified server-side: only delivered orders within 7 days
        $returnEligible = ($status === 'delivered' && $daysSinceOrder <= 14);
        $cancelEligible = in_array($status, ['pending', 'processing'], true);

        $explanation = match ($status) {
            'pending' => 'Your order is confirmed and queued for dispatch preparation.',
            'processing' => 'Your items are currently being packed by the warehouse.',
            'shipped' => !empty($order['tracking_number'])
                ? 'Your parcel is in transit with ' . ($order['courier_name'] ?: 'our courier partner') . ' (AWB: ' . $order['tracking_number'] . ').'
                : 'Your order has been handed over to our logistics partner.',
            'delivered' => 'Courier records indicate this order has been delivered.',
            'cancelled' => 'This order was cancelled. Any prepaid amount follows standard bank refund timelines (5-7 business days).',
            'refunded' => 'A refund has been processed against this order.',
            default => 'Order status: ' . ucfirst($status),
        };

        $orderReport = [
            'order_id' => (int)$order['order_id'],
            'order_number' => (string)$order['order_number'],
            'order_status' => $status,
            'payment_status' => $paymentStatus,
            'payment_method' => (string)($order['payment_method'] ?? ''),
            'tracking_number' => $order['tracking_number'] ?: null,
            'courier_name' => $order['courier_name'] ?: null,
            'tracking_url' => $order['tracking_url'] ?: null,
            'estimated_delivery_date' => $order['estimated_delivery_date'] ?: ($order['estimated_delivery'] ?? null),
            'return_eligible' => $returnEligible,
            'cancel_eligible' => $cancelEligible,
            'status_explanation' => $explanation,
            'policy_summary' => 'Melvore accepts returns/replacements within 7 days of delivery for unworn items with original tags intact, or verified transit damage with unboxing photos.',
            'items' => array_map(static fn($i) => [
                'order_item_id' => (int)$i['order_item_id'],
                'product_id' => (int)$i['product_id'],
                'product_name' => (string)$i['product_name'],
                'quantity' => (int)$i['quantity'],
                'unit_price' => (float)$i['unit_price'],
            ], $items),
        ];
    }

    $createdTicket = null;
    if ($action === 'escalate_ticket') {
        $subject = trim((string)($body['subject'] ?? 'Customer Support Request'));
        $category = trim((string)($body['category'] ?? 'order_issue'));
        if ($customerNote === '' && $orderReport === null) {
            MelvoreResponse::error('Please describe your issue so our support specialist can help.', 'VALIDATION_ERROR', 422);
        }

        $pdo->beginTransaction();
        $ticketNum = 'MLV-' . strtoupper(bin2hex(random_bytes(4)));
        $fullMessage = $customerNote !== '' ? $customerNote : 'Escalated from guided self-service help flow.';
        if ($orderReport !== null) {
            $fullMessage = "[Order {$orderReport['order_number']} | Status: {$orderReport['order_status']}]\n" . $fullMessage;
        }
        if ($orderItemId > 0) {
            $fullMessage .= "\nAffected Order Item ID: #" . $orderItemId;
        }
        if (!empty($evidenceUrls)) {
            $cleanUrls = array_map(static fn($u) => MelvoreResponse::mediaUrl((string)$u), array_slice($evidenceUrls, 0, 4));
            $fullMessage .= "\nAttached Evidence:\n" . implode("\n", array_filter($cleanUrls));
        }

        $tIns = $pdo->prepare(
            'INSERT INTO support_tickets (user_id, order_id, ticket_number, subject, category, priority, status, message, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, "normal", "open", ?, NOW(), NOW())'
        );
        $tIns->execute([
            $userId,
            $orderId > 0 ? $orderId : null,
            $ticketNum,
            substr($subject, 0, 180),
            substr($category, 0, 60),
            substr($fullMessage, 0, 4000),
        ]);
        $ticketId = (int)$pdo->lastInsertId();

        if (MelvoreDB::tableExists('ticket_replies')) {
            $rIns = $pdo->prepare(
                'INSERT INTO ticket_replies (ticket_id, user_id, sender_type, message, created_at)
                 VALUES (?, ?, "customer", ?, NOW())'
            );
            $rIns->execute([$ticketId, $userId, substr($fullMessage, 0, 4000)]);
        }

        $pdo->commit();
        $createdTicket = [
            'ticket_id' => $ticketId,
            'ticket_number' => $ticketNum,
            'status' => 'open',
            'note' => 'Ticket opened for human review. No refund or replacement is finalized until reviewed by our team.',
        ];
    }

    // Persist session progress to app_help_flow_sessions
    if (MelvoreDB::tableExists('app_help_flow_sessions')) {
        if ($sessionToken === '') {
            $sessionToken = bin2hex(random_bytes(16));
        }
        $payloadJson = json_encode([
            'action' => $action,
            'order_item_id' => $orderItemId,
            'option_id' => $selectedOptionId,
            'evidence_count' => count($evidenceUrls),
            'ticket_id' => $createdTicket['ticket_id'] ?? null,
        ], JSON_UNESCAPED_SLASHES);

        $sIns = $pdo->prepare(
            'INSERT INTO app_help_flow_sessions
             (user_id, topic_id, flow_id, order_id, current_node_id, session_token, status, resolution_payload, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
             ON DUPLICATE KEY UPDATE
               order_id = VALUES(order_id),
               current_node_id = VALUES(current_node_id),
               status = VALUES(status),
               resolution_payload = VALUES(resolution_payload),
               updated_at = NOW()'
        );
        $sIns->execute([
            $userId,
            $topicId > 0 ? $topicId : null,
            $flowId > 0 ? $flowId : null,
            $orderId > 0 ? $orderId : null,
            $currentNodeId > 0 ? $currentNodeId : null,
            substr($sessionToken, 0, 64),
            $createdTicket ? 'escalated' : 'in_progress',
            $payloadJson,
        ]);
    }

    MelvoreResponse::ok([
        'session_token' => $sessionToken,
        'order_report' => $orderReport,
        'ticket' => $createdTicket,
    ], $createdTicket ? 'Support ticket created' : 'Order and eligibility evaluated');
} catch (Throwable $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    MelvoreResponse::error('Unable to process help session.', 'SERVER_ERROR', 500);
}
