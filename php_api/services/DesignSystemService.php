<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/includes/db.php';
require_once dirname(__DIR__) . '/includes/response.php';

/**
 * Central Remote Design System Engine.
 * Manages Default (0) and Live (1) manifests, schema validation, versioning,
 * checksum integrity, compatibility fallback, release snapshots, and diffs.
 */
final class MelvoreDesignSystemService
{
    public const SUPPORTED_SCHEMA_VERSION = '1.0.0';

    public static function rootDir(): string
    {
        return dirname(__DIR__) . '/design-system';
    }

    public static function manifestFilePath(string $slot): string
    {
        $clean = ($slot === 'live') ? 'live' : 'default';
        return self::rootDir() . '/' . $clean . '/manifest.json';
    }

    public static function releasesDir(): string
    {
        return self::rootDir() . '/releases';
    }

    public static function assetsDir(): string
    {
        return self::rootDir() . '/assets';
    }

    public static function getActiveDesignFlag(PDO $pdo): int
    {
        if (!MelvoreDB::tableExists('app_design_settings')) {
            return 0;
        }
        try {
            $val = $pdo->query("SELECT active_design FROM app_design_settings WHERE setting_key = 'global' LIMIT 1")->fetchColumn();
            return ((int)$val === 1) ? 1 : 0;
        } catch (Throwable) {
            return 0;
        }
    }

    public static function activeSlot(PDO $pdo): string
    {
        return self::getActiveDesignFlag($pdo) === 1 ? 'live' : 'default';
    }

    /**
     * Loads and validates the active published manifest for public consumption.
     * Never exposes draft releases. Automatically falls back to stable default
     * if live manifest is missing, corrupt, or incompatible with appSchemaVersion.
     */
    public static function resolvePublishedManifest(
        PDO $pdo,
        ?string $overrideSlot = null,
        ?string $appSchemaVersion = null
    ): array {
        $activeFlag = self::getActiveDesignFlag($pdo);
        $requestedSlot = $overrideSlot !== null
            ? (($overrideSlot === 'live') ? 'live' : 'default')
            : ($activeFlag === 1 ? 'live' : 'default');

        $fallbackUsed = false;
        $fallbackReason = null;

        $manifest = self::loadPublishedSlotManifest($pdo, $requestedSlot);
        $validation = self::validateManifest($manifest, $requestedSlot);

        if ($validation['valid'] && $appSchemaVersion !== null && $appSchemaVersion !== '') {
            $minApp = (string)($manifest['min_app_schema_version'] ?? '1.0.0');
            if (version_compare($appSchemaVersion, $minApp, '<')) {
                $validation['valid'] = false;
                $validation['errors'][] = "Manifest requires app schema >= {$minApp}, client is {$appSchemaVersion}.";
            }
        }

        if (!$validation['valid']) {
            $fallbackUsed = true;
            $fallbackReason = implode(' ', $validation['errors']);
            $manifest = self::loadPublishedSlotManifest($pdo, 'default');
            $defValidation = self::validateManifest($manifest, 'default');
            if (!$defValidation['valid']) {
                $manifest = self::readJsonFile(self::manifestFilePath('default')) ?? self::hardcodedSafetyManifest();
            }
            $requestedSlot = 'default';
        }

        $checksum = self::computeChecksum($manifest);
        $manifest['checksum_sha256'] = $checksum;

        return [
            'active_design' => $activeFlag,
            'resolved_slot' => $requestedSlot,
            'fallback_used' => $fallbackUsed,
            'fallback_reason' => $fallbackReason,
            'checksum_sha256' => $checksum,
            'etag' => '"' . substr($checksum, 0, 16) . '"',
            'manifest' => $manifest,
        ];
    }

    public static function computeManifestVersionToken(PDO $pdo): string
    {
        $resolved = self::resolvePublishedManifest($pdo);
        $m = $resolved['manifest'];
        return ($resolved['resolved_slot'])
            . ':' . ($m['design_version'] ?? '1.0.0')
            . ':' . substr((string)$resolved['checksum_sha256'], 0, 12);
    }

