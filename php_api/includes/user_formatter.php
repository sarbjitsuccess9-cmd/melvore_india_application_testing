<?php
declare(strict_types=1);

require_once __DIR__ . '/response.php';

/**
 * Formats safe user & address payloads matching Melvore Android UserDto and AddressDto.
 */
final class MelvoreUserFormatter
{
    public static function formatUser(array $row): array
    {
        $avatar = $row['avatar'] ?? ($row['profile_image'] ?? null);
        return [
            'user_id' => (int)($row['user_id'] ?? 0),
            'name' => (string)($row['name'] ?? ''),
            'email' => (string)($row['email'] ?? ''),
            'mobile_primary' => $row['mobile_primary'] ?? null,
            'mobile_alternate' => $row['mobile_alternate'] ?? null,
            'avatar' => MelvoreResponse::mediaUrl($avatar !== null ? (string)$avatar : null),
            'email_verified' => ((int)($row['email_verified'] ?? 0)) === 1,
            'mobile_verified' => ((int)($row['mobile_verified'] ?? 0)) === 1,
            'is_subscribed' => ((int)($row['is_subscribed'] ?? 0)) === 1,
            'social_provider' => (string)($row['social_provider'] ?? 'none'),
            'role' => (string)($row['role'] ?? 'customer'),
            'created_at' => (string)($row['created_at'] ?? ''),
        ];
    }

    public static function formatAddress(array $row): array
    {
        return [
            'address_id' => (int)($row['address_id'] ?? 0),
            'full_name' => (string)($row['full_name'] ?? ''),
            'address_line1' => (string)($row['address_line1'] ?? ''),
            'address_line2' => $row['address_line2'] ?? null,
            'city' => (string)($row['city'] ?? ''),
            'state' => (string)($row['state'] ?? ''),
            'pincode' => (string)($row['pincode'] ?? ''),
            'country' => (string)($row['country'] ?? 'India'),
            'mobile' => (string)($row['mobile'] ?? ''),
            'is_default' => ((int)($row['is_default'] ?? 0)) === 1,
            'address_type' => (string)($row['address_type'] ?? 'home'),
            'created_at' => $row['created_at'] ?? null,
        ];
    }
}
