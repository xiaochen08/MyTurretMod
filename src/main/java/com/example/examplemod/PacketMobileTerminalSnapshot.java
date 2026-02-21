package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Full snapshot payload sent once when the mobile terminal opens.
 */
public class PacketMobileTerminalSnapshot {
    private final List<MobileTerminalEntry> entries;

    public PacketMobileTerminalSnapshot(List<MobileTerminalEntry> entries) {
        this.entries = entries;
    }

    public static void encode(PacketMobileTerminalSnapshot msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entries.size());
        for (MobileTerminalEntry entry : msg.entries) {
            entry.encode(buf);
        }
    }

    public static PacketMobileTerminalSnapshot decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<MobileTerminalEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(MobileTerminalEntry.decode(buf));
        }
        return new PacketMobileTerminalSnapshot(entries);
    }

    public static void handle(PacketMobileTerminalSnapshot msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> handleClient(msg));
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketMobileTerminalSnapshot msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MobileTerminalScreen screen) {
            screen.applySnapshot(msg.entries);
        }
    }
}