    public static function validateManifest(?array $manifest, ?string $expectedSlot = null): array
    {
        $errors = [];
        if (!is_array($manifest)) {
            return ['valid' => false, 'errors' => ['Manifest JSON could not be decoded as an object.']];
        }
        foreach (['manifest_id', 'slot', 'design_version', 'schema_version', 'min_app_schema_version', 'tokens', 'navigation', 'components', 'pages', 'worlds'] as $req) {
            if (!array_key_exists($req, $manifest)) {
                $errors[] = "Missing required top-level field: {$req}.";
            }
        }
        $slot = (string)($manifest['slot'] ?? '');
        if (!in_array($slot, ['default', 'live'], true)) {
            $errors[] = "Invalid slot '{$slot}'; must be 'default' or 'live'.";
        } elseif ($expectedSlot !== null && $slot !== $expectedSlot) {
            // Normalize only when validating cross-slot unless mismatched intentionally
            $errors[] = "Manifest slot '{$slot}' does not match expected slot '{$expectedSlot}'.";
        }
        $colors = $manifest['tokens']['colors'] ?? null;
        if (!is_array($colors)) {
            $errors[] = 'tokens.colors object is required.';
        } else {
            foreach (['background', 'surface', 'primary', 'accent', 'text_primary', 'text_secondary', 'border'] as $ck) {
                $hex = (string)($colors[$ck] ?? '');
                if (!preg_match('/^#[0-9A-Fa-f]{6,8}$/', $hex)) {
                    $errors[] = "Invalid hex color for tokens.colors.{$ck}: '{$hex}'.";
                }
            }
        }
        $tabs = $manifest['navigation']['tabs'] ?? null;
        if (!is_array($tabs) || count($tabs) < 3) {
            $errors[] = 'navigation.tabs must define at least 3 navigation tabs.';
        }
        foreach (['home', 'style_worlds', 'categories', 'search', 'product_details', 'cart'] as $pageKey) {
            if (empty($manifest['pages'][$pageKey]) || !is_array($manifest['pages'][$pageKey])) {
                $errors[] = "Missing page layout definition for pages.{$pageKey}.";
            }
        }
        return ['valid' => count($errors) === 0, 'errors' => $errors];
    }

