package com.example.examplemod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

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
}
