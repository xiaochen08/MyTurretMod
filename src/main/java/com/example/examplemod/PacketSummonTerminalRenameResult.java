package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
import java.util.UUID;

public class PacketSummonTerminalRenameResult {
    private final BlockPos terminalPos;
    private final UUID turretUuid;
    private final int requestId;
    private final boolean success;
    private final String appliedName;
    private final String errorKey;

    public PacketSummonTerminalRenameResult(BlockPos terminalPos, UUID turretUuid, int requestId, boolean success, String appliedName, String errorKey) {
        this.terminalPos = terminalPos;
        this.turretUuid = turretUuid;
        this.requestId = requestId;
        this.success = success;
        this.appliedName = appliedName;
        this.errorKey = errorKey;
    }

    public static void encode(PacketSummonTerminalRenameResult msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.terminalPos);
        buf.writeUUID(msg.turretUuid);
        buf.writeVarInt(msg.requestId);
        buf.writeBoolean(msg.success);
        buf.writeUtf(msg.appliedName == null ? "" : msg.appliedName, 64);
        buf.writeUtf(msg.errorKey == null ? "" : msg.errorKey, 128);
    }

    public static PacketSummonTerminalRenameResult decode(FriendlyByteBuf buf) {
        return new PacketSummonTerminalRenameResult(
                buf.readBlockPos(),
                buf.readUUID(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readUtf(64),
                buf.readUtf(128)
        );
    }

    public static void handle(PacketSummonTerminalRenameResult msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> handleClient(msg));
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketSummonTerminalRenameResult msg) {
        SummonTerminalClientCache.INSTANCE.handleRenameResult(
                msg.terminalPos,
                msg.turretUuid,
                msg.requestId,
                msg.success,
                msg.appliedName,
                msg.errorKey
        );
    }
}

