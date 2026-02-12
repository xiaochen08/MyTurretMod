package com.example.examplemod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.components.Button;
import com.example.examplemod.PacketHandler;
import com.example.examplemod.PacketToggleMode;
import net.minecraft.util.Mth;



public class TurretScreen extends AbstractContainerScreen<TurretMenu> {
    // 确保你的图片放在 assets/examplemod/textures/gui/turret_ui.png
    private static final ResourceLocation TEXTURE = new ResourceLocation("examplemod", "textures/gui/turret_ui.png");
    
    // 🔘 按钮引用
    private Button modeBtn;
    private Button upgradeBtn;
    
    // ✨ 动画过渡变量
    private float cachedRange = -1.0f; // -1 表示未初始化

    public TurretScreen(TurretMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        // 1. 设置 GUI 的真实大小 (根据你的设计图)
        this.imageWidth = 290;
        this.imageHeight = 256;

        // 2. 隐藏原版自带的 "Inventory" 和 "Title" 文字
        // 把它们移到屏幕外面去，防止和我们设计的文字重叠
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }
    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // 留空，什么都不写，就是最好的隐藏！
    }


    @Override
    protected void init() {
        super.init();

        // 计算 GUI 左上角的坐标
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // ==========================================
        // 🛠️ 顶部工具栏设计 (Top Toolbar)
        // ==========================================
        // 将按钮移至右上角，且缩小尺寸 (20x20)，防止遮挡信息
        
        // 1. 战术切换按钮 [T]
        // 位置：右上角 (x + 245, y + 5)
        this.modeBtn = this.addRenderableWidget(new Button.Builder(Component.literal("🛡"), (btn) -> {
             if (this.menu.turret != null) {
                PacketHandler.sendToServer(new PacketToggleMode(this.menu.turret.getId()));
                // 乐观更新
                boolean newState = !this.menu.turret.isFollowMode();
                this.menu.turret.setFollowMode(newState);
            }
        }).bounds(x + 245, y + 5, 20, 20).build()); // 20x20 小按钮

        // 2. 安装模式按钮 [U]
        // 位置：右上角 (x + 268, y + 5)
        this.upgradeBtn = this.addRenderableWidget(new Button.Builder(Component.literal("🔒"), (btn) -> {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
        }).bounds(x + 268, y + 5, 20, 20).build());
    }

    // ✅ 移除旧的 ModeSwitchButton 类 (不再需要)
    // private class ModeSwitchButton extends Button { ... }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);






        // 计算屏幕中心位置
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // ==========================================
        // 🎨 2. 绘制背景图 (核心修复)
        // ==========================================
        // 参数解释：
        // TEXTURE: 图片资源
        // x, y: 屏幕上的画图位置
        // 0, 0: 图片上的起始坐标 (u, v)
        // this.imageWidth, this.imageHeight: 要画多大 (290, 256)
        // 512, 512: 【关键】你的 PNG 图片文件的真实画布大小！
        // (因为 290 > 256，所以必须用 512 的画布，否则 UV 映射会错乱)
        gfx.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 512, 512);

        // 获取实体对象
        SkeletonTurret t = this.menu.turret;

        // ==========================================
        // 🧍‍♂️ 3. 绘制 3D 模型
        // ==========================================
        // ✅ 移除跟随鼠标逻辑，改为固定朝向 (保持 UI 稳定)
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                gfx,
                x + 145,   // 居中 X
                y + 105,   // 居中 Y
                35,        // 缩放
                0,         // 固定 X 偏角
                0,         // 固定 Y 偏角
                t
        );
        // ==========================================
        // ✨ 3. 绘制能力说明 (中间红色)
        // ==========================================
        String abilityTxt = t.getNewAbilityDesc(t.getLevel()); // 这里的 getLevel 对应 tier
        int txtWidth = this.font.width(abilityTxt);
        // 居中显示
        gfx.drawString(this.font, abilityTxt, x + 145 - txtWidth / 2, y + 108, 0xFFFF5555, false);

        // ==========================================
        // 📝 4. 绘制左侧信息
        // ==========================================
        int leftX = x + 15;
        int topY = y + 25;
        int gap = 15;

        // 标题 (深灰色)
        gfx.drawString(this.font, "机体信息", leftX, topY - 12, 0x404040, false);

        // 属性列表
        gfx.drawString(this.font, "❤ 生命值: " + (int)t.getHealth(), leftX, topY, 0xFFFF5555, false);
        gfx.drawString(this.font, "🛡 综合护甲: " + t.getArmorValue(), leftX, topY + gap, 0xFF5555FF, false);
        gfx.drawString(this.font, "📶 机体等级: " + t.getLevel(), leftX, topY + gap*2, 0xFF55FF55, false);
        gfx.drawString(this.font, "🔋 升级进度: " + t.getXp(), leftX, topY + gap*3, 0xFF00FF00, false);


        // ==========================================
        // 📝 5. 绘制右侧信息
        // 📝 绘制右侧信息
        int rightX = x + 200;

        gfx.drawString(this.font, "战术终端", rightX, topY - 12, 0x404040, false);

        // ✅ 已移除：重叠的“状态: xxx”和“战术模式: xxx”文字
        // 现在的状态仅通过右上角的图标按钮 + Tooltip 展示，保持界面整洁

        // 伤害 (上移填补空缺)
        String dmg = String.format("%.1f", t.getAttributeValue(Attributes.ATTACK_DAMAGE));
        gfx.drawString(this.font, "⚔ 武器伤害: " + dmg, rightX, topY, 0xFFFFAA00, false);

        // 热度
        gfx.drawString(this.font, "🔥 武器热度: " + t.getHeat(), rightX, topY + gap, 0xFFFF5555, false);

        // 射速
        float delay = t.getFireDelay();
        String rate = String.format("%.1f/s", 20.0f / (delay > 0 ? delay : 20));
        gfx.drawString(this.font, "🚀 射击频率: " + rate, rightX, topY + gap*2, 0xFF00FFFF, false);

        // 击杀
        gfx.drawString(this.font, "☠ 击杀数: " + t.getKillCount(), rightX, topY + gap*3, 0xFF555555, false);

        // 射程 (带平滑过渡动画)
        // ✅ [Fix] 使用统一的射程计算接口，确保 GUI 显示与实际逻辑一致
        double targetRange = t.getAttackRange();
        
        // 异常处理：确保数值非负
        if (targetRange < 0) targetRange = 0;
        
        // 初始化或更新
        if (this.cachedRange < 0) {
            this.cachedRange = (float)targetRange;
        } else {
            // 使用 lerp 插值实现平滑过渡 (0.1f 为平滑系数)
            this.cachedRange = Mth.lerp(0.1f, this.cachedRange, (float)targetRange);
        }
        
        // 多语言与单位支持
        String rangeStr = String.format("%.0f", this.cachedRange);
        Component rangeText = Component.translatable("gui.examplemod.range_label", rangeStr);
        
        // 如果没有翻译键，默认显示格式 (fallback)
        if (rangeText.getString().equals("gui.examplemod.range_label")) {
             rangeText = Component.literal("🏹 有效射程: " + rangeStr + " m");
        }
        
        gfx.drawString(this.font, rangeText, rightX, topY + gap*4, 0xFFFFFFFF, false);

        // ✅ 新增：传送模块状态显示
        boolean hasTp = t.hasTeleportModule();
        String tpStatus = hasTp ? "§a[已安装]" : "§c[未安装]";
        gfx.drawString(this.font, "传送模块: " + tpStatus, rightX, topY + gap*5, 0xFFFFFFFF, false);
    }

    // 必须保留 render 方法以显示 Tooltip (鼠标悬停提示)
    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        // 1. 先画背景和物品
        super.render(gfx, mouseX, mouseY, partialTick);

        // 2. 画悬浮提示 (Tooltip)
        this.renderTooltip(gfx, mouseX, mouseY);

        // ✅ 3. 动态更新按钮文字
        updateButtonLabels();
    }

    private void updateButtonLabels() {
        // A. 更新战术模式按钮
        if (this.modeBtn != null && this.menu.turret != null) {
            boolean inTeam = this.menu.turret.isFollowMode();
            if (inTeam) {
                this.modeBtn.setMessage(Component.literal("⚔")); // ⚔ = 跟随/战斗
                this.modeBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("§a当前：跟随模式\n§7点击切换至定点守卫")));
            } else {
                this.modeBtn.setMessage(Component.literal("🛡")); // 🛡 = 守卫
                this.modeBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("§c当前：定点守卫\n§7点击切换至跟随模式")));
            }
        }

        // B. 更新升级模式按钮
        if (this.upgradeBtn != null) {
            boolean upgrading = this.menu.isUpgrading();
            if (upgrading) {
                this.upgradeBtn.setMessage(Component.literal("⚡")); // ⚡ = 开启
                this.upgradeBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("§e当前：安装模式\n§7升级槽已解锁")));
            } else {
                this.upgradeBtn.setMessage(Component.literal("🔒")); // 🔒 = 锁定
                this.upgradeBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("§7当前：锁定模式\n§7点击解锁升级槽")));
            }
        }
    }



}