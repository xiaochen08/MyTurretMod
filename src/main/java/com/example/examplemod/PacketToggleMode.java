package com.example.examplemod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketToggleMode {
    private final int entityId;

    public PacketToggleMode(int entityId) {
        this.entityId = entityId;
    }

    // 1. 编码：把 ID 写进包里
    public static void encode(PacketToggleMode msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
    }

    // 2. 解码：从包里读出 ID
    public static PacketToggleMode decode(FriendlyByteBuf buf) {
        return new PacketToggleMode(buf.readInt());
    }

    // 3. 处理：服务器收到后做什么
    public static void handle(PacketToggleMode msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 获取发送包的玩家
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level() != null) {
                // 通过 ID 找到那个骷髅
                Entity entity = player.level().getEntity(msg.entityId);
                // 确认它是我们的骷髅炮塔
                if (entity instanceof SkeletonTurret turret) {
                    // 切换模式：如果是跟随，就变守卫；如果是守卫，就变跟随
                    boolean newState = !turret.isFollowMode();
                    turret.setFollowMode(newState);

                    // (可选) 给玩家发个提示消息
                    // player.sendSystemMessage(Component.literal("模式已切换: " + (newState ? "跟随" : "守卫")));
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
