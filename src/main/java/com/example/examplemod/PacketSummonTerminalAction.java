package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class PacketSummonTerminalAction {
    public static final int ACTION_RECALL = 0;
    public static final int ACTION_RENAME = 1;

    private final BlockPos terminalPos;
    private final UUID turretUuid;
    private final int page;
    private final int action;
    private final String payload;
    private final int requestId;

    public PacketSummonTerminalAction(BlockPos terminalPos, UUID turretUuid, int page, int action, String payload) {
        this(terminalPos, turretUuid, page, action, payload, 0);
    }

    public PacketSummonTerminalAction(BlockPos terminalPos, UUID turretUuid, int page, int action, String payload, int requestId) {
        this.terminalPos = terminalPos;
        this.turretUuid = turretUuid;
        this.page = page;
        this.action = action;
        this.payload = payload;
        this.requestId = requestId;
    }

    public static void encode(PacketSummonTerminalAction msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.terminalPos);
        buf.writeUUID(msg.turretUuid);
        buf.writeVarInt(msg.page);
        buf.writeVarInt(msg.action);
        buf.writeUtf(msg.payload, 64);
        buf.writeVarInt(msg.requestId);
    }

    public static PacketSummonTerminalAction decode(FriendlyByteBuf buf) {
        return new PacketSummonTerminalAction(
                buf.readBlockPos(),
                buf.readUUID(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(64),
                buf.readVarInt()
        );
    }

    public static void handle(PacketSummonTerminalAction msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (player.level().getBlockState(msg.terminalPos).getBlock() != ExampleMod.SUMMON_TERMINAL_BLOCK.get()) {
                if (msg.action == ACTION_RENAME) {
                    sendRenameResult(player, msg, false, "", "message.examplemod.summon_terminal.rename_terminal_invalid");
                }
                return;
            }
            if (player.distanceToSqr(msg.terminalPos.getX() + 0.5, msg.terminalPos.getY() + 0.5, msg.terminalPos.getZ() + 0.5) > 64.0) {
                if (msg.action == ACTION_RENAME) {
                    sendRenameResult(player, msg, false, "", "message.examplemod.summon_terminal.rename_out_of_range");
                }
                return;
            }

            SkeletonTurret turret = SummonTerminalService.findOwnedTurretByUuid(player, msg.turretUuid);
            if (turret == null) {
                if (msg.action == ACTION_RENAME) {
                    sendRenameResult(player, msg, false, "", "message.examplemod.summon_terminal.rename_target_missing");
                }
                return;
            }

            if (msg.action == ACTION_RECALL) {
                handleRecall(player, turret);
                broadcastDeltaToRelatedClients(player, msg.terminalPos, turret);
            } else if (msg.action == ACTION_RENAME) {
                handleRename(player, msg, turret);
            }

            float distance = (float) Math.sqrt(turret.distanceToSqr(player));
            SummonTerminalEntry updated = SummonTerminalEntry.fromTurret(turret, distance);
            PacketHandler.INSTANCE.sendTo(new PacketSummonTerminalDelta(msg.terminalPos, updated), player.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
        });
        ctx.setPacketHandled(true);
    }

    private static void handleRecall(ServerPlayer player, SkeletonTurret turret) {
        if (!turret.hasTeleportModule()) {
            return;
        }

        Vec3 anchor = SummonTerminalRecallMath.computeAnchor(player.position(), turret.getId());
        turret.teleportToSafeSpotFromTerminal(player);

        turret.getPersistentData().putDouble("LastTerminalRecallX", anchor.x);
        turret.getPersistentData().putDouble("LastTerminalRecallY", anchor.y);
        turret.getPersistentData().putDouble("LastTerminalRecallZ", anchor.z);
    }

    private static void handleRename(ServerPlayer player, PacketSummonTerminalAction msg, SkeletonTurret turret) {
        String sanitized = SkeletonTurret.sanitizeBaseNameInput(msg.payload);
        if (sanitized.isEmpty()) {
            sendRenameResult(player, msg, false, turret.getBaseName(), "message.examplemod.summon_terminal.rename_invalid");
            return;
        }

        if (!turret.applyPlayerBaseName(sanitized)) {
            sendRenameResult(player, msg, false, turret.getBaseName(), "message.examplemod.summon_terminal.rename_rejected");
            return;
        }

        sendRenameResult(player, msg, true, turret.getBaseName(), "");
        // Requester gets a page-stable full refresh.
        pushFullSync(player, msg.terminalPos, msg.page);
        // All viewers currently watching this terminal get the live delta update.
        broadcastDeltaToRelatedClients(player, msg.terminalPos, turret);
    }

    private static void sendRenameResult(ServerPlayer player, PacketSummonTerminalAction msg, boolean success, String appliedName, String errorKey) {
        PacketHandler.INSTANCE.sendTo(
                new PacketSummonTerminalRenameResult(msg.terminalPos, msg.turretUuid, msg.requestId, success, appliedName, errorKey),
                player.connection.connection,
                net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
        );
    }

    private static void pushFullSync(ServerPlayer player, BlockPos terminalPos, int page) {
        for (SummonTerminalEntry entry : SummonTerminalService.buildAllEntries(player)) {
            PacketHandler.INSTANCE.sendTo(
                    new PacketSummonTerminalDelta(terminalPos, entry),
                    player.connection.connection,
                    net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
            );
        }
        SummonTerminalSnapshot snapshot = SummonTerminalService.buildSnapshot(player, terminalPos, page);
        PacketHandler.INSTANCE.sendTo(
                new PacketSummonTerminalSnapshot(snapshot),
                player.connection.connection,
                net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
        );
    }

    private static void broadcastDeltaToRelatedClients(ServerPlayer actor, BlockPos terminalPos, SkeletonTurret turret) {
        List<ServerPlayer> viewers = collectRelatedViewers(actor, terminalPos);
        for (ServerPlayer viewer : viewers) {
            float distance = (float) Math.sqrt(turret.distanceToSqr(viewer));
            SummonTerminalEntry entry = SummonTerminalEntry.fromTurret(turret, distance);
            PacketHandler.INSTANCE.sendTo(
                    new PacketSummonTerminalDelta(terminalPos, entry),
                    viewer.connection.connection,
                    net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
            );
        }
    }

    private static List<ServerPlayer> collectRelatedViewers(ServerPlayer actor, BlockPos terminalPos) {
        List<ServerPlayer> result = new ArrayList<>();
        if (actor.server == null) return result;
        for (ServerPlayer player : actor.server.getPlayerList().getPlayers()) {
            if (player.level() != actor.level()) continue;
            if (!(player.containerMenu instanceof SummonTerminalMenu menu)) continue;
            if (!menu.terminalPos().equals(terminalPos)) continue;
            if (player.distanceToSqr(terminalPos.getX() + 0.5, terminalPos.getY() + 0.5, terminalPos.getZ() + 0.5) > 64.0) continue;
            result.add(player);
        }
        return result;
    }
}
