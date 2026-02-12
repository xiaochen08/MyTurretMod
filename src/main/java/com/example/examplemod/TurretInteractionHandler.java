package com.example.examplemod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.SimpleMenuProvider;

@Mod.EventBusSubscriber(modid = "examplemod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TurretInteractionHandler {

    // ⚠️ 此类逻辑已全部迁移至 SkeletonTurret.mobInteract 方法中，实现 Entity-Centric 架构
    // 保留此类仅为了防止潜在的编译错误或引用丢失，但不再处理任何事件。
    
    // @SubscribeEvent -> 注释掉以禁用
    public static void onPlayerInteract(PlayerInteractEvent.EntityInteract event) {
        // Deprecated: Logic moved to SkeletonTurret.java
    }
}