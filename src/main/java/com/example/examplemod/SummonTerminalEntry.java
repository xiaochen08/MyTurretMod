package com.example.examplemod;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record SummonTerminalEntry(
        UUID turretUuid,
        int unitId,
        int level,
        String baseName,
        float health,
        float maxHealth,
        int progressPercent,
        float distance,
        boolean hasTeleportModule,
        boolean following
) {
    public static SummonTerminalEntry fromTurret(SkeletonTurret turret, float distance) {
        return new SummonTerminalEntry(
                turret.getUUID(),
                turret.getEntityData().get(SkeletonTurret.UNIT_ID),
                turret.getLevel(),
                turret.getBaseName(),
                turret.getHealth(),
                turret.getMaxHealth(),
                turret.getKillProgressPercent(),
                distance,
                turret.hasTeleportModule(),
                turret.isFollowing()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(turretUuid);
        buf.writeVarInt(unitId);
        buf.writeVarInt(level);
        buf.writeUtf(baseName, 32);
        buf.writeFloat(health);
        buf.writeFloat(maxHealth);
        buf.writeVarInt(progressPercent);
        buf.writeFloat(distance);
        buf.writeBoolean(hasTeleportModule);
        buf.writeBoolean(following);
    }

    public static SummonTerminalEntry decode(FriendlyByteBuf buf) {
        return new SummonTerminalEntry(
                buf.readUUID(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(32),
                buf.readFloat(),
                buf.readFloat(),
                buf.readVarInt(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }
}
