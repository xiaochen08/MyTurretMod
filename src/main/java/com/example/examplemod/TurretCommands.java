package com.example.examplemod;

import com.mojang.brigadier.CommandDispatcher;
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
            .requires(source -> source.hasPermission(2))
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
}
