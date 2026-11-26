<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $faqs = [];
    if (MelvoreDB::tableExists('faqs')) {
        $stmt = $pdo->query('SELECT * FROM faqs WHERE is_active = 1 ORDER BY display_order ASC, faq_id ASC');
        foreach ($stmt->fetchAll() as $r) {
            $faqs[] = [
                'faq_id' => (int)($r['faq_id'] ?? ($r['id'] ?? 0)),
                'question' => (string)($r['question'] ?? ''),
                'answer' => (string)($r['answer'] ?? ''),
                'category' => (string)($r['category'] ?? 'General'),
                'display_order' => (int)($r['display_order'] ?? 0),
            ];
        }
    }
    MelvoreResponse::okFlat(['faqs' => $faqs], 'FAQs loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load FAQs.', 'SERVER_ERROR', 500);
}
