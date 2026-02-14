package com.example.examplemod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class TeleportRequestGateway {
    private static final Map<UUID, Long> PLAYER_COOLDOWN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_COOLDOWN_NOTICE_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_COOLDOWN_NOTICE_SECOND = new ConcurrentHashMap<>();
    private static final int COOLDOWN_NOTICE_THROTTLE_TICKS = 20;
    private static final int NOTICE_CACHE_TTL_TICKS = 20 * 30;

    private TeleportRequestGateway() {}

    public record Verdict(boolean allowed, int remainingTicks, String reason) {}

    public static Verdict checkPlayerCommandCooldown(ServerPlayer player, TeleportRequestSource source) {
        long now = player.serverLevel().getGameTime();
        long cooldownUntil = PLAYER_COOLDOWN_UNTIL.getOrDefault(player.getUUID(), 0L);
        int remain = (int) Math.max(0L, cooldownUntil - now);
        if (remain > 0) {
            TurretModuleLog.warn("teleport denied source={} player={} remainTicks={} ts={}",
                    source, player.getUUID(), remain, Instant.now());
            return new Verdict(false, remain, "cooldown");
        }
        return new Verdict(true, 0, "ok");
    }

    public static void markPlayerCommandTeleport(ServerPlayer player, TeleportRequestSource source) {
        int cooldownTicks = Math.max(0, TurretConfig.COMMON.playerTeleportCommandCooldownTicks.get());
        long now = player.serverLevel().getGameTime();
        PLAYER_COOLDOWN_UNTIL.put(player.getUUID(), now + cooldownTicks);
        TurretModuleLog.info("teleport mark source={} player={} cooldownTicks={} ts={}",
                source, player.getUUID(), cooldownTicks, Instant.now());
    }

    public static void cleanupPlayerCooldownCache(long nowGameTime) {
        PLAYER_COOLDOWN_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= nowGameTime);
        LAST_COOLDOWN_NOTICE_TICK.entrySet().removeIf(entry -> entry.getValue() + NOTICE_CACHE_TTL_TICKS <= nowGameTime);
        LAST_COOLDOWN_NOTICE_SECOND.entrySet().removeIf(entry -> !PLAYER_COOLDOWN_UNTIL.containsKey(entry.getKey()));
    }

    public static boolean isTrackedTeleportCommand(String rawInput) {
        if (rawInput == null) return false;
        String normalized = rawInput.trim();
        if (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (normalized.isEmpty()) return false;
        String head = normalized.split("\\s+")[0].toLowerCase();
        return head.equals("tp") || head.equals("teleport") || head.equals("warp");
    }

    public static boolean guardTurretTeleport(
            SkeletonTurret turret,
            TeleportRequestSource source,
            boolean damageTriggered,
            Supplier<Boolean> teleportAction
    ) {
        if (!turret.hasTeleportModule()) {
            TurretModuleLog.warn("teleport denied source={} turret={} reason=module_missing", source, turret.getUUID());
            return false;
        }
        int remain = turret.getTeleportCooldown();
        if (remain > 0) {
            TurretModuleLog.warn("teleport denied source={} turret={} reason=cooldown remainTicks={}", source, turret.getUUID(), remain);
            return false;
        }

        boolean ok = false;
        try {
            ok = teleportAction.get();
        } catch (Exception e) {
            TurretModuleLog.error("teleport action threw exception source=" + source + " turret=" + turret.getUUID(), e);
        }

        if (!ok) {
            TurretModuleLog.warn("teleport failed source={} turret={} reason=action_failed", source, turret.getUUID());
            return false;
        }

        turret.setTeleportCooldown(turret.getMaxTeleportCooldown());
        TurretModuleLog.info("teleport success source={} turret={} cooldownTicks={} damageTriggered={} ts={}",
                source, turret.getUUID(), turret.getTeleportCooldown(), damageTriggered, Instant.now());
        return true;
    }

    public static void notifyTeleportDeniedToOwner(SkeletonTurret turret, LivingEntity owner, int remainingTicks) {
        if (!(owner instanceof ServerPlayer player)) return;
        int turretNumber = turret.getEntityData().get(SkeletonTurret.UNIT_ID);
        sendUnifiedCooldownNotice(player, turretNumber, remainingTicks);
    }

    public static void notifyPlayerCommandCooldown(ServerPlayer player, int remainingTicks) {
        sendUnifiedCooldownNotice(player, player.getId(), remainingTicks);
    }

    private static void sendUnifiedCooldownNotice(ServerPlayer player, int number, int remainingTicks) {
        if (remainingTicks <= 0) return;
        int remainingSeconds = Math.max(1, (remainingTicks + 19) / 20);

        Integer lastSecond = LAST_COOLDOWN_NOTICE_SECOND.get(player.getUUID());
        if (lastSecond != null && lastSecond == remainingSeconds) {
            return;
        }

        long now = player.serverLevel().getGameTime();
        long lastSent = LAST_COOLDOWN_NOTICE_TICK.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        if (now - lastSent < COOLDOWN_NOTICE_THROTTLE_TICKS) {
            return;
        }
        LAST_COOLDOWN_NOTICE_TICK.put(player.getUUID(), now);
        LAST_COOLDOWN_NOTICE_SECOND.put(player.getUUID(), remainingSeconds);
        String formattedNo = String.format("%03d", Math.floorMod(number, 1000));

        Component message = Component.empty()
                .append(Component.literal("\u7f16\u53f7#" + formattedNo + " ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("\u4f20\u9001\u51b7\u5374\u4e2d\uff0c").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("\u5269\u4f59\u65f6\u95f4" + remainingSeconds + "\u79d2").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.literal("\u3002").withStyle(ChatFormatting.GRAY));

        // Action-bar display keeps chat clean and avoids spammy repeated notices.
        player.displayClientMessage(message, true);
        TurretModuleLog.info("teleport cooldown notice player={} number={} remainTicks={} remainSeconds={}",
                player.getUUID(), formattedNo, remainingTicks, remainingSeconds);
    }
}
