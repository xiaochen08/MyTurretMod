package com.example.examplemod;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
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
        INSTANCE.messageBuilder(PacketSummonTerminalRequest.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketSummonTerminalRequest::decode)
                .encoder(PacketSummonTerminalRequest::encode)
                .consumerMainThread(PacketSummonTerminalRequest::handle)
                .add();

        INSTANCE.messageBuilder(PacketSummonTerminalSnapshot.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSummonTerminalSnapshot::decode)
                .encoder(PacketSummonTerminalSnapshot::encode)
                .consumerMainThread(PacketSummonTerminalSnapshot::handle)
                .add();

        INSTANCE.messageBuilder(PacketSummonTerminalAction.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketSummonTerminalAction::decode)
                .encoder(PacketSummonTerminalAction::encode)
                .consumerMainThread(PacketSummonTerminalAction::handle)
                .add();

        INSTANCE.messageBuilder(PacketSummonTerminalDelta.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSummonTerminalDelta::decode)
                .encoder(PacketSummonTerminalDelta::encode)
                .consumerMainThread(PacketSummonTerminalDelta::handle)
                .add();

        INSTANCE.messageBuilder(PacketSummonTerminalRenameResult.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSummonTerminalRenameResult::decode)
                .encoder(PacketSummonTerminalRenameResult::encode)
                .consumerMainThread(PacketSummonTerminalRenameResult::handle)
                .add();

        INSTANCE.messageBuilder(PacketMobileTerminalRequest.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketMobileTerminalRequest::decode)
                .encoder(PacketMobileTerminalRequest::encode)
                .consumerMainThread(PacketMobileTerminalRequest::handle)
                .add();

        INSTANCE.messageBuilder(PacketMobileTerminalSnapshot.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketMobileTerminalSnapshot::decode)
                .encoder(PacketMobileTerminalSnapshot::encode)
                .consumerMainThread(PacketMobileTerminalSnapshot::handle)
                .add();

        INSTANCE.messageBuilder(PacketMobileTerminalAction.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketMobileTerminalAction::decode)
                .encoder(PacketMobileTerminalAction::encode)
                .consumerMainThread(PacketMobileTerminalAction::handle)
                .add();

        INSTANCE.messageBuilder(PacketMobileTerminalDelta.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketMobileTerminalDelta::decode)
                .encoder(PacketMobileTerminalDelta::encode)
                .consumerMainThread(PacketMobileTerminalDelta::handle)
                .add();

        INSTANCE.messageBuilder(PacketManualBookmarkUpdate.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketManualBookmarkUpdate::decode)
                .encoder(PacketManualBookmarkUpdate::encode)
                .consumerMainThread(PacketManualBookmarkUpdate::handle)
                .add();
    }


    public static void sendToServer(Object message) {
        INSTANCE.sendToServer(message);
    }

    // 发送给所有人（只要他们在周围）
}
