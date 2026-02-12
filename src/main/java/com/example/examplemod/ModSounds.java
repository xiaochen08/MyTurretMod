package com.example.examplemod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    // 创建注册器
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "examplemod");

    // 1. 打印循环声 (滋滋滋...)
    public static final RegistryObject<SoundEvent> PRINT_LOOP = register("print_loop");

    // 2. 打印完成 (叮！)
    public static final RegistryObject<SoundEvent> PRINT_COMPLETE = register("print_complete");

    // 3. 蓝屏死机 (哔——！)
    public static final RegistryObject<SoundEvent> PRINT_ERROR = register("print_error");

    // 4. 爆炸声 (轰！)
    public static final RegistryObject<SoundEvent> PRINT_EXPLODE = register("print_explode");

    // 辅助方法
    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("examplemod", name)));
    }

    // 注册到总线
    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}