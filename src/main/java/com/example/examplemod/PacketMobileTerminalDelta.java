package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Delta update payload for incremental UI refresh after the initial snapshot.
 */
public class PacketMobileTerminalDelta {
    private final boolean removed;
    private final UUID turretUuid;
    private final MobileTerminalEntry entry;

    public PacketMobileTerminalDelta(MobileTerminalEntry entry) {
        this(false, entry.turretUuid(), entry);
    }

    public PacketMobileTerminalDelta(boolean removed, UUID turretUuid, MobileTerminalEntry entry) {
        this.removed = removed;
        this.turretUuid = turretUuid;
        this.entry = entry;
    }

    public static void encode(PacketMobileTerminalDelta msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.removed);
        buf.writeUUID(msg.turretUuid);
        buf.writeBoolean(msg.entry != null);
        if (msg.entry != null) {
            msg.entry.encode(buf);
        }
    }

    public static PacketMobileTerminalDelta decode(FriendlyByteBuf buf) {
        boolean removed = buf.readBoolean();
        UUID uuid = buf.readUUID();
        boolean hasEntry = buf.readBoolean();
        MobileTerminalEntry entry = hasEntry ? MobileTerminalEntry.decode(buf) : null;
        return new PacketMobileTerminalDelta(removed, uuid, entry);
    }

    public static void handle(PacketMobileTerminalDelta msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> handleClient(msg));
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketMobileTerminalDelta msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MobileTerminalScreen screen) {
            if (msg.removed) {
                screen.removeEntry(msg.turretUuid);
            } else if (msg.entry != null) {
                screen.applyDelta(msg.entry);
            }
        }
    }
}

