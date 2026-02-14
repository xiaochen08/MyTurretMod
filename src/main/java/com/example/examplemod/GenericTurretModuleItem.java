package com.example.examplemod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GenericTurretModuleItem extends Item {
    public static final String MODULE_ID_KEY = "ModuleId";
    public static final String MODULE_LEVEL_KEY = "ModuleLevel";

    private final String fixedModuleId;
    private final String legacyLevelKey;

    public GenericTurretModuleItem(Properties properties, String fixedModuleId, String legacyLevelKey) {
        super(properties);
        this.fixedModuleId = fixedModuleId;
        this.legacyLevelKey = legacyLevelKey;
    }

    public String fixedModuleId() {
        return fixedModuleId;
    }

    public String getModuleId(ItemStack stack) {
        migrateLegacyTags(stack);
        return stack.getOrCreateTag().getString(MODULE_ID_KEY);
    }

    public int getModuleLevel(ItemStack stack) {
        migrateLegacyTags(stack);
        return TurretUpgradeTierPlan.clampLevel(stack.getOrCreateTag().getInt(MODULE_LEVEL_KEY));
    }

    public void setModuleLevel(ItemStack stack, int level) {
        stack.getOrCreateTag().putString(MODULE_ID_KEY, fixedModuleId);
        stack.getOrCreateTag().putInt(MODULE_LEVEL_KEY, TurretUpgradeTierPlan.clampLevel(level));
    }

    public void migrateLegacyTags(ItemStack stack) {
        var tag = stack.getOrCreateTag();
        if (!tag.contains(MODULE_ID_KEY)) {
            tag.putString(MODULE_ID_KEY, fixedModuleId);
        }
        if (tag.contains(legacyLevelKey) && !tag.contains(MODULE_LEVEL_KEY)) {
            tag.putInt(MODULE_LEVEL_KEY, TurretUpgradeTierPlan.clampLevel(tag.getInt(legacyLevelKey)));
        }
        if (!tag.contains(MODULE_LEVEL_KEY)) {
            tag.putInt(MODULE_LEVEL_KEY, 1);
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        int level = getModuleLevel(stack);
        String moduleId = getModuleId(stack);
        TurretUpgradeModule module = TurretModuleRegistry.get(moduleId);
        String baseName = module != null ? module.metadata().name() : "通用模块";
        String suffix = TurretUpgradeTierPlan.suffixForLevel(level);
        MutableComponent out = Component.literal(baseName);
        if (!suffix.isEmpty()) {
            out = out.append(Component.literal("+" + suffix));
        }
        return out;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int moduleLevel = getModuleLevel(stack);
        String moduleId = getModuleId(stack);
        TurretUpgradeModule module = TurretModuleRegistry.get(moduleId);
        if (module != null) {
            // 模块专属标签颜色
            int labelColor = moduleId.equals("teleport") ? 0x00FFC8 : (moduleId.equals("multi_shot") ? 0xFF35A0 : 0xFFFFFF);
            
            tooltip.add(Component.literal("ID: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(module.metadata().moduleId()).withStyle(Style.EMPTY.withColor(labelColor))));
            tooltip.add(Component.literal("类型: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(module.metadata().name()).withStyle(Style.EMPTY.withColor(labelColor))));
            tooltip.add(Component.literal("说明: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(module.metadata().description()).withStyle(ChatFormatting.WHITE)));
            
            // 品质颜色
            int rarityColor = switch (module.metadata().rarity()) {
                case COMMON -> 0x9D9D9D;
                case UNCOMMON -> 0x0070DD;
                case RARE -> 0x0070DD;
                case EPIC -> 0xA335EE;
                case LEGENDARY -> 0xFF8000;
                default -> 0xFFFFFF;
            };

            tooltip.add(Component.literal("稀有度: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(module.metadata().rarity().name()).withStyle(Style.EMPTY.withColor(rarityColor))));
            
            // 动态数值显示
            List<Component> stats = module.getDisplayStats(moduleLevel);
            if (stats != null) {
                tooltip.addAll(stats);
            }
            
            // 叙事注入
            if (module.metadata().lore() != null && !module.metadata().lore().isEmpty()) {
                tooltip.add(Component.empty());
                for (String line : module.metadata().lore()) {
                    tooltip.add(Component.literal(line).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                }
            }
        }
        tooltip.add(Component.literal("等级: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(moduleLevel)).withStyle(ChatFormatting.WHITE)));
    }
}
