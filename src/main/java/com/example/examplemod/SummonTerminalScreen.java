package com.example.examplemod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SummonTerminalScreen extends AbstractContainerScreen<SummonTerminalMenu> {
    private static final ResourceLocation TEX = new ResourceLocation("examplemod", "textures/gui/summon_terminal.png");

    // Expanded design canvas, rendered at fixed pixel scale.
    private static final int BASE_W = 264;
    private static final int BASE_H = 320;
    private static final float UI_SCALE = 1.5f;

    // Top baseline layout
    private static final int TOP_Y = 18;
    private static final int THUMB_X = 10;
    private static final int THUMB_W = 34;
    private static final int THUMB_H = 34;
    private static final int NAME_X = 52;
    private static final int ACTION_Y = 24;
    private static final int BTN_SIZE = 18;
    private static final int ACTION_RECALL_X = BASE_W - 58;
    private static final int ACTION_RENAME_X = BASE_W - 34;

    // Content area
    private static final int LIST_X = 10;
    private static final int LIST_Y = 62;
    private static final int LIST_W = 92;
    private static final int LIST_ROW_H = 24;
    private static final int LIST_ROWS = 8;

    private static final int DETAIL_X = 110;
    private static final int DETAIL_Y = 62;
    private static final int DETAIL_W = BASE_W - DETAIL_X - 10;
    private static final int DETAIL_H = 226;

    private static final int CLOSE_X = BASE_W - 16;
    private static final int CLOSE_Y = 4;
    private static final int CLOSE_W = 12;
    private static final int CLOSE_H = 12;

    private static final int PAGE_PREV_X = 96;
    private static final int PAGE_NEXT_X = 152;
    private static final int PAGE_BTN_Y = BASE_H - 20;
    private static final int PAGE_BTN_W = 18;
    private static final int PAGE_BTN_H = 14;

    private static final int SCROLL_W = 4;
    private static final int SCROLL_H = LIST_ROWS * LIST_ROW_H - 2;
    private static final int SCROLL_X = LIST_X + LIST_W - SCROLL_W - 1;

    private final List<SummonTerminalEntry> pageEntries = new ArrayList<>();
    private final Map<UUID, SummonTerminalEntry> cache = new LinkedHashMap<>();

    private int page;
    private int totalPages = 1;
    private int totalCount = 0;
    private UUID selected;

    private EditBox renameBox;
    private boolean renameMode;
    private boolean waitingSnapshot;
    private boolean draggingScroll;

    private int pressRecallTicks;
    private int pressRenameTicks;
    private int pressCloseTicks;
    private int pressPrevTicks;
    private int pressNextTicks;
    private int errorFlashTicks;
    private int renameRequestSeq;
    private String renameErrorKey;

    public SummonTerminalScreen(SummonTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = Math.round(BASE_W * UI_SCALE);
        this.imageHeight = Math.round(BASE_H * UI_SCALE);
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();

        renameBox = new EditBox(this.font, DETAIL_X + 8, DETAIL_Y + DETAIL_H - 20, DETAIL_W - 16, 16,
                Component.translatable("gui.examplemod.summon_terminal.rename"));
        renameBox.setMaxLength(14);
        renameBox.setVisible(false);
        renameBox.setCanLoseFocus(false);

        requestPage(0);

        SummonTerminalSnapshot latest = SummonTerminalClientCache.INSTANCE.latest();
        if (latest != null && isFor(latest.terminalPos())) {
            applySnapshot(latest, toCacheMap(SummonTerminalClientCache.INSTANCE.allCached()));
        }
    }

    public boolean isFor(net.minecraft.core.BlockPos pos) {
        return this.menu.terminalPos().equals(pos);
    }

    public void applySnapshot(SummonTerminalSnapshot snapshot, Map<UUID, SummonTerminalEntry> cached) {
        this.waitingSnapshot = false;
        this.page = snapshot.page();
        this.totalPages = snapshot.totalPages();
        this.totalCount = snapshot.totalCount();
        this.pageEntries.clear();
        this.pageEntries.addAll(snapshot.entries());
        this.cache.clear();
        this.cache.putAll(cached);

        if (selected == null && !pageEntries.isEmpty()) {
            selected = pageEntries.get(0).turretUuid();
        }
        if (selected != null && pageEntries.stream().noneMatch(e -> e.turretUuid().equals(selected)) && !pageEntries.isEmpty()) {
            selected = pageEntries.get(0).turretUuid();
        }
    }

    public void applyDelta(Map<UUID, SummonTerminalEntry> cached) {
        this.cache.clear();
        this.cache.putAll(cached);
        for (int i = 0; i < pageEntries.size(); i++) {
            SummonTerminalEntry old = pageEntries.get(i);
            SummonTerminalEntry now = cache.get(old.turretUuid());
            if (now != null) pageEntries.set(i, now);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);

        double uiX = toUiX(mouseX);
        double uiY = toUiY(mouseY);

        if (isHoverRecall(uiX, uiY)) {
            SummonTerminalEntry e = selectedEntry();
            if (e == null) {
                gfx.renderTooltip(this.font, Component.translatable("gui.examplemod.summon_terminal.no_target"), mouseX, mouseY);
            } else if (!e.hasTeleportModule()) {
                gfx.renderTooltip(this.font, Component.translatable("gui.examplemod.summon_terminal.teleport_module_missing"), mouseX, mouseY);
            } else if (e.following()) {
                gfx.renderTooltip(this.font, Component.translatable("gui.examplemod.summon_terminal.following"), mouseX, mouseY);
            } else {
                gfx.renderTooltip(this.font, Component.translatable("gui.examplemod.summon_terminal.recall_hint"), mouseX, mouseY);
            }
        } else if (isHoverRename(uiX, uiY)) {
            gfx.renderTooltip(this.font, Component.translatable("gui.examplemod.summon_terminal.rename_hint"), mouseX, mouseY);
        }

        if (SummonTerminalClientCache.INSTANCE.isRenameSyncActive()) {
            int frame = SummonTerminalClientCache.INSTANCE.renameSpinnerFrame();
            String dots = ".".repeat(frame + 1);
            Component syncing = Component.translatable("gui.examplemod.summon_terminal.syncing_names", dots);
            int x = this.width - this.font.width(syncing) - 12;
            gfx.drawString(this.font, syncing, x, 8, 0xA8FFD4, false);
        }
        if (renameErrorKey != null && errorFlashTicks > 0) {
            Component err = Component.translatable(renameErrorKey);
            int x = this.width - this.font.width(err) - 12;
            gfx.drawString(this.font, err, x, 22, 0xFF7A7A, false);
        }

        if (renameMode) {
            gfx.renderTooltip(this.font, Component.translatable("gui.examplemod.summon_terminal.rename_confirm_hint"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        gfx.pose().pushPose();
        gfx.pose().translate(leftPos, topPos, 0);
        gfx.pose().scale(UI_SCALE, UI_SCALE, 1.0f);

        // Expanded background with gradient mask.
        gfx.fillGradient(0, 0, BASE_W, BASE_H, 0xE02D1B4E, 0xF00F051A);
        gfx.fillGradient(2, 2, BASE_W - 2, BASE_H - 2, 0x80432568, 0x40130620);

        Component titleComp = Component.translatable("block.examplemod.summon_terminal")
                .copy()
                .withStyle(ChatFormatting.BOLD, ChatFormatting.BLUE);
        gfx.drawString(font, titleComp, (BASE_W - font.width(titleComp)) / 2, 7, 0x55AAFF, false);
        gfx.blit(TEX, 6, 6, 180, 0, 12, 12, 256, 256);

        // Baseline strip (left thumbnail / center name / right actions)
        gfx.fillGradient(8, TOP_Y - 4, BASE_W - 8, TOP_Y + 34, 0x5A422060, 0x2A1B0B2F);
        renderTopRow(gfx);

        // Always-visible full info blocks
        renderList(gfx);
        renderDetail(gfx);
        renderScrollBar(gfx);
        renderTopButtons(gfx);

        String pageText = (page + 1) + "/" + totalPages + " (" + totalCount + ")";
        gfx.drawString(font, pageText, 126 - font.width(pageText) / 2, BASE_H - 16, 0xDDDDDD, false);

        if (waitingSnapshot) {
            int spinner = (int) ((System.currentTimeMillis() / 200) % 4);
            String dots = ".".repeat(spinner);
            gfx.drawString(font, Component.translatable("gui.examplemod.summon_terminal.loading", dots), BASE_W - 68, BASE_H - 16, 0xBBBBFF, false);
        }

        if (renameMode) {
            renameBox.setVisible(true);
            renameBox.render(gfx, toUiIntX(mouseX), toUiIntY(mouseY), partialTick);
        } else {
            renameBox.setVisible(false);
        }

        gfx.pose().popPose();
    }

    private void renderTopRow(GuiGraphics gfx) {
        SummonTerminalEntry e = selectedEntry();

        // Left thumbnail block
        gfx.fill(THUMB_X, TOP_Y, THUMB_X + THUMB_W, TOP_Y + THUMB_H, 0x66251440);
        gfx.fill(THUMB_X + 2, TOP_Y + 2, THUMB_X + THUMB_W - 2, TOP_Y + THUMB_H - 2, 0x88412A66);
        gfx.blit(TEX, THUMB_X + 10, TOP_Y + 10, 228, 20, 12, 12, 256, 256);

        Component name = Component.translatable("gui.examplemod.summon_terminal.no_target");
        if (e != null) {
            String base = displayBaseName(e.baseName(), e.level());
            name = Component.literal(base + "  #" + String.format("%03d", e.unitId()));
        }
        gfx.drawString(font, name, NAME_X, TOP_Y + 12, 0xFFFFFF, false);

        renderActions(gfx);
    }

    private void renderTopButtons(GuiGraphics gfx) {
        int closeBg = pressCloseTicks > 0 ? 0xCCAA6666 : 0x66333333;
        gfx.fill(CLOSE_X, CLOSE_Y, CLOSE_X + CLOSE_W, CLOSE_Y + CLOSE_H, closeBg);
        gfx.drawString(font, "x", CLOSE_X + 3, CLOSE_Y + 2, 0xFFFFFF, false);

        int prevBg = page > 0 ? (pressPrevTicks > 0 ? 0xCC6666AA : 0x66333366) : 0x44222222;
        int nextBg = page < totalPages - 1 ? (pressNextTicks > 0 ? 0xCC6666AA : 0x66333366) : 0x44222222;
        gfx.fill(PAGE_PREV_X, PAGE_BTN_Y, PAGE_PREV_X + PAGE_BTN_W, PAGE_BTN_Y + PAGE_BTN_H, prevBg);
        gfx.fill(PAGE_NEXT_X, PAGE_BTN_Y, PAGE_NEXT_X + PAGE_BTN_W, PAGE_BTN_Y + PAGE_BTN_H, nextBg);
        gfx.drawString(font, "<", PAGE_PREV_X + 6, PAGE_BTN_Y + 3, 0xFFFFFF, false);
        gfx.drawString(font, ">", PAGE_NEXT_X + 6, PAGE_BTN_Y + 3, 0xFFFFFF, false);
    }

    private void renderList(GuiGraphics gfx) {
        gfx.fill(LIST_X - 1, LIST_Y - 1, LIST_X + LIST_W + 1, LIST_Y + SCROLL_H + 1, 0x55321D54);

        for (int i = 0; i < LIST_ROWS; i++) {
            int rowX = LIST_X;
            int rowY = LIST_Y + i * LIST_ROW_H;
            int rowW = LIST_W;
            int rowH = LIST_ROW_H - 2;

            if (i >= pageEntries.size()) {
                gfx.fill(rowX, rowY, rowX + rowW, rowY + rowH, 0x18000000);
                continue;
            }

            SummonTerminalEntry e = pageEntries.get(i);
            boolean selectedRow = e.turretUuid().equals(selected);
            int bg = selectedRow ? 0x33FFFFFF : 0x18000000;
            gfx.fill(rowX, rowY, rowX + rowW, rowY + rowH, bg);
            if (selectedRow) {
                int border = 0xCCB38BFF;
                gfx.fill(rowX, rowY, rowX + rowW, rowY + 2, border);
                gfx.fill(rowX, rowY + rowH - 2, rowX + rowW, rowY + rowH, border);
                gfx.fill(rowX, rowY, rowX + 2, rowY + rowH, border);
                gfx.fill(rowX + rowW - 2, rowY, rowX + rowW, rowY + rowH, border);
            }

            String id = String.format("#%03d", e.unitId());
            gfx.drawString(font, id, rowX + 4, rowY + 3, 0xFFFFFF, false);
            String listName = displayBaseName(e.baseName(), e.level());
            gfx.drawString(font, listName, rowX + 4, rowY + 13, 0x4DFFB8, false);
        }
    }

    private void renderDetail(GuiGraphics gfx) {
        SummonTerminalEntry e = selectedEntry();
        int x = DETAIL_X;
        int y = DETAIL_Y;

        gfx.fill(x - 1, y - 1, x + DETAIL_W + 1, y + DETAIL_H + 1, 0x55321D54);
        gfx.fill(x, y, x + DETAIL_W, y + DETAIL_H, 0x2A0E0618);

        if (e == null) {
            gfx.drawString(font, Component.translatable("gui.examplemod.summon_terminal.no_data"), x + 8, y + 8, 0xCCCCCC, false);
            return;
        }

        float hpPct = e.maxHealth() <= 0 ? 0 : e.health() / e.maxHealth();
        int hpColor = gradient(hpPct);
        gfx.blit(TEX, x + 8, y + 10, 180, 20, 10, 10, 256, 256);
        String hp = ((int) e.health()) + "/" + ((int) e.maxHealth());
        gfx.drawString(font, Component.translatable("gui.examplemod.summon_terminal.health", hp), x + 22, y + 12, hpColor, false);

        gfx.blit(TEX, x + 8, y + 34, 196, 20, 10, 10, 256, 256);
        int barX = x + 22;
        int barY = y + 36;
        int barW = DETAIL_W - 30;
        int fillW = Math.round(Mth.clamp(e.progressPercent(), 0, 100) / 100.0f * barW);
        gfx.fill(barX, barY, barX + barW, barY + 8, 0x33111111);
        if (fillW > 0) {
            gfx.fillGradient(barX, barY, barX + fillW, barY + 8, 0xFF9B59B6, 0xFFF368E0);
        }
        gfx.drawString(font, "LV " + e.level() + "  " + e.progressPercent() + "%", barX + 2, barY + 1, 0xFFFFFF, false);

        gfx.blit(TEX, x + 8, y + 58, 212, 20, 10, 10, 256, 256);
        boolean warn = e.distance() >= 64.0f;
        String dist = String.format("%.1f m", e.distance());
        if (warn) dist = "! " + dist;
        gfx.drawString(font, Component.translatable("gui.examplemod.summon_terminal.distance", dist), x + 22, y + 60, warn ? 0xFFAA33 : 0xDDDDDD, false);

        gfx.drawString(font, Component.translatable("gui.examplemod.summon_terminal.unit_id", String.format("%03d", e.unitId())), x + 8, y + 84, 0xD3C7EC, false);
        gfx.drawString(font, Component.translatable("gui.examplemod.summon_terminal.name", displayBaseName(e.baseName(), e.level())), x + 8, y + 98, 0xD3C7EC, false);
        String statusTextKey = e.following()
                ? "gui.examplemod.summon_terminal.status_following"
                : "gui.examplemod.summon_terminal.status_guard";
        gfx.drawString(font, Component.translatable("gui.examplemod.summon_terminal.status", Component.translatable(statusTextKey)), x + 8, y + 112, 0xD3C7EC, false);
        String moduleTextKey = e.hasTeleportModule()
                ? "gui.examplemod.summon_terminal.installed"
                : "gui.examplemod.summon_terminal.not_installed";
        gfx.drawString(font, Component.translatable("gui.examplemod.summon_terminal.teleport_module", Component.translatable(moduleTextKey)), x + 8, y + 126, 0xD3C7EC, false);

    }

    private void renderActions(GuiGraphics gfx) {
        SummonTerminalEntry e = selectedEntry();

        boolean recallDisabled = e == null || e.following() || !e.hasTeleportModule();
        int recallBg = recallDisabled ? 0x66404040 : 0xAA9B59B6;
        if (pressRecallTicks > 0 && !recallDisabled) recallBg = 0xFFB977D8;
        if (errorFlashTicks > 0 && e != null && !e.hasTeleportModule()) recallBg = 0xAAFF5555;

        gfx.fill(ACTION_RECALL_X, ACTION_Y, ACTION_RECALL_X + BTN_SIZE, ACTION_Y + BTN_SIZE, recallBg);
        gfx.blit(TEX, ACTION_RECALL_X + 3, ACTION_Y + 3, 228, 20, 12, 12, 256, 256);
        if (recallDisabled) {
            gfx.fill(ACTION_RECALL_X, ACTION_Y, ACTION_RECALL_X + BTN_SIZE, ACTION_Y + BTN_SIZE, 0x55000000);
        }

        int renameBg = pressRenameTicks > 0 ? 0xFF777777 : 0x55555555;
        gfx.fill(ACTION_RENAME_X, ACTION_Y, ACTION_RENAME_X + BTN_SIZE, ACTION_Y + BTN_SIZE, renameBg);
        gfx.blit(TEX, ACTION_RENAME_X + 3, ACTION_Y + 3, 244, 20, 12, 12, 256, 256);
    }

    private void renderScrollBar(GuiGraphics gfx) {
        int x = SCROLL_X;
        int y = LIST_Y;
        gfx.fill(x, y, x + SCROLL_W, y + SCROLL_H, 0x33000000);

        if (totalPages <= 1) {
            return;
        }

        int knobH = Math.max(14, SCROLL_H / totalPages);
        int knobY = y + Math.round((SCROLL_H - knobH) * (page / (float) (totalPages - 1)));
        gfx.fill(x, knobY, x + SCROLL_W, knobY + knobH, draggingScroll ? 0xCCB38BFF : 0x88B38BFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        double uiX = toUiX(mouseX);
        double uiY = toUiY(mouseY);

        if (!isInUiBounds(uiX, uiY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (renameMode) {
            if (renameBox.mouseClicked(uiX, uiY, button)) {
                playUiClick();
                return true;
            }
            renameMode = false;
            renameBox.setFocused(false);
            playUiClick();
            return true;
        }

        if (isRect(uiX, uiY, CLOSE_X, CLOSE_Y, CLOSE_W, CLOSE_H)) {
            pressCloseTicks = 4;
            playUiClick();
            onClose();
            return true;
        }

        if (isRect(uiX, uiY, PAGE_PREV_X, PAGE_BTN_Y, PAGE_BTN_W, PAGE_BTN_H) && page > 0) {
            pressPrevTicks = 4;
            requestPage(page - 1);
            playUiClick();
            return true;
        }

        if (isRect(uiX, uiY, PAGE_NEXT_X, PAGE_BTN_Y, PAGE_BTN_W, PAGE_BTN_H) && page < totalPages - 1) {
            pressNextTicks = 4;
            requestPage(page + 1);
            playUiClick();
            return true;
        }

        if (isHoverScroll(uiX, uiY) && totalPages > 1) {
            draggingScroll = true;
            updatePageFromScroll(uiY);
            playUiClick();
            return true;
        }

        for (int i = 0; i < pageEntries.size(); i++) {
            int rowX = LIST_X;
            int rowY = LIST_Y + i * LIST_ROW_H;
            if (isRect(uiX, uiY, rowX, rowY, LIST_W, LIST_ROW_H - 2)) {
                selected = pageEntries.get(i).turretUuid();
                playUiClick();
                return true;
            }
        }

        if (isHoverRecall(uiX, uiY)) {
            SummonTerminalEntry e = selectedEntry();
            if (e != null && !e.following() && e.hasTeleportModule()) {
                PacketHandler.sendToServer(new PacketSummonTerminalAction(menu.terminalPos(), e.turretUuid(), page, PacketSummonTerminalAction.ACTION_RECALL, ""));
                pressRecallTicks = 5;
                playUiClick();
            } else if (e != null && !e.hasTeleportModule()) {
                PacketHandler.sendToServer(new PacketSummonTerminalAction(menu.terminalPos(), e.turretUuid(), page, PacketSummonTerminalAction.ACTION_RECALL, ""));
                errorFlashTicks = 10;
                playUiError();
            } else {
                playUiError();
            }
            return true;
        }

        if (isHoverRename(uiX, uiY)) {
            SummonTerminalEntry e = selectedEntry();
            if (e == null) {
                playUiError();
                return true;
            }
            renameMode = true;
            renameBox.setValue(e.baseName());
            renameBox.setFocused(true);
            pressRenameTicks = 5;
            playUiClick();
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScroll) {
            updatePageFromScroll(toUiY(mouseY));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScroll) {
            draggingScroll = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        double uiX = toUiX(mouseX);
        double uiY = toUiY(mouseY);
        if (isHoverListOrScroll(uiX, uiY) && totalPages > 1) {
            int target = page + (delta < 0 ? 1 : -1);
            requestPage(Mth.clamp(target, 0, totalPages - 1));
            playUiClick();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (renameMode) {
            if (keyCode == 257 || keyCode == 335) {
                confirmRename();
                playUiClick();
                return true;
            }
            if (keyCode == 256) {
                renameMode = false;
                renameBox.setFocused(false);
                playUiClick();
                return true;
            }
            return renameBox.keyPressed(keyCode, scanCode, modifiers) || renameBox.canConsumeInput();
        }

        if (keyCode == 262) {
            requestPage(Mth.clamp(page + 1, 0, totalPages - 1));
            playUiClick();
            return true;
        }
        if (keyCode == 263) {
            requestPage(Mth.clamp(page - 1, 0, totalPages - 1));
            playUiClick();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (renameMode) {
            return renameBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void confirmRename() {
        SummonTerminalEntry e = selectedEntry();
        if (e != null) {
            String value = renameBox.getValue().trim();
            if (!value.isEmpty()) {
                int requestId = ++renameRequestSeq;
                renameErrorKey = null;
                SummonTerminalClientCache.INSTANCE.markRenameSyncStart(menu.terminalPos(), e.turretUuid(), requestId, e.baseName(), value);
                PacketHandler.sendToServer(new PacketSummonTerminalAction(
                        menu.terminalPos(),
                        e.turretUuid(),
                        page,
                        PacketSummonTerminalAction.ACTION_RENAME,
                        value,
                        requestId
                ));
                waitingSnapshot = true;
            } else {
                playUiError();
            }
        }
        renameMode = false;
        renameBox.setFocused(false);
    }

    private boolean isHoverRecall(double uiX, double uiY) {
        return isRect(uiX, uiY, ACTION_RECALL_X, ACTION_Y, BTN_SIZE, BTN_SIZE);
    }

    private boolean isHoverRename(double uiX, double uiY) {
        return isRect(uiX, uiY, ACTION_RENAME_X, ACTION_Y, BTN_SIZE, BTN_SIZE);
    }

    private boolean isHoverScroll(double uiX, double uiY) {
        return isRect(uiX, uiY, SCROLL_X, LIST_Y, SCROLL_W, SCROLL_H);
    }

    private boolean isHoverListOrScroll(double uiX, double uiY) {
        return isRect(uiX, uiY, LIST_X, LIST_Y, LIST_W, SCROLL_H) || isHoverScroll(uiX, uiY);
    }

    private void updatePageFromScroll(double uiY) {
        float pct = (float) ((uiY - LIST_Y) / (double) SCROLL_H);
        pct = Mth.clamp(pct, 0f, 1f);
        int target = Math.round(pct * (totalPages - 1));
        if (target != page) {
            requestPage(target);
        }
    }

    private SummonTerminalEntry selectedEntry() {
        if (selected == null) return null;
        for (SummonTerminalEntry e : pageEntries) {
            if (e.turretUuid().equals(selected)) return e;
        }
        return cache.get(selected);
    }

    private void requestPage(int p) {
        this.page = p;
        this.waitingSnapshot = true;
        PacketHandler.sendToServer(new PacketSummonTerminalRequest(menu.terminalPos(), p));
    }

    private static String levelName(int level) {
        String roman = switch (Math.max(1, Math.min(level, 10))) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            default -> "X";
        };
        return Component.translatable("gui.examplemod.summon_terminal.level_name", roman).getString();
    }

    private static String displayBaseName(String baseName, int level) {
        if (baseName == null || baseName.isBlank()) {
            return levelName(level);
        }
        if (SkeletonTurret.DEFAULT_BASE_NAME_TOKEN.equals(baseName) || SkeletonTurret.isLegacyDefaultBaseName(baseName)) {
            return Component.translatable("name.examplemod.turret.base_default").getString();
        }
        return baseName;
    }

    private static int gradient(float pct) {
        pct = Mth.clamp(pct, 0f, 1f);
        int r;
        int g;
        if (pct > 0.5f) {
            float t = (pct - 0.5f) / 0.5f;
            r = (int) Mth.lerp(t, 0xFF, 0x55);
            g = 0xFF;
        } else {
            float t = pct / 0.5f;
            r = 0xFF;
            g = (int) Mth.lerp(t, 0x55, 0xFF);
        }
        return (r << 16) | (g << 8) | 0x55;
    }

    private static Map<UUID, SummonTerminalEntry> toCacheMap(List<SummonTerminalEntry> list) {
        Map<UUID, SummonTerminalEntry> map = new LinkedHashMap<>();
        for (SummonTerminalEntry e : list) {
            map.put(e.turretUuid(), e);
        }
        return map;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        SummonTerminalClientCache.INSTANCE.clientTick();
        String renameError = SummonTerminalClientCache.INSTANCE.consumeRenameErrorKey();
        if (renameError != null) {
            renameErrorKey = renameError;
            errorFlashTicks = Math.max(errorFlashTicks, 25);
            waitingSnapshot = false;
            playUiError();
        }
        if (pressRecallTicks > 0) pressRecallTicks--;
        if (pressRenameTicks > 0) pressRenameTicks--;
        if (pressCloseTicks > 0) pressCloseTicks--;
        if (pressPrevTicks > 0) pressPrevTicks--;
        if (pressNextTicks > 0) pressNextTicks--;
        if (errorFlashTicks > 0) errorFlashTicks--;
    }

    private void playUiClick() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    private void playUiError() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.8f));
        }
    }

    private double toUiX(double mouseX) {
        return (mouseX - leftPos) / UI_SCALE;
    }

    private double toUiY(double mouseY) {
        return (mouseY - topPos) / UI_SCALE;
    }

    private int toUiIntX(int mouseX) {
        return (int) Math.floor(toUiX(mouseX));
    }

    private int toUiIntY(int mouseY) {
        return (int) Math.floor(toUiY(mouseY));
    }

    private boolean isInUiBounds(double uiX, double uiY) {
        return uiX >= 0 && uiY >= 0 && uiX <= BASE_W && uiY <= BASE_H;
    }

    private boolean isRect(double uiX, double uiY, int x, int y, int w, int h) {
        return uiX >= x && uiX <= x + w && uiY >= y && uiY <= y + h;
    }
}
