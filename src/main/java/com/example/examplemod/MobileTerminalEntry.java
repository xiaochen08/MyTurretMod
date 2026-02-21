package com.example.examplemod;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * Client-facing skeleton unit snapshot used by the mobile terminal UI.
 */
public record MobileTerminalEntry(
        UUID turretUuid,
        int unitId,
        String shortUuid,
        String displayName,
        int level,
        float health,
        float maxHealth,
        float attack,
        boolean following,
        boolean captain,
        boolean squadMember,
        int killCount,
        int armor,
        int heat,
        float fireRate,
        float attackRange,
        boolean hasTeleportModule,
        int teleportModuleLevel,
        int teleportCooldownTicks,
        int progressPercent,
        float distance
) {
    public static MobileTerminalEntry fromTurret(SkeletonTurret turret, float distance) {
        UUID uuid = turret.getUUID();
        String baseName = turret.getBaseName();
        String display = (baseName == null || baseName.isBlank())
                ? SkeletonTurret.DEFAULT_BASE_NAME_TOKEN
                : baseName;
        return new MobileTerminalEntry(
                uuid,
                turret.getEntityData().get(SkeletonTurret.UNIT_ID),
                shortUuid(uuid),
                display,
                turret.getLevel(),
                turret.getHealth(),
                turret.getMaxHealth(),
                (float) turret.getWeaponDamage(),
                turret.isFollowing(),
                turret.isCaptain(),
                turret.isSquadMember(),
                turret.getKillCount(),
                turret.getArmorValue(),
                turret.getHeat(),
                turret.getFireRate(),
                (float) turret.getAttackRange(),
                turret.hasTeleportModule(),
                turret.getTeleportModuleLevel(),
                turret.getTeleportCooldown(),
                turret.getKillProgressPercent(),
                distance
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(turretUuid);
        buf.writeVarInt(unitId);
        buf.writeUtf(shortUuid, 8);
        buf.writeUtf(displayName, 48);
        buf.writeVarInt(level);
        buf.writeFloat(health);
        buf.writeFloat(maxHealth);
        buf.writeFloat(attack);
        buf.writeBoolean(following);
        buf.writeBoolean(captain);
        buf.writeBoolean(squadMember);
        buf.writeVarInt(killCount);
        buf.writeVarInt(armor);
        buf.writeVarInt(heat);
        buf.writeFloat(fireRate);
        buf.writeFloat(attackRange);
        buf.writeBoolean(hasTeleportModule);
        buf.writeVarInt(teleportModuleLevel);
        buf.writeVarInt(teleportCooldownTicks);
        buf.writeVarInt(progressPercent);
        buf.writeFloat(distance);
    }

    public static MobileTerminalEntry decode(FriendlyByteBuf buf) {
        return new MobileTerminalEntry(
                buf.readUUID(),
                buf.readVarInt(),
                buf.readUtf(8),
                buf.readUtf(48),
                buf.readVarInt(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readFloat()
        );
    }

    private static String shortUuid(UUID uuid) {
        String raw = uuid.toString().replace("-", "");
        return raw.substring(0, Math.min(4, raw.length())).toUpperCase();
    }
}
