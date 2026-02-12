package com.example.examplemod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;


@Mod.EventBusSubscriber(modid = "examplemod", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // ✅ Register Vanilla Renderer
        event.registerEntityRenderer(ExampleMod.TURRET_ENTITY.get(), TurretRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // ✅ Register Vanilla Model Layer
        event.registerLayerDefinition(TurretModel.LAYER_LOCATION, TurretModel::createBodyLayer);
    }

    // 📂 ClientModEvents.java (添加到类里面，registerRenderers 方法的后面)



    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 绑定逻辑和画面
            MenuScreens.register(ExampleMod.TURRET_MENU.get(), TurretScreen::new);
        });
    }
}

