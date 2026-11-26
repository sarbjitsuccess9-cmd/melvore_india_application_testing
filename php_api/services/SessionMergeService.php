<?php
declare(strict_types=1);

require_once dirname(__DIR__) . '/includes/db.php';
require_once __DIR__ . '/CartHelper.php';
require_once __DIR__ . '/RecommendationService.php';

/**
 * Merges guest shopping cart, vibe satisfaction responses, audio/motion preferences,
 * and browsing signals into the authenticated customer account without duplicating responses or rewards.
 */
final class MelvoreSessionMergeService
{
    public static function mergeGuestExperienceToUser(PDO $pdo, int $userId, string $guestId): void
    {
        if ($userId <= 0 || $guestId === '') {
            return;
        }

        // 1. Existing cart merge
        MelvoreCartHelper::mergeGuestCartToUser($pdo, $userId, $guestId);

        // 2. Merge one-time vibe satisfaction feedback without duplicating user submissions or rewards
        if (MelvoreDB::tableExists('app_style_world_feedback')) {
            try {
                $gStmt = $pdo->prepare(
                    'SELECT feedback_id, style_world_id FROM app_style_world_feedback
                     WHERE guest_id = ? AND user_id IS NULL'
                );
                $gStmt->execute([$guestId]);
                $guestFeedbacks = $gStmt->fetchAll();
                foreach ($guestFeedbacks as $gf) {
                    $fid = (int)$gf['feedback_id'];
                    $wid = (int)$gf['style_world_id'];

                    $uChk = $pdo->prepare(
                        'SELECT feedback_id FROM app_style_world_feedback WHERE user_id = ? AND style_world_id = ? LIMIT 1'
                    );
                    $uChk->execute([$userId, $wid]);
                    if ($uChk->fetchColumn()) {
                        // User already answered this world on their account; drop orphan guest link so unique constraint never collides
                        $pdo->prepare('DELETE FROM app_style_world_feedback WHERE feedback_id = ?')->execute([$fid]);
                    } else {
                        $pdo->prepare('UPDATE app_style_world_feedback SET user_id = ?, guest_id = NULL WHERE feedback_id = ?')
                            ->execute([$userId, $fid]);
                    }
                }
            } catch (Throwable $e) {
                error_log('[Melvore][SessionMerge::feedback] ' . $e->getMessage());
            }
        }

        // 3. Merge user audio/motion/video preferences (only initialize from guest if user has no saved record yet)
        if (MelvoreDB::tableExists('app_user_preferences')) {
            try {
                $uPrefChk = $pdo->prepare('SELECT pref_id FROM app_user_preferences WHERE user_id = ? LIMIT 1');
                $uPrefChk->execute([$userId]);
                $hasUserPref = (bool)$uPrefChk->fetchColumn();

                $gPrefStmt = $pdo->prepare('SELECT * FROM app_user_preferences WHERE guest_id = ? ORDER BY updated_at DESC LIMIT 1');
                $gPrefStmt->execute([$guestId]);
                $gPref = $gPrefStmt->fetch();

                if ($gPref) {
                    if (!$hasUserPref) {
                        $hasVideoAuto = MelvoreDB::columnExists('app_user_preferences', 'video_autoplay_muted');
                        $hasDataSaver = MelvoreDB::columnExists('app_user_preferences', 'data_saver_mode');
                        if ($hasVideoAuto && $hasDataSaver) {
                            $pdo->prepare(
                                'INSERT INTO app_user_preferences
                                    (user_id, guest_id, ambient_audio_enabled, ui_sfx_enabled, reduced_motion, video_autoplay_muted, data_saver_mode, audio_consent_answered, preferred_world_slug)
                                 VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?)
                                 ON DUPLICATE KEY UPDATE updated_at = NOW()'
                            )->execute([
                                $userId,
                                (int)($gPref['ambient_audio_enabled'] ?? 0),
                                (int)($gPref['ui_sfx_enabled'] ?? 0),
                                (int)($gPref['reduced_motion'] ?? 0),
                                (int)($gPref['video_autoplay_muted'] ?? 1),
                                (int)($gPref['data_saver_mode'] ?? 0),
                                (int)($gPref['audio_consent_answered'] ?? 0),
                                $gPref['preferred_world_slug'] ?? null,
                            ]);
                        } else {
                            $pdo->prepare(
                                'INSERT INTO app_user_preferences
                                    (user_id, guest_id, ambient_audio_enabled, ui_sfx_enabled, reduced_motion, audio_consent_answered, preferred_world_slug)
                                 VALUES (?, NULL, ?, ?, ?, ?, ?)
                                 ON DUPLICATE KEY UPDATE updated_at = NOW()'
                            )->execute([
                                $userId,
                                (int)($gPref['ambient_audio_enabled'] ?? 0),
                                (int)($gPref['ui_sfx_enabled'] ?? 0),
                                (int)($gPref['reduced_motion'] ?? 0),
                                (int)($gPref['audio_consent_answered'] ?? 0),
                                $gPref['preferred_world_slug'] ?? null,
                            ]);
                        }
                    }
                }
            } catch (Throwable $e) {
                error_log('[Melvore][SessionMerge::preferences] ' . $e->getMessage());
            }
        }

        // 4. Attach guest personalization signals and brand views to the authenticated account
        if (MelvoreDB::tableExists('app_user_style_signals')) {
            try {
                $pdo->prepare('UPDATE app_user_style_signals SET user_id = ? WHERE guest_id = ? AND user_id IS NULL')
                    ->execute([$userId, $guestId]);
            } catch (Throwable $e) {
                error_log('[Melvore][SessionMerge::signals] ' . $e->getMessage());
            }
        }
        if (MelvoreDB::tableExists('app_user_brand_activity')) {
            try {
                $pdo->prepare('UPDATE app_user_brand_activity SET user_id = ? WHERE guest_id = ? AND user_id IS NULL')
                    ->execute([$userId, $guestId]);
            } catch (Throwable $e) {
                error_log('[Melvore][SessionMerge::brandActivity] ' . $e->getMessage());
            }
        }

        MelvoreRecommendationService::invalidateForActor($pdo, $userId, $guestId);
    }
}
