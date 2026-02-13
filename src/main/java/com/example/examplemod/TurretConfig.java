package com.example.examplemod;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class TurretConfig {
    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public static class Common {
        public final ForgeConfigSpec.IntValue teleportCooldownBase;
        public final ForgeConfigSpec.IntValue teleportCooldownReductionPerTier;
        public final ForgeConfigSpec.IntValue teleportCooldownMin;

        public final ForgeConfigSpec.DoubleValue enderPearlDropChanceBase;
        public final ForgeConfigSpec.DoubleValue enderPearlDropChanceBonus;
        public final ForgeConfigSpec.DoubleValue deathPlaqueDropChance;
        public final ForgeConfigSpec.BooleanValue enableDeathPlaqueGc;
        public final ForgeConfigSpec.IntValue deathPlaqueItemTtlSeconds;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("TeleportModule");
            teleportCooldownBase = builder
                    .comment("Base cooldown for teleportation in ticks (default: 60 ticks = 3 seconds)")
                    .defineInRange("teleportCooldownBase", 60, 20, 1200);

            teleportCooldownReductionPerTier = builder
                    .comment("Cooldown reduction per tier in ticks (default: 10 ticks)")
                    .defineInRange("teleportCooldownReductionPerTier", 10, 0, 50);

            teleportCooldownMin = builder
                    .comment("Minimum cooldown for teleportation in ticks (default: 10 ticks = 0.5 seconds)")
                    .defineInRange("teleportCooldownMin", 10, 5, 100);
            builder.pop();

            builder.push("Loot");
            enderPearlDropChanceBase = builder
                    .comment("Base chance for hostile mobs to drop Ender Pearls (0.0 - 1.0, default: 0.05 = 5%)")
                    .defineInRange("enderPearlDropChanceBase", 0.05, 0.0, 1.0);

            enderPearlDropChanceBonus = builder
                    .comment("Max random bonus chance for Ender Pearl drops (0.0 - 1.0, default: 0.10 = 10%)")
                    .defineInRange("enderPearlDropChanceBonus", 0.10, 0.0, 1.0);

            deathPlaqueDropChance = builder
                    .comment("Chance for SkeletonTurret to drop death plaque on fatal damage for first two deaths")
                    .defineInRange("deathPlaqueDropChance", 0.65, 0.0, 1.0);

            enableDeathPlaqueGc = builder
                    .comment("Enable server-side GC for dropped death plaque item entities")
                    .define("enableDeathPlaqueGc", true);

            deathPlaqueItemTtlSeconds = builder
                    .comment("TTL for dropped death plaque item entities in seconds before cleanup")
                    .defineInRange("deathPlaqueItemTtlSeconds", 900, 60, 86400);
            builder.pop();
        }
    }
}
