package com.example.examplemod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class TeleportUpgradeItem extends GenericTurretModuleItem {
    public static final String MODULE_LEVEL_KEY = "TeleportModuleLevel";

    public TeleportUpgradeItem(Properties properties) {
        super(properties, "teleport", MODULE_LEVEL_KEY);
    }

    public static int getLevel(ItemStack stack) {
        if (stack.getItem() instanceof TeleportUpgradeItem item) {
            return item.getModuleLevel(stack);
        }
        return TeleportModuleRules.clampLevel(stack.getOrCreateTag().getInt(MODULE_LEVEL_KEY));
    }

    public static void setLevel(ItemStack stack, int level) {
        if (stack.getItem() instanceof TeleportUpgradeItem item) {
            item.setModuleLevel(stack, level);
            return;
        }
        stack.getOrCreateTag().putInt(MODULE_LEVEL_KEY, TeleportModuleRules.clampLevel(level));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int moduleLevel = getLevel(stack);
        int cooldownSeconds = TeleportModuleRules.configByLevel(moduleLevel).teleportCooldownSeconds();
        String cooldownText = String.format("%.1fs", (double) cooldownSeconds);

        tooltip.add(Component.translatable("tooltip.examplemod.teleport_module.title", moduleLevel)
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        tooltip.add(Component.translatable("tooltip.examplemod.teleport_module.subtitle")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.examplemod.teleport_module.separator")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.examplemod.teleport_module.cooldown", cooldownText)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.examplemod.teleport_module.stability", "100%")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.examplemod.teleport_module.desc")
                .withStyle(ChatFormatting.WHITE));

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.examplemod.teleport_module.quote")
                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
    }
}
