package com.example.examplemod;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Mobile terminal UI with sci-fi style and percentage-based anchor layout.
 *
 * <p>The root panel is intentionally reduced to 40% of the previous full-screen-style layout
 * to address oversized UI on common 1080p/1440p clients.</p>
 *
 * <p>All user-facing text is localized through language keys so zh_cn and en_us render naturally.</p>
 */
public class MobileTerminalScreen extends AbstractContainerScreen<MobileTerminalMenu> {
    private static final int BG_MAIN = 0xC0101010;
    private static final int BG_PANEL = 0xB0151518;
    private static final int CYAN = 0xFF00FFFF;
    private static final int NEON_GREEN = 0xFF39FF14;
    private static final float ROOT_SHRINK_RATIO = 0.40f;

    private final List<MobileTerminalEntry> entries = new ArrayList<>();
    private UUID selectedUuid;
    private int scrollOffset;
    private boolean fullSnapshotRequested;

    private UiRect root = UiRect.empty();
    private UiRect leftPanel = UiRect.empty();
    private UiRect rightPanel = UiRect.empty();
    private UiRect bottomPanel = UiRect.empty();
    private UiRect listViewport = UiRect.empty();
    private UiRect followBtn = UiRect.empty();
    private UiRect guardBtn = UiRect.empty();
    private UiRect omniBtn = UiRect.empty();
    private UiRect defenseBtn = UiRect.empty();
    private UiRect scavengeBtn = UiRect.empty();
    private UiRect purgeBtn = UiRect.empty();
    private UiRect recallBtn = UiRect.empty();

