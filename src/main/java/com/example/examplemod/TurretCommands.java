package com.example.examplemod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.item.ItemStack;

public class TurretCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("skull")
            .requires(source -> source.hasPermission(2)) // Ops only
            .then(Commands.literal("dropcounter")
                .then(Commands.argument("skeletonId", IntegerArgumentType.integer())
                    .executes(TurretCommands::checkDropCount)))
            .then(Commands.literal("tp")
                .then(Commands.argument("pos", Vec3Argument.vec3())
                    .executes(TurretCommands::teleportToRecord)))
            .then(Commands.literal("givemodule")
                .executes(TurretCommands::giveModule)));
    }

    private static int giveModule(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = new ItemStack(ExampleMod.TELEPORT_UPGRADE_MODULE.get());
        if (!player.getInventory().add(stack)) {
             player.drop(stack, false);
        }
        player.sendSystemMessage(Component.literal("§aGiven Teleport Upgrade Module"));
        return 1;
    }

    private static int checkDropCount(CommandContext<CommandSourceStack> context) {
        int targetId = IntegerArgumentType.getInteger(context, "skeletonId");
        ServerLevel level = context.getSource().getLevel();
        int count = 0;
        boolean found = false;
        int remaining = 0;

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof SkeletonTurret turret) {
                int unitId = turret.getEntityData().get(SkeletonTurret.UNIT_ID);
                if (unitId == targetId) {
                    found = true;
                    int drops = turret.getEntityData().get(SkeletonTurret.DROP_COUNT);
                    remaining = 3 - drops;
                    break;
                }
            }
        }

        if (found) {
            final int finalRemaining = remaining;
            context.getSource().sendSuccess(() -> Component.literal("§a[系统] 骷髅 #" + targetId + " 剩余掉落次数: " + finalRemaining), false);
        } else {
            context.getSource().sendFailure(Component.literal("§c[错误] 未找到编号为 #" + targetId + " 的活跃骷髅炮台"));
        }
        return 1;
    }

    private static int teleportToRecord(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Vec3 pos = Vec3Argument.getVec3(context, "pos");

        player.teleportTo(pos.x, pos.y, pos.z);
        // 3 seconds of Invulnerability (Resistance V = 100% damage reduction)
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4, false, false, true));
        
        // Visual effects (optional, server side particles around player)
        // Handled by client usually, but we can play a sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

        context.getSource().sendSuccess(() -> Component.literal("§a[系统] 已传送至记录卡坐标 (3秒无敌)"), false);
        return 1;
    }
}
