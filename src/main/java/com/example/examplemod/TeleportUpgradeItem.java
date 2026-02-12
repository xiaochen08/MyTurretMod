package com.example.examplemod;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TeleportUpgradeItem extends Item {
    public TeleportUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // 使用本地化键，支持中英文
        tooltip.add(Component.translatable("item.examplemod.teleport_upgrade_module.desc"));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.translatable("item.examplemod.teleport_upgrade_module.usage"));
    }
}
