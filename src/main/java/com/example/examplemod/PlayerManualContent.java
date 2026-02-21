package com.example.examplemod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PlayerManualContent {
    private PlayerManualContent() {}

    public interface SectionProvider {
        List<Section> provide();
    }

    public record Section(String id, String titleKey, List<Entry> entries) {}

    public record Entry(
            String id,
            String sectionId,
            String titleKey,
            String summaryKey,
            String bodyKey,
            String usageKey,
            String faqKey,
            String iconItemId
    ) {
        public ItemStack iconStack() {
            if (iconItemId == null || iconItemId.isBlank()) {
                return ItemStack.EMPTY;
            }
            ResourceLocation id = ResourceLocation.tryParse(iconItemId.toLowerCase(Locale.ROOT));
            if (id == null) {
                return ItemStack.EMPTY;
            }
            var item = ForgeRegistries.ITEMS.getValue(id);
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }
    }

    private static final List<SectionProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    static {
        registerProvider(PlayerManualContent::coreSections);
    }

    public static void registerProvider(SectionProvider provider) {
        if (provider != null) {
            PROVIDERS.add(provider);
        }
    }

    public static List<Section> allSections() {
        List<Section> result = new ArrayList<>();
        for (SectionProvider provider : PROVIDERS) {
            result.addAll(provider.provide());
        }
        return result;
    }

    private static List<Section> coreSections() {
        List<Section> sections = new ArrayList<>();

        sections.add(new Section("basic", "manual.examplemod.section.basic", List.of(
                new Entry("basic_start", "basic",
                        "manual.examplemod.entry.basic_start.title",
                        "manual.examplemod.entry.basic_start.summary",
                        "manual.examplemod.entry.basic_start.body",
                        "manual.examplemod.entry.basic_start.usage",
                        "manual.examplemod.entry.basic_start.faq",
                        "examplemod:player_manual"),
                new Entry("basic_controls", "basic",
                        "manual.examplemod.entry.basic_controls.title",
                        "manual.examplemod.entry.basic_controls.summary",
                        "manual.examplemod.entry.basic_controls.body",
                        "manual.examplemod.entry.basic_controls.usage",
                        "manual.examplemod.entry.basic_controls.faq",
                        "examplemod:turret_wand")
        )));

        sections.add(new Section("skeleton", "manual.examplemod.section.skeleton", List.of(
                new Entry("skeleton_follow", "skeleton",
                        "manual.examplemod.entry.skeleton_follow.title",
                        "manual.examplemod.entry.skeleton_follow.summary",
                        "manual.examplemod.entry.skeleton_follow.body",
                        "manual.examplemod.entry.skeleton_follow.usage",
                        "manual.examplemod.entry.skeleton_follow.faq",
                        "examplemod:turret_wand"),
                new Entry("skeleton_guard", "skeleton",
                        "manual.examplemod.entry.skeleton_guard.title",
                        "manual.examplemod.entry.skeleton_guard.summary",
                        "manual.examplemod.entry.skeleton_guard.body",
                        "manual.examplemod.entry.skeleton_guard.usage",
                        "manual.examplemod.entry.skeleton_guard.faq",
                        "minecraft:shield"),
                new Entry("skeleton_captain", "skeleton",
                        "manual.examplemod.entry.skeleton_captain.title",
                        "manual.examplemod.entry.skeleton_captain.summary",
                        "manual.examplemod.entry.skeleton_captain.body",
                        "manual.examplemod.entry.skeleton_captain.usage",
                        "manual.examplemod.entry.skeleton_captain.faq",
                        "minecraft:golden_helmet")
        )));

        sections.add(new Section("items", "manual.examplemod.section.items", List.of(
                new Entry("item_wand", "items",
                        "manual.examplemod.entry.item_wand.title",
                        "manual.examplemod.entry.item_wand.summary",
                        "manual.examplemod.entry.item_wand.body",
                        "manual.examplemod.entry.item_wand.usage",
                        "manual.examplemod.entry.item_wand.faq",
                        "examplemod:turret_wand"),
                new Entry("item_glitch_chip", "items",
                        "manual.examplemod.entry.item_glitch_chip.title",
                        "manual.examplemod.entry.item_glitch_chip.summary",
                        "manual.examplemod.entry.item_glitch_chip.body",
                        "manual.examplemod.entry.item_glitch_chip.usage",
                        "manual.examplemod.entry.item_glitch_chip.faq",
                        "examplemod:glitch_chip"),
                new Entry("item_terminal", "items",
                        "manual.examplemod.entry.item_terminal.title",
                        "manual.examplemod.entry.item_terminal.summary",
                        "manual.examplemod.entry.item_terminal.body",
                        "manual.examplemod.entry.item_terminal.usage",
                        "manual.examplemod.entry.item_terminal.faq",
                        "examplemod:summon_terminal"),
                new Entry("item_tp_module", "items",
                        "manual.examplemod.entry.item_tp_module.title",
                        "manual.examplemod.entry.item_tp_module.summary",
                        "manual.examplemod.entry.item_tp_module.body",
                        "manual.examplemod.entry.item_tp_module.usage",
                        "manual.examplemod.entry.item_tp_module.faq",
                        "examplemod:teleport_upgrade_module"),
                new Entry("item_multishot_module", "items",
                        "manual.examplemod.entry.item_multishot_module.title",
                        "manual.examplemod.entry.item_multishot_module.summary",
                        "manual.examplemod.entry.item_multishot_module.body",
                        "manual.examplemod.entry.item_multishot_module.usage",
                        "manual.examplemod.entry.item_multishot_module.faq",
                        "examplemod:multi_shot_upgrade_module"),
                new Entry("item_core_module_example", "items",
                        "manual.examplemod.entry.item_core_module_example.title",
                        "manual.examplemod.entry.item_core_module_example.summary",
                        "manual.examplemod.entry.item_core_module_example.body",
                        "manual.examplemod.entry.item_core_module_example.usage",
                        "manual.examplemod.entry.item_core_module_example.faq",
                        "minecraft:nether_star"),
                new Entry("item_death_plaque", "items",
                        "manual.examplemod.entry.item_death_plaque.title",
                        "manual.examplemod.entry.item_death_plaque.summary",
                        "manual.examplemod.entry.item_death_plaque.body",
                        "manual.examplemod.entry.item_death_plaque.usage",
                        "manual.examplemod.entry.item_death_plaque.faq",
                        "examplemod:death_record_card"),
                new Entry("item_player_manual", "items",
                        "manual.examplemod.entry.item_player_manual.title",
                        "manual.examplemod.entry.item_player_manual.summary",
                        "manual.examplemod.entry.item_player_manual.body",
                        "manual.examplemod.entry.item_player_manual.usage",
                        "manual.examplemod.entry.item_player_manual.faq",
                        "examplemod:player_manual")
        )));

        sections.add(new Section("advanced", "manual.examplemod.section.advanced", List.of(
                new Entry("advanced_mining", "advanced",
                        "manual.examplemod.entry.advanced_mining.title",
                        "manual.examplemod.entry.advanced_mining.summary",
                        "manual.examplemod.entry.advanced_mining.body",
                        "manual.examplemod.entry.advanced_mining.usage",
                        "manual.examplemod.entry.advanced_mining.faq",
                        "minecraft:diamond_pickaxe"),
                new Entry("advanced_troubleshooting", "advanced",
                        "manual.examplemod.entry.advanced_troubleshooting.title",
                        "manual.examplemod.entry.advanced_troubleshooting.summary",
                        "manual.examplemod.entry.advanced_troubleshooting.body",
                        "manual.examplemod.entry.advanced_troubleshooting.usage",
                        "manual.examplemod.entry.advanced_troubleshooting.faq",
                        "minecraft:book")
        )));

        sections.add(new Section("upgrade", "manual.examplemod.section.upgrade", List.of(
                new Entry("upgrade_module_path", "upgrade",
                        "manual.examplemod.entry.upgrade_module_path.title",
                        "manual.examplemod.entry.upgrade_module_path.summary",
                        "manual.examplemod.entry.upgrade_module_path.body",
                        "manual.examplemod.entry.upgrade_module_path.usage",
                        "manual.examplemod.entry.upgrade_module_path.faq",
                        "minecraft:anvil"),
                new Entry("upgrade_quick_commands", "upgrade",
                        "manual.examplemod.entry.upgrade_quick_commands.title",
                        "manual.examplemod.entry.upgrade_quick_commands.summary",
                        "manual.examplemod.entry.upgrade_quick_commands.body",
                        "manual.examplemod.entry.upgrade_quick_commands.usage",
                        "manual.examplemod.entry.upgrade_quick_commands.faq",
                        "minecraft:command_block")
        )));

        return sections;
    }
}
