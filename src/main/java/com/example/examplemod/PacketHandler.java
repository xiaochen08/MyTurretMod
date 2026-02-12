package com.example.examplemod;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("examplemod", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    // 注册所有数据包
    public static void register() {
        int id = 0;

        INSTANCE.messageBuilder(PacketToggleMode.class, id++, NetworkDirection.PLAY_TO_SERVER) // 注意这里是 Client -> Server
                .decoder(PacketToggleMode::decode)
                .encoder(PacketToggleMode::encode)
                .consumerMainThread(PacketToggleMode::handle)
                .add();

        INSTANCE.messageBuilder(PacketShowDialogue.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketShowDialogue::decode)
                .encoder(PacketShowDialogue::encode)
                .consumerMainThread(PacketShowDialogue::handle)
                .add();
    }


    public static void sendToServer(Object message) {
        INSTANCE.sendToServer(message);
    }

    // 发送给所有人（只要他们在周围）
    public static void sendToAllClients(Object message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}
