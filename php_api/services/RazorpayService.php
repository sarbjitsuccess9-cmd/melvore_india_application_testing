<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/config/env.php';

/**
 * Razorpay REST API v1 Client & Signature Verifier (NO Cashfree).
 */
final class MelvoreRazorpayService
{
    public static function keyId(PDO $pdo): string
    {
        $envVal = MelvoreEnv::get('RAZORPAY_KEY_ID', '');
        if ($envVal !== '') {
            return $envVal;
        }
        return self::settingFallback($pdo, 'razorpay_key_id');
    }

    public static function keySecret(PDO $pdo): string
    {
        $envVal = MelvoreEnv::get('RAZORPAY_KEY_SECRET', '');
        if ($envVal !== '') {
            return $envVal;
        }
        return self::settingFallback($pdo, 'razorpay_key_secret');
    }

    public static function webhookSecret(PDO $pdo): string
    {
        $envVal = MelvoreEnv::get('RAZORPAY_WEBHOOK_SECRET', '');
        if ($envVal !== '') {
            return $envVal;
        }
        return self::settingFallback($pdo, 'razorpay_webhook_secret');
    }

    public static function verifyCheckoutSignature(string $razorpayOrderId, string $razorpayPaymentId, string $signature, string $secret): bool
    {
        if ($razorpayOrderId === '' || $razorpayPaymentId === '' || $signature === '' || $secret === '') {
            return false;
        }
        $expected = hash_hmac('sha256', $razorpayOrderId . '|' . $razorpayPaymentId, $secret);
        return hash_equals($expected, $signature);
    }

    public static function verifyWebhookSignature(string $rawPayload, string $headerSignature, string $webhookSecret): bool
    {
        if ($rawPayload === '' || $headerSignature === '' || $webhookSecret === '') {
            return false;
        }
        $expected = hash_hmac('sha256', $rawPayload, $webhookSecret);
        return hash_equals($expected, $headerSignature);
    }

    public static function createOrder(string $keyId, string $keySecret, int $amountPaise, string $receipt, array $notes = []): array
    {
        return self::request('POST', 'https://api.razorpay.com/v1/orders', $keyId, $keySecret, [
            'amount' => $amountPaise,
            'currency' => 'INR',
            'receipt' => $receipt,
            'payment_capture' => 1,
            'notes' => $notes,
        ]);
    }

    public static function fetchPayment(string $keyId, string $keySecret, string $paymentId): array
    {
        return self::request('GET', 'https://api.razorpay.com/v1/payments/' . rawurlencode($paymentId), $keyId, $keySecret);
    }

    public static function capturePayment(string $keyId, string $keySecret, string $paymentId, int $amountPaise): array
    {
        return self::request(
            'POST',
            'https://api.razorpay.com/v1/payments/' . rawurlencode($paymentId) . '/capture',
            $keyId,
            $keySecret,
            ['amount' => $amountPaise, 'currency' => 'INR']
        );
    }

    public static function refundPayment(string $keyId, string $keySecret, string $paymentId, ?int $amountPaise = null, array $notes = []): array
    {
        $payload = ['notes' => $notes];
        if ($amountPaise !== null && $amountPaise > 0) {
            $payload['amount'] = $amountPaise;
        }
        return self::request(
            'POST',
            'https://api.razorpay.com/v1/payments/' . rawurlencode($paymentId) . '/refund',
            $keyId,
            $keySecret,
            $payload
        );
    }

    private static function request(string $method, string $url, string $keyId, string $keySecret, ?array $body = null): array
    {
        if ($keyId === '' || $keySecret === '') {
            throw new RuntimeException('Razorpay credentials are not configured on the server.');
        }
        $ch = curl_init($url);
        $opts = [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 20,
            CURLOPT_CONNECTTIMEOUT => 8,
            CURLOPT_USERPWD => $keyId . ':' . $keySecret,
            CURLOPT_HTTPHEADER => ['Content-Type: application/json'],
            CURLOPT_CUSTOMREQUEST => $method,
        ];
        if ($body !== null) {
            $opts[CURLOPT_POSTFIELDS] = json_encode($body, JSON_UNESCAPED_SLASHES);
        }
        curl_setopt_array($ch, $opts);
        $resp = curl_exec($ch);
        $httpCode = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $err = curl_error($ch);
        curl_close($ch);

        if ($resp === false) {
            throw new RuntimeException('Razorpay network error: ' . $err);
        }
        $decoded = json_decode($resp, true);
        if (!is_array($decoded) || $httpCode >= 400) {
            $msg = is_array($decoded) && isset($decoded['error']['description'])
                ? (string)$decoded['error']['description']
                : "Razorpay HTTP {$httpCode}";
            throw new RuntimeException($msg);
        }
        return $decoded;
    }

    private static function settingFallback(PDO $pdo, string $key): string
    {
        if (!MelvoreDB::tableExists('site_settings')) {
            return '';
        }
        $stmt = $pdo->prepare('SELECT setting_value FROM site_settings WHERE setting_key = ? LIMIT 1');
        $stmt->execute([$key]);
        $val = $stmt->fetchColumn();
        return $val !== false ? trim((string)$val) : '';
    }
}