    public MobileTerminalScreen(MobileTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 0;
        this.imageHeight = 0;
        this.titleLabelY = 10000;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        updateLayout();
        syncData();
    }

    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        updateLayout();
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // Fully custom text rendering in renderBg.
    }

    @Override
    public void renderBackground(GuiGraphics gfx) {
        gfx.fillGradient(0, 0, this.width, this.height, BG_MAIN, BG_MAIN);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        drawPanel(gfx, root, 0xA0121218, CYAN);
        drawPanel(gfx, leftPanel, BG_PANEL, CYAN);
        drawPanel(gfx, rightPanel, BG_PANEL, NEON_GREEN);
        drawPanel(gfx, bottomPanel, BG_PANEL, CYAN);

        gfx.drawString(this.font, t("gui.examplemod.mobile_terminal.header"),
                root.x + 8, root.y + 8, 0xFFB8FFFF, false);

        renderLeftList(gfx);
        renderRightStatus(gfx, mouseX, mouseY);
        renderBottomButtons(gfx, mouseX, mouseY);
        renderOuterButtons(gfx, mouseX, mouseY);
    }

    /**
     * Updates all UI anchor rectangles by percentages of the current client viewport.
     *
     * <p>Algorithm:
     * 1) Build a large base panel from screen margins.
     * 2) Shrink to 40% to match requested smaller size.
     * 3) Center the root panel.
     * 4) Split into left/right content and bottom command bar by percentages.</p>
     */
    private void updateLayout() {
        int marginX = Math.max(12, Math.round(this.width * 0.06f));
        int marginY = Math.max(12, Math.round(this.height * 0.08f));

        int baseW = Math.max(280, this.width - marginX * 2);
        int baseH = Math.max(180, this.height - marginY * 2);

        int panelW = Math.max(420, Math.round(baseW * ROOT_SHRINK_RATIO));
        int panelH = Math.max(260, Math.round(baseH * ROOT_SHRINK_RATIO));
        panelW = Math.min(panelW, this.width - 24);
        panelH = Math.min(panelH, this.height - 24);

        int rootX = (this.width - panelW) / 2;
        int rootY = (this.height - panelH) / 2;
        int gap = Math.max(6, Math.round(panelW * 0.015f));
        int titleBand = 22;
        int bottomH = Math.max(36, Math.round(panelH * 0.18f));
        int contentH = Math.max(120, panelH - titleBand - gap - bottomH);

        int leftW = Math.max(140, Math.round(panelW * 0.40f));
        int rightW = Math.max(180, panelW - leftW - gap);

        root = new UiRect(rootX, rootY, panelW, panelH);
        leftPanel = new UiRect(root.x, root.y + titleBand, leftW, contentH);
        rightPanel = new UiRect(leftPanel.right() + gap, root.y + titleBand, rightW, contentH);
        bottomPanel = new UiRect(root.x, root.y + titleBand + contentH + gap, panelW, bottomH);

        listViewport = leftPanel.inset(6, 20, 6, 6);

        int rightBtnW = Math.max(66, Math.round(rightPanel.w * 0.32f));
        int rightBtnH = Math.max(18, Math.round(rightPanel.h * 0.11f));
        int rightBtnGap = Math.max(6, Math.round(rightPanel.w * 0.025f));
        int rightBtnY = rightPanel.y + rightPanel.h - rightBtnH - 8;
        followBtn = new UiRect(rightPanel.x + 10, rightBtnY, rightBtnW, rightBtnH);
        guardBtn = new UiRect(followBtn.right() + rightBtnGap, rightBtnY, rightBtnW, rightBtnH);

        int bottomBtnGap = Math.max(8, Math.round(bottomPanel.w * 0.02f));
        int bottomBtnW = (bottomPanel.w - bottomBtnGap - 16) / 2;
        int bottomBtnH = Math.max(20, Math.round(bottomPanel.h * 0.62f));
        int bottomBtnY = bottomPanel.y + (bottomPanel.h - bottomBtnH) / 2;
        omniBtn = new UiRect(bottomPanel.x + 4, bottomBtnY, bottomBtnW, bottomBtnH);
        defenseBtn = new UiRect(omniBtn.right() + bottomBtnGap, bottomBtnY, bottomBtnW, bottomBtnH);

        int outerW = Math.max(88, Math.round(root.w * 0.16f));
        int outerH = Math.max(18, Math.round(root.h * 0.10f));
        int outerGap = 6;
        int outerX = root.right() + 8;
        if (outerX + outerW > this.width - 6) {
            outerX = Math.max(6, root.x - outerW - 8);
        }
        int outerY = root.y + 24;
        scavengeBtn = new UiRect(outerX, outerY, outerW, outerH);
        purgeBtn = new UiRect(outerX, scavengeBtn.bottom() + outerGap, outerW, outerH);
        recallBtn = new UiRect(outerX, purgeBtn.bottom() + outerGap, outerW, outerH);
    }

    private void renderLeftList(GuiGraphics gfx) {
        gfx.drawString(this.font, t("gui.examplemod.mobile_terminal.left_title"),
                leftPanel.x + 6, leftPanel.y + 6, 0xFF98FFFF, false);

        gfx.fill(listViewport.x, listViewport.y, listViewport.right(), listViewport.bottom(), 0x55101014);
        drawBorder(gfx, listViewport, 0x6600FFFF);

        int rowH = Math.max(24, Math.round(listViewport.h * 0.16f));
        int visibleRows = Math.max(1, listViewport.h / rowH);
        int maxScroll = Math.max(0, entries.size() - visibleRows);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        for (int i = 0; i < visibleRows; i++) {
            int idx = scrollOffset + i;
            if (idx >= entries.size()) break;
            MobileTerminalEntry e = entries.get(idx);

            int rowY = listViewport.y + i * rowH;
            UiRect row = new UiRect(listViewport.x + 2, rowY + 1, listViewport.w - 4, rowH - 2);
            boolean selected = e.turretUuid().equals(selectedUuid);
            gfx.fill(row.x, row.y, row.right(), row.bottom(), selected ? 0x6640A8A8 : 0x33202024);
            if (selected) drawBorder(gfx, row, CYAN);

            String id = String.format(Locale.ROOT, "%03d", e.unitId());
            String level = String.valueOf(e.level());
            Component line1 = t("gui.examplemod.mobile_terminal.list_line1", id, resolveDisplayName(e), level);
            Component line2 = t("gui.examplemod.mobile_terminal.list_line2",
                    String.format(Locale.ROOT, "%.1f", e.attack()), e.shortUuid());
            drawClippedString(gfx, line1, row.x + 4, row.y + 3, row.w - 8, 0xFFE6FFFF);
            drawClippedString(gfx, line2, row.x + 4, row.y + 12, row.w - 98, 0xFF8BE0E0);

            float hpPct = e.maxHealth() <= 0 ? 0 : Mth.clamp(e.health() / e.maxHealth(), 0f, 1f);
            int barX = row.x + row.w - 86;
            int barY = row.y + row.h - 6;
            int barW = 80;
            gfx.fill(barX, barY, barX + barW, barY + 3, 0x66000000);
            int fillW = Math.max(0, Math.round(barW * hpPct));
            if (fillW > 0) {
                gfx.fillGradient(barX, barY, barX + fillW, barY + 3, 0xFFBB3030, 0xFF30D060);
            }
        }
    }

    private void renderRightStatus(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(this.font, t("gui.examplemod.mobile_terminal.right_title"),
                rightPanel.x + 6, rightPanel.y + 6, 0xFFB8FFB8, false);

        MobileTerminalEntry selected = selectedEntry();
        if (selected == null) {
            gfx.drawString(this.font, t("gui.examplemod.mobile_terminal.no_selection"),
                    rightPanel.x + 10, rightPanel.y + 28, 0xFFAAAAAA, false);
            renderTechButton(gfx, followBtn, t("gui.examplemod.mobile_terminal.action_follow"), false, false);
            renderTechButton(gfx, guardBtn, t("gui.examplemod.mobile_terminal.action_guard"), false, false);
            return;
        }

        int infoX = rightPanel.x + 10;
        int infoY = rightPanel.y + 26;

        drawClippedString(gfx, t("gui.examplemod.mobile_terminal.target_prefix", resolveDisplayName(selected)),
                infoX, infoY, rightPanel.w - 20, 0xFFE0FFE0);
        drawClippedString(gfx, t("gui.examplemod.mobile_terminal.stat_line",
                String.valueOf(selected.level()),
                String.valueOf(Math.round(selected.health())),
                String.valueOf(Math.round(selected.maxHealth()))),
                infoX, infoY + 12, rightPanel.w - 20, 0xFFCFEFCF);
        drawClippedString(gfx, t("gui.examplemod.mobile_terminal.attack_line",
                String.format(Locale.ROOT, "%.1f", selected.attack())),
                infoX, infoY + 24, rightPanel.w - 20, 0xFFCFEFCF);

        int iconY = infoY + 40;
        renderStatusIcon(gfx, infoX, iconY, t("gui.examplemod.mobile_terminal.icon_attack"), 0xFFEF5555, selected.attack() > 0.0f);
        renderStatusIcon(gfx, infoX + 56, iconY, t("gui.examplemod.mobile_terminal.icon_follow"), CYAN, selected.following());
        renderStatusIcon(gfx, infoX + 112, iconY, t("gui.examplemod.mobile_terminal.icon_guard"), NEON_GREEN, !selected.following());

        int boxX = infoX;
        int boxY = iconY + 16;
        int boxW = rightPanel.w - 20;
        int boxH = Math.max(42, followBtn.y - boxY - 6);
        UiRect metricsBox = new UiRect(boxX, boxY, boxW, boxH);
        gfx.fill(metricsBox.x, metricsBox.y, metricsBox.right(), metricsBox.bottom(), 0x3310131A);
        drawBorder(gfx, metricsBox, 0x5539FF14);

        String roleValue = selected.captain()
                ? t("gui.examplemod.mobile_terminal.value_role_captain").getString()
                : (selected.squadMember()
                ? t("gui.examplemod.mobile_terminal.value_role_squad").getString()
                : t("gui.examplemod.mobile_terminal.value_role_reserve").getString());
        String statusValue = selected.following()
                ? t("gui.examplemod.mobile_terminal.value_following").getString()
                : t("gui.examplemod.mobile_terminal.value_guard").getString();
        String moduleValue = selected.hasTeleportModule()
                ? t("gui.examplemod.mobile_terminal.value_level", selected.teleportModuleLevel()).getString()
                : t("gui.examplemod.mobile_terminal.value_none").getString();
        int tpCdSeconds = Math.max(0, (selected.teleportCooldownTicks() + 19) / 20);

        List<Component> leftMetrics = List.of(
                t("gui.examplemod.mobile_terminal.metric_role", roleValue),
                t("gui.examplemod.mobile_terminal.metric_status", statusValue),
                t("gui.examplemod.mobile_terminal.metric_kills", selected.killCount()),
                t("gui.examplemod.mobile_terminal.metric_armor", selected.armor()),
                t("gui.examplemod.mobile_terminal.metric_heat", selected.heat())
        );
        List<Component> rightMetrics = List.of(
                t("gui.examplemod.mobile_terminal.metric_distance", String.format(Locale.ROOT, "%.1f", selected.distance())),
                t("gui.examplemod.mobile_terminal.metric_fire_rate", String.format(Locale.ROOT, "%.1f", selected.fireRate())),
                t("gui.examplemod.mobile_terminal.metric_range", String.format(Locale.ROOT, "%.1f", selected.attackRange())),
                t("gui.examplemod.mobile_terminal.metric_tp_module", moduleValue),
                t("gui.examplemod.mobile_terminal.metric_tp_cd", tpCdSeconds)
        );

        int colGap = 8;
        int colW = (metricsBox.w - 12 - colGap) / 2;
        int leftX = metricsBox.x + 6;
        int rightX = leftX + colW + colGap;
        int lineH = 10;
        int maxRows = Math.max(3, Math.min(5, (metricsBox.h - 18) / lineH));
        for (int i = 0; i < maxRows; i++) {
            int rowY = metricsBox.y + 4 + i * lineH;
            drawClippedString(gfx, leftMetrics.get(i), leftX, rowY, colW, 0xFFCDE6DA);
            drawClippedString(gfx, rightMetrics.get(i), rightX, rowY, colW, 0xFFCDE6DA);
        }

        int barY = metricsBox.bottom() - 8;
        int barX = metricsBox.x + 6;
        int barW = metricsBox.w - 12;
        int progress = Mth.clamp(selected.progressPercent(), 0, 100);
        drawClippedString(gfx, t("gui.examplemod.mobile_terminal.metric_upgrade", progress),
                barX, barY - 10, barW, 0xFF9AE4B8);
        gfx.fill(barX, barY, barX + barW, barY + 3, 0x66151515);
        int fillW = Math.round(barW * (progress / 100.0f));
        if (fillW > 0) {
            gfx.fillGradient(barX, barY, barX + fillW, barY + 3, 0xFF2F8F62, 0xFF45FF8B);
        }

        renderTechButton(gfx, followBtn, t("gui.examplemod.mobile_terminal.action_follow"), followBtn.contains(mouseX, mouseY), true);
        renderTechButton(gfx, guardBtn, t("gui.examplemod.mobile_terminal.action_guard"), guardBtn.contains(mouseX, mouseY), true);
    }

    private void renderBottomButtons(GuiGraphics gfx, int mouseX, int mouseY) {
        renderTechButton(gfx, omniBtn, t("gui.examplemod.mobile_terminal.btn_omni"), omniBtn.contains(mouseX, mouseY), true);
        renderTechButton(gfx, defenseBtn, t("gui.examplemod.mobile_terminal.btn_defense"), defenseBtn.contains(mouseX, mouseY), true);
    }

    private void renderOuterButtons(GuiGraphics gfx, int mouseX, int mouseY) {
        renderTechButton(gfx, scavengeBtn, t("gui.examplemod.mobile_terminal.btn_scavenge"), scavengeBtn.contains(mouseX, mouseY), true);
        renderTechButton(gfx, purgeBtn, t("gui.examplemod.mobile_terminal.btn_purge"), purgeBtn.contains(mouseX, mouseY), true);
        renderTechButton(gfx, recallBtn, t("gui.examplemod.mobile_terminal.btn_recall_all"), recallBtn.contains(mouseX, mouseY), true);
    }

    private void renderTechButton(GuiGraphics gfx, UiRect rect, Component text, boolean hovered, boolean enabled) {
        int bg;
        int border;
        int textColor;
        if (!enabled) {
            bg = 0x66222222;
            border = 0x66444444;
            textColor = 0xFF888888;
        } else if (hovered) {
            bg = 0xAA174448;
            border = 0xFF00FFFF;
            textColor = 0xFFFFFFFF;
        } else {
            bg = 0xAA0E2326;
            border = 0xFF39FF14;
            textColor = 0xFFCBFFEE;
        }
        gfx.fill(rect.x, rect.y, rect.right(), rect.bottom(), bg);
        drawBorder(gfx, rect, border);
        drawCenteredFittedText(gfx, text, rect, textColor);
    }

    private void renderStatusIcon(GuiGraphics gfx, int x, int y, Component label, int color, boolean active) {
        int iconW = 10;
        int iconH = 10;
        int c = active ? color : 0xFF4A4A4A;
        gfx.fill(x, y, x + iconW, y + iconH, c);
        gfx.drawString(this.font, fit(label, 40), x + 13, y + 1, active ? 0xFFEFFFFF : 0xFF909090, false);
    }

    private void drawPanel(GuiGraphics gfx, UiRect rect, int fillColor, int borderColor) {
        gfx.fill(rect.x, rect.y, rect.right(), rect.bottom(), fillColor);
        drawBorder(gfx, rect, borderColor);
    }

    private static void drawBorder(GuiGraphics gfx, UiRect rect, int color) {
        gfx.fill(rect.x, rect.y, rect.right(), rect.y + 1, color);
        gfx.fill(rect.x, rect.bottom() - 1, rect.right(), rect.bottom(), color);
        gfx.fill(rect.x, rect.y, rect.x + 1, rect.bottom(), color);
        gfx.fill(rect.right() - 1, rect.y, rect.right(), rect.bottom(), color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (listViewport.contains(mouseX, mouseY)) {
            int rowH = Math.max(24, Math.round(listViewport.h * 0.16f));
            int visibleRows = Math.max(1, listViewport.h / rowH);
            int relativeRow = (int) ((mouseY - listViewport.y) / rowH);
            int idx = scrollOffset + relativeRow;
            if (relativeRow >= 0 && relativeRow < visibleRows && idx >= 0 && idx < entries.size()) {
                selectedUuid = entries.get(idx).turretUuid();
                playUiClick();
                return true;
            }
        }

        MobileTerminalEntry selected = selectedEntry();
        if (followBtn.contains(mouseX, mouseY) && selected != null) {
            PacketHandler.sendToServer(new PacketMobileTerminalAction(PacketMobileTerminalAction.ACTION_SET_FOLLOW, selected.turretUuid()));
            playUiClick();
            return true;
        }
        if (guardBtn.contains(mouseX, mouseY) && selected != null) {
            PacketHandler.sendToServer(new PacketMobileTerminalAction(PacketMobileTerminalAction.ACTION_SET_GUARD, selected.turretUuid()));
            playUiClick();
            return true;
        }
        if (omniBtn.contains(mouseX, mouseY)) {
            PacketHandler.sendToServer(new PacketMobileTerminalAction(PacketMobileTerminalAction.ACTION_OMNI_SUMMON, null));
            return true;
        }
        if (defenseBtn.contains(mouseX, mouseY)) {
            PacketHandler.sendToServer(new PacketMobileTerminalAction(PacketMobileTerminalAction.ACTION_ABS_DEFENSE, null));
            playUiClick();
            return true;
        }
        if (scavengeBtn.contains(mouseX, mouseY)) {
            PacketHandler.sendToServer(new PacketMobileTerminalAction(PacketMobileTerminalAction.ACTION_TEAM_SCAVENGE, null));
            playUiClick();
            return true;
        }
        if (purgeBtn.contains(mouseX, mouseY)) {
            PacketHandler.sendToServer(new PacketMobileTerminalAction(PacketMobileTerminalAction.ACTION_TEAM_PURGE, null));
            playUiClick();
            return true;
        }
        if (recallBtn.contains(mouseX, mouseY)) {
            PacketHandler.sendToServer(new PacketMobileTerminalAction(PacketMobileTerminalAction.ACTION_RECALL_ALL, null));
            playUiClick();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!listViewport.contains(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        int rowH = Math.max(24, Math.round(listViewport.h * 0.16f));
        int visibleRows = Math.max(1, listViewport.h / rowH);
        int maxScroll = Math.max(0, entries.size() - visibleRows);
        int next = scrollOffset + (delta < 0 ? 1 : -1);
        scrollOffset = Mth.clamp(next, 0, maxScroll);
        return true;
    }

    /**
     * Requests initial full data exactly once when the GUI opens.
     * Later state changes should be propagated by delta packets only.
     */
    private void syncData() {
        if (fullSnapshotRequested) return;
        fullSnapshotRequested = true;
        PacketHandler.sendToServer(new PacketMobileTerminalRequest());
    }

    public void applySnapshot(List<MobileTerminalEntry> snapshot) {
        UUID keep = selectedUuid;
        this.entries.clear();
        this.entries.addAll(snapshot);
        if (keep != null && entries.stream().anyMatch(e -> e.turretUuid().equals(keep))) {
            selectedUuid = keep;
        } else {
            selectedUuid = entries.isEmpty() ? null : entries.get(0).turretUuid();
        }
    }

    public void applyDelta(MobileTerminalEntry delta) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).turretUuid().equals(delta.turretUuid())) {
                entries.set(i, delta);
                return;
            }
        }
        entries.add(delta);
    }

    public void removeEntry(UUID turretUuid) {
        entries.removeIf(e -> e.turretUuid().equals(turretUuid));
        if (selectedUuid != null && selectedUuid.equals(turretUuid)) {
            selectedUuid = entries.isEmpty() ? null : entries.get(0).turretUuid();
        }
    }

    private MobileTerminalEntry selectedEntry() {
        if (selectedUuid == null) return null;
        for (MobileTerminalEntry e : entries) {
            if (e.turretUuid().equals(selectedUuid)) {
                return e;
            }
        }
        return null;
    }

    private Component t(String key, Object... args) {
        return Component.translatable(key, args);
    }

    private String resolveDisplayName(MobileTerminalEntry entry) {
        return TurretTextResolver.resolveBaseName(entry.displayName()).getString();
    }

    private String fit(Component text, int maxWidth) {
        String raw = text.getString();
        if (this.font.width(raw) <= maxWidth) return raw;
        String ellipsis = "...";
        int safe = Math.max(0, maxWidth - this.font.width(ellipsis));
        String cut = this.font.plainSubstrByWidth(raw, safe);
        return cut + ellipsis;
    }

    private void drawCenteredFittedText(GuiGraphics gfx, Component text, UiRect rect, int color) {
        String shown = fit(text, Math.max(8, rect.w - 8));
        int tx = rect.x + (rect.w - this.font.width(shown)) / 2;
        int ty = rect.y + (rect.h - 8) / 2;
        gfx.drawString(this.font, shown, tx, ty, color, false);
    }

    private void drawClippedString(GuiGraphics gfx, Component text, int x, int y, int maxWidth, int color) {
        gfx.drawString(this.font, fit(text, maxWidth), x, y, color, false);
    }

    private void playUiClick() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    private record UiRect(int x, int y, int w, int h) {
        static UiRect empty() {
            return new UiRect(0, 0, 0, 0);
        }

        int right() {
            return x + w;
        }

        int bottom() {
            return y + h;
        }

        UiRect inset(int left, int top, int right, int bottom) {
            int nx = x + left;
            int ny = y + top;
            int nw = Math.max(0, w - left - right);
            int nh = Math.max(0, h - top - bottom);
            return new UiRect(nx, ny, nw, nh);
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx <= right() && my >= y && my <= bottom();
        }
    }
}
