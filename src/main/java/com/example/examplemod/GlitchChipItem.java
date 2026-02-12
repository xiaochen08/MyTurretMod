package com.example.examplemod;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Random;

public class GlitchChipItem extends Item {
    private static final Random RAND = new Random();

    public GlitchChipItem(Properties properties) {
        super(properties);
    }

    // 添加鼠标悬停提示 (Lore)
    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        tooltip.add(Component.literal("§7“它还是热的，里面保存着未完成的构建数据。”"));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("§6[ 修复方案 ]"));
        tooltip.add(Component.literal("§f ➤ §b消耗 1 钻石: §a100% 完美复原"));
        tooltip.add(Component.literal("§f ➤ §7消耗 1 铁锭: §e50% 概率复原"));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("§8>> 右键使用以尝试修复 <<"));
    }

    // 右键触发修复逻辑
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack chipStack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.pass(chipStack);
        }

        // 1. 检查是否有钻石 (100% 成功)
        if (player.getInventory().countItem(Items.DIAMOND) > 0) {
            performRepair(level, player, chipStack, Items.DIAMOND, 1.0f);
            return InteractionResultHolder.consume(chipStack);
        }

        // 2. 检查是否有铁锭 (50% 成功)
        if (player.getInventory().countItem(Items.IRON_INGOT) > 0) {
            performRepair(level, player, chipStack, Items.IRON_INGOT, 0.5f);
            return InteractionResultHolder.consume(chipStack);
        }

        // 3. 啥都没有，提示玩家
        player.displayClientMessage(Component.literal("§c需要 [铁锭] 或 [钻石] 才能进行数据恢复！"), true);
        return InteractionResultHolder.fail(chipStack);
    }

    private void performRepair(Level level, Player player, ItemStack chipStack, Item material, float successRate) {
        // 消耗材料 (如果是创造模式就不消耗)
        if (!player.getAbilities().instabuild) {
            findAndConsumeItem(player, material);
            chipStack.shrink(1); // 消耗掉手里的芯片
        }

        // 判定成功/失败
        if (RAND.nextFloat() < successRate) {
            // 🎉 成功！
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
            player.displayClientMessage(Component.literal("§a>> 数据恢复成功！重构完成。"), true);

            // 给一个新的法杖
            ItemStack turretWand = new ItemStack(ExampleMod.TURRET_WAND.get());

            // 如果是钻石修的，也许可以给它加个“出厂附魔”？(这里先只给普通的)
            if (!player.getInventory().add(turretWand)) {
                player.drop(turretWand, false); // 背包满了就丢地上
            }
        } else {
            // 💥 失败！
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0f, 1.0f);
            player.displayClientMessage(Component.literal("§c>> 错误：数据损坏，修复失败。"), true);

            // 掉几个废料安慰一下
            player.drop(new ItemStack(Items.IRON_NUGGET, 2), false);
        }
    }

    // 辅助方法：找到并消耗背包里的材料
    private void findAndConsumeItem(Player player, Item itemToConsume) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == itemToConsume) {
                stack.shrink(1);
                return;
            }
        }
    }
}
