package com.example.examplemod;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ExampleMod.TURRET_ENTITY.get(), TurretRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TurretModel.LAYER_LOCATION, TurretModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ExampleMod.TURRET_MENU.get(), TurretScreen::new);
            MenuScreens.register(ExampleMod.SUMMON_TERMINAL_MENU.get(), SummonTerminalScreen::new);
            ClientLanguageState.refreshFromClient();
        });
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(PreparationBarrier stage,
                                                  net.minecraft.server.packs.resources.ResourceManager resourceManager,
                                                  net.minecraft.util.profiling.ProfilerFiller preparationsProfiler,
                                                  net.minecraft.util.profiling.ProfilerFiller reloadProfiler,
                                                  Executor backgroundExecutor,
                                                  Executor gameExecutor) {
                return CompletableFuture.runAsync(() -> {}, backgroundExecutor)
                        .thenCompose(stage::wait)
                        .thenRunAsync(ClientLanguageState::refreshFromClient, gameExecutor);
            }
        });
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex != 1) return 0xFFFFFF;
            return MultiShotModuleRules.levelColor(MultiShotUpgradeModuleItem.getLevel(stack));
        }, ExampleMod.MULTI_SHOT_UPGRADE_MODULE.get());
    }
}
