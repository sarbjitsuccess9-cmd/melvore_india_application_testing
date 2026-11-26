<?php
declare(strict_types=1);

require_once dirname(__DIR__, 2) . '/includes/db.php';
require_once dirname(__DIR__, 2) . '/includes/response.php';
require_once dirname(__DIR__, 2) . '/includes/rate_limit.php';

MelvoreResponse::requireMethod('POST');
MelvoreRateLimit::enforce('forms_contact', 8, 600);

try {
    $body = MelvoreResponse::jsonBody();
    $name = trim((string)($body['name'] ?? ''));
    $email = strtolower(trim((string)($body['email'] ?? '')));
    $phone = trim((string)($body['phone'] ?? ''));
    $subject = trim((string)($body['subject'] ?? 'Customer Inquiry'));
    $message = trim((string)($body['message'] ?? ''));

    if ($name === '' || !filter_var($email, FILTER_VALIDATE_EMAIL) || $message === '') {
        MelvoreResponse::error('Name, valid email, and message are required.', 'VALIDATION_ERROR', 400);
    }

    $pdo = MelvoreDB::pdo();
    $contactId = 0;
    if (MelvoreDB::tableExists('contact_messages')) {
        $ins = $pdo->prepare(
            'INSERT INTO contact_messages (name, email, phone, subject, message, created_at)
             VALUES (?, ?, ?, ?, ?, NOW())'
        );
        $ins->execute([$name, $email, $phone, $subject, $message]);
        $contactId = (int)$pdo->lastInsertId();
    }

    MelvoreResponse::okFlat([
        'contact_id' => $contactId,
    ], 'Thank you. Your message has been received by Melvore Support.', 201);
} catch (Throwable $e) {
    MelvoreResponse::error('Unable to submit contact inquiry.', 'SERVER_ERROR', 500);
}
