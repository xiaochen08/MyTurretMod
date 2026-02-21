package com.example.examplemod;

import java.nio.file.Files;
import java.nio.file.Paths;

public class TurretKillUpgradeSyncTests {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Turret Kill Upgrade Sync Tests");
        System.out.println("================================================");
        try {
            testAwardKillScoreHookExists();
            testKillIncrementTriggersUpgradeCheck();
            testFriendlyFilterGuardExists();
            testProjectileAttributionNotFakePlayer();
            System.out.println("\nALL KILL-UPGRADE SYNC TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\nTEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testAwardKillScoreHookExists() throws Exception {
        String src = Files.readString(Paths.get("src/main/java/com/example/examplemod/SkeletonTurret.java"));
        assertContains(src, "public void awardKillScore(Entity killedEntity, int scoreValue, DamageSource damageSource)",
                "missing server kill callback hook");
        assertContains(src, "incrementKillCount();", "kill callback must increment kill count");
        System.out.println("[OK] awardKillScore hook coverage");
    }

    static void testKillIncrementTriggersUpgradeCheck() throws Exception {
        String src = Files.readString(Paths.get("src/main/java/com/example/examplemod/SkeletonTurret.java"));
        assertContains(src, "public void incrementKillCount()", "kill count method missing");
        assertContains(src, "checkKillUpgrade();", "incrementKillCount must trigger upgrade check");
        assertContains(src, "performUpgrade(tier + 1);", "upgrade execution path missing");
        System.out.println("[OK] kill->upgrade path coverage");
    }

    static void testFriendlyFilterGuardExists() throws Exception {
        String src = Files.readString(Paths.get("src/main/java/com/example/examplemod/SkeletonTurret.java"));
        assertContains(src, "private boolean shouldCountForUpgrade(LivingEntity target)", "kill filter method missing");
        assertContains(src, "if (target instanceof Player) return false;", "player exclusion missing");
        assertContains(src, "if (target instanceof SkeletonTurret) return false;", "ally turret exclusion missing");
        assertContains(src, "if (target instanceof IronGolem) return false;", "iron golem exclusion missing");
        System.out.println("[OK] friendly filter guard coverage");
    }

    static void testProjectileAttributionNotFakePlayer() throws Exception {
        String src = Files.readString(Paths.get("src/main/java/com/example/examplemod/ExampleMod.java"));
        assertContains(src, "LivingEntity attributedShooter", "projectile attribution variable missing");
        assertContains(src, "damageSources().arrow(arrow, attributedShooter)", "projectile damage should be attributed to shooter");
        assertNotContains(src, "damageSources().arrow(arrow, fakePlayer)", "projectile damage must not be attributed to fake player directly");
        System.out.println("[OK] projectile kill attribution coverage");
    }

    static void assertContains(String content, String expected, String message) {
        if (!content.contains(expected)) {
            throw new RuntimeException(message + ": " + expected);
        }
    }

    static void assertNotContains(String content, String expected, String message) {
        if (content.contains(expected)) {
            throw new RuntimeException(message + ": " + expected);
        }
    }
}
