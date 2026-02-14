package com.example.examplemod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiShotUpgradeModuleItem extends GenericTurretModuleItem {
    public static final String MODULE_LEVEL_KEY = "MultiShotModuleLevel";
    private static final Pattern MULTIPLIER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*[xX×]");

    public MultiShotUpgradeModuleItem(Properties properties) {
        super(properties, "multi_shot", MODULE_LEVEL_KEY);
    }

    public static int getLevel(ItemStack stack) {
        if (stack.getItem() instanceof MultiShotUpgradeModuleItem item) {
            return item.getModuleLevel(stack);
        }
        return MultiShotModuleRules.clampLevel(stack.getOrCreateTag().getInt(MODULE_LEVEL_KEY));
    }

    public static void setLevel(ItemStack stack, int level) {
        if (stack.getItem() instanceof MultiShotUpgradeModuleItem item) {
            item.setModuleLevel(stack, level);
            return;
        }
        stack.getOrCreateTag().putInt(MODULE_LEVEL_KEY, MultiShotModuleRules.clampLevel(level));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int moduleLevel = getLevel(stack);
        TurretUpgradeModule module = TurretModuleRegistry.get("multi_shot");

        Component levelBadge = Component.literal(String.format("[Lv.%d]", moduleLevel))
                .withStyle(moduleLevel >= 4 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED);
        tooltip.add(Component.translatable(this.getDescriptionId())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal(" "))
                .append(levelBadge));

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("类型: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("RARE 稀有组件").withStyle(ChatFormatting.BLUE)));
        tooltip.add(Component.literal("⚶ 箭矢数量: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("+" + MultiShotModuleRules.arrowsForLevel(moduleLevel))
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)));

        resolveShotMultiplier(module).ifPresent(multiplier ->
                tooltip.add(Component.literal("⚡ 射击倍率: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(multiplier + "x").withStyle(ChatFormatting.LIGHT_PURPLE))));

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("说明:").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("为炮台提供额外箭矢并智能分发目标。")
                .withStyle(ChatFormatting.WHITE));

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("---").withStyle(ChatFormatting.DARK_GRAY));

        if (module != null && module.metadata().lore() != null) {
            for (String line : module.metadata().lore()) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                if (line.startsWith("系统临终诗")) {
                    tooltip.add(Component.literal(">> " + line)
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
                } else {
                    tooltip.add(Component.literal("\"" + line + "\"")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                }
            }
        }
    }

    private Optional<String> resolveShotMultiplier(@Nullable TurretUpgradeModule module) {
        if (module == null || module.metadata().lore() == null) {
            return Optional.empty();
        }
        for (String line : module.metadata().lore()) {
            if (line == null) {
                continue;
            }
            Matcher matcher = MULTIPLIER_PATTERN.matcher(line);
            if (matcher.find()) {
                return Optional.of(matcher.group(1));
            }
        }
        return Optional.empty();
    }
}
