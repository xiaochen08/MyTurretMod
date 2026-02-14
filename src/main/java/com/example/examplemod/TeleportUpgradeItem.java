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

        tooltip.add(Component.literal("⚡ 传送模块 ⚡")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal("   "))
                .append(Component.literal(String.format("[Lv.%d]", moduleLevel))
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));

        tooltip.add(Component.literal(">> 史诗级空间装置 <<")
                .withStyle(ChatFormatting.DARK_PURPLE));

        tooltip.add(Component.literal("------------------------")
                .withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(Component.literal("⌛ 冷却充能: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(cooldownText).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal(" (极速/MAX)").withStyle(ChatFormatting.DARK_GREEN)));

        tooltip.add(Component.literal("🌌 空间稳定性: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal("100%").withStyle(ChatFormatting.AQUA)));

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("提供战术传送与冷却控制能力")
                .withStyle(ChatFormatting.WHITE));

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("\"距离只是幻觉。\"")
                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
    }
}
