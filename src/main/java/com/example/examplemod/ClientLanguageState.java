package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@OnlyIn(Dist.CLIENT)
public final class ClientLanguageState {
    private static final AtomicReference<String> CURRENT_LANGUAGE = new AtomicReference<>("en_us");
    private static final AtomicBoolean ENGLISH_US = new AtomicBoolean(true);

    private ClientLanguageState() {}

    public static void refreshFromClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getLanguageManager() == null) {
            CURRENT_LANGUAGE.set("en_us");
            ENGLISH_US.set(true);
            return;
        }

        String selected = mc.getLanguageManager().getSelected();
        if (selected == null || selected.isBlank()) {
            selected = "en_us";
        }
        selected = selected.toLowerCase(Locale.ROOT);
        CURRENT_LANGUAGE.set(selected);
        ENGLISH_US.set("en_us".equals(selected));
    }

    public static @NotNull String currentLanguage() {
        return CURRENT_LANGUAGE.get();
    }

    public static boolean isEnglishUs() {
        return ENGLISH_US.get();
    }

    public static Component tr(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static Component trOrEnglishFallback(String key, String englishFallback, Object... args) {
        if (I18n.exists(key)) {
            return Component.translatable(key, args);
        }
        if (args == null || args.length == 0) {
            return Component.literal(englishFallback);
        }
        return Component.literal(String.format(Locale.US, englishFallback, args));
    }
}
