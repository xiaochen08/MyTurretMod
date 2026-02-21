package com.example.examplemod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSummonTerminalSnapshot {
    private final SummonTerminalSnapshot snapshot;

    public PacketSummonTerminalSnapshot(SummonTerminalSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public static void encode(PacketSummonTerminalSnapshot msg, FriendlyByteBuf buf) {
        msg.snapshot.encode(buf);
    }

    public static PacketSummonTerminalSnapshot decode(FriendlyByteBuf buf) {
        return new PacketSummonTerminalSnapshot(SummonTerminalSnapshot.decode(buf));
    }

    public static void handle(PacketSummonTerminalSnapshot msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> handleClient(msg));
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketSummonTerminalSnapshot msg) {
        SummonTerminalClientCache.INSTANCE.applySnapshot(msg.snapshot);
    }
}
