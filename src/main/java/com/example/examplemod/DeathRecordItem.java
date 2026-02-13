package com.example.examplemod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class DeathRecordItem extends Item {
    public DeathRecordItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack stack = context.getItemInHand();
        CompoundTag data = DeathPlaqueDataCodec.getDataTag(stack);
        if (data == null) return InteractionResult.FAIL;

        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());

        SkeletonTurret turret = ExampleMod.TURRET_ENTITY.get().spawn(serverLevel, null, context.getPlayer(), spawnPos, MobSpawnType.MOB_SUMMONED, true, false);
        if (turret == null) return InteractionResult.FAIL;

        turret.restoreFromRecord(data);
        turret.setFollowMode(false);
        turret.setFollowing(false);

        if (context.getPlayer() != null) {
            turret.setOwner(context.getPlayer());
        }

        stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("Version");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag data = DeathPlaqueDataCodec.getDataTag(stack);
        if (data == null) return;

        int id = data.getInt("UnitID");
        tooltip.add(Component.literal("编号#" + id + " 铭牌").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("等级: " + data.getInt("Tier") + "  击杀: " + data.getInt("KillCount")).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("右键地面召唤并恢复对应骷髅炮台").withStyle(ChatFormatting.AQUA));
    }
}
