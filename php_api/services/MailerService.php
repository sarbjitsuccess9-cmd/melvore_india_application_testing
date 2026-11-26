<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/config/env.php';

/**
 * Lightweight transactional mailer using configured MAIL_FROM.
 */
final class MelvoreMailerService
{
    public static function sendOtpEmail(string $toEmail, string $otp, string $purpose = 'Verification'): bool
    {
        $from = MelvoreEnv::mailFrom();
        $subject = "Melvore {$purpose} Code: {$otp}";
        $body = "Your Melvore {$purpose} code is: {$otp}\n\nThis code expires in 10 minutes. If you did not request this, please ignore this email.\n\n— Melvore India";
        $headers = implode("\r\n", [
            "From: Melvore <{$from}>",
            "Reply-To: {$from}",
            'Content-Type: text/plain; charset=UTF-8',
            'X-Mailer: PHP/' . PHP_VERSION,
        ]);

        if (function_exists('mail')) {
            return @mail($toEmail, $subject, $body, $headers);
        }
        error_log("[MelvoreMailer] Mail dispatch fallback for {$toEmail} ({$purpose})");
        return false;
    }
}
