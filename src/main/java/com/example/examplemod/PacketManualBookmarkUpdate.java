package com.example.examplemod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketManualBookmarkUpdate {
    private final int slot;
    private final List<String> bookmarks;

    public PacketManualBookmarkUpdate(int slot, List<String> bookmarks) {
        this.slot = slot;
        this.bookmarks = bookmarks == null ? List.of() : bookmarks;
    }

    public static void encode(PacketManualBookmarkUpdate msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.slot);
        buf.writeVarInt(msg.bookmarks.size());
        for (String id : msg.bookmarks) {
            buf.writeUtf(id, 64);
        }
    }

    public static PacketManualBookmarkUpdate decode(FriendlyByteBuf buf) {
        int slot = buf.readVarInt();
        int size = Math.max(0, Math.min(128, buf.readVarInt()));
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readUtf(64));
        }
        return new PacketManualBookmarkUpdate(slot, list);
    }

    public static void handle(PacketManualBookmarkUpdate msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (msg.slot < 0 || msg.slot >= player.getInventory().getContainerSize()) return;

            ItemStack stack = player.getInventory().getItem(msg.slot);
            if (!stack.is(ExampleMod.PLAYER_MANUAL.get())) return;

            PlayerManualItem.ensureVersion(stack);
            PlayerManualItem.writeBookmarks(stack, msg.bookmarks);
        });
        ctx.setPacketHandled(true);
    }
}

