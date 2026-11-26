<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';

MelvoreResponse::requireMethod('GET');

try {
    $pdo = MelvoreDB::pdo();
    $topics = [];
    if (MelvoreDB::tableExists('app_help_topics')) {
        $tStmt = $pdo->query(
            'SELECT topic_id, slug, title, subtitle, icon_key, category, requires_order, display_order
             FROM app_help_topics
             WHERE is_active = 1
             ORDER BY display_order ASC, topic_id ASC'
        );
        foreach ($tStmt->fetchAll() as $row) {
            $topics[] = [
                'topic_id' => (int)$row['topic_id'],
                'slug' => (string)$row['slug'],
                'title' => (string)$row['title'],
                'subtitle' => (string)($row['subtitle'] ?? ''),
                'icon_key' => (string)($row['icon_key'] ?? 'help'),
                'category' => (string)($row['category'] ?? 'general'),
                'requires_order' => ((int)($row['requires_order'] ?? 0)) === 1,
                'display_order' => (int)($row['display_order'] ?? 0),
            ];
        }
    }

    $faqs = [];
    if (MelvoreDB::tableExists('faqs')) {
        $fStmt = $pdo->query(
            'SELECT faq_id, category, question, answer, display_order
             FROM faqs
             WHERE is_active = 1
             ORDER BY display_order ASC, faq_id ASC'
        );
        foreach ($fStmt->fetchAll() as $fr) {
            $faqs[] = [
                'faq_id' => (int)$fr['faq_id'],
                'category' => (string)($fr['category'] ?? 'General'),
                'question' => (string)$fr['question'],
                'answer' => (string)$fr['answer'],
            ];
        }
    }

    MelvoreResponse::ok([
        'topics' => $topics,
        'faqs' => $faqs,
    ], 'Help topics and FAQs loaded');
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to load help center.', 'SERVER_ERROR', 500);
}