    public static function computeChecksum(array $manifest): string
    {
        $clone = $manifest;
        unset($clone['checksum_sha256']);
        self::recursiveKsort($clone);
        $encoded = json_encode($clone, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
        return hash('sha256', (string)$encoded);
    }

    public static function getAdminWorkspace(PDO $pdo): array
    {
        $activeDesign = self::getActiveDesignFlag($pdo);
        $defaultPub = self::loadPublishedSlotManifest($pdo, 'default');
        $livePub = self::loadPublishedSlotManifest($pdo, 'live');
        $defaultDraft = self::loadLatestDraft($pdo, 'default');
        $liveDraft = self::loadLatestDraft($pdo, 'live');

        $releases = [];
        if (MelvoreDB::tableExists('app_design_releases')) {
            $stmt = $pdo->query(
                'SELECT release_id, slot, status, version_tag, schema_version, min_app_schema_version,
                        release_notes, checksum_sha256, published_at, created_at, updated_at
                 FROM app_design_releases ORDER BY release_id DESC LIMIT 60'
            );
            $releases = $stmt->fetchAll() ?: [];
        }

        $history = [];
        if (MelvoreDB::tableExists('app_design_release_history')) {
            $stmt = $pdo->query(
                'SELECT history_id, release_id, action_type, slot, previous_active_design, new_active_design,
                        actor_user_id, notes, created_at
                 FROM app_design_release_history ORDER BY history_id DESC LIMIT 50'
            );
            $history = $stmt->fetchAll() ?: [];
        }

        $assets = self::listDesignAssets($pdo);

        return [
            'active_design' => $activeDesign,
            'active_slot' => $activeDesign === 1 ? 'live' : 'default',
            'default_manifest' => $defaultPub,
            'live_manifest' => $livePub,
            'default_draft' => $defaultDraft,
            'live_draft' => $liveDraft,
            'releases' => $releases,
            'history' => $history,
            'assets' => $assets,
        ];
    }

    public static function saveDraft(PDO $pdo, string $slot, array $manifestData, int $adminUserId, string $notes = ''): array
    {
        $cleanSlot = ($slot === 'live') ? 'live' : 'default';
        $manifestData['slot'] = $cleanSlot;
        $validation = self::validateManifest($manifestData, $cleanSlot);
        if (!$validation['valid']) {
            return ['ok' => false, 'errors' => $validation['errors']];
        }

        $checksum = self::computeChecksum($manifestData);
        $manifestData['checksum_sha256'] = $checksum;
        $versionTag = (string)($manifestData['design_version'] ?? '1.0.0');
        $schemaVer = (string)($manifestData['schema_version'] ?? self::SUPPORTED_SCHEMA_VERSION);
        $minAppVer = (string)($manifestData['min_app_schema_version'] ?? self::SUPPORTED_SCHEMA_VERSION);
        $json = json_encode($manifestData, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);

        $releaseId = null;
        if (MelvoreDB::tableExists('app_design_releases')) {
            $stmt = $pdo->prepare(
                'INSERT INTO app_design_releases
                 (slot, status, version_tag, schema_version, min_app_schema_version, release_notes, manifest_json, checksum_sha256, created_by)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'
            );
            $stmt->execute([
                $cleanSlot,
                'draft',
                $versionTag,
                $schemaVer,
                $minAppVer,
                $notes !== '' ? $notes : (string)($manifestData['release_notes'] ?? 'Draft saved from Design Studio'),
                $json,
                $checksum,
                $adminUserId,
            ]);
            $releaseId = (int)$pdo->lastInsertId();
            self::logHistory($pdo, $releaseId, 'save_draft', $cleanSlot, null, null, $adminUserId, $notes ?: "Saved {$cleanSlot} draft ({$versionTag})", $json);
        }

        return [
            'ok' => true,
            'release_id' => $releaseId,
            'slot' => $cleanSlot,
            'version_tag' => $versionTag,
            'checksum_sha256' => $checksum,
        ];
    }

    public static function publishSlot(
        PDO $pdo,
        string $slot,
        ?array $manifestData,
        int $adminUserId,
        ?string $versionTag = null,
        string $notes = ''
    ): array {
        $cleanSlot = ($slot === 'live') ? 'live' : 'default';
        if ($manifestData === null) {
            $draft = self::loadLatestDraft($pdo, $cleanSlot);
            $manifestData = $draft ?? self::loadPublishedSlotManifest($pdo, $cleanSlot);
        }
        $manifestData['slot'] = $cleanSlot;
        if ($versionTag !== null && trim($versionTag) !== '') {
            $manifestData['design_version'] = trim($versionTag);
        }
        $manifestData['published_at'] = gmdate('c');
        if ($notes !== '') {
            $manifestData['release_notes'] = $notes;
        }

        $validation = self::validateManifest($manifestData, $cleanSlot);
        if (!$validation['valid']) {
            return ['ok' => false, 'errors' => $validation['errors']];
        }

        $checksum = self::computeChecksum($manifestData);
        $manifestData['checksum_sha256'] = $checksum;
        $json = json_encode($manifestData, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);

        // Persist atomically to filesystem manifest and releases snapshot
        self::writeFileAtomic(self::manifestFilePath($cleanSlot), (string)$json);
        $ver = preg_replace('/[^a-zA-Z0-9._-]/', '_', (string)$manifestData['design_version']);
        $snapshotPath = self::releasesDir() . '/v' . $ver . '-' . $cleanSlot . '.json';
        self::writeFileAtomic($snapshotPath, (string)$json);

        $releaseId = null;
        if (MelvoreDB::tableExists('app_design_releases')) {
            // Archive prior published release for same slot (keep default & live independent!)
            $pdo->prepare("UPDATE app_design_releases SET status = 'archived' WHERE slot = ? AND status = 'published'")
                ->execute([$cleanSlot]);

            $stmt = $pdo->prepare(
                'INSERT INTO app_design_releases
                 (slot, status, version_tag, schema_version, min_app_schema_version, release_notes, manifest_json, checksum_sha256, created_by, published_by, published_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())'
            );
            $stmt->execute([
                $cleanSlot,
                'published',
                (string)$manifestData['design_version'],
                (string)($manifestData['schema_version'] ?? self::SUPPORTED_SCHEMA_VERSION),
                (string)($manifestData['min_app_schema_version'] ?? self::SUPPORTED_SCHEMA_VERSION),
                (string)($manifestData['release_notes'] ?? ''),
                $json,
                $checksum,
                $adminUserId,
                $adminUserId,
            ]);
            $releaseId = (int)$pdo->lastInsertId();

            if (MelvoreDB::tableExists('app_design_settings')) {
                $col = $cleanSlot === 'live' ? 'live_release_id' : 'default_release_id';
                $pdo->prepare("INSERT INTO app_design_settings (setting_key, active_design, {$col}, updated_by) VALUES ('global', 0, ?, ?) ON DUPLICATE KEY UPDATE {$col} = VALUES({$col}), updated_by = VALUES(updated_by)")
                    ->execute([$releaseId, $adminUserId]);
            }

            if (MelvoreDB::tableExists('app_design_page_configs') && !empty($manifestData['pages']) && is_array($manifestData['pages'])) {
                $pStmt = $pdo->prepare(
                    'INSERT INTO app_design_page_configs (slot, page_key, layout_template, hero_height_dp, grid_columns, sections_json, is_published, updated_by)
                     VALUES (?, ?, ?, ?, ?, ?, 1, ?)
                     ON DUPLICATE KEY UPDATE
                        layout_template = VALUES(layout_template), hero_height_dp = VALUES(hero_height_dp),
                        grid_columns = VALUES(grid_columns), sections_json = VALUES(sections_json),
                        is_published = 1, updated_by = VALUES(updated_by)'
                );
                foreach ($manifestData['pages'] as $pageKey => $pageCfg) {
                    if (!is_array($pageCfg)) {
                        continue;
                    }
                    $pStmt->execute([
                        $cleanSlot,
                        (string)$pageKey,
                        (string)($pageCfg['layout_template'] ?? 'editorial_magazine'),
                        (int)($pageCfg['hero_height_dp'] ?? 420),
                        (int)($pageCfg['grid_columns'] ?? 2),
                        json_encode($pageCfg['sections'] ?? [], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE),
                        $adminUserId,
                    ]);
                }
            }

            self::logHistory($pdo, $releaseId, 'publish', $cleanSlot, null, null, $adminUserId, $notes ?: "Published {$cleanSlot} v{$manifestData['design_version']}", $json);
        }

        return [
            'ok' => true,
            'release_id' => $releaseId,
            'slot' => $cleanSlot,
            'design_version' => (string)$manifestData['design_version'],
            'checksum_sha256' => $checksum,
        ];
    }

    public static function switchActiveDesign(PDO $pdo, int $activeDesign, int $adminUserId, string $notes = ''): array
    {
        $nextFlag = ($activeDesign === 1) ? 1 : 0;
        $targetSlot = ($nextFlag === 1) ? 'live' : 'default';

        $candidate = self::loadPublishedSlotManifest($pdo, $targetSlot);
        $validation = self::validateManifest($candidate, $targetSlot);
        if (!$validation['valid']) {
            return ['ok' => false, 'errors' => $validation['errors']];
        }

        $prevFlag = self::getActiveDesignFlag($pdo);
        if (MelvoreDB::tableExists('app_design_settings')) {
            $pdo->prepare(
                "INSERT INTO app_design_settings (setting_key, active_design, updated_by)
                 VALUES ('global', ?, ?)
                 ON DUPLICATE KEY UPDATE active_design = VALUES(active_design), updated_by = VALUES(updated_by)"
            )->execute([$nextFlag, $adminUserId]);

            self::logHistory(
                $pdo,
                null,
                'switch_active',
                $targetSlot,
                $prevFlag,
                $nextFlag,
                $adminUserId,
                $notes !== '' ? $notes : "Switched active storefront design from {$prevFlag} to {$nextFlag} ({$targetSlot})",
                null
            );
        }

        return [
            'ok' => true,
            'previous_active_design' => $prevFlag,
            'active_design' => $nextFlag,
            'active_slot' => $targetSlot,
            'checksum_sha256' => self::computeChecksum($candidate),
        ];
    }

    public static function rollbackToRelease(PDO $pdo, int $releaseId, int $adminUserId, string $notes = ''): array
    {
        if (!MelvoreDB::tableExists('app_design_releases')) {
            return ['ok' => false, 'errors' => ['app_design_releases table does not exist.']];
        }
        $stmt = $pdo->prepare('SELECT * FROM app_design_releases WHERE release_id = ? LIMIT 1');
        $stmt->execute([$releaseId]);
        $row = $stmt->fetch();
        if (!$row) {
            return ['ok' => false, 'errors' => ['Release not found.']];
        }
        $decoded = json_decode((string)$row['manifest_json'], true);
        if (!is_array($decoded)) {
            return ['ok' => false, 'errors' => ['Stored release JSON is invalid.']];
        }
        $slot = ($row['slot'] === 'live') ? 'live' : 'default';
        $res = self::publishSlot(
            $pdo,
            $slot,
            $decoded,
            $adminUserId,
            (string)$row['version_tag'],
            $notes !== '' ? $notes : "Rolled back {$slot} to release #{$releaseId} ({$row['version_tag']})"
        );
        if (!empty($res['ok'])) {
            $targetFlag = ($slot === 'live') ? 1 : 0;
            $prevFlag = self::getActiveDesignFlag($pdo);
            $pdo->prepare("UPDATE app_design_settings SET active_design = ?, active_release_id = ?, updated_by = ? WHERE setting_key = 'global'")
                ->execute([$targetFlag, $res['release_id'], $adminUserId]);
            self::logHistory($pdo, (int)$res['release_id'], 'rollback', $slot, $prevFlag, $targetFlag, $adminUserId, "Rollback to release #{$releaseId}", null);
        }
        return $res;
    }

    public static function duplicateRelease(PDO $pdo, int $releaseId, string $targetSlot, int $adminUserId): array
    {
        if (!MelvoreDB::tableExists('app_design_releases')) {
            return ['ok' => false, 'errors' => ['app_design_releases table unavailable.']];
        }
        $stmt = $pdo->prepare('SELECT * FROM app_design_releases WHERE release_id = ? LIMIT 1');
        $stmt->execute([$releaseId]);
        $row = $stmt->fetch();
        if (!$row) {
            return ['ok' => false, 'errors' => ['Source release not found.']];
        }
        $decoded = json_decode((string)$row['manifest_json'], true);
        if (!is_array($decoded)) {
            return ['ok' => false, 'errors' => ['Source manifest JSON invalid.']];
        }
        $cleanTarget = ($targetSlot === 'live') ? 'live' : 'default';
        $decoded['slot'] = $cleanTarget;
        $decoded['manifest_id'] = 'melvore_' . $cleanTarget . '_copy_' . date('Ymd_His');
        $decoded['design_version'] = (string)$row['version_tag'] . '-copy';
        $decoded['release_notes'] = "Duplicated from release #{$releaseId} ({$row['slot']} {$row['version_tag']})";
        return self::saveDraft($pdo, $cleanTarget, $decoded, $adminUserId, $decoded['release_notes']);
    }

    public static function compareReleases(PDO $pdo, int $leftReleaseId, int $rightReleaseId): array
    {
        if (!MelvoreDB::tableExists('app_design_releases')) {
            return ['ok' => false, 'errors' => ['app_design_releases unavailable.']];
        }
        $stmt = $pdo->prepare('SELECT * FROM app_design_releases WHERE release_id IN (?, ?)');
        $stmt->execute([$leftReleaseId, $rightReleaseId]);
        $rows = [];
        foreach ($stmt->fetchAll() as $r) {
            $rows[(int)$r['release_id']] = $r;
        }
        if (!isset($rows[$leftReleaseId]) || !isset($rows[$rightReleaseId])) {
            return ['ok' => false, 'errors' => ['One or both releases not found.']];
        }
        $leftJson = json_decode((string)$rows[$leftReleaseId]['manifest_json'], true) ?: [];
        $rightJson = json_decode((string)$rows[$rightReleaseId]['manifest_json'], true) ?: [];

        $diff = [];
        foreach (['tokens', 'navigation', 'components', 'pages', 'worlds'] as $section) {
            $a = json_encode($leftJson[$section] ?? [], JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
            $b = json_encode($rightJson[$section] ?? [], JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
            if ($a !== $b) {
                $diff[$section] = [
                    'changed' => true,
                    'left' => $leftJson[$section] ?? new stdClass(),
                    'right' => $rightJson[$section] ?? new stdClass(),
                ];
            } else {
                $diff[$section] = ['changed' => false];
            }
        }
        return [
            'ok' => true,
            'left' => [
                'release_id' => $leftReleaseId,
                'slot' => $rows[$leftReleaseId]['slot'],
                'version_tag' => $rows[$leftReleaseId]['version_tag'],
            ],
            'right' => [
                'release_id' => $rightReleaseId,
                'slot' => $rows[$rightReleaseId]['slot'],
                'version_tag' => $rows[$rightReleaseId]['version_tag'],
            ],
            'sections' => $diff,
        ];
    }

    public static function listDesignAssets(PDO $pdo): array
    {
        if (!MelvoreDB::tableExists('app_design_assets')) {
            return [];
        }
        $rows = $pdo->query('SELECT * FROM app_design_assets WHERE is_active = 1 ORDER BY asset_type ASC, asset_id DESC')->fetchAll() ?: [];
        foreach ($rows as &$r) {
            $r['url'] = MelvoreResponse::mediaUrl((string)($r['relative_path'] ?? ''));
        }
        return $rows;
    }

    private static function loadPublishedSlotManifest(PDO $pdo, string $slot): ?array
    {
        $clean = ($slot === 'live') ? 'live' : 'default';
        $fileManifest = self::readJsonFile(self::manifestFilePath($clean));
        $dbManifest = null;

        if (MelvoreDB::tableExists('app_design_releases')) {
            $stmt = $pdo->prepare(
                "SELECT manifest_json FROM app_design_releases
                 WHERE slot = ? AND status = 'published'
                 ORDER BY release_id DESC LIMIT 1"
            );
            $stmt->execute([$clean]);
            $raw = $stmt->fetchColumn();
            if (is_string($raw) && $raw !== '') {
                $decoded = json_decode($raw, true);
                if (is_array($decoded)) {
                    $dbManifest = $decoded;
                }
            }
        }

        if ($dbManifest === null) {
            return $fileManifest;
        }
        if ($fileManifest === null) {
            return $dbManifest;
        }

        $fileVersion = (string)($fileManifest['design_version'] ?? '0.0.0');
        $dbVersion = (string)($dbManifest['design_version'] ?? '0.0.0');
        $verCmp = version_compare($fileVersion, $dbVersion);
        if ($verCmp > 0) {
            return $fileManifest;
        }
        if ($verCmp < 0) {
            return $dbManifest;
        }

        // Same version: prefer whichever has richer kit metadata or a newer published_at timestamp
        $filePublished = strtotime((string)($fileManifest['published_at'] ?? '')) ?: 0;
        $dbPublished = strtotime((string)($dbManifest['published_at'] ?? '')) ?: 0;
        if ($filePublished > $dbPublished) {
            return $fileManifest;
        }
        $fileHasKits = isset($fileManifest['worlds']) && is_array($fileManifest['worlds']) && self::manifestHasWorldKits($fileManifest['worlds']);
        $dbHasKits = isset($dbManifest['worlds']) && is_array($dbManifest['worlds']) && self::manifestHasWorldKits($dbManifest['worlds']);
        if ($fileHasKits && !$dbHasKits) {
            return $fileManifest;
        }

        return $dbManifest;
    }

    private static function manifestHasWorldKits(array $worlds): bool
    {
        foreach ($worlds as $w) {
            if (is_array($w) && !empty($w['kit']) && is_array($w['kit'])) {
                return true;
            }
        }
        return false;
    }

    private static function loadLatestDraft(PDO $pdo, string $slot): ?array
    {
        if (!MelvoreDB::tableExists('app_design_releases')) {
            return null;
        }
        $stmt = $pdo->prepare(
            "SELECT manifest_json FROM app_design_releases
             WHERE slot = ? AND status = 'draft'
             ORDER BY release_id DESC LIMIT 1"
        );
        $stmt->execute([$slot === 'live' ? 'live' : 'default']);
        $raw = $stmt->fetchColumn();
        if (is_string($raw) && $raw !== '') {
            $decoded = json_decode($raw, true);
            if (is_array($decoded)) {
                return $decoded;
            }
        }
        return null;
    }

    private static function readJsonFile(string $path): ?array
    {
        if (!is_file($path) || !is_readable($path)) {
            return null;
        }
        $raw = file_get_contents($path);
        if ($raw === false) {
            return null;
        }
        $decoded = json_decode($raw, true);
        return is_array($decoded) ? $decoded : null;
    }

    private static function writeFileAtomic(string $path, string $content): void
    {
        $dir = dirname($path);
        if (!is_dir($dir)) {
            mkdir($dir, 0775, true);
        }
        $tmp = $path . '.tmp.' . bin2hex(random_bytes(4));
        file_put_contents($tmp, $content, LOCK_EX);
        rename($tmp, $path);
    }

    private static function logHistory(
        PDO $pdo,
        ?int $releaseId,
        string $actionType,
        string $slot,
        ?int $prevDesign,
        ?int $newDesign,
        int $actorUserId,
        string $notes,
        ?string $snapshotJson
    ): void {
        if (!MelvoreDB::tableExists('app_design_release_history')) {
            return;
        }
        $stmt = $pdo->prepare(
            'INSERT INTO app_design_release_history
             (release_id, action_type, slot, previous_active_design, new_active_design, actor_user_id, notes, snapshot_json)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([$releaseId, $actionType, $slot, $prevDesign, $newDesign, $actorUserId, $notes, $snapshotJson]);
    }

    private static function recursiveKsort(array &$arr): void
    {
        if (array_is_list($arr)) {
            foreach ($arr as &$v) {
                if (is_array($v)) {
                    self::recursiveKsort($v);
                }
            }
            return;
        }
        ksort($arr);
        foreach ($arr as &$v) {
            if (is_array($v)) {
                self::recursiveKsort($v);
            }
        }
    }

    private static function hardcodedSafetyManifest(): array
    {
        return [
            'manifest_id' => 'melvore_emergency_default',
            'slot' => 'default',
            'design_version' => '1.0.0',
            'schema_version' => self::SUPPORTED_SCHEMA_VERSION,
            'min_app_schema_version' => '1.0.0',
            'tokens' => [
                'colors' => [
                    'mode' => 'light',
                    'background' => '#FFFFFF',
                    'surface' => '#F7F7FA',
                    'surface_elevated' => '#EFEFF5',
                    'surface_variant' => '#F1F1F5',
                    'primary' => '#09090B',
                    'on_primary' => '#FFFFFF',
                    'accent' => '#C9A24B',
                    'secondary_accent' => '#9CA3AF',
                    'text_primary' => '#09090B',
                    'text_secondary' => '#52525B',
                    'border' => '#E4E4E7',
                ],
                'typography' => ['display_font' => 'editorial_serif', 'body_font' => 'clean_sans', 'heading_scale' => 1.0, 'body_scale' => 1.0, 'letter_spacing_eyebrow' => 1.8],
                'spacing' => ['screen_horizontal_dp' => 14, 'section_gap_dp' => 24, 'card_padding_dp' => 10, 'grid_gutter_dp' => 12],
                'radii' => ['card_dp' => 6, 'button_dp' => 4, 'chip_dp' => 999, 'search_bar_dp' => 999, 'hero_dp' => 4, 'sheet_dp' => 16, 'border_width_dp' => 1],
                'motion' => ['intensity' => 'subtle', 'hero_parallax' => true, 'card_entrance' => 'fade_slide', 'carousel_interval_ms' => 4800],
            ],
            'navigation' => [
                'top_bar_style' => 'editorial_masthead',
                'top_bar_title' => 'MELVORE',
                'top_bar_subtitle' => 'FASHION HOUSE',
                'show_search_icon' => true,
                'show_bag_badge' => true,
                'bottom_bar_variant' => 'flat_editorial',
                'bottom_bar_radius_dp' => 0,
                'bottom_bar_elevation_dp' => 0,
                'tabs' => [
                    ['route' => 'home', 'label' => 'Home', 'icon_key' => 'home', 'active_icon_key' => 'home_filled', 'visible' => true, 'order' => 1, 'badge_text' => ''],
                    ['route' => 'discover', 'label' => 'Discover', 'icon_key' => 'explore', 'active_icon_key' => 'explore_filled', 'visible' => true, 'order' => 2, 'badge_text' => ''],
                    ['route' => 'videos', 'label' => 'Reels', 'icon_key' => 'play', 'active_icon_key' => 'play_filled', 'visible' => true, 'order' => 3, 'badge_text' => ''],
                    ['route' => 'elite', 'label' => 'Luxe', 'icon_key' => 'diamond', 'active_icon_key' => 'diamond_filled', 'visible' => true, 'order' => 4, 'badge_text' => ''],
                    ['route' => 'bag', 'label' => 'Bag', 'icon_key' => 'bag', 'active_icon_key' => 'bag_filled', 'visible' => true, 'order' => 5, 'badge_text' => ''],
                ],
            ],
            'components' => [
                'button' => ['primary_variant' => 'editorial_sharp', 'corner_radius_dp' => 4, 'min_height_dp' => 48, 'uppercase_labels' => true, 'show_trailing_arrow' => false, 'border_width_dp' => 1],
                'search_bar' => ['variant' => 'pill_elevated', 'placeholder' => 'Search silhouettes, worlds, or houses…', 'corner_radius_dp' => 999, 'height_dp' => 50, 'leading_icon_key' => 'search', 'show_visual_hint_icon' => true],
                'product_card' => ['variant' => 'editorial_portrait', 'image_aspect_ratio' => 0.78, 'corner_radius_dp' => 6, 'show_brand' => true, 'show_rating' => true, 'show_quick_add' => true, 'badge_position' => 'top_left', 'price_alignment' => 'start'],
                'category_strip' => ['variant' => 'circular_story', 'columns' => 4, 'show_eyebrow' => true, 'tile_radius_dp' => 999],
                'section_header' => ['variant' => 'magazine_serif', 'show_eyebrow' => true, 'show_divider' => false],
                'states' => ['loading_variant' => 'shimmer_editorial', 'empty_variant' => 'framed_editorial', 'error_variant' => 'outlined_retry_card', 'empty_title' => 'Nothing Curated Here Yet', 'empty_subtitle' => 'Explore seasonal drops to continue.', 'error_cta_label' => 'Retry'],
            ],
            'pages' => [
                'home' => ['page_key' => 'home', 'layout_template' => 'editorial_magazine', 'hero_height_dp' => 420, 'grid_columns' => 2, 'sections' => []],
                'style_worlds' => ['page_key' => 'style_worlds', 'layout_template' => 'world_editorial_story', 'hero_height_dp' => 440, 'grid_columns' => 2, 'sections' => []],
                'categories' => ['page_key' => 'categories', 'layout_template' => 'structured_departments', 'hero_height_dp' => 260, 'grid_columns' => 2, 'sections' => []],
                'search' => ['page_key' => 'search', 'layout_template' => 'editorial_finder', 'hero_height_dp' => 180, 'grid_columns' => 2, 'sections' => []],
                'product_details' => ['page_key' => 'product_details', 'layout_template' => 'atelier_product_sheet', 'hero_height_dp' => 460, 'grid_columns' => 2, 'sections' => []],
                'cart' => ['page_key' => 'cart', 'layout_template' => 'clean_tote_summary', 'hero_height_dp' => 0, 'grid_columns' => 1, 'sections' => []],
            ],
            'worlds' => [],
        ];
    }
}
