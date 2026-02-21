package com.example.examplemod;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-side helpers for mobile terminal data and target resolution.
 */
public final class MobileTerminalService {
    private MobileTerminalService() {}

    public static List<MobileTerminalEntry> buildEntries(ServerPlayer player) {
        List<MobileTerminalEntry> entries = new ArrayList<>();
        for (SkeletonTurret turret : SummonTerminalService.findOwnedTurrets(player)) {
            float distance = (float) Math.sqrt(turret.distanceToSqr(player));
            entries.add(MobileTerminalEntry.fromTurret(turret, distance));
        }
        return entries;
    }

    public static SkeletonTurret findOwnedByUuid(ServerPlayer player, UUID uuid) {
        return SummonTerminalService.findOwnedTurretByUuid(player, uuid);
    }
}
