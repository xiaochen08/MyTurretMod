package com.example.examplemod;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public class TeleportTurretUpgradeModule implements TurretUpgradeModule {
    private static final TurretModuleMetadata META = new TurretModuleMetadata(
            "teleport",
            "传送模块",
            "提供战术传送与冷却控制能力",
            "ender_pearl",
            TurretModuleRarity.EPIC,
            List.of(
                "实验记录 E-17：“第 17 次迭代，冷却液泄漏，操作员瞳孔出血，仍坚持记录射击倍率提升至 4.3×。”",
                "最后语音 0x3F2A：“妈妈，我看见自己的影子在墙上被切成四片，对不起，我按下了连射按钮。”",
                "系统临终诗：“当铀玻璃碎成 7 瓣，时间也学会了齐射。”"
            )
    );

    @Override
    public String id() {
        return META.moduleId();
    }

    @Override
    public TurretModuleMetadata metadata() {
        return META;
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public void refreshState(SkeletonTurret turret, TurretModuleState state) {
        int bestLevel = 0;
        for (int i = 5; i < 10; i++) {
            ItemStack stack = turret.inventory.getItem(i);
            if (!(stack.getItem() instanceof GenericTurretModuleItem item)) {
                continue;
            }
            if (!id().equals(item.getModuleId(stack))) {
                continue;
            }
            bestLevel = Math.max(bestLevel, item.getModuleLevel(stack));
        }
        state.setTeleportLevel(bestLevel);
        state.setTeleportInstalled(bestLevel > 0);
    }

    @Override
    public void onTick(SkeletonTurret turret, TurretModuleState state) {
        int nextCooldown = computeNextCooldown(turret.getTeleportCooldown(), state.isComboActive(), turret.tickCount);
        if (nextCooldown != turret.getTeleportCooldown()) {
            turret.setTeleportCooldown(nextCooldown);
        }
    }

    static int computeNextCooldown(int currentCooldown, boolean comboActive, int tickCount) {
        if (!comboActive || currentCooldown <= 0) {
            return currentCooldown;
        }
        return tickCount % 20 == 0 ? Math.max(0, currentCooldown - 1) : currentCooldown;
    }

    @Override
    public List<Component> getDisplayStats(int level) {
        int cooldown = TeleportModuleRules.configByLevel(level).teleportCooldownSeconds();
        return List.of(
            Component.literal("冷却时间: ").append(Component.literal(cooldown + "s").withStyle(Style.EMPTY.withColor(0x00FFFF)))
        );
    }
}
