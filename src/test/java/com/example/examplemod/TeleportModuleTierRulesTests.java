package com.example.examplemod;

public class TeleportModuleTierRulesTests {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Teleport Module Tier Rules Tests");
        System.out.println("================================================");

        try {
            testTierDataTable();
            testMaterialAndSuffixParityWithMultiShot();
            System.out.println("\nALL TELEPORT MODULE TIER TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\nTEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testTierDataTable() {
        int[] teleportCd = {10, 8, 6, 3, 1};
        double[] ranges = {0, 0, 6, 15, 30};
        int[] blackHoleCd = {0, 0, 15, 12, 6};
        boolean[] filter = {false, false, true, true, true};

        for (int level = 1; level <= 5; level++) {
            TeleportModuleRules.TierConfig cfg = TeleportModuleRules.configByLevel(level);
            if (cfg.levelId() != level) throw new RuntimeException("Level id mismatch at " + level);
            if (cfg.teleportCooldownSeconds() != teleportCd[level - 1]) throw new RuntimeException("Teleport cooldown mismatch at " + level);
            if (Math.abs(cfg.blackHoleRange() - ranges[level - 1]) > 1e-9) throw new RuntimeException("Blackhole range mismatch at " + level);
            if (cfg.blackHoleCooldownSeconds() != blackHoleCd[level - 1]) throw new RuntimeException("Blackhole cooldown mismatch at " + level);
            if (cfg.friendlyFilterEnabled() != filter[level - 1]) throw new RuntimeException("Friendly filter mismatch at " + level);
        }
        System.out.println("[OK] teleport tier data table");
    }

    static void testMaterialAndSuffixParityWithMultiShot() {
        for (int currentLevel = 0; currentLevel < 5; currentLevel++) {
            String multi = MultiShotModuleRules.nextUpgradeMaterialId(currentLevel);
            String tele = TurretUpgradeTierPlan.nextMaterialByCurrentLevel(currentLevel);
            if (!multi.equals(tele)) throw new RuntimeException("Material parity broken at currentLevel=" + currentLevel);
        }
        for (int level = 1; level <= 5; level++) {
            String suffix = MultiShotModuleRules.levelSuffix(level);
            if (suffix.isEmpty()) throw new RuntimeException("Missing suffix at level " + level);
        }
        System.out.println("[OK] parity with multishot material/suffix plan");
    }
}
