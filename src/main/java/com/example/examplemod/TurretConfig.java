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
        // Teleport Module Configs
        public final ForgeConfigSpec.IntValue teleportCooldownBase;
        public final ForgeConfigSpec.IntValue teleportCooldownReductionPerTier;
        public final ForgeConfigSpec.IntValue teleportCooldownMin;
        
        // Loot Configs
        public final ForgeConfigSpec.DoubleValue enderPearlDropChanceBase;
        public final ForgeConfigSpec.DoubleValue enderPearlDropChanceBonus;
        
        // Feature Configs
        public final ForgeConfigSpec.BooleanValue enableDeathRecordDrop;
        
        // Fall Death Configs
        public final ForgeConfigSpec.BooleanValue enableFallDeathCapture;
        public final ForgeConfigSpec.DoubleValue fallDeathHeightThreshold;

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
            builder.pop();
            
            builder.push("Features");
            enableDeathRecordDrop = builder
                    .comment("Enable Death Record Card drops for Skeleton Turrets (default: true)")
                    .define("enableDeathRecordDrop", true);

            builder.comment("Fall Death Capture Settings")
                   .push("FallDeath");
            
            enableFallDeathCapture = builder
                    .comment("Enable capturing fall damage deaths for Death Records (default: true)")
                    .define("enableFallDeathCapture", true);

            fallDeathHeightThreshold = builder
                    .comment("Minimum fall height to trigger Death Record capture (default: 8.0 blocks)")
                    .defineInRange("fallDeathHeightThreshold", 8.0, 1.0, 384.0);
            
            builder.pop();
            builder.pop();
        }
    }
}
