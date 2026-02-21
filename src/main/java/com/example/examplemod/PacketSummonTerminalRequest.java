package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSummonTerminalRequest {
    private final BlockPos terminalPos;
    private final int page;

    public PacketSummonTerminalRequest(BlockPos terminalPos, int page) {
        this.terminalPos = terminalPos;
        this.page = page;
    }

    public static void encode(PacketSummonTerminalRequest msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.terminalPos);
        buf.writeVarInt(msg.page);
    }

    public static PacketSummonTerminalRequest decode(FriendlyByteBuf buf) {
        return new PacketSummonTerminalRequest(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(PacketSummonTerminalRequest msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (player.level().getBlockState(msg.terminalPos).getBlock() != ExampleMod.SUMMON_TERMINAL_BLOCK.get()) return;
            if (player.distanceToSqr(msg.terminalPos.getX() + 0.5, msg.terminalPos.getY() + 0.5, msg.terminalPos.getZ() + 0.5) > 64.0) return;

            SummonTerminalSnapshot snapshot = SummonTerminalService.buildSnapshot(player, msg.terminalPos, msg.page);
            PacketHandler.INSTANCE.sendTo(new PacketSummonTerminalSnapshot(snapshot), player.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
        });
        ctx.setPacketHandled(true);
    }
}
