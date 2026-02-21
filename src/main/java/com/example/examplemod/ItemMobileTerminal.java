package com.example.examplemod;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/**
 * Portable Necro terminal item with owner-bound biometric security.
 *
 * <p>Forge 1.20+ uses {@link #use(Level, Player, InteractionHand)} as the modern equivalent
 * of legacy {@code onItemRightClick}.</p>
 *
 * <p>Crafting recipe registration (data pack json):
 * place {@code examplemod:summon_terminal} at row2/col2 and {@code minecraft:emerald}
 * at row3/col2, all other slots empty.</p>
 */
public class ItemMobileTerminal extends Item {
    public static final String OWNER_UUID_TAG = "OwnerUUID";

    public ItemMobileTerminal(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = stack.getOrCreateTag();

        if (!tag.hasUUID(OWNER_UUID_TAG)) {
            if (!level.isClientSide) {
                tag.putUUID(OWNER_UUID_TAG, player.getUUID());
                player.sendSystemMessage(Component.literal("生物识别已绑定").withStyle(ChatFormatting.AQUA));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        UUID owner = tag.getUUID(OWNER_UUID_TAG);
        if (!player.getUUID().equals(owner)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.literal("权限拒绝：生物特征不匹配").withStyle(ChatFormatting.RED));
                level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider(
                            (id, inv, p) -> new MobileTerminalMenu(id, inv, serverPlayer.getUUID()),
                            Component.translatable("gui.examplemod.mobile_terminal.title")
                    ),
                    buf -> buf.writeUUID(serverPlayer.getUUID())
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

