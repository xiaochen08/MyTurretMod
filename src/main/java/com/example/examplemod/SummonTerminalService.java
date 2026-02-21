package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class SummonTerminalService {
    public static final int MAX_CACHE = 256;
    public static final int PAGE_SIZE = 8;

    private SummonTerminalService() {}

    public static SummonTerminalSnapshot buildSnapshot(ServerPlayer player, BlockPos pos, int page) {
        List<SkeletonTurret> owned = findOwnedTurrets(player);
        int totalCount = Math.min(MAX_CACHE, owned.size());
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) PAGE_SIZE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));

        int start = safePage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, totalCount);

        List<SummonTerminalEntry> entries = new ArrayList<>();
        for (int i = start; i < end; i++) {
            SkeletonTurret turret = owned.get(i);
            float distance = (float) Math.sqrt(turret.distanceToSqr(player));
            entries.add(SummonTerminalEntry.fromTurret(turret, distance));
        }

        return new SummonTerminalSnapshot(pos, safePage, totalPages, totalCount, entries);
    }

    public static List<SkeletonTurret> findOwnedTurrets(ServerPlayer player) {
        List<SkeletonTurret> list = new ArrayList<>();
        UUID owner = player.getUUID();
        for (Entity entity : player.serverLevel().getAllEntities()) {
            if (!(entity instanceof SkeletonTurret turret)) continue;
            if (turret.getOwnerUUID() == null || !owner.equals(turret.getOwnerUUID())) continue;
            list.add(turret);
        }
        list.sort(Comparator.comparingInt(t -> t.getEntityData().get(SkeletonTurret.UNIT_ID)));
        if (list.size() > MAX_CACHE) {
            return new ArrayList<>(list.subList(0, MAX_CACHE));
        }
        return list;
    }

    public static List<SummonTerminalEntry> buildAllEntries(ServerPlayer player) {
        List<SummonTerminalEntry> entries = new ArrayList<>();
        for (SkeletonTurret turret : findOwnedTurrets(player)) {
            float distance = (float) Math.sqrt(turret.distanceToSqr(player));
            entries.add(SummonTerminalEntry.fromTurret(turret, distance));
        }
        return entries;
    }

    public static SkeletonTurret findOwnedTurretByUuid(ServerPlayer player, UUID uuid) {
        UUID owner = player.getUUID();
        for (Entity entity : player.serverLevel().getAllEntities()) {
            if (!(entity instanceof SkeletonTurret turret)) continue;
            if (!uuid.equals(turret.getUUID())) continue;
            if (turret.getOwnerUUID() == null || !owner.equals(turret.getOwnerUUID())) continue;
            return turret;
        }
        return null;
    }
}
