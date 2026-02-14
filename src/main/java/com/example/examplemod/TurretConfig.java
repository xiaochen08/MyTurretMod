package com.example.examplemod;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class TurretConfig {
    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public enum DisplayMode {
        TRADITIONAL("traditional"),
        INFO_BAR("info_bar");

        private final String value;

        DisplayMode(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static DisplayMode fromConfigValue(String value) {
            if (TRADITIONAL.value.equalsIgnoreCase(value)) {
                return TRADITIONAL;
            }
            return INFO_BAR;
        }
    }

    public static DisplayMode getDisplayMode() {
        return DisplayMode.fromConfigValue(COMMON.hudPromptDisplayMode.get());
    }

    public static void setDisplayMode(DisplayMode mode) {
        COMMON.hudPromptDisplayMode.set(mode.value());
    }

    public static class Common {
        public final ForgeConfigSpec.IntValue teleportCooldownBase;
        public final ForgeConfigSpec.IntValue teleportCooldownReductionPerTier;
        public final ForgeConfigSpec.IntValue teleportCooldownMin;
        public final ForgeConfigSpec.DoubleValue teleportCooldownScale;
        public final ForgeConfigSpec.DoubleValue blackHoleRangeScale;
        public final ForgeConfigSpec.DoubleValue blackHoleCooldownScale;
        public final ForgeConfigSpec.IntValue blackHoleEntityScanCap;
        public final ForgeConfigSpec.IntValue playerTeleportCommandCooldownTicks;
        public final ForgeConfigSpec.IntValue teleportCooldownCacheCleanupIntervalTicks;
        public final ForgeConfigSpec.IntValue moduleStateResyncIntervalTicks;
        public final ForgeConfigSpec.DoubleValue multiShotRange;
        public final ForgeConfigSpec.DoubleValue multiShotDamageScalePerAttackDamage;
        public final ForgeConfigSpec.DoubleValue multiShotSpeedScalePerAttackSpeed;
        public final ForgeConfigSpec.BooleanValue moduleVerboseLog;

        public final ForgeConfigSpec.DoubleValue enderPearlDropChanceBase;
        public final ForgeConfigSpec.DoubleValue enderPearlDropChanceBonus;
        public final ForgeConfigSpec.DoubleValue deathPlaqueDropChance;
        public final ForgeConfigSpec.BooleanValue enableDeathPlaqueGc;
        public final ForgeConfigSpec.IntValue deathPlaqueItemTtlSeconds;
        public final ForgeConfigSpec.BooleanValue enableDeathRecordDrop;
        public final ForgeConfigSpec.BooleanValue enableFallDeathCapture;
        public final ForgeConfigSpec.IntValue fallDeathHeightThreshold;
        public final ForgeConfigSpec.ConfigValue<String> hudPromptDisplayMode;

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

            teleportCooldownScale = builder
                    .comment("Global multiplier for turret teleport cooldown. 1.0 keeps tier table unchanged.")
                    .defineInRange("teleportCooldownScale", 1.0, 0.1, 5.0);

            blackHoleRangeScale = builder
                    .comment("Global multiplier for turret black-hole range.")
                    .defineInRange("blackHoleRangeScale", 1.0, 0.1, 5.0);

            blackHoleCooldownScale = builder
                    .comment("Global multiplier for turret black-hole cooldown.")
                    .defineInRange("blackHoleCooldownScale", 1.0, 0.1, 5.0);

            blackHoleEntityScanCap = builder
                    .comment("Max number of entities scanned per black-hole tick.")
                    .defineInRange("blackHoleEntityScanCap", 64, 8, 256);

            playerTeleportCommandCooldownTicks = builder
                    .comment("Cooldown for player teleport commands (tp/teleport/warp), in ticks.")
                    .defineInRange("playerTeleportCommandCooldownTicks", 40, 0, 2400);

            teleportCooldownCacheCleanupIntervalTicks = builder
                    .comment("Cleanup interval for teleport cooldown cache entries.")
                    .defineInRange("teleportCooldownCacheCleanupIntervalTicks", 200, 20, 24000);
            builder.pop();

            builder.push("UpgradeModuleSystem");
            moduleStateResyncIntervalTicks = builder
                    .comment("Periodic module state resync interval in ticks")
                    .defineInRange("moduleStateResyncIntervalTicks", 40, 10, 400);

            multiShotRange = builder
                    .comment("Multi-shot target acquisition range")
                    .defineInRange("multiShotRange", 24.0, 6.0, 128.0);

            multiShotDamageScalePerAttackDamage = builder
                    .comment("Extra arrow damage scale per attack damage point above baseline")
                    .defineInRange("multiShotDamageScalePerAttackDamage", 0.08, 0.0, 1.0);

            multiShotSpeedScalePerAttackSpeed = builder
                    .comment("Extra arrow speed scale per attack speed point above baseline")
                    .defineInRange("multiShotSpeedScalePerAttackSpeed", 0.05, 0.0, 1.0);

            moduleVerboseLog = builder
                    .comment("Enable verbose logging for turret upgrade module system")
                    .define("moduleVerboseLog", false);
            builder.pop();

            builder.push("DeathRecord");
            enableDeathRecordDrop = builder
                    .comment("Enable death record drops for turret fatal damage")
                    .define("enableDeathRecordDrop", true);

            enableFallDeathCapture = builder
                    .comment("Enable fall-death capture for turrets")
                    .define("enableFallDeathCapture", true);

            fallDeathHeightThreshold = builder
                    .comment("Min fall height to trigger death capture")
                    .defineInRange("fallDeathHeightThreshold", 30, 5, 512);

            hudPromptDisplayMode = builder
                    .comment("Prompt display mode for turret screen: traditional | info_bar")
                    .defineInList("hudPromptDisplayMode", DisplayMode.INFO_BAR.value(), List.of(DisplayMode.TRADITIONAL.value(), DisplayMode.INFO_BAR.value()));
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
