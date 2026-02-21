package com.example.examplemod;

import java.util.List;

public final class TurretUpgradeTierPlan {
    public record CommonTier(int level, String materialId, String suffix, int anvilXpCost, int materialCost) {}

    private static final List<CommonTier> TIERS = List.of(
            new CommonTier(1, "minecraft:copper_block", "I", 2, 1),
            new CommonTier(2, "minecraft:iron_block", "II", 3, 1),
            new CommonTier(3, "minecraft:diamond_block", "III", 4, 1),
            new CommonTier(4, "minecraft:blaze_rod", "IIII", 5, 1),
            new CommonTier(5, "minecraft:ancient_debris", "IIIII", 6, 1)
    );

    private TurretUpgradeTierPlan() {}

    public static int maxLevel() {
        return 5;
    }

    public static int clampLevel(int level) {
        return Math.max(0, Math.min(maxLevel(), level));
    }

    public static CommonTier tierByLevel(int level) {
        int clamped = clampLevel(level);
        if (clamped <= 0) return TIERS.get(0);
        return TIERS.get(clamped - 1);
    }

    public static String suffixForLevel(int level) {
        return level <= 0 ? "" : tierByLevel(level).suffix();
    }

    public static String nextMaterialByCurrentLevel(int currentLevel) {
        int next = clampLevel(currentLevel) + 1;
        if (next > 5) return "";
        return tierByLevel(next).materialId();
    }

    public static int anvilXpCostForCurrentLevel(int currentLevel) {
        int next = clampLevel(currentLevel) + 1;
        if (next > 5) return 0;
        return tierByLevel(next).anvilXpCost();
    }

    public static int materialCostForCurrentLevel(int currentLevel) {
        int next = clampLevel(currentLevel) + 1;
        if (next > 5) return 0;
        return tierByLevel(next).materialCost();
    }
}
