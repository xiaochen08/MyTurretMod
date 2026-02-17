package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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

        SkeletonTurret turret = ExampleMod.TURRET_ENTITY.get().create(serverLevel);
        if (turret != null) {
            turret.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0F, 0.0F);
            turret.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.MOB_SUMMONED, null, null);

            if (context.getPlayer() != null) {
                turret.setOwner(context.getPlayer());
            }

            serverLevel.addFreshEntity(turret);
            itemStack.shrink(1);

            if (context.getPlayer() != null) {
                level.playSound(null, spawnPos, SoundEvents.ANVIL_PLACE,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 1.2f);
                context.getPlayer().sendSystemMessage(Component.literal("[系统] 炮台部署完成，已绑定主人。"));
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("右键方块面放置骷髅炮台"));
        tooltip.add(Component.literal("放置后自动绑定为你的炮台"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
