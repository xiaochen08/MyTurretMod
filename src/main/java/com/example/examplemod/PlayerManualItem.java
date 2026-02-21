package com.example.examplemod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PlayerManualItem extends Item {
    public static final int CURRENT_VERSION = 1;
    private static final String VERSION_TAG = "ManualVersion";
    private static final String BOOKMARKS_TAG = "ManualBookmarks";
    private static final int MAX_BOOKMARKS = 64;
    private static final int MAX_BOOKMARK_ID_LEN = 64;

    public PlayerManualItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ensureVersion(stack);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ManualClientHooks.openManual(player, hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.examplemod.player_manual.open"));
        tooltip.add(Component.translatable("tooltip.examplemod.player_manual.version", stack.getOrCreateTag().getInt(VERSION_TAG)));
    }

    public static boolean ensureVersion(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        int old = tag.getInt(VERSION_TAG);
        if (old < CURRENT_VERSION) {
            tag.putInt(VERSION_TAG, CURRENT_VERSION);
            return true;
        }
        return false;
    }

    public static Set<String> readBookmarks(ItemStack stack) {
        LinkedHashSet<String> bookmarks = new LinkedHashSet<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(BOOKMARKS_TAG, 9)) {
            return bookmarks;
        }
        ListTag list = tag.getList(BOOKMARKS_TAG, 8);
        for (int i = 0; i < list.size() && bookmarks.size() < MAX_BOOKMARKS; i++) {
            String raw = list.getString(i);
            if (raw != null && !raw.isBlank() && raw.length() <= MAX_BOOKMARK_ID_LEN) {
                bookmarks.add(raw);
            }
        }
        return bookmarks;
    }

    public static void writeBookmarks(ItemStack stack, List<String> rawBookmarks) {
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        if (rawBookmarks != null) {
            for (String id : rawBookmarks) {
                if (id == null) continue;
                String trimmed = id.trim();
                if (trimmed.isEmpty() || trimmed.length() > MAX_BOOKMARK_ID_LEN) continue;
                cleaned.add(trimmed);
                if (cleaned.size() >= MAX_BOOKMARKS) break;
            }
        }

        ListTag list = new ListTag();
        for (String id : cleaned) {
            list.add(StringTag.valueOf(id));
        }
        stack.getOrCreateTag().put(BOOKMARKS_TAG, list);
    }

    public static List<String> toList(Set<String> bookmarks) {
        return new ArrayList<>(bookmarks);
    }
}
