package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents; // 关键导包：声音
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TurretItem extends Item {

    public TurretItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack itemStack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos spawnPos = pos.relative(direction);

        // 创建实体
        SkeletonTurret turret = ExampleMod.TURRET_ENTITY.get().create(serverLevel);
        if (turret != null) {
            turret.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0F, 0.0F);
            turret.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.MOB_SUMMONED, null, null);
            turret.setCustomName(Component.literal("§7[D] 灰烬·哨兵"));

            serverLevel.addFreshEntity(turret);
            itemStack.shrink(1); // 消耗物品

            // --- ✅ 核心功能：放置时的声音和文字提示 ---
            if (context.getPlayer() != null) {
                // 1. 播放沉重的金属放置音效
                level.playSound(null, spawnPos, SoundEvents.ANVIL_PLACE,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 1.2f);
                // 2. 发送系统提示消息
                context.getPlayer().sendSystemMessage(Component.literal("§a[系统] §f防御塔部署完毕。正在初始化 AI..."));
            }
        }

        return InteractionResult.CONSUME;
    }

    // ==========================================
    // ✅ 升级：详细的战术操作手册
    // ==========================================
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7===================="));
        tooltip.add(Component.literal("§7“ 档案编号: X-799 | 绝密 ”"));
        tooltip.add(Component.literal("§8“ 他们曾是父亲、丈夫和儿子。现在，他们是所有权归联邦的资产。”"));
        tooltip.add(Component.literal("§b[战术部署终端]"));
        tooltip.add(Component.literal("§7用于在指定坐标折跃一名骷髅先锋。"));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("§e[ 战术终端 ]"));
        tooltip.add(Component.literal("§f ➤ §6右键地面: §7激活休眠机体"));
        tooltip.add(Component.literal("§f ➤ §6Shift+右键: §7切换 杀戮/守护 协议"));
        tooltip.add(Component.literal("§f ➤ §6濒死抢修: §a注入纳米修复液 (消耗寿命)"));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("§f ➤ §6用铁毡命名纸然后放到书的左边修改名称: §7修改代号 (或放入背包第25格)"));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("§d[强化指令]"));
        tooltip.add(Component.literal("§f ➤ §9青金石右键: §7强化装备附魔 (随机部位)"));
        tooltip.add(Component.literal("§f ➤ §b升级材料: §7手持特定材料右键注入"));
        tooltip.add(Component.literal("     §7(铜锭 -> 铁锭 -> 金锭 -> 钻石 -> 下界合金碎片)"));
        tooltip.add(Component.literal("§7===================="));
        tooltip.add(Component.literal("§c[ 警告 ]"));
        tooltip.add(Component.literal("§7该机体可能会产生幻觉、记忆闪回或试图与操作者交流。"));
        tooltip.add(Component.literal("§7请忽略所有语音内容，那只是系统的噪音。"));

        super.appendHoverText(stack, level, tooltip, flag);
    }
}