package com.example.examplemod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class TurretTextResolver {
    private TurretTextResolver() {}

    public static Component resolveBaseName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Component.translatable("name.examplemod.turret.base_default");
        }
        if (SkeletonTurret.DEFAULT_BASE_NAME_TOKEN.equals(rawName) || SkeletonTurret.isLegacyDefaultBaseName(rawName)) {
            return Component.translatable("name.examplemod.turret.base_default");
        }
        return Component.literal(rawName);
    }

    public static Component resolveOverheadStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return Component.empty();
        }

        String[] parts = rawStatus.split(":", -1);
        String code = parts[0];
        try {
            return switch (code) {
                case "status.brutal" -> Component.translatable(
                        "status.examplemod.brutal",
                        parseInt(parts, 1, 0))
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
                case "status.build" -> Component.translatable(
                        "status.examplemod.build",
                        dots(parseInt(parts, 2, 0)),
                        parseInt(parts, 1, 0))
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
                case "status.recycle" -> Component.translatable(
                        "status.examplemod.recycle",
                        dots(parseInt(parts, 2, 0)),
                        parseInt(parts, 1, 0))
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
                case "status.inventory_full" -> Component.translatable(
                        "status.examplemod.inventory_full",
                        parseInt(parts, 1, 0))
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
                case "status.low_space" -> Component.translatable(
                        "status.examplemod.low_space",
                        parseInt(parts, 1, 0))
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
                case "status.scavenge" -> Component.translatable(
                        "status.examplemod.scavenge",
                        dots(parseInt(parts, 1, 0)))
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
                case "status.purge" -> Component.translatable(
                        "status.examplemod.purge",
                        dots(parseInt(parts, 2, 0)),
                        parseInt(parts, 1, 0))
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                default -> Component.literal(rawStatus);
            };
        } catch (Exception ignored) {
            return Component.literal(rawStatus);
        }
    }


    private static int parseInt(String[] parts, int index, int fallback) {
        if (index >= parts.length) {
            return fallback;
        }
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String dots(int count) {
        if (count <= 0) {
            return "";
        }
        return ".".repeat(Math.min(3, count));
    }
}
