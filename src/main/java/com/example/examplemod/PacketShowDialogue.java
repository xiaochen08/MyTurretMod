package com.example.examplemod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketShowDialogue {
    private final int entityId;
    private final String text;

    public PacketShowDialogue(int entityId, String text) {
        this.entityId = entityId;
        this.text = text;
    }

    public static void encode(PacketShowDialogue msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeUtf(msg.text);
    }

    public static PacketShowDialogue decode(FriendlyByteBuf buf) {
        return new PacketShowDialogue(buf.readInt(), buf.readUtf());
    }

    public static void handle(PacketShowDialogue msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 确保这只在客户端运行
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // 调用我们即将写的渲染器
                ClientDialogueHandler.startDialogue(msg.entityId, msg.text);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}