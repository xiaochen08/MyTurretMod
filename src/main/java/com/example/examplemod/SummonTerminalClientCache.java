package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SummonTerminalClientCache {
    public static final SummonTerminalClientCache INSTANCE = new SummonTerminalClientCache();

    private final Map<UUID, SummonTerminalEntry> entries = new LinkedHashMap<>();
    private final Map<UUID, PendingRename> pendingRenames = new LinkedHashMap<>();
    private SummonTerminalSnapshot latest;
    private long renameSyncUntilMs;
    private BlockPos deferredPos;
    private boolean deferredDeltaOnly;
    private String pendingRenameErrorKey;

    private record PendingRename(BlockPos terminalPos, int requestId, String previousName) {}

    private SummonTerminalClientCache() {}

    public void applySnapshot(SummonTerminalSnapshot snapshot) {
        latest = snapshot;
        for (SummonTerminalEntry e : snapshot.entries()) {
            upsertEntry(e);
        }
        renameSyncUntilMs = 0L;
        notifyScreen(snapshot.terminalPos(), false);
    }

    public void applyDelta(BlockPos pos, SummonTerminalEntry entry) {
        upsertEntry(entry);
        notifyScreen(pos, true);
    }

    private void upsertEntry(SummonTerminalEntry e) {
        entries.remove(e.turretUuid());
        entries.put(e.turretUuid(), e);
        while (entries.size() > SummonTerminalService.MAX_CACHE) {
            UUID first = entries.keySet().iterator().next();
            entries.remove(first);
        }
    }

    private void notifyScreen(BlockPos pos, boolean deltaOnly) {
        Minecraft mc = Minecraft.getInstance();
        if (isInCombat(mc)) {
            deferredPos = pos;
            deferredDeltaOnly = deltaOnly;
            return;
        }
        if (mc.screen instanceof SummonTerminalScreen screen) {
            if (screen.isFor(pos)) {
                if (deltaOnly) {
                    screen.applyDelta(entries);
                } else if (latest != null) {
                    screen.applySnapshot(latest, entries);
                }
            }
        }
    }

    public void markRenameSyncStart() {
        renameSyncUntilMs = System.currentTimeMillis() + 2500L;
    }

    public void markRenameSyncStart(BlockPos pos, UUID turretUuid, int requestId, String previousName, String requestedName) {
        markRenameSyncStart();
        pendingRenames.put(turretUuid, new PendingRename(pos, requestId, previousName));
        SummonTerminalEntry old = entries.get(turretUuid);
        if (old != null) {
            SummonTerminalEntry optimistic = new SummonTerminalEntry(
                    old.turretUuid(),
                    old.unitId(),
                    old.level(),
                    requestedName,
                    old.health(),
                    old.maxHealth(),
                    old.progressPercent(),
                    old.distance(),
                    old.hasTeleportModule(),
                    old.following()
            );
            upsertEntry(optimistic);
            notifyScreen(pos, true);
        }
    }

    public void handleRenameResult(BlockPos pos, UUID turretUuid, int requestId, boolean success, String appliedName, String errorKey) {
        PendingRename pending = pendingRenames.get(turretUuid);
        if (pending != null && requestId < pending.requestId) {
            // Stale response arrives after a newer rename request; ignore.
            return;
        }

        if (pending != null) {
            pendingRenames.remove(turretUuid);
        }
        renameSyncUntilMs = 0L;

        if (success) {
            SummonTerminalEntry old = entries.get(turretUuid);
            if (old != null && appliedName != null && !appliedName.isBlank()) {
                SummonTerminalEntry updated = new SummonTerminalEntry(
                        old.turretUuid(),
                        old.unitId(),
                        old.level(),
                        appliedName,
                        old.health(),
                        old.maxHealth(),
                        old.progressPercent(),
                        old.distance(),
                        old.hasTeleportModule(),
                        old.following()
                );
                upsertEntry(updated);
            }
            notifyScreen(pos, true);
            return;
        }

        if (pending != null) {
            SummonTerminalEntry old = entries.get(turretUuid);
            if (old != null) {
                SummonTerminalEntry rollback = new SummonTerminalEntry(
                        old.turretUuid(),
                        old.unitId(),
                        old.level(),
                        pending.previousName(),
                        old.health(),
                        old.maxHealth(),
                        old.progressPercent(),
                        old.distance(),
                        old.hasTeleportModule(),
                        old.following()
                );
                upsertEntry(rollback);
            }
        }

        pendingRenameErrorKey = (errorKey == null || errorKey.isBlank())
                ? "message.examplemod.summon_terminal.rename_sync_failed"
                : errorKey;
        notifyScreen(pos, true);
    }

    public String consumeRenameErrorKey() {
        String key = pendingRenameErrorKey;
        pendingRenameErrorKey = null;
        return key;
    }

    public boolean isRenameSyncActive() {
        return renameSyncUntilMs > System.currentTimeMillis();
    }

    public int renameSpinnerFrame() {
        return (int) ((System.currentTimeMillis() / 250L) % 4L);
    }

    public void clientTick() {
        if (deferredPos == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (isInCombat(mc)) return;
        BlockPos pos = deferredPos;
        boolean delta = deferredDeltaOnly;
        deferredPos = null;
        deferredDeltaOnly = false;
        notifyScreen(pos, delta);
    }

    private static boolean isInCombat(Minecraft mc) {
        return mc.player != null && mc.player.hurtTime > 0;
    }

    public SummonTerminalSnapshot latest() {
        return latest;
    }

    public List<SummonTerminalEntry> allCached() {
        return new ArrayList<>(entries.values());
    }
}
