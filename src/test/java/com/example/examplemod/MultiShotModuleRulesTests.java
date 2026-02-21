package com.example.examplemod;

import java.util.List;

public class MultiShotModuleRulesTests {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Multi-Shot Module Rules Tests");
        System.out.println("================================================");

        try {
            testArrowCountByLevel();
            testVolleyCountByEnemyNumber();
            testAttackBonusMultiplier();
            testUpgradeMaterialMappingAndNames();
            System.out.println("\nALL MULTI-SHOT RULE TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\nTEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testArrowCountByLevel() {
        int[] expected = {1, 2, 3, 4, 5, 6};
        for (int level = 0; level <= 5; level++) {
            int arrows = MultiShotModuleRules.arrowsForLevel(level);
            if (arrows != expected[level]) {
                throw new RuntimeException("Unexpected arrows for level " + level + ": " + arrows);
            }
        }
        System.out.println("[OK] level -> arrow count mapping");
    }

    static void testVolleyCountByEnemyNumber() {
        int level = 5; // max 6 arrows
        int[] enemies = {0, 1, 2, 4, 6, 10};
        int[] expected = {0, 1, 2, 4, 6, 6};
        for (int i = 0; i < enemies.length; i++) {
            int actual = MultiShotModuleRules.computeVolleyCount(level, enemies[i]);
            if (actual != expected[i]) {
                throw new RuntimeException("Volley mismatch for enemies=" + enemies[i] + ", got " + actual);
            }
        }
        System.out.println("[OK] enemy count limiting and cap logic");
    }

    static void testAttackBonusMultiplier() {
        double base = MultiShotModuleRules.attackBonusMultiplier(1.0, 4.0);
        double boosted = MultiShotModuleRules.attackBonusMultiplier(10.0, 8.0);
        if (Math.abs(base - 1.0) > 1e-9) {
            throw new RuntimeException("Base multiplier should be 1.0");
        }
        if (boosted <= base) {
            throw new RuntimeException("Boosted multiplier should be greater than base");
        }
        System.out.println("[OK] attack damage/speed stacking multiplier");
    }

    static void testUpgradeMaterialMappingAndNames() {
        List<String> expectedMaterials = List.of(
                "minecraft:copper_block",
                "minecraft:iron_block",
                "minecraft:diamond_block",
                "minecraft:blaze_rod",
                "minecraft:ancient_debris"
        );
        List<String> expectedSuffix = List.of("", "I", "II", "III", "IIII", "IIIII");

        for (int level = 0; level < 5; level++) {
            String actual = MultiShotModuleRules.nextUpgradeMaterialId(level);
            if (!expectedMaterials.get(level).equals(actual)) {
                throw new RuntimeException("Upgrade material mismatch at level " + level + ": " + actual);
            }
        }
        for (int level = 0; level <= 5; level++) {
            String actualSuffix = MultiShotModuleRules.levelSuffix(level);
            if (!expectedSuffix.get(level).equals(actualSuffix)) {
                throw new RuntimeException("Level suffix mismatch at level " + level + ": " + actualSuffix);
            }
        }
        System.out.println("[OK] upgrade materials and level suffix naming");
    }
}
