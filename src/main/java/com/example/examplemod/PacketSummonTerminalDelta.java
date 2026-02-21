package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSummonTerminalDelta {
    private final BlockPos terminalPos;
    private final SummonTerminalEntry entry;

    public PacketSummonTerminalDelta(BlockPos terminalPos, SummonTerminalEntry entry) {
        this.terminalPos = terminalPos;
        this.entry = entry;
    }

    public static void encode(PacketSummonTerminalDelta msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.terminalPos);
        msg.entry.encode(buf);
    }

    public static PacketSummonTerminalDelta decode(FriendlyByteBuf buf) {
        return new PacketSummonTerminalDelta(buf.readBlockPos(), SummonTerminalEntry.decode(buf));
    }

    public static void handle(PacketSummonTerminalDelta msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> handleClient(msg));
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketSummonTerminalDelta msg) {
        SummonTerminalClientCache.INSTANCE.applyDelta(msg.terminalPos, msg.entry);
    }
}
