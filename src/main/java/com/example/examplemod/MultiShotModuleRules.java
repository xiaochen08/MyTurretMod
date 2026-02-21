package com.example.examplemod;

public final class MultiShotModuleRules {
    public static final int MAX_LEVEL = 5;
    public static final int MAX_ARROWS = 6;

    private MultiShotModuleRules() {}

    public static int clampLevel(int level) {
        return Math.max(0, Math.min(MAX_LEVEL, level));
    }

    public static int arrowsForLevel(int level) {
        return Math.min(MAX_ARROWS, TurretUpgradeTierPlan.clampLevel(level) + 1);
    }

    public static int computeVolleyCount(int level, int enemyCount) {
        if (enemyCount <= 0) return 0;
        return Math.min(arrowsForLevel(level), enemyCount);
    }

    public static String levelSuffix(int level) {
        return TurretUpgradeTierPlan.suffixForLevel(level);
    }

    public static int levelColor(int level) {
        return switch (clampLevel(level)) {
            case 0 -> 0x9EA7B3;
            case 1 -> 0xC77739;
            case 2 -> 0xD8DEE9;
            case 3 -> 0x4FD1FF;
            case 4 -> 0xFFAA28;
            case 5 -> 0xA63D2E;
            default -> 0xFFFFFF;
        };
    }

    public static String nextUpgradeMaterialId(int currentLevel) {
        return TurretUpgradeTierPlan.nextMaterialByCurrentLevel(currentLevel);
    }

    public static double attackBonusMultiplier(double attackDamage, double attackSpeed) {
        double damageBonus = 1.0 + Math.max(0.0, attackDamage - 1.0) * 0.08;
        double speedBonus = 1.0 + Math.max(0.0, attackSpeed - 4.0) * 0.05;
        return damageBonus * speedBonus;
    }
}
