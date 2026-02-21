package com.example.examplemod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Requests one full snapshot when the mobile terminal UI is opened.
 */
public class PacketMobileTerminalRequest {
    public static void encode(PacketMobileTerminalRequest msg, net.minecraft.network.FriendlyByteBuf buf) {}

    public static PacketMobileTerminalRequest decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new PacketMobileTerminalRequest();
    }

    public static void handle(PacketMobileTerminalRequest msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!(player.containerMenu instanceof MobileTerminalMenu menu)) return;
            if (!player.getUUID().equals(menu.ownerUuid())) return;

            PacketHandler.INSTANCE.sendTo(
                    new PacketMobileTerminalSnapshot(MobileTerminalService.buildEntries(player)),
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT
            );
        });
        ctx.setPacketHandled(true);
    }
}

