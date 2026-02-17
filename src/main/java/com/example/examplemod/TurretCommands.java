package com.example.examplemod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class TurretCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("skull")
                .then(Commands.literal("tpplaque")
                        .requires(source -> source.getEntity() instanceof ServerPlayer)
                        .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                .executes(TurretCommands::teleportToPlaque)))))
                .then(Commands.literal("givemodule")
                        .requires(source -> source.hasPermission(2))
                        .executes(TurretCommands::giveModule)));

        dispatcher.register(Commands.literal("jdx")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(TurretCommands::commandScavenge));

        dispatcher.register(Commands.literal("hui")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(TurretCommands::commandRecall));

        dispatcher.register(Commands.literal("lai")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(TurretCommands::commandRecall));

        dispatcher.register(Commands.literal("sha")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(TurretCommands::commandPurge));
    }

    private static int teleportToPlaque(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TeleportRequestGateway.Verdict verdict = TeleportRequestGateway.checkPlayerCommandCooldown(
                player, TeleportRequestSource.PLAYER_PLAQUE
        );
        if (!verdict.allowed()) {
            TeleportRequestGateway.notifyPlayerCommandCooldown(player, verdict.remainingTicks());
            return 0;
        }

        double x = DoubleArgumentType.getDouble(context, "x");
        double y = DoubleArgumentType.getDouble(context, "y");
        double z = DoubleArgumentType.getDouble(context, "z");
        player.teleportTo(x, y, z);
        TeleportRequestGateway.markPlayerCommandTeleport(player, TeleportRequestSource.PLAYER_PLAQUE);
        context.getSource().sendSuccess(() -> Component.literal("\u5df2\u4f20\u9001\u5230\u6b7b\u4ea1\u94ed\u724c\u6389\u843d\u70b9"), false);
        return 1;
    }

    private static int giveModule(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = new ItemStack(ExampleMod.TELEPORT_UPGRADE_MODULE.get());
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.sendSystemMessage(Component.literal("Given Teleport Upgrade Module"));
        return 1;
    }

    private static int commandScavenge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        List<SkeletonTurret> turrets = level.getEntitiesOfClass(
                SkeletonTurret.class,
                player.getBoundingBox().inflate(600.0),
                t -> t.getOwnerUUID() != null && t.getOwnerUUID().equals(player.getUUID()) && t.isFollowing()
        );

        int memberCount = 0;
        for (SkeletonTurret t : turrets) {
            if (!t.isCaptain()) {
                t.setCommandScavenging(true);
                memberCount++;
            }
        }

        if (memberCount > 0) {
            player.sendSystemMessage(Component.literal("§e[战术] 已命令 " + memberCount + " 名队员执行广域拾荒。"));
        } else {
            player.sendSystemMessage(Component.literal("§7[战术] 没有可执行拾荒指令的跟随队员。"));
        }
        return 1;
    }

    private static int commandRecall(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        List<SkeletonTurret> allTurrets = level.getEntitiesOfClass(
                SkeletonTurret.class,
                player.getBoundingBox().inflate(600.0),
                t -> t.getOwnerUUID() != null && t.getOwnerUUID().equals(player.getUUID())
        );

        int count = 0;
        for (SkeletonTurret t : allTurrets) {
            if (!(t.isCaptain() || t.isSquadMember())) {
                continue;
            }
            if (t.isPurgeActive()) t.stopPurgeMode();
            if (t.isCommandScavenging()) t.setCommandScavenging(false);
            if (t.isCommandRescue()) t.setCommandRescue(false);

            t.setTarget(null);
            t.getNavigation().stop();
            if (!t.isFollowing()) t.setFollowing(true);
            t.teleportToSafeSpot(player);
            level.sendParticles(ParticleTypes.CLOUD, t.getX(), t.getY() + 1.0, t.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
            count++;
        }

        if (count > 0) {
            player.sendSystemMessage(Component.literal("§a[系统] 强制召回已执行，" + count + " 名队员归队。"));
        } else {
            player.sendSystemMessage(Component.literal("§c[系统] 未检测到编队成员。"));
        }
        return 1;
    }

    private static int commandPurge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        List<SkeletonTurret> squad = level.getEntitiesOfClass(
                SkeletonTurret.class,
                player.getBoundingBox().inflate(100.0),
                t -> t.getOwnerUUID() != null && t.getOwnerUUID().equals(player.getUUID()) && t.isAlive()
                        && (t.isCaptain() || t.isSquadMember())
        );

        if (squad.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c[系统] 附近没有可用作战单位。"));
            return 1;
        }

        int count = squad.size();
        float angleStep = 360.0f / count;
        for (int i = 0; i < count; i++) {
            SkeletonTurret t = squad.get(i);
            float assignedAngle = i * angleStep;
            t.startPurgeMode(assignedAngle);
        }

        player.sendSystemMessage(Component.literal("§6[指挥] 清剿模式已开启，参与单位: " + count));
        return 1;
    }
}
