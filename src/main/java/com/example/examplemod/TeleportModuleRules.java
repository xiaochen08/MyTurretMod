package com.example.examplemod;

import java.util.List;

public final class TeleportModuleRules {
    public record TierConfig(
            int levelId,
            List<String> upgradeMaterials,
            String nameSuffix,
            int teleportCooldownSeconds,
            double blackHoleRange,
            int blackHoleCooldownSeconds,
            boolean friendlyFilterEnabled
    ) {}

    private static final List<TierConfig> TIERS = List.of(
            new TierConfig(1, List.of(TurretUpgradeTierPlan.tierByLevel(1).materialId()), "闪现I", 10, 0.0, 0, false),
            new TierConfig(2, List.of(TurretUpgradeTierPlan.tierByLevel(2).materialId()), "闪现II", 8, 0.0, 0, false),
            new TierConfig(3, List.of(TurretUpgradeTierPlan.tierByLevel(3).materialId()), "闪现III", 6, 6.0, 15, true),
            new TierConfig(4, List.of(TurretUpgradeTierPlan.tierByLevel(4).materialId()), "闪现IV", 3, 15.0, 12, true),
            new TierConfig(5, List.of(TurretUpgradeTierPlan.tierByLevel(5).materialId()), "闪现V", 1, 30.0, 6, true)
    );

    private TeleportModuleRules() {}

    public static int clampLevel(int level) {
        return TurretUpgradeTierPlan.clampLevel(level);
    }

    public static TierConfig configByLevel(int level) {
        int clamped = clampLevel(level);
        if (clamped <= 0) return TIERS.get(0);
        return TIERS.get(clamped - 1);
    }

    public static int cooldownTicksForLevel(int level) {
        if (level <= 0) return 0;
        return configByLevel(level).teleportCooldownSeconds() * 20;
    }

    public static int blackHoleCooldownTicksForLevel(int level) {
        if (level <= 0) return 0;
        return configByLevel(level).blackHoleCooldownSeconds() * 20;
    }

    public static double blackHoleRangeForLevel(int level) {
        if (level <= 0) return 0.0;
        return configByLevel(level).blackHoleRange();
    }

    public static String nameSuffix(int level) {
        if (level <= 0) return "";
        return configByLevel(level).nameSuffix();
    }
}
