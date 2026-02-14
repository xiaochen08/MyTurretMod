package com.example.examplemod;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Collections; // 记得导入这个

public class MultiShotTurretUpgradeModule implements TurretUpgradeModule {

    // 1. 修改 META：清空描述和Lore，避免与新UI重复
    private static final TurretModuleMetadata META = new TurretModuleMetadata(
            "multi_shot",
            "多重射击模块",
            "", // 描述留空，我们在 getDisplayStats 里自己写
            "arrow",
            TurretModuleRarity.RARE,
            Collections.emptyList() // 清空旧的实验记录
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
    public void refreshState(SkeletonTurret turret, TurretModuleState state) {
        int best = 0;
        for (int i = 5; i < 10; i++) {
            ItemStack stack = turret.inventory.getItem(i);
            if (!(stack.getItem() instanceof GenericTurretModuleItem item)) {
                continue;
            }
            if (!id().equals(item.getModuleId(stack))) {
                continue;
            }
            best = Math.max(best, item.getModuleLevel(stack));
        }
        state.setMultiShotLevel(best);
    }

    @Override
    public void onRangedAttack(SkeletonTurret turret, LivingEntity primaryTarget, int tier, TurretModuleState state) {
        int level = state.getMultiShotLevel();
        if (level <= 0 || turret.level().isClientSide) {
            return;
        }

        double range = TurretConfig.COMMON.multiShotRange.get();
        List<LivingEntity> targets = collectTargets(turret, primaryTarget, range);
        int totalShotCount = MultiShotModuleRules.computeVolleyCount(level, targets.size());
        int extraShots = Math.max(0, totalShotCount - 1);
        if (extraShots <= 0) {
            return;
        }

        double attackDamage = turret.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double attackSpeed = turret.getAttributeValue(Attributes.ATTACK_SPEED);
        double damageMultiplier = 1.0 + Math.max(0.0, attackDamage - 1.0) * TurretConfig.COMMON.multiShotDamageScalePerAttackDamage.get();
        float speedMultiplier = (float) (1.0 + Math.max(0.0, attackSpeed - 1.0) * TurretConfig.COMMON.multiShotSpeedScalePerAttackSpeed.get());

        for (int i = 1; i <= extraShots; i++) {
            turret.shootModuleArrow(targets.get(i), tier, speedMultiplier, damageMultiplier, true);
        }
    }

    private List<LivingEntity> collectTargets(SkeletonTurret turret, LivingEntity primaryTarget, double range) {
        List<LivingEntity> result = new ArrayList<>();
        if (isValidTarget(turret, primaryTarget)) {
            result.add(primaryTarget);
        }

        List<LivingEntity> nearby = turret.level().getEntitiesOfClass(
                LivingEntity.class,
                turret.getBoundingBox().inflate(range),
                t -> isValidTarget(turret, t) && t != primaryTarget
        );
        nearby.sort(Comparator.comparingDouble(t -> t.distanceToSqr(turret)));
        result.addAll(nearby);
        return result;
    }

    private boolean isValidTarget(SkeletonTurret turret, LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        if (target instanceof Player || target instanceof SkeletonTurret || target instanceof IronGolem) return false;
        if (target.getPersistentData().getBoolean("IsFriendlyZombie")) return false;
        if (target.getPersistentData().getBoolean("IsFriendlyCreeper")) return false;
        if (!(target instanceof Enemy || target instanceof Monster)) return false;
        return turret.hasLineOfSight(target);
    }

    // 2. 重写显示逻辑：构建完整的 Tooltip
    @Override
    public List<Component> getDisplayStats(int level) {
        List<Component> tooltip = new ArrayList<>();

        // 计算数值 (用于显示)
        int arrowCount = MultiShotModuleRules.arrowsForLevel(level);
        // 估算一个显示用的倍率 (如果你有具体的规则类可以替换这里，这里模拟 3.0x ~ 5.0x 的成长)
        double displayMultiplier = 1.5 + (level * 0.7);

        // ---------------------------------------------------------
        // [1. 标题行] ⚡ 多重射击模块 ⚡ [Lv.X]
        // ---------------------------------------------------------
        // \u00A7d = 亮紫色, \u00A7l = 粗体, \u00A76 = 金色
        tooltip.add(Component.literal("\u00A7d\u00A7l⚡ 多重射击模块 ⚡   \u00A76\u00A7l[Lv." + level + "]"));

        // ---------------------------------------------------------
        // [2. 副标题] >> 史诗级火力覆盖系统 <<
        // ---------------------------------------------------------
        // \u00A75 = 深紫色
        tooltip.add(Component.literal("\u00A75>> 史诗级火力覆盖系统 <<"));

        // ---------------------------------------------------------
        // [3. 属性区]
        // ---------------------------------------------------------
        // 分隔线 (深灰色)
        tooltip.add(Component.literal("\u00A78------------------------"));

        // 属性 A: 箭矢数量
        // \u00A77 = 灰色, \u00A7a = 绿色, \u00A72 = 深绿色
        tooltip.add(Component.literal("\u00A77➳ 齐射数量: \u00A7a\u00A7l+" + arrowCount + " \u00A72(MAX)"));

        // 属性 B: 射击倍率 (带视觉条)
        // \u00A7b = 青色
        tooltip.add(Component.literal("\u00A77✕ 射击倍率: \u00A7b████ " + String.format("%.1fx", displayMultiplier)));

        // 空行
        tooltip.add(Component.literal(""));

        // ---------------------------------------------------------
        // [4. 功能描述]
        // ---------------------------------------------------------
        // \u00A7f = 白色
        tooltip.add(Component.literal("\u00A7f为炮台提供额外箭矢并智能分发目标。"));

        // 空行
        tooltip.add(Component.literal(""));

        // ---------------------------------------------------------
        // [5. 底部短语 Flavor Text]
        // ---------------------------------------------------------
        // \u00A73 = 深青色, \u00A7o = 斜体
        tooltip.add(Component.literal("\u00A73\u00A7o\"既然能覆盖一切，何必瞄准？\""));

        return tooltip;
    }
}