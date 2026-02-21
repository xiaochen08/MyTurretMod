package com.example.examplemod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
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
    public Component getName(ItemStack stack) {
        CompoundTag data = DeathPlaqueDataCodec.getDataTag(stack);
        int id = data.getInt("UnitID");
        if (id > 0) {
            return Component.translatable("item.examplemod.death_record_card.named", String.format("%03d", id));
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag data = DeathPlaqueDataCodec.getDataTag(stack);
        if (data == null) return;

        int id = data.getInt("UnitID");
        tooltip.add(Component.translatable("tooltip.examplemod.death_record.id_plaque", String.format("%03d", id)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(
                "tooltip.examplemod.death_record.level_kills",
                data.getInt("Tier"),
                data.getInt("KillCount"))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.examplemod.death_record.use_on_ground").withStyle(ChatFormatting.AQUA));
    }
}
