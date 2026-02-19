package com.example.examplemod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class TurretScreen extends AbstractContainerScreen<TurretMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("examplemod", "textures/gui/turret_ui.png");

    private static final int INFO_BAR_X = 181;
    private static final int INFO_BAR_Y = 108;
    private static final int INFO_BAR_WIDTH = 99;
    private static final int INFO_BAR_HEIGHT = 60;
    private static final int INFO_BAR_LINE_HEIGHT = 10;
    private static final int INFO_BAR_VISIBLE_LINES = 5;

    private Button modeBtn;
    private float cachedRange = -1.0f;
    private int infoBarScroll = 0;
    private final TurretInfoBarBuffer infoBarBuffer = new TurretInfoBarBuffer();

    public TurretScreen(TurretMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 290;
        this.imageHeight = 256;
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // Intentionally empty: texture already contains labels.
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        this.modeBtn = this.addRenderableWidget(new Button.Builder(Component.literal("G"), (btn) -> {
            if (this.menu.turret != null) {
                PacketHandler.sendToServer(new PacketToggleMode(this.menu.turret.getId()));
            }
        }).bounds(x + 245, y + 5, 20, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        gfx.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 512, 512);

        SkeletonTurret turret = this.menu.turret;
        if (turret == null) {
            return;
        }

        InventoryScreen.renderEntityInInventoryFollowsMouse(gfx, x + 145, y + 105, 35, 0, 0, turret);

        int leftX = x + 15;
        int topY = y + 25;
        int gap = 15;
        int rightX = x + 200;

        gfx.drawString(this.font, Component.translatable("gui.examplemod.body_info"), leftX, topY - 12, 0x404040, false);
        gfx.drawString(this.font, Component.translatable("gui.examplemod.health_value", (int) turret.getHealth()), leftX, topY, 0xFFFF5555, false);
        gfx.drawString(this.font, Component.translatable("gui.examplemod.armor_value", turret.getArmorValue()), leftX, topY + gap, 0xFF5555FF, false);
        gfx.drawString(this.font, Component.translatable("gui.examplemod.level_value", turret.getLevel()), leftX, topY + gap * 2, 0xFF55FF55, false);
        gfx.drawString(this.font, Component.translatable("gui.examplemod.upgrade_progress_value", turret.getXp()), leftX, topY + gap * 3, 0xFF00FF00, false);

        gfx.drawString(this.font, Component.translatable("gui.examplemod.tactical_terminal"), rightX, topY - 12, 0x404040, false);

        String dmg = String.format("%.1f", turret.getWeaponDamage());
        gfx.drawString(this.font, Component.translatable("gui.examplemod.weapon_damage_value", dmg), rightX, topY, 0xFFFFAA00, false);
        gfx.drawString(this.font, Component.translatable("gui.examplemod.weapon_heat_value", turret.getHeat()), rightX, topY + gap, 0xFFFF5555, false);

        float delay = turret.getFireDelay();
        String rate = String.format("%.1f/s", 20.0f / (delay > 0 ? delay : 20));
        gfx.drawString(this.font, Component.translatable("gui.examplemod.fire_rate_value", rate), rightX, topY + gap * 2, 0xFF00FFFF, false);
        gfx.drawString(this.font, Component.translatable("gui.examplemod.kill_count_value", turret.getKillCount()), rightX, topY + gap * 3, 0xFF555555, false);

        double targetRange = Math.max(0.0, turret.getAttackRange());
        if (this.cachedRange < 0) {
            this.cachedRange = (float) targetRange;
        } else {
            this.cachedRange = Mth.lerp(0.1f, this.cachedRange, (float) targetRange);
        }
        String rangeStr = String.format("%.0f", this.cachedRange);
        gfx.drawString(this.font, Component.translatable("gui.examplemod.range_label", rangeStr), rightX, topY + gap * 4, 0xFFFFFFFF, false);

        String tpStatusKey = turret.hasTeleportModule() ? "gui.examplemod.state_on" : "gui.examplemod.state_off";
        gfx.drawString(this.font, Component.translatable("gui.examplemod.teleport_module_state", Component.translatable(tpStatusKey)), rightX, topY + gap * 5, 0xFFFFFFFF, false);

        if (TurretConfig.getDisplayMode() == TurretConfig.DisplayMode.TRADITIONAL) {
            renderLegacyPrompt(gfx, x, y, turret);
        } else {
            renderInfoBar(gfx, x, y, turret);
        }
    }

    private void renderLegacyPrompt(GuiGraphics gfx, int x, int y, SkeletonTurret turret) {
        // Legacy prompt intentionally hidden.
    }

    private void renderInfoBar(GuiGraphics gfx, int x, int y, SkeletonTurret turret) {
        List<TurretInfoBarBuffer.PromptSlot> slots = collectPromptSlots(turret);
        int barX = x + INFO_BAR_X;
        int barY = y + INFO_BAR_Y;

        gfx.fill(barX, barY, barX + INFO_BAR_WIDTH, barY + INFO_BAR_HEIGHT, 0xA0101010);
        gfx.drawString(this.font, Component.translatable("gui.examplemod.info_bar_title"), barX + 4, barY + 3, 0xFFDDDDDD, false);

        int totalLines = slots.size();
        if (infoBarScroll > Math.max(0, totalLines - INFO_BAR_VISIBLE_LINES)) {
            infoBarScroll = Math.max(0, totalLines - INFO_BAR_VISIBLE_LINES);
        }

        for (int i = 0; i < INFO_BAR_VISIBLE_LINES; i++) {
            int idx = i + infoBarScroll;
            if (idx >= totalLines) {
                break;
            }
            TurretInfoBarBuffer.PromptSlot slot = slots.get(idx);
            int lineY = barY + 14 + (i * INFO_BAR_LINE_HEIGHT);
            int color = idx == 0 ? 0xFFFF5555 : 0xFFE0E0E0;
            gfx.drawString(this.font, toPrefixedComponent(slot), barX + 4, lineY, color, false);
        }

        if (totalLines > INFO_BAR_VISIBLE_LINES) {
            String scrollInfo = (infoBarScroll + 1) + "/" + (totalLines - INFO_BAR_VISIBLE_LINES + 1);
            gfx.drawString(this.font, scrollInfo, barX + INFO_BAR_WIDTH - 28, barY + INFO_BAR_HEIGHT - 9, 0xFFAAAAAA, false);
        }
    }

    private List<TurretInfoBarBuffer.PromptSlot> collectPromptSlots(SkeletonTurret turret) {
        infoBarBuffer.clear();
        long baseSeq = this.minecraft == null || this.minecraft.level == null ? 0L : this.minecraft.level.getGameTime();
        Integer unitId = turret.getEntityData().get(SkeletonTurret.UNIT_ID);

        List<String> prompts = new ArrayList<>();
        prompts.add(turret.getNewAbilityDesc(turret.getLevel()));

        String status = turret.getOverheadStatus();
        if (status != null && !status.isBlank()) {
            prompts.add(TurretTextResolver.resolveOverheadStatus(status).getString().replace('\n', ' '));
        }


        infoBarBuffer.upsertPromptBatch(unitId, prompts, baseSeq);
        return infoBarBuffer.orderedSlots();
    }

    private Component toPrefixedComponent(TurretInfoBarBuffer.PromptSlot slot) {
        Component idText = slot.skeletonId() <= 0
                ? Component.translatable("gui.examplemod.skeleton_id_unknown")
                : Component.literal(String.valueOf(slot.skeletonId()));
        return Component.translatable("gui.examplemod.skeleton_prompt_prefix", idText).append(slot.prompt());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (TurretConfig.getDisplayMode() != TurretConfig.DisplayMode.INFO_BAR) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int barX = x + INFO_BAR_X;
        int barY = y + INFO_BAR_Y;
        boolean inInfoBar = mouseX >= barX && mouseX <= barX + INFO_BAR_WIDTH && mouseY >= barY && mouseY <= barY + INFO_BAR_HEIGHT;
        if (!inInfoBar) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        if (delta < 0) {
            infoBarScroll++;
        } else if (delta > 0) {
            infoBarScroll = Math.max(0, infoBarScroll - 1);
        }
        return true;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
        updateButtonLabels();
    }

    private void updateButtonLabels() {
        if (this.modeBtn == null || this.menu.turret == null) {
            return;
        }
        boolean inTeam = this.menu.turret.isFollowMode();
        if (inTeam) {
            this.modeBtn.setMessage(Component.literal("F"));
            this.modeBtn.setTooltip(Tooltip.create(Component.translatable("gui.examplemod.mode_follow_tooltip")));
        } else {
            this.modeBtn.setMessage(Component.literal("G"));
            this.modeBtn.setTooltip(Tooltip.create(Component.translatable("gui.examplemod.mode_guard_tooltip")));
        }
    }
}
