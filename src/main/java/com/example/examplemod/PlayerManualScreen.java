package com.example.examplemod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PlayerManualScreen extends Screen {
    private static final int PANEL_W = 428;
    private static final int PANEL_H = 286;
    private static final int ROW_H = 18;

    private static final int SLOT_SIZE = 18;
    private static final int SLOT_GAP = 4;
    private static final int RESULT_GAP = 8;
    private static final int DETAIL_SCROLLBAR_W = 12;

    private final ItemStack sourceStack;
    private final int sourceSlot;

    private final List<PlayerManualContent.Section> sections = PlayerManualContent.allSections();
    private final List<PlayerManualContent.Entry> visibleEntries = new ArrayList<>();
    private final Set<String> bookmarks = new LinkedHashSet<>();

    private String activeSection = "all";
    private String searchQuery = "";
    private boolean bookmarkOnly = false;
    private boolean detailCollapsed = false;

    private int selectedEntryIndex = 0;
    private int listScroll = 0;
    private int detailScroll = 0;

    private int left;
    private int top;

    private int detailTextLeft;
    private int detailTextTop;
    private int detailTextWidth;
    private int detailTextHeight;
    private int detailMaxScroll;
    private boolean detailDragActive;

    private EditBox searchBox;
    private Button bookmarkButton;
    private Button detailToggleButton;

    public PlayerManualScreen(ItemStack sourceStack, int sourceSlot) {
        super(Component.translatable("manual.examplemod.title"));
        this.sourceStack = sourceStack;
        this.sourceSlot = sourceSlot;
        this.bookmarks.addAll(PlayerManualItem.readBookmarks(sourceStack));
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_W) / 2;
        this.top = (this.height - PANEL_H) / 2;

        int x = left + 10;
        addRenderableWidget(Button.builder(Component.translatable("manual.examplemod.section.all"), b -> {
            activeSection = "all";
            rebuildEntries();
        }).bounds(x, top + 8, 48, 16).build());
        x += 52;

        addRenderableWidget(Button.builder(Component.translatable("manual.examplemod.section.basic"), b -> {
            activeSection = "basic";
            rebuildEntries();
        }).bounds(x, top + 8, 62, 16).build());
        x += 66;

        addRenderableWidget(Button.builder(Component.translatable("manual.examplemod.section.skeleton"), b -> {
            activeSection = "skeleton";
            rebuildEntries();
        }).bounds(x, top + 8, 68, 16).build());
        x += 72;

        addRenderableWidget(Button.builder(Component.translatable("manual.examplemod.section.items"), b -> {
            activeSection = "items";
            rebuildEntries();
        }).bounds(x, top + 8, 58, 16).build());
        x += 62;

        addRenderableWidget(Button.builder(Component.translatable("manual.examplemod.section.upgrade"), b -> {
            activeSection = "upgrade";
            rebuildEntries();
        }).bounds(x, top + 8, 64, 16).build());
        x += 68;

        addRenderableWidget(Button.builder(Component.translatable("manual.examplemod.section.advanced"), b -> {
            activeSection = "advanced";
            rebuildEntries();
        }).bounds(x, top + 8, 68, 16).build());

        addRenderableWidget(Button.builder(Component.translatable("manual.examplemod.bookmarks_only"), b -> {
            bookmarkOnly = !bookmarkOnly;
            rebuildEntries();
        }).bounds(left + PANEL_W - 102, top + 28, 92, 16).build());

        searchBox = new EditBox(this.font, left + 10, top + 28, 196, 16, Component.translatable("manual.examplemod.search_hint"));
        searchBox.setMaxLength(64);
        searchBox.setResponder(s -> {
            searchQuery = s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
            rebuildEntries();
        });
        addRenderableWidget(searchBox);
        setInitialFocus(searchBox);

        bookmarkButton = addRenderableWidget(Button.builder(ClientLanguageState.tr("manual.examplemod.ui.bookmark_off"), b -> toggleBookmark())
                .bounds(left + PANEL_W - 92, top + 50, 38, 16)
                .build());

        detailToggleButton = addRenderableWidget(Button.builder(ClientLanguageState.tr("manual.examplemod.ui.collapse"), b -> {
            detailCollapsed = !detailCollapsed;
            detailScroll = 0;
            syncDetailToggleLabel();
        }).bounds(left + PANEL_W - 50, top + 50, 40, 16).build());

        rebuildEntries();
    }

    private void rebuildEntries() {
        String selectedId = getSelectedEntry() == null ? null : getSelectedEntry().id();
        visibleEntries.clear();
        for (PlayerManualContent.Section section : sections) {
            for (PlayerManualContent.Entry entry : section.entries()) {
                if (!"all".equals(activeSection) && !entry.sectionId().equals(activeSection)) {
                    continue;
                }
                if (bookmarkOnly && !bookmarks.contains(entry.id())) {
                    continue;
                }
                if (!searchQuery.isEmpty() && !matchesSearch(entry, searchQuery)) {
                    continue;
                }
                visibleEntries.add(entry);
            }
        }

        selectedEntryIndex = 0;
        if (selectedId != null) {
            for (int i = 0; i < visibleEntries.size(); i++) {
                if (visibleEntries.get(i).id().equals(selectedId)) {
                    selectedEntryIndex = i;
                    break;
                }
            }
        }

        listScroll = Mth.clamp(listScroll, 0, Math.max(0, visibleEntries.size() - listVisibleCount()));
        detailScroll = 0;
        detailDragActive = false;
        updateBookmarkButton();
        syncDetailToggleLabel();
    }

    private boolean matchesSearch(PlayerManualContent.Entry entry, String query) {
        String haystack = (I18n.get(entry.titleKey()) + " "
                + I18n.get(entry.summaryKey()) + " "
                + I18n.get(entry.bodyKey()) + " "
                + I18n.get(entry.usageKey()) + " "
                + I18n.get(entry.faqKey())).toLowerCase(Locale.ROOT);
        return haystack.contains(query);
    }

    private int listVisibleCount() {
        return (PANEL_H - 82) / ROW_H;
    }

    private PlayerManualContent.Entry getSelectedEntry() {
        if (visibleEntries.isEmpty()) {
            return null;
        }
        selectedEntryIndex = Mth.clamp(selectedEntryIndex, 0, visibleEntries.size() - 1);
        return visibleEntries.get(selectedEntryIndex);
    }

    private void toggleBookmark() {
        PlayerManualContent.Entry entry = getSelectedEntry();
        if (entry == null) {
            return;
        }
        if (bookmarks.contains(entry.id())) {
            bookmarks.remove(entry.id());
        } else {
            bookmarks.add(entry.id());
        }
        updateBookmarkButton();
        PacketHandler.sendToServer(new PacketManualBookmarkUpdate(sourceSlot, PlayerManualItem.toList(bookmarks)));
        if (bookmarkOnly) {
            rebuildEntries();
        }
    }

    private void updateBookmarkButton() {
        if (bookmarkButton == null) {
            return;
        }
        PlayerManualContent.Entry entry = getSelectedEntry();
        boolean marked = entry != null && bookmarks.contains(entry.id());
        bookmarkButton.setMessage(ClientLanguageState.tr(marked
                ? "manual.examplemod.ui.bookmark_on"
                : "manual.examplemod.ui.bookmark_off"));
        bookmarkButton.active = entry != null;
    }

    private void syncDetailToggleLabel() {
        if (detailToggleButton != null) {
            detailToggleButton.setMessage(ClientLanguageState.tr(detailCollapsed
                    ? "manual.examplemod.ui.expand"
                    : "manual.examplemod.ui.collapse"));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listLeft = left + 10;
        int listTop = top + 54;
        int listW = 152;
        int listH = PANEL_H - 64;
        if (isInside(mouseX, mouseY, listLeft, listTop, listW, listH)) {
            int row = ((int) mouseY - listTop) / ROW_H;
            int idx = listScroll + row;
            if (idx >= 0 && idx < visibleEntries.size()) {
                selectedEntryIndex = idx;
                detailScroll = 0;
                updateBookmarkButton();
                syncDetailToggleLabel();
                return true;
            }
        }

        if (!detailCollapsed && detailMaxScroll > 0) {
            int[] bar = detailScrollbarRect();
            if (bar != null && isInside(mouseX, mouseY, bar[0], bar[1], bar[2], bar[3])) {
                detailDragActive = true;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        detailDragActive = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (detailDragActive && detailMaxScroll > 0) {
            int[] track = detailScrollbarTrackRect();
            int[] thumb = detailScrollbarRect();
            if (track == null || thumb == null) {
                return false;
            }

            int thumbH = thumb[3];
            int movable = Math.max(1, track[3] - thumbH);
            int target = Mth.clamp((int) mouseY - track[1] - (thumbH / 2), 0, movable);
            detailScroll = Mth.clamp(Math.round((float) target / movable * detailMaxScroll), 0, detailMaxScroll);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int listLeft = left + 10;
        int listTop = top + 54;
        int listW = 152;
        int listH = PANEL_H - 64;

        if (isInside(mouseX, mouseY, listLeft, listTop, listW, listH)) {
            int maxScroll = Math.max(0, visibleEntries.size() - listVisibleCount());
            listScroll = Mth.clamp(listScroll - (int) Math.signum(delta), 0, maxScroll);
            return true;
        }

        if (!detailCollapsed && isInside(mouseX, mouseY, detailTextLeft, detailTextTop, detailTextWidth + DETAIL_SCROLLBAR_W + 2, detailTextHeight)) {
            detailScroll = Mth.clamp(detailScroll - (int) Math.signum(delta), 0, Math.max(0, detailMaxScroll));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);
        gfx.fill(left, top, left + PANEL_W, top + PANEL_H, 0xE0101024);
        gfx.fill(left + 2, top + 2, left + PANEL_W - 2, top + PANEL_H - 2, 0xE0201A3A);

        gfx.drawString(this.font, Component.translatable("manual.examplemod.title"), left + 10, top + 36, 0xFFFFFF, false);
        gfx.drawString(this.font, Component.translatable("manual.examplemod.version", PlayerManualItem.CURRENT_VERSION), left + PANEL_W - 118, top + 36, 0x9FB2FF, false);

        searchBox.render(gfx, mouseX, mouseY, partialTick);

        renderEntryList(gfx);
        renderEntryDetail(gfx);

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void renderEntryList(GuiGraphics gfx) {
        int listLeft = left + 10;
        int listTop = top + 54;
        int listW = 152;
        int listH = PANEL_H - 64;
        gfx.fill(listLeft, listTop, listLeft + listW, listTop + listH, 0x55222A44);

        int start = listScroll;
        int end = Math.min(visibleEntries.size(), start + listVisibleCount());
        int y = listTop + 2;
        for (int i = start; i < end; i++) {
            PlayerManualContent.Entry entry = visibleEntries.get(i);
            boolean selected = i == selectedEntryIndex;
            int bg = selected ? 0x66A080FF : 0x44223355;
            gfx.fill(listLeft + 2, y, listLeft + listW - 2, y + ROW_H - 2, bg);
            if (bookmarks.contains(entry.id())) {
                gfx.drawString(this.font, I18n.get("manual.examplemod.ui.bookmark_on"), listLeft + 6, y + 5, 0xFFD452, false);
            }
            gfx.drawString(this.font, Component.translatable(entry.titleKey()), listLeft + 18, y + 5, 0xFFFFFF, false);
            y += ROW_H;
        }
    }

    private void renderEntryDetail(GuiGraphics gfx) {
        int detailLeft = left + 170;
        int detailTop = top + 54;
        int detailW = PANEL_W - 180;
        int detailH = PANEL_H - 64;

        gfx.fill(detailLeft, detailTop, detailLeft + detailW, detailTop + detailH, 0x55302854);

        PlayerManualContent.Entry selected = getSelectedEntry();
        if (selected == null) {
            gfx.drawString(this.font, Component.translatable("manual.examplemod.empty"), detailLeft + 10, detailTop + 10, 0xBBBBBB, false);
            detailTextLeft = detailLeft + 8;
            detailTextTop = detailTop + 42;
            detailTextWidth = Math.max(1, detailW - 24 - DETAIL_SCROLLBAR_W);
            detailTextHeight = Math.max(1, detailH - 48);
            detailMaxScroll = 0;
            return;
        }

        gfx.drawString(this.font, Component.translatable(selected.titleKey()), detailLeft + 10, detailTop + 10, 0x6EC0FF, false);
        ItemStack icon = selected.iconStack();
        if (!icon.isEmpty()) {
            gfx.renderItem(icon, detailLeft + detailW - 24, detailTop + 8);
        }
        gfx.drawString(this.font, Component.translatable(selected.summaryKey()), detailLeft + 10, detailTop + 26, 0xA8FFD4, false);

        int contentStartY = detailTop + 42;
        RecipeLayout recipe = recipeForEntry(selected);
        if (recipe != null) {
            contentStartY = renderRecipeLayout(gfx, recipe, detailLeft + 8, contentStartY, detailW - 16);
            contentStartY += 6;
        } else if (isUpgradeEntry(selected)) {
            contentStartY = renderUpgradeVisuals(gfx, selected, detailLeft + 8, contentStartY, detailW - 16);
            contentStartY += 6;
        }

        detailTextLeft = detailLeft + 8;
        detailTextTop = contentStartY;
        detailTextWidth = Math.max(1, detailW - 24 - DETAIL_SCROLLBAR_W);
        detailTextHeight = Math.max(1, detailTop + detailH - contentStartY - 8);

        if (detailCollapsed) {
            gfx.drawString(this.font, ClientLanguageState.tr("manual.examplemod.ui.collapsed_hint"), detailTextLeft, detailTextTop, 0xBFCDE0, false);
            detailMaxScroll = 0;
            detailScroll = 0;
            return;
        }

        List<FormattedCharSequence> lines = buildDetailLines(selected, recipe, detailTextWidth);
        int maxVisible = Math.max(1, detailTextHeight / this.font.lineHeight);
        detailMaxScroll = Math.max(0, lines.size() - maxVisible);
        detailScroll = Mth.clamp(detailScroll, 0, detailMaxScroll);

        int lineY = detailTextTop;
        for (int i = detailScroll; i < lines.size() && i < detailScroll + maxVisible; i++) {
            gfx.drawString(this.font, lines.get(i), detailTextLeft, lineY, 0xE8E8E8, false);
            lineY += this.font.lineHeight;
        }

        renderDetailScrollbar(gfx);
    }

    private int renderRecipeLayout(GuiGraphics gfx, RecipeLayout recipe, int x, int y, int width) {
        gfx.drawString(this.font, ClientLanguageState.tr("manual.examplemod.ui.crafting_grid"), x, y, 0x6EC0FF, false);
        int gridX = x;
        int gridY = y + 10;

        int gridW = SLOT_SIZE * 3 + SLOT_GAP * 2;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = gridX + col * (SLOT_SIZE + SLOT_GAP);
                int slotY = gridY + row * (SLOT_SIZE + SLOT_GAP);
                renderSlot(gfx, slotX, slotY, SLOT_SIZE);

                ItemStack stack = recipe.grid[row * 3 + col];
                if (!stack.isEmpty()) {
                    gfx.renderItem(stack, slotX + 1, slotY + 1);
                }
            }
        }

        int arrowX = gridX + gridW + 2;
        int arrowY = gridY + SLOT_SIZE + (SLOT_GAP / 2) - 3;
        gfx.drawString(this.font, Component.literal("->"), arrowX, arrowY, 0xD5DDE8, false);

        int resultX = gridX + gridW + RESULT_GAP + 16;
        int resultY = gridY + SLOT_SIZE + (SLOT_GAP / 2) - 2;
        renderSlot(gfx, resultX, resultY, SLOT_SIZE);
        if (!recipe.result.isEmpty()) {
            gfx.renderItem(recipe.result, resultX + 1, resultY + 1);
        }

        if (recipe.exampleOnly) {
            gfx.drawString(this.font, ClientLanguageState.tr("manual.examplemod.ui.example"), Math.min(resultX + SLOT_SIZE + 6, x + width - 50), resultY + 5, 0xFFDD66, false);
        }

        return gridY + SLOT_SIZE * 3 + SLOT_GAP * 2;
    }

    private boolean isUpgradeEntry(PlayerManualContent.Entry entry) {
        if (entry == null) {
            return false;
        }
        return "upgrade_module_path".equals(entry.id()) || "upgrade_quick_commands".equals(entry.id());
    }

    private int renderUpgradeVisuals(GuiGraphics gfx, PlayerManualContent.Entry entry, int x, int y, int width) {
        if ("upgrade_module_path".equals(entry.id())) {
            return renderUpgradeModuleFlow(gfx, x, y);
        }
        return renderUpgradeCommandFlow(gfx, x, y);
    }

    private int renderUpgradeModuleFlow(GuiGraphics gfx, int x, int y) {
        gfx.drawString(this.font, ClientLanguageState.tr("manual.examplemod.ui.upgrade_flow_title"), x, y, 0x6EC0FF, false);
        int rowY = y + 12;
        ItemStack anvil = stack(Items.ANVIL);

        ItemStack tpLv1 = stack(ExampleMod.TELEPORT_UPGRADE_MODULE.get());
        TeleportUpgradeItem.setLevel(tpLv1, 1);
        ItemStack tpLv2 = stack(ExampleMod.TELEPORT_UPGRADE_MODULE.get());
        TeleportUpgradeItem.setLevel(tpLv2, 2);
        ItemStack tpMat = stackFromId(TurretUpgradeTierPlan.tierByLevel(2).materialId());
        renderUpgradeStepRow(gfx, x, rowY, tpLv1, tpMat, anvil, tpLv2, I18n.get("manual.examplemod.ui.tp_lv1_to_lv2"));

        rowY += 22;
        ItemStack msLv1 = stack(ExampleMod.MULTI_SHOT_UPGRADE_MODULE.get());
        MultiShotUpgradeModuleItem.setLevel(msLv1, 1);
        ItemStack msLv2 = stack(ExampleMod.MULTI_SHOT_UPGRADE_MODULE.get());
        MultiShotUpgradeModuleItem.setLevel(msLv2, 2);
        ItemStack msMat = stackFromId(TurretUpgradeTierPlan.tierByLevel(2).materialId());
        renderUpgradeStepRow(gfx, x, rowY, msLv1, msMat, anvil, msLv2, I18n.get("manual.examplemod.ui.ms_lv1_to_lv2"));

        rowY += 24;
        gfx.drawString(this.font, ClientLanguageState.tr("manual.examplemod.ui.tier_material_chain"), x, rowY, 0xA8FFD4, false);
        rowY += 10;
        for (int level = 2; level <= TurretUpgradeTierPlan.maxLevel(); level++) {
            TurretUpgradeTierPlan.CommonTier tier = TurretUpgradeTierPlan.tierByLevel(level);
            ItemStack mat = stackFromId(tier.materialId());
            int px = x + (level - 2) * 28;
            renderSlot(gfx, px, rowY, SLOT_SIZE);
            if (!mat.isEmpty()) {
                gfx.renderItem(mat, px + 1, rowY + 1);
            }
            gfx.drawString(this.font, "L" + level, px + 2, rowY + 20, 0xCFD9E7, false);
        }
        return rowY + 32;
    }

    private void renderUpgradeStepRow(GuiGraphics gfx, int x, int y, ItemStack module, ItemStack material, ItemStack anvil, ItemStack result, String label) {
        renderSlot(gfx, x, y, SLOT_SIZE);
        gfx.renderItem(module, x + 1, y + 1);
        gfx.drawString(this.font, "+", x + 22, y + 5, 0xD8E0EB, false);

        renderSlot(gfx, x + 30, y, SLOT_SIZE);
        if (!material.isEmpty()) {
            gfx.renderItem(material, x + 31, y + 1);
        }
        gfx.drawString(this.font, "+", x + 52, y + 5, 0xD8E0EB, false);

        renderSlot(gfx, x + 60, y, SLOT_SIZE);
        gfx.renderItem(anvil, x + 61, y + 1);

        gfx.drawString(this.font, "->", x + 82, y + 5, 0xD8E0EB, false);
        renderSlot(gfx, x + 96, y, SLOT_SIZE);
        gfx.renderItem(result, x + 97, y + 1);
        gfx.drawString(this.font, label, x + 118, y + 5, 0xCFD9E7, false);
    }

    private int renderUpgradeCommandFlow(GuiGraphics gfx, int x, int y) {
        gfx.drawString(this.font, ClientLanguageState.tr("manual.examplemod.ui.upgrade_commands_title"), x, y, 0x6EC0FF, false);
        int rowY = y + 12;

        ItemStack cmd = stack(Items.COMMAND_BLOCK);
        ItemStack module = stack(ExampleMod.TELEPORT_UPGRADE_MODULE.get());
        ItemStack plaque = stack(ExampleMod.DEATH_RECORD_ITEM.get());
        renderCommandRow(gfx, x, rowY, cmd, stack(Items.BOOK), module, "/skull givemodule");

        rowY += 22;
        renderCommandRow(gfx, x, rowY, cmd, stack(Items.ENDER_PEARL), plaque, "/skull tpplaque <x> <y> <z>");

        rowY += 24;
        gfx.drawString(this.font, ClientLanguageState.tr("manual.examplemod.ui.command_flow_hint"), x, rowY, 0xA8FFD4, false);
        rowY += 12;
        gfx.drawString(this.font, ClientLanguageState.tr("manual.examplemod.ui.command_permission_hint"), x, rowY, 0xCFD9E7, false);
        return rowY + 10;
    }

    private void renderCommandRow(GuiGraphics gfx, int x, int y, ItemStack leftIcon, ItemStack centerIcon, ItemStack rightIcon, String text) {
        renderSlot(gfx, x, y, SLOT_SIZE);
        gfx.renderItem(leftIcon, x + 1, y + 1);
        gfx.drawString(this.font, "->", x + 21, y + 5, 0xD8E0EB, false);

        renderSlot(gfx, x + 34, y, SLOT_SIZE);
        gfx.renderItem(centerIcon, x + 35, y + 1);
        gfx.drawString(this.font, "->", x + 55, y + 5, 0xD8E0EB, false);

        renderSlot(gfx, x + 68, y, SLOT_SIZE);
        gfx.renderItem(rightIcon, x + 69, y + 1);
        gfx.drawString(this.font, text, x + 90, y + 5, 0xCFD9E7, false);
    }

    private void renderSlot(GuiGraphics gfx, int x, int y, int size) {
        gfx.fill(x, y, x + size, y + size, 0xFF141922);
        gfx.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF2E3A49);
        gfx.fill(x + 2, y + 2, x + size - 2, y + size - 2, 0xFF1D2733);
    }

    private List<FormattedCharSequence> buildDetailLines(PlayerManualContent.Entry selected, RecipeLayout recipe, int wrapWidth) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.addAll(this.font.split(Component.literal(I18n.get("manual.examplemod.ui.item_name_header") + " " + I18n.get(selected.titleKey())), wrapWidth));
        lines.addAll(this.font.split(Component.literal(" "), wrapWidth));

        lines.addAll(this.font.split(Component.translatable("manual.examplemod.ui.material_list_header"), wrapWidth));
        if (recipe == null || recipe.materials.isEmpty()) {
            lines.addAll(this.font.split(Component.translatable("manual.examplemod.ui.no_crafting_recipe"), wrapWidth));
        } else {
            for (MaterialLine material : recipe.materials) {
                lines.addAll(this.font.split(Component.literal("- " + material.display()), wrapWidth));
            }
        }
        lines.addAll(this.font.split(Component.literal(" "), wrapWidth));

        lines.addAll(this.font.split(Component.translatable("manual.examplemod.ui.feature_desc_header"), wrapWidth));
        lines.addAll(this.font.split(Component.translatable(selected.bodyKey()), wrapWidth));
        lines.addAll(this.font.split(Component.literal(" "), wrapWidth));

        lines.addAll(this.font.split(Component.translatable("manual.examplemod.ui.usage_condition_header"), wrapWidth));
        lines.addAll(this.font.split(Component.translatable(selected.usageKey()), wrapWidth));

        if (recipe != null && recipe.exampleOnly) {
            lines.addAll(this.font.split(Component.translatable("manual.examplemod.ui.example_recipe_notice"), wrapWidth));
        }

        lines.addAll(this.font.split(Component.literal(" "), wrapWidth));
        lines.addAll(this.font.split(Component.translatable("manual.examplemod.faq_header"), wrapWidth));
        lines.addAll(this.font.split(Component.translatable(selected.faqKey()), wrapWidth));
        return lines;
    }

    private void renderDetailScrollbar(GuiGraphics gfx) {
        if (detailMaxScroll <= 0 || detailTextHeight <= 0) {
            return;
        }

        int[] track = detailScrollbarTrackRect();
        int[] thumb = detailScrollbarRect();
        if (track == null || thumb == null) {
            return;
        }

        gfx.fill(track[0], track[1], track[0] + track[2], track[1] + track[3], 0xFF1E2A37);
        gfx.fill(thumb[0], thumb[1], thumb[0] + thumb[2], thumb[1] + thumb[3], 0xFF5D7791);
    }

    private int[] detailScrollbarTrackRect() {
        if (detailTextHeight <= 0) {
            return null;
        }
        int x = detailTextLeft + detailTextWidth + 2;
        int y = detailTextTop;
        int w = DETAIL_SCROLLBAR_W;
        int h = detailTextHeight;
        return new int[]{x, y, w, h};
    }

    private int[] detailScrollbarRect() {
        if (detailMaxScroll <= 0 || detailTextHeight <= 0) {
            return null;
        }
        int[] track = detailScrollbarTrackRect();
        int thumbH = Math.max(20, detailTextHeight / 4);
        int movable = Math.max(1, detailTextHeight - thumbH);
        int offset = Math.round((float) detailScroll / detailMaxScroll * movable);
        return new int[]{track[0], track[1] + offset, track[2], thumbH};
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private RecipeLayout recipeForEntry(PlayerManualContent.Entry entry) {
        if (entry == null) {
            return null;
        }

        return switch (entry.id()) {
            case "item_wand" -> new RecipeLayout(
                    grid(
                            ItemStack.EMPTY, stack(Items.ROTTEN_FLESH), ItemStack.EMPTY,
                            ItemStack.EMPTY, stack(Items.OBSIDIAN), ItemStack.EMPTY,
                            ItemStack.EMPTY, stack(Items.IRON_BLOCK), ItemStack.EMPTY
                    ),
                    stack(ExampleMod.TURRET_WAND.get()),
                    List.of(
                            MaterialLine.ofItem(stack(Items.ROTTEN_FLESH), 1),
                            MaterialLine.ofItem(stack(Items.OBSIDIAN), 1),
                            MaterialLine.ofItem(stack(Items.IRON_BLOCK), 1)
                    ),
                    false
            );
            case "item_terminal" -> new RecipeLayout(
                    grid(
                            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                            stack(Items.REDSTONE_TORCH), stack(Items.GLOWSTONE), stack(Items.REDSTONE_TORCH),
                            ItemStack.EMPTY, stack(Items.REDSTONE_BLOCK), ItemStack.EMPTY
                    ),
                    stack(ExampleMod.SUMMON_TERMINAL_ITEM.get()),
                    List.of(
                            MaterialLine.ofItem(stack(Items.REDSTONE_TORCH), 2),
                            MaterialLine.ofItem(stack(Items.GLOWSTONE), 1),
                            MaterialLine.ofItem(stack(Items.REDSTONE_BLOCK), 1)
                    ),
                    false
            );
            case "item_tp_module" -> new RecipeLayout(
                    grid(
                            ItemStack.EMPTY, stack(Items.LAPIS_BLOCK), ItemStack.EMPTY,
                            ItemStack.EMPTY, stack(Items.ENDER_PEARL), ItemStack.EMPTY,
                            ItemStack.EMPTY, stack(Items.REDSTONE_BLOCK), ItemStack.EMPTY
                    ),
                    stack(ExampleMod.TELEPORT_UPGRADE_MODULE.get()),
                    List.of(
                            MaterialLine.ofItem(stack(Items.LAPIS_BLOCK), 1),
                            MaterialLine.ofItem(stack(Items.ENDER_PEARL), 1),
                            MaterialLine.ofItem(stack(Items.REDSTONE_BLOCK), 1)
                    ),
                    false
            );
            case "item_multishot_module" -> new RecipeLayout(
                    grid(
                            ItemStack.EMPTY, stack(Items.AMETHYST_SHARD), ItemStack.EMPTY,
                            ItemStack.EMPTY, stack(Items.BOW), ItemStack.EMPTY,
                            ItemStack.EMPTY, stack(Items.STRING), ItemStack.EMPTY
                    ),
                    stack(ExampleMod.MULTI_SHOT_UPGRADE_MODULE.get()),
                    List.of(
                            MaterialLine.ofItem(stack(Items.AMETHYST_SHARD), 1),
                            MaterialLine.ofItem(stack(Items.BOW), 1),
                            MaterialLine.ofItem(stack(Items.STRING), 1)
                    ),
                    false
            );
            case "item_core_module_example" -> new RecipeLayout(
                    grid(
                            stack(Items.REDSTONE_BLOCK), stack(Items.ENDER_PEARL), stack(Items.REDSTONE_BLOCK),
                            stack(Items.GLOWSTONE), stack(ExampleMod.TELEPORT_UPGRADE_MODULE.get()), stack(Items.GLOWSTONE),
                            stack(Items.REDSTONE_BLOCK), stack(ExampleMod.MULTI_SHOT_UPGRADE_MODULE.get()), stack(Items.REDSTONE_BLOCK)
                    ),
                    stack(Items.NETHER_STAR),
                    List.of(
                            MaterialLine.ofItem(stack(Items.REDSTONE_BLOCK), 4),
                            MaterialLine.ofItem(stack(Items.ENDER_PEARL), 1),
                            MaterialLine.ofItem(stack(Items.GLOWSTONE), 2),
                            MaterialLine.ofItem(stack(ExampleMod.TELEPORT_UPGRADE_MODULE.get()), 1),
                            MaterialLine.ofItem(stack(ExampleMod.MULTI_SHOT_UPGRADE_MODULE.get()), 1)
                    ),
                    true
            );
            case "item_player_manual" -> new RecipeLayout(
                    grid(
                            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                            ItemStack.EMPTY, stack(Items.OAK_PLANKS), stack(Items.COBBLESTONE),
                            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
                    ),
                    stack(ExampleMod.PLAYER_MANUAL.get()),
                    List.of(
                            MaterialLine.ofTextKey("manual.examplemod.ui.any_planks", 1),
                            MaterialLine.ofItem(stack(Items.COBBLESTONE), 1)
                    ),
                    false
            );
            default -> null;
        };
    }

    private ItemStack[] grid(ItemStack s0, ItemStack s1, ItemStack s2,
                             ItemStack s3, ItemStack s4, ItemStack s5,
                             ItemStack s6, ItemStack s7, ItemStack s8) {
        return new ItemStack[]{s0, s1, s2, s3, s4, s5, s6, s7, s8};
    }

    private ItemStack stack(Item item) {
        return new ItemStack(item);
    }

    private ItemStack stack(java.util.function.Supplier<? extends Item> supplier) {
        return supplier == null ? ItemStack.EMPTY : new ItemStack(supplier.get());
    }

    private ItemStack stackFromId(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static final class RecipeLayout {
        private final ItemStack[] grid;
        private final ItemStack result;
        private final List<MaterialLine> materials;
        private final boolean exampleOnly;

        private RecipeLayout(ItemStack[] grid, ItemStack result, List<MaterialLine> materials, boolean exampleOnly) {
            this.grid = grid;
            this.result = result;
            this.materials = materials;
            this.exampleOnly = exampleOnly;
        }
    }

    private static final class MaterialLine {
        private final String localizedName;
        private final int count;

        private MaterialLine(String localizedName, int count) {
            this.localizedName = localizedName;
            this.count = count;
        }

        private static MaterialLine ofItem(ItemStack stack, int count) {
            return new MaterialLine(stack.getHoverName().getString(), count);
        }

        private static MaterialLine ofTextKey(String key, int count) {
            return new MaterialLine(I18n.get(key), count);
        }

        private String display() {
            return localizedName + " x" + count;
        }
    }
}
