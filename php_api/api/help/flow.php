<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/auth.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $topicId = (int)($_GET['topic_id'] ?? 0);
    $topicSlug = trim((string)($_GET['slug'] ?? ''));

    if ($topicId <= 0 && $topicSlug === '') {
        MelvoreResponse::error('Help topic_id or slug is required.', 'VALIDATION_ERROR', 422);
    }

    if (!MelvoreDB::tableExists('app_help_topics') || !MelvoreDB::tableExists('app_help_flows')) {
        MelvoreResponse::error('Guided help flows are not configured.', 'NOT_FOUND', 404);
    }

    if ($topicId > 0) {
        $tStmt = $pdo->prepare('SELECT * FROM app_help_topics WHERE topic_id = ? AND is_active = 1 LIMIT 1');
        $tStmt->execute([$topicId]);
    } else {
        $tStmt = $pdo->prepare('SELECT * FROM app_help_topics WHERE slug = ? AND is_active = 1 LIMIT 1');
        $tStmt->execute([$topicSlug]);
    }
    $topic = $tStmt->fetch();
    if (!$topic) {
        MelvoreResponse::error('Help topic not found.', 'NOT_FOUND', 404);
    }
    $tid = (int)$topic['topic_id'];

    $fStmt = $pdo->prepare('SELECT * FROM app_help_flows WHERE topic_id = ? AND is_active = 1 ORDER BY flow_id ASC LIMIT 1');
    $fStmt->execute([$tid]);
    $flow = $fStmt->fetch();

    $nodes = [];
    if ($flow && MelvoreDB::tableExists('app_help_flow_nodes')) {
        $fid = (int)$flow['flow_id'];
        $nStmt = $pdo->prepare('SELECT * FROM app_help_flow_nodes WHERE flow_id = ? ORDER BY display_order ASC, node_id ASC');
        $nStmt->execute([$fid]);
        $rawNodes = $nStmt->fetchAll();

        $optionsByNode = [];
        if (MelvoreDB::tableExists('app_help_flow_options') && !empty($rawNodes)) {
            $nIds = array_map(static fn($n) => (int)$n['node_id'], $rawNodes);
            $ph = implode(',', array_fill(0, count($nIds), '?'));
            $oStmt = $pdo->prepare("SELECT * FROM app_help_flow_options WHERE node_id IN ($ph) ORDER BY display_order ASC, option_id ASC");
            $oStmt->execute($nIds);
            foreach ($oStmt->fetchAll() as $opt) {
                $optionsByNode[(int)$opt['node_id']][] = [
                    'option_id' => (int)$opt['option_id'],
                    'label' => (string)$opt['label'],
                    'next_node_id' => !empty($opt['next_node_id']) ? (int)$opt['next_node_id'] : null,
                    'action_key' => (string)($opt['action_key'] ?? ''),
                    'escalate_to_ticket' => ((int)($opt['escalate_to_ticket'] ?? 0)) === 1,
                ];
            }
        }

        foreach ($rawNodes as $rn) {
            $nid = (int)$rn['node_id'];
            $nodes[] = [
                'node_id' => $nid,
                'node_key' => (string)($rn['node_key'] ?? ''),
                'node_type' => (string)($rn['node_type'] ?? 'question'),
                'title' => (string)$rn['title'],
                'body_text' => (string)($rn['body_text'] ?? ''),
                'policy_text' => (string)($rn['policy_text'] ?? ''),
                'requires_order_select' => ((int)($rn['requires_order_select'] ?? 0)) === 1,
                'requires_item_select' => ((int)($rn['requires_item_select'] ?? 0)) === 1,
                'allows_photo_upload' => ((int)($rn['allows_photo_upload'] ?? 0)) === 1,
                'allows_note_input' => ((int)($rn['allows_note_input'] ?? 0)) === 1,
                'is_terminal' => ((int)($rn['is_terminal'] ?? 0)) === 1,
                'can_escalate_ticket' => ((int)($rn['can_escalate_ticket'] ?? 1)) === 1,
                'options' => $optionsByNode[$nid] ?? [],
            ];
        }
    }

    MelvoreResponse::ok([
        'topic' => [
            'topic_id' => $tid,
            'slug' => (string)$topic['slug'],
            'title' => (string)$topic['title'],
            'subtitle' => (string)($topic['subtitle'] ?? ''),
            'requires_order' => ((int)($topic['requires_order'] ?? 0)) === 1,
        ],
        'flow' => $flow ? [
            'flow_id' => (int)$flow['flow_id'],
            'title' => (string)($flow['title'] ?? $topic['title']),
            'start_node_id' => !empty($flow['start_node_id']) ? (int)$flow['start_node_id'] : ($nodes[0]['node_id'] ?? null),
            'nodes' => $nodes,
        ] : null,
    ], 'Guided help flow loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load guided help flow.', 'SERVER_ERROR', 500);
}
