package com.example.examplemod;


// 📋 请检查并添加这些导包
import java.util.Map;
import java.util.HashMap;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;



import javax.annotation.Nullable;
import java.util.*;



import net.minecraft.world.ContainerListener;
import net.minecraft.world.Container;

public class SkeletonTurret extends net.minecraft.world.entity.monster.Skeleton {



    // ✅ 新增：智能止损变量
    // 记录上一次所在的区块位置
    // 语音冷却记录

    private final Map<TurretDialogue.Type, Long> speechCooldowns = new HashMap<>();
    public Map<TurretDialogue.Type, Long> getSpeechCooldowns() { return speechCooldowns; }
    private net.minecraft.world.level.ChunkPos keptChunkPos;
    private double spawnX, spawnY, spawnZ;
    private long lastHeatStackTime = 0;
    private int consecutiveMisses = 0;   // 连续未造成伤害的次数
    private int blockedSightTime = 0;    // 视线被遮挡的时间 (tick)
    private long lastDamageTimestamp = 0; // 上次造成伤害的时间戳 (用于辅助判断)

    // 🔍 1. 定义跟随模式的数据ID (放在类定义的最上面)
    private static final EntityDataAccessor<Boolean> FOLLOW_MODE = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.BOOLEAN);
    // ✅ 新增：状态同步 (用于 HUD 显示)
    private static final EntityDataAccessor<Boolean> IS_PURGE_ACTIVE = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_SCAVENGING = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.BOOLEAN);
    // ✅ 新增：身份编号 (001-999)
    public static final EntityDataAccessor<Integer> UNIT_ID = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DROP_COUNT = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.INT);
    // RANGE_LEVEL removed - derived from TIER

    // ==================== 🗣️ 头顶显示系统数据 ====================
    // 1. 台词内容 (空字符串代表没说话)
    private static final EntityDataAccessor<String> DATA_DIALOGUE_TEXT = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.STRING);
    // 2. 台词剩余显示时间 (Tick)
    private static final EntityDataAccessor<Integer> DATA_DIALOGUE_TIMER = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.INT);
    // 3. 状态栏内容 (用于显示 ⚠ 25s 自毁 / 🎒 背包已满 等)
    private static final EntityDataAccessor<String> DATA_STATUS_OVERLAY = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.STRING);
    // ✅ 新增：把热度变成同步数据，这样 UI 才能实时看到它跳动！
    private static final EntityDataAccessor<Integer> DATA_HEAT = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> IS_FOLLOWING = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> KILL_COUNT = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_BRUTAL = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> UPGRADE_PROGRESS = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.INT);
    // ✅ 新增：队长标识
    private static final EntityDataAccessor<Boolean> IS_CAPTAIN = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_SQUAD_MEMBER = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.BOOLEAN);
    // ✅ 新增：同步的基础名字 (解决改名后变回原样的问题)
    // ✅ 只保留这一个！这是我们唯一要用的“真名字”
    private static final EntityDataAccessor<String> SYNC_BASE_NAME = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.STRING);
    // ✅ 新增：同步的主人UUID (解决客户端无法获取主人信息的问题)



    // ✅ 新增：主人身份同步通道 (解决 HUD 不显示的核心)
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID_SYNC = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.OPTIONAL_UUID);

    // DATA_LEVEL removed - derived from TIER

    private static final EntityDataAccessor<Integer> DATA_XP = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.INT);

    // 注意：fireDelay 如果是逻辑变量，不需要同步，只需公开访问
    private int decayTimer = 0;
    private int eatCooldown = 0;
    
    // ✅ 新增：传送后攻击延迟和无敌时间
    private int postTeleportAttackDelay = 0;
    private int invincibilityTimer = 0;

    public void notifyTeleport() {
        this.invincibilityTimer = 6; // 0.3s (6 ticks)
        this.postTeleportAttackDelay = 4; // 0.2s (4 ticks)
        this.shotCounter = 0; // 重置攻击节奏
        this.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0F, 1.0F);

        // Speed Boost: 40% - 80% based on tier
        // Tier 0-1: Speed II (+40%)
        // Tier 2-3: Speed III (+60%)
        // Tier 4+: Speed IV (+80%)
        int tier = getTier();
        int amplifier = 1; // Base Speed II
        if (tier >= 4) amplifier = 3;
        else if (tier >= 2) amplifier = 2;
        
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, amplifier)); // 5 seconds duration
        
        // Spawn particles
        spawnTeleportParticles();
    }

    public void spawnTeleportParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            // Use Soul Fire Flame for blue trail effect
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 0.5, this.getZ(), 25, 0.5, 0.5, 0.5, 0.1);
            serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
        }
    }



    private int shotCounter = 0;
    private int overheatCooldown = 0;
    private int xpBuffer = 0;

    // ✅ 新增：强制救援模式开关
    private boolean isCommandRescue = false;

    public void setCommandRescue(boolean rescue) {
        this.isCommandRescue = rescue;
    }

    public boolean isCommandRescue() {
        return this.isCommandRescue;
    }

    // ✅ 新增：狂暴技能的计时器
    private int brutalityActiveTimer = 0;
    private int brutalityCooldown = 0;

    private UUID ownerUUID;
    // ✅ 新增：记录入队时间 (用于排序：谁先来谁在上面)
    private long squadJoinTime = 0;
    // ✅ 新增：传送模块状态
    private static final EntityDataAccessor<Boolean> HAS_TELEPORT_MODULE = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.BOOLEAN);
    // ✅ 新增：传送冷却 (Tick)
    private int teleportCooldown = 0;
    public int getTeleportCooldown() { return this.teleportCooldown; } // Added getter

    // ✅ 新增：死亡记录卡掉落标志 (幂等性校验)
    private boolean deathRecordDropped = false;
    public boolean hasDroppedRecord() { return this.deathRecordDropped; }
    public void setDroppedRecord(boolean dropped) { this.deathRecordDropped = dropped; }

    public final SimpleContainer inventory = new SimpleContainer(45);

    public SkeletonTurret(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
        // ✅ 监听背包变化，检测传送模块
        this.inventory.addListener(new ContainerListener() {
            @Override
            public void containerChanged(Container container) {
                checkTeleportModule();
            }
        });
    }

    private void checkTeleportModule() {
        if (this.level().isClientSide) return;
        boolean hasModule = false;
        // 检查升级槽位 (5-9)
        for (int i = 5; i < 10; i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (stack.getItem() == ExampleMod.TELEPORT_UPGRADE_MODULE.get()) {
                hasModule = true;
                break;
            }
        }
        
        boolean current = this.hasTeleportModule();
        if (hasModule != current) {
            this.setHasTeleportModule(hasModule);
            // 播放音效 (仅在安装时)
            if (hasModule) {
                this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        }
    }

    // ==========================================
    // 🖨️ 3D 打印核心数据 (Phase 1)
    // ==========================================
    // 打印进度：0.0 (无) -> 1.0 (完成)
    private static final EntityDataAccessor<Float> PRINT_PROGRESS = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.FLOAT);

    // 打印状态机：0=正常, 1=打印中, 2=蓝屏死机, 3=逆向回收
    private static final EntityDataAccessor<Integer> PRINT_STATE = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.INT);



    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TIER, 0);
        // RANGE_LEVEL removed

        this.entityData.define(FOLLOW_MODE, false);
        this.entityData.define(IS_FOLLOWING, false);
        this.entityData.define(KILL_COUNT, 0);

        this.entityData.define(IS_BRUTAL, false);
        this.entityData.define(UPGRADE_PROGRESS, 0);
        this.entityData.define(IS_CAPTAIN, false);
        this.entityData.define(IS_SQUAD_MEMBER, false);
        this.entityData.define(OWNER_UUID_SYNC, Optional.empty());
        this.entityData.define(IS_PURGE_ACTIVE, false);
        this.entityData.define(IS_SCAVENGING, false);
        this.entityData.define(UNIT_ID, 0);
        this.entityData.define(DROP_COUNT, 0);
        this.entityData.define(SYNC_BASE_NAME, "先锋队员");
        this.entityData.define(PRINT_PROGRESS, 0.0f);
        this.entityData.define(PRINT_STATE, 0);
        this.entityData.define(DATA_HEAT, 0);
        this.entityData.define(DATA_DIALOGUE_TEXT, "");
        this.entityData.define(DATA_DIALOGUE_TIMER, 0);
        this.entityData.define(DATA_STATUS_OVERLAY, "");
        // DATA_LEVEL removed
        this.entityData.define(DATA_XP, 0);
        this.entityData.define(HAS_TELEPORT_MODULE, false);

    }
    


    // ✅ 新增：强制拾荒模式状态

    public void setCommandScavenging(boolean scavenging) {
        this.entityData.set(IS_SCAVENGING, scavenging); // 存入同步数据
        if (scavenging) {
            this.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 1.0f, 1.0f);
        }
    }

    public boolean isCommandScavenging() {
        return this.entityData.get(IS_SCAVENGING);
    }

    // 🔍 2. 获取当前模式 (true=跟随, false=守卫)
    public boolean isFollowMode() {
        return this.entityData.get(FOLLOW_MODE);
    }

    // 🔍 3. 切换模式 (由数据包调用)
    public void setFollowMode(boolean shouldFollow) {
        // ✅ [Fix] 恢复记录卡召唤实体的自由切换模式功能
        this.entityData.set(FOLLOW_MODE, shouldFollow);
        this.entityData.set(IS_FOLLOWING, shouldFollow); // ✅ 修复：同步更新 AI 使用的状态变量
    }


    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ATTACK_SPEED, 1.0) // ✅ 新增：基础攻速属性 (默认为1.0，即正常倍率)
                .add(Attributes.FOLLOW_RANGE, 256.0); // ✅ 新增：把导航视野扩大到 256 格！
    }


    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        this.updateStatsAndEquip();
        // 👇 设为打印状态，进度归零
        setPrintState(1);
        this.entityData.set(PRINT_PROGRESS, 0.0f);
        this.spawnX = this.getX();
        this.spawnY = this.getY();
        this.spawnZ = this.getZ();
        return result;
    }
    // 重写远程攻击方法，记录射击次数
    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        // ❌ 删除这行：super.performRangedAttack(target, distanceFactor);
        // 原版方法射出的是没有 NBT 标签的普通箭，导致友军伤害判断失效！

        // ✅ 改为调用我们自定义的射击方法 (带 NBT 标签、带特效、带等级)
        this.shootLinearArrow(target, getTier());

        // 记录失误次数 (保持你之前的逻辑)
        this.consecutiveMisses++;
        if (!this.getSensing().hasLineOfSight(target)) {
            this.consecutiveMisses++;
        }
    }
    private void shootLinearArrow(LivingEntity target, int tier) {
        ItemStack itemstack = this.getProjectile(this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof net.minecraft.world.item.BowItem)));
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, itemstack, 1.0f);

        arrow.setOwner(this);
        arrow.getPersistentData().putBoolean("IsTurretArrow", true);
        arrow.getPersistentData().putInt("TurretTier", tier);
        arrow.getPersistentData().putInt("TurretID", this.getId());

        if (this.entityData.get(IS_BRUTAL)) {
            arrow.getPersistentData().putBoolean("IsBrutalArrow", true);
        }

        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setNoGravity(true);

        // ✅ [修改] 获取当前热度 (即攻速层数) -> 叠加 -> 保存
        int currentHeat = this.entityData.get(DATA_HEAT);
        // 攻速叠加逻辑：
        // Tier 0: 0% Boost (Max 0)
        // Tier 1: 225% Boost (Max 30)
        // ...
        // Tier 4: 900% Boost (Max 120) -> Total 1000%
        int maxHeat = getTier() * 30; 
        
        // 每次命中叠加 1 层 (clamp 至上限)
        // ✅ [修正] 攻速叠加间隔控制 (<= 50ms)
        if (currentHeat < maxHeat) {
            long now = System.currentTimeMillis();
            if (now - this.lastHeatStackTime >= 50) {
                this.entityData.set(DATA_HEAT, currentHeat + 1);
                currentHeat++;
                this.lastHeatStackTime = now;
            }
        }
        
        // 记录最后一次射击时间 (用于脱战衰减)
        this.lastDamageTimestamp = this.tickCount;


        double d0 = target.getX() - this.getX();
        double d1 = target.getEyeY() - arrow.getY();
        double d2 = target.getZ() - this.getZ();

        double spread = 0.2;
        double rX = (this.random.nextDouble() - 0.5) * spread;
        double rY = (this.random.nextDouble() - 0.5) * spread;
        double rZ = (this.random.nextDouble() - 0.5) * spread;

        // ✅ 根据射程等级调整箭矢速度 (v^2 正比于射程)
        // Level 1 (20m): ~1.6 (Vanilla 3.0 is ~64m) -> Vanilla default logic uses power based on charge time.
        // Let's scale base velocity: 
        // L1(20m): 3.0 (Vanilla standard) - actually vanilla bow is 3.0 for full charge.
        // But for long range, we need more.
        // L2(32m): 3.5
        // L3(64m): 4.5
        // L4(128m): 6.0
        // L5(256m): 8.0
        float velocity = 3.0F;
        switch(getRangeLevel()) {
            case 2: velocity = 3.5F; break;
            case 3: velocity = 4.5F; break;
            case 4: velocity = 6.0F; break;
            case 5: velocity = 8.0F; break;
        }

        arrow.shoot(d0 + rX, d1 + rY, d2 + rZ, velocity, 0.0F);

        // ✅ [修改] 移除热度伤害加成 (改用攻速流)
        // 伤害公式：基础4 + 等级*5
        double dmg = (4.0 + (tier * 5.0));

        arrow.setBaseDamage(Math.min(dmg, 200.0));
        int pierce = (tier == 5) ? 10 : (tier + 1);
        arrow.setPierceLevel((byte) pierce);

        // ✅ [修改] 音调随热度变高 (听觉反馈)
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F + (currentHeat * 0.005f));
        this.level().addFreshEntity(arrow);
    }


    private void tickCaptainLogic() {
        if (this.level().isClientSide) return;
        if (this.tickCount % 1200 != 0) return; // 60s

        // 评分: Level * 100 + SurvivalTime + GearScore
        int score = getTier() * 100 + (this.tickCount / 1200); 
        for (ItemStack s : this.getArmorSlots()) {
            if (s.isEnchanted()) score += 20;
            if (s.getItem() instanceof net.minecraft.world.item.ArmorItem) score += 10;
        }

        // Scan nearby turrets
        List<SkeletonTurret> friends = this.level().getEntitiesOfClass(SkeletonTurret.class, this.getBoundingBox().inflate(30.0));
        SkeletonTurret currentCaptain = null;
        int maxScore = -1;

        for (SkeletonTurret t : friends) {
            if (t.entityData.get(IS_CAPTAIN)) {
                currentCaptain = t;
                break; 
            }
        }

        if (currentCaptain == null) {
            if (score > 50 && !this.entityData.get(IS_CAPTAIN)) {
                this.entityData.set(IS_CAPTAIN, true);
                this.entityData.set(IS_SQUAD_MEMBER, false);
                this.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                if (this.ownerUUID != null) {
                     Player p = this.level().getPlayerByUUID(this.ownerUUID);
                     if (p != null) p.displayClientMessage(Component.literal("§b[系统] " + this.getDisplayName().getString() + " 已晋升为队长 (评分:" + score + ")"), true);
                }
            }
        } else {
            if (this.entityData.get(IS_CAPTAIN) && currentCaptain != this) {
                // Conflict: downgrade
                this.entityData.set(IS_CAPTAIN, false);
                this.entityData.set(IS_SQUAD_MEMBER, true);
            } else if (!this.entityData.get(IS_CAPTAIN)) {
                this.entityData.set(IS_SQUAD_MEMBER, true);
            }
        }
    }

    private void manageHeatDecay() {
        int currentHeat = this.entityData.get(DATA_HEAT);

        // 脱战判定：5秒 (100 tick) 无射击
        long timeSinceLast = this.tickCount - this.lastDamageTimestamp;
        
        if (timeSinceLast > 100 && currentHeat > 0) {
            // 每秒 (20 tick) 衰减 20%
            if (this.tickCount % 20 == 0) {
                int decay = Math.max(1, (int)(currentHeat * 0.2)); // 至少减1
                this.entityData.set(DATA_HEAT, Math.max(0, currentHeat - decay));
            }
        }
    }

    // ✅ 新增：狂暴技能管理系统 (5秒爆发 + 动态CD)
    private void manageBrutalityAbility() {
        if (this.level().isClientSide) return;

        // 1. 检查是否全身满附魔
        boolean isFullEnchant = true;
        net.minecraft.world.entity.EquipmentSlot[] checkSlots = {
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET,
                net.minecraft.world.entity.EquipmentSlot.MAINHAND
        };
        for (EquipmentSlot slot : checkSlots) {
            ItemStack s = this.getItemBySlot(slot);
            if (s.isEmpty() || !s.isEnchanted()) {
                isFullEnchant = false;
                break;
            }
        }

        if (!isFullEnchant) {
            if (this.entityData.get(IS_BRUTAL)) {
                this.entityData.set(IS_BRUTAL, false);
            }
            brutalityActiveTimer = 0;
            return;
        }

        // 2. 状态机逻辑
        boolean isBrutalActive = this.entityData.get(IS_BRUTAL);

        if (isBrutalActive) {
            // [状态 A]: 正在狂暴 (5秒)
            brutalityActiveTimer--;
            if (brutalityActiveTimer <= 0) {
                this.entityData.set(IS_BRUTAL, false);

                // 冷却：基础25秒 - 每级减3秒
                int reduceSeconds = this.getTier() * 3;
                int cdSeconds = Math.max(5, 25 - reduceSeconds);
                brutalityCooldown = cdSeconds * 20;

                this.playSound(SoundEvents.LAVA_EXTINGUISH, 1.0f, 0.5f);
            }
        } else {
            // [状态 B]: 等待冷却
            if (brutalityCooldown > 0) {
                brutalityCooldown--;
            } else {
                // 开启狂暴
                this.entityData.set(IS_BRUTAL, true);
                brutalityActiveTimer = 100; // 5秒

                this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 0.5f);
                if (this.level() instanceof ServerLevel sl) {
                    sl.players().forEach(p -> {
                        if (p.distanceToSqr(this) < 256) {
                            p.displayClientMessage(Component.literal("§4⚡ 炮台进入狂暴模式！(5s)"), true);
                        }
                    });
                }
            }
        }
    }
    // ==================== 🚀 传送模块逻辑 ====================
    public boolean hasTeleportModule() { return this.entityData.get(HAS_TELEPORT_MODULE); }
    public void setHasTeleportModule(boolean has) { this.entityData.set(HAS_TELEPORT_MODULE, has); }
    
    public boolean canTeleport() { 
        // 必须安装模块且冷却完毕
        return this.hasTeleportModule() && teleportCooldown <= 0; 
    }
    public void setTeleportCooldown(int ticks) { this.teleportCooldown = ticks; }
    
    private void tickTeleportCooldown() {
        if (teleportCooldown > 0) teleportCooldown--;
    }

    // 获取当前等级对应的传送冷却 (Tick)
    // Configurable via TurretConfig
    public int getMaxTeleportCooldown() {
        int tier = getTier();
        int base = TurretConfig.COMMON.teleportCooldownBase.get();
        int reduction = TurretConfig.COMMON.teleportCooldownReductionPerTier.get();
        int min = TurretConfig.COMMON.teleportCooldownMin.get();
        return Math.max(min, base - (tier * reduction));
    }

    @Override
    public void tick() {
        super.tick();
        
        // ✅ [Fix] 死亡时立即停止所有自定义逻辑，防止“诈尸”或动画抽搐
        if (this.isDeadOrDying()) {
            this.setDeltaMovement(0, -0.2, 0); // 确保尸体倒地
            return; 
        }

        tickTeleportCooldown();

        // ✅ [Fix] 记录卡召唤实体的物理与状态修正
        if (!this.level().isClientSide && this.getPersistentData().getBoolean("RecordSummoned")) {
            // 1. 物理修正 (仅在非乘骑、非水下、非飞行时)
            if (!this.isInWater() && !this.isPassenger() && !this.isNoGravity()) {
                // 检测悬空
                if (!this.onGround()) {
                    // 施加额外重力 (防止漂浮)
                    this.setDeltaMovement(this.getDeltaMovement().add(0, -0.08, 0));

                    // 严重偏移检测 (与下方方块距离)
                    // 只在非上升状态下修正 (防止打断跳跃)
                    if (this.getDeltaMovement().y <= 0.01) {
                        BlockPos pos = this.blockPosition();
                        int groundY = pos.getY();
                        boolean foundGround = false;
                        
                        // 向下探测 5 格
                        for (int i = 0; i < 5; i++) {
                            BlockPos p = pos.below(i);
                            if (!this.level().isEmptyBlock(p)) {
                                groundY = p.getY() + 1; // 地面之上
                                foundGround = true;
                                break;
                            }
                        }

                        if (foundGround) {
                            double diff = this.getY() - groundY;
                            // 如果悬空高度在 0.5 到 3.0 之间，且不是在跳跃，强制吸附
                            if (diff > 0.5 && diff < 3.0) {
                                this.setPos(this.getX(), groundY, this.getZ());
                                this.setDeltaMovement(this.getDeltaMovement().multiply(1, 0, 1)); // 清空垂直速度
                                this.setOnGround(true);
                            }
                        }
                    }
                }
            }
        }
        
        // ✅ 更新计时器
        if (this.invincibilityTimer > 0) this.invincibilityTimer--;
        if (this.postTeleportAttackDelay > 0) this.postTeleportAttackDelay--;
        

        
        // ✅ 确保每一帧都检查热度衰减
        manageHeatDecay();

        // 检查并初始化编号
        if (!this.level().isClientSide && this.entityData.get(UNIT_ID) == 0) {
            this.entityData.set(UNIT_ID, this.random.nextInt(999) + 1);
            updateCustomName(); // 生成后立刻刷新名字
        }
        // ==================== 📡 数据同步补丁 ====================
        if (!this.level().isClientSide) {// ==================== 💳 方案一：身份卡系统 (Slot 25) ====================
            // 每秒检查一次 (20 tick)
            if (this.tickCount % 20 == 0) {
                // 获取第 25 格的物品 (倒数第二格，因为 26 是属性书)
                ItemStack idCard = this.inventory.getItem(39);

                // 获取当前的名字
                String currentName = this.entityData.get(SYNC_BASE_NAME);

                // 情况 A: 插槽里有带名字的物品 (命名牌、纸、剑...都可以)
                if (!idCard.isEmpty() && idCard.hasCustomHoverName()) {
                    String cardName = idCard.getHoverName().getString();

                    // 如果卡上的名字和现在的名字不一样，就强制覆盖！
                    if (!cardName.equals(currentName)) {
                        this.entityData.set(SYNC_BASE_NAME, cardName);
                        updateCustomName(); // 立即刷新头顶显示

                        // 播放一个提示音效 (可选)
                        this.playSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0f, 1.0f);
                    }
                }
                // 情况 B: 插槽是空的 (或者物品没名字) -> 恢复默认
                else {
                    // 如果现在的名字不是默认的 "先锋队员"，就恢复它
                    if (!currentName.equals("先锋队员")) {
                        this.entityData.set(SYNC_BASE_NAME, "先锋队员");
                        updateCustomName();
                    }
                }
            }
            // ===================================================================
            // [服务端]: 如果我有主人，但同步通道里是空的，或者不对，就赶紧更新通道
        if (this.ownerUUID != null) {
                UUID syncedId = this.entityData.get(OWNER_UUID_SYNC).orElse(null);
                if (!this.ownerUUID.equals(syncedId)) {
                    this.entityData.set(OWNER_UUID_SYNC, Optional.of(this.ownerUUID));
                }
            }
        } else {
            // [客户端]: 时刻从通道里读取最新的主人是谁，赋值给本地变量
            // 这样 HUD 就能读到 ownerUUID 了！
            this.ownerUUID = this.entityData.get(OWNER_UUID_SYNC).orElse(null);
        }
        // =======================================================

        this.updateOverheadStatus();

// ==================== 🗣️ 头顶文字管理 (新增) ====================

        // 1. 台词计时器递减
        int speechTimer = this.entityData.get(DATA_DIALOGUE_TIMER);
        if (speechTimer > 0) {
            this.entityData.set(DATA_DIALOGUE_TIMER, speechTimer - 1);
        } else {
            // 时间到了，清空台词
            if (!this.entityData.get(DATA_DIALOGUE_TEXT).isEmpty()) {
                this.entityData.set(DATA_DIALOGUE_TEXT, "");
            }

        }



        if (!this.level().isClientSide) {
            // ==================== 🔧 核心：濒死倒计时逻辑 ====================

            // ===============================================================

            // ==================== 🛡️ 智能战斗监控 ====================
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                if (target.getLastHurtByMob() == this) {
                    if (this.tickCount - target.getLastHurtByMobTimestamp() < 10) {
                        this.consecutiveMisses = 0;
                        this.lastDamageTimestamp = this.tickCount;
                    }
                }

                if (this.consecutiveMisses >= 5) {
                    this.setTarget(null);
                    this.consecutiveMisses = 0;
                    this.playSound(SoundEvents.DISPENSER_FAIL, 1.0f, 1.5f);

                    // ✅ 修复：必须是 [跟随模式] 且 [不在坚守] 时，才允许跑向主人！
                    if (this.ownerUUID != null && this.isFollowing()) {
                        LivingEntity owner = this.level().getPlayerByUUID(this.ownerUUID);
                        if (owner != null) this.getNavigation().moveTo(owner, 1.0);
                    }
                }

                boolean canSee = this.getSensing().hasLineOfSight(target);
                if (!canSee) {
                    this.blockedSightTime++;
                    if (this.blockedSightTime > 20 && this.blockedSightTime % 10 == 0) {
                        this.getNavigation().moveTo(target, 1.2);
                    }
                    if (this.blockedSightTime > 60) {
                        this.setTarget(null);
                        this.blockedSightTime = 0;
                    }
                } else {
                    this.blockedSightTime = 0;
                }
            } else {
                this.consecutiveMisses = 0;
                this.blockedSightTime = 0;
            }

            // ==================== 🚑 紧急回防 ====================
            if (this.isCommandScavenging() && this.ownerUUID != null) {
                Player owner = this.level().getPlayerByUUID(this.ownerUUID);
                if (owner != null && owner.hurtTime > 0) {
                    this.setCommandScavenging(false);
                    this.teleportToSafeSpot(owner);
                    this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);
                }
            }

            // ==================== 🧩 日常功能 ====================
            if (this.getHealth() < this.getMaxHealth()) autoEat();
            if (this.tickCount % 20 == 0) updateInfoBookAndSlots();
            // --- 🗣️ 语音系统挂载: 闲聊 & 状态 ---

            // 1. 闲聊 (每10秒尝试一次)
            if (this.tickCount % 200 == 0) {
                TurretDialogue.trySpeak(this, TurretDialogue.Type.IDLE);
            }

            // 2. 低血量检测 (每秒检测)
            if (this.tickCount % 20 == 0) {
                float hp = this.getHealth();
                float max = this.getMaxHealth();
                if (hp < max * 0.2f) {
                    TurretDialogue.trySpeak(this, TurretDialogue.Type.DYING);
                } else if (hp < max * 0.5f) {
                    TurretDialogue.trySpeak(this, TurretDialogue.Type.LOW_HP);
                }
            }
            lockInfoBook();
            if (this.tickCount % 10 == 0) tauntNearbyMonsters();

            manageBrutalityAbility();
            manageTorchBehavior();
            handlePickupAndXp();
            checkOwnerDistanceAndTeleport();
            tickPrintLogic();
            tickCaptainLogic();
            // 如果正在打印或回收，禁止执行后续的打怪/移动 AI
            if (getPrintState() != 0) {
                // 🛑 绝对锚定：X, Y, Z 速度全部归零！
                // 之前是 this.getDeltaMovement().y，这会导致它能被推上天。
                // 现在改成 0，它就像钉在地上一样。
                this.setDeltaMovement(0, 0, 0);

                // 额外保险：强制重置位置到生成点 (防止被挤偏)
                // (spawnX, spawnY, spawnZ 是我们在 finalizeSpawn 里记录的)
                if (this.spawnY != 0) { // 确保 spawnY 已被赋值
                    // 只锁 Y 轴，允许水平微量挤压，或者全锁
                    this.setPos(this.getX(), this.spawnY, this.getZ());
                }
                return;
            }

            this.updateOverheadStatus();

        } // ⬅️ 结束 if (!isClientSide)
    } // 🟢 结束 tick() 方法


    private void updateOverheadStatus() {
        String status = "";

        // ✅ 1. 动态呼吸点算法 (每 0.5秒 变一次)
        // 这里的逻辑是：用总时间除以 10，然后对 4 取余数，得到 0, 1, 2, 3 循环
        int step = (this.tickCount / 10) % 4;
        String dots = switch (step) {
            case 0 -> ".";
            case 1 -> "..";
            case 2 -> "...";
            default -> ""; // 第 4 拍留空，产生闪烁感
        };

        // ==========================================
        // ⬇️ 状态判断逻辑 ⬇️
        // ==========================================

        // 优先级 2: 狂暴倒计时
        if (this.entityData.get(IS_BRUTAL)) {
            int sec = this.brutalityActiveTimer / 20;
            status = "§4§l⚡ 残暴模式: " + sec + "s";
        }
        // 优先级 3: 打印/回收中
        else if (getPrintState() != 0) {
            int percent = (int)(getPrintProgress() * 100);
            // 既然也是进行中，我们顺手也加上点，看着更舒服！
            status = (getPrintState() == 3)
                    ? "§e§l♻ 回收中" + dots + ": " + percent + "%"
                    : "§b§l▨ 构建中" + dots + ": " + percent + "%";
        }
        // 优先级 4: 背包已满 (当处于拾荒模式时)
        else if (this.isCommandScavenging() && isInventoryFull()) {
            status = "§6§l🎒 背包已满 (ID:" + this.entityData.get(UNIT_ID) + ")";
        }
        // 优先级 4.5: 空间不足 (<10%)
        else if (this.isCommandScavenging() && getFreeSlotCount() < 5) {
            status = "§e§l⚠ 空间不足 (ID:" + this.entityData.get(UNIT_ID) + ")";
        }
        // 优先级 5: 拾荒中 (✅ 应用动画)
        else if (this.isCommandScavenging()) {
            status = "§e§l⚗ 正在拾荒" + dots;
        }
        // 优先级 6: 清剿中 (✅ 应用动画)
        else if (this.isPurgeActive()) {
            // 加上杀敌数统计，配合呼吸点，更有战术感
            status = "§c§l⚔ 清剿进行中" + dots + " §7[" + this.purgeKillCount + "]";
        }

        // 更新数据 (只有变化时才发包，节省流量)
        if (!status.equals(this.entityData.get(DATA_STATUS_OVERLAY))) {
            this.entityData.set(DATA_STATUS_OVERLAY, status);
        }
    }

    // 辅助：检查背包是否满了 (只检查储物格 12-26)
    private boolean isInventoryFull() {
        for (int i = 12; i < 37; i++) {
            if (this.inventory.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    private int getFreeSlotCount() {
        int free = 0;
        for (int i = 12; i < 37; i++) {
            if (this.inventory.getItem(i).isEmpty()) free++;
        }
        return free;
    }

    // 自动整理
    public void sortInventory() {
        // 简单排序：将高价值物品移到前面 (12-36)
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 12; i < 37; i++) {
            ItemStack s = this.inventory.getItem(i);
            if (!s.isEmpty()) {
                stacks.add(s);
                this.inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        
        stacks.sort((a, b) -> Integer.compare(getItemValue(b), getItemValue(a))); // Descending
        
        for (int i = 0; i < stacks.size(); i++) {
            if (i + 12 < 37) {
                this.inventory.setItem(i + 12, stacks.get(i));
            }
        }
    }

    private int getItemValue(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        Item item = stack.getItem();
        if (item == Items.NETHERITE_INGOT) return 100;
        if (item == Items.DIAMOND) return 90;
        if (item == Items.EMERALD) return 80;
        if (item == Items.GOLD_INGOT) return 70;
        if (item == Items.IRON_INGOT) return 60;
        return 0;
    }

    // ✅ 新增：供外部调用的“说话”接口
    public void setOverheadDialogue(String text) {
        this.entityData.set(DATA_DIALOGUE_TEXT, text);
        this.entityData.set(DATA_DIALOGUE_TIMER, 80); // 显示 4 秒 (80 tick)
    }

    // Getter 供渲染器使用
    public String getOverheadDialogue() { return this.entityData.get(DATA_DIALOGUE_TEXT); }
    public String getOverheadStatus() { return this.entityData.get(DATA_STATUS_OVERLAY); }





    // ==========================================
    // 🖨️ 3D 打印逻辑核心
    // ==========================================
    private int summonRetryCount = 0;

    private void tickPrintLogic() {
        int state = this.entityData.get(PRINT_STATE);
        float progress = this.entityData.get(PRINT_PROGRESS);

        // [状态 0] 正常状态：什么都不做
        if (state == 0) return;

        // [状态 1] 正在打印 (Printing)
        if (state == 1) {
            // 1. 进度增加 (速度：大约 5 秒完成)
            // 如果你想快点，把 0.01f 改大，比如 0.02f
            if (progress > 0.0f && progress < 0.02f) {
                // 音量 1.0, 音调 1.0
                this.playSound(ModSounds.PRINT_LOOP.get(), 0.5f, 1.0f);
            }

            progress += 0.01f;

            // 2. 蓝屏判定 (BSOD) - 已移除
            // if (!this.level().isClientSide && progress > 0.6f && progress < 0.8f) { ... }

            // 3. 环境互动：烧灼地面 (粒子特效)
            if (this.level().isClientSide) {
                // 在当前打印高度生成火花
                double y = this.getY() + (this.getBbHeight() * progress);
                if (this.random.nextFloat() < 0.3f) {
                    this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0, 0.05, 0);
                    this.level().addParticle(ParticleTypes.FLAME, this.getX() + (random.nextDouble()-0.5), this.getY(), this.getZ() + (random.nextDouble()-0.5), 0, 0.01, 0);
                }
// ... (上面的代码不变)

// ... (tickPrintLogic 方法内部) ...

            } else {
                // 服务端：气浪排斥逻辑 (已升级)
                if (progress > 0.1f) {
                    // 范围：以自身为中心，向外扩 1.5 格 (稍微大一点点)
                    List<LivingEntity> pushTargets = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(1.5));

                    for (LivingEntity e : pushTargets) {
                        // 🛑 调用刚才写的通用过滤器
                        if (isImmuneToPush(e)) {
                            continue; // 是自己人/骷髅/特定单位，跳过，不推！
                        }

                        // 对杂鱼执行推开操作
                        // 稍微减小一点力度 (0.1 -> 0.08)，防止把苦力怕推到玩家脸上
                        if (!e.isShiftKeyDown()) {
                            e.push(0, 0.08, 0);
                        }
                    }
                }
            }

            // ... (tickPrintLogic 方法后续) ...

            // ... (下面的代码不变)

            // 4. 完成判定
            if (progress >= 1.0f) {
                progress = 1.0f;
                setPrintState(0); // 切换回正常状态
                // 播放完成音效
                this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                // 震开周围 (冲击波)
                if (!this.level().isClientSide) {
                    ((ServerLevel)this.level()).sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY()+1, this.getZ(), 1, 0, 0, 0, 0);
                    // 召唤成功确认
                    if (this.ownerUUID != null) {
                        Player p = this.level().getPlayerByUUID(this.ownerUUID);
                        if (p != null) p.displayClientMessage(Component.literal("§a[系统] 机体构建完成，系统上线。"), true);
                    }
                }
            }
        }

        // [状态 2] 蓝屏死机 (Failed)
        else if (state == 2) {
             // 自动重试逻辑 (最多3次)
            if (!this.level().isClientSide) {
                if (this.summonRetryCount < 3) {
                    this.summonRetryCount++;
                    this.entityData.set(PRINT_PROGRESS, 0.0f); 
                    this.setPrintState(1); 
                    return;
                }
            }

            // 卡在当前进度不动，冒黑烟
            if (!this.level().isClientSide && this.tickCount % 20 == 0) {
                this.hurt(this.level().damageSources().generic(), 2.0f);
            }
            this.playSound(ModSounds.PRINT_ERROR.get(), 0.5f, 1.5f);


            // 倒计时爆炸 (暂时写个简单的自毁，以后加掉落芯片)
            if (!this.level().isClientSide && this.tickCount % 20 == 0) {
                // 简易爆炸逻辑
                this.hurt(this.level().damageSources().generic(), 2.0f); // 自己扣血直到炸掉

            }
            // 逻辑已移动到 die() 方法，防止 tick 重复掉落



            // 声音逻辑已移动到 die()

        }

        // [状态 3] 逆向回收 (Recycling)
        else if (state == 3) {
            progress -= 0.02f; // 倒退速度快一点

            // 特效：吸入粒子
            if (this.level().isClientSide) {
                this.level().addParticle(ParticleTypes.PORTAL, this.getX(), this.getY() + 1, this.getZ(), (random.nextDouble()-0.5), (random.nextDouble()-0.5), (random.nextDouble()-0.5));
            }

            if (progress <= 0.0f) {
                progress = 0.0f;
                if (!this.level().isClientSide) {
                    // 掉落回收芯片 (先用红石代替，等后面我们做芯片)
                    this.spawnAtLocation(ExampleMod.GLITCH_CHIP.get());
                    this.discard(); // 彻底删除
                }
            }
        }

        // 更新进度
        this.entityData.set(PRINT_PROGRESS, progress);
    }

    // 辅助方法：设置状态
    public void setPrintState(int state) {
        this.entityData.set(PRINT_STATE, state);
    }

    // 辅助方法：获取进度 (给渲染器用)
    public float getPrintProgress() {
        return this.entityData.get(PRINT_PROGRESS);
    }

    // 辅助方法：获取状态
    public int getPrintState() {
        return this.entityData.get(PRINT_STATE);
    }

    private void checkOwnerDistanceAndTeleport() {

        if (this.level().isClientSide) return;
        if (this.isPurgeActive()) return;
        if (!this.entityData.get(IS_FOLLOWING)) return;
        if (this.isCommandScavenging()) return;


        Player owner = this.level().getNearestPlayer(this, -1.0);
        if (ownerUUID != null) {
            Player p = this.level().getPlayerByUUID(ownerUUID);
            if (p != null) owner = p;
        }

        if (owner == null) return;
        if (ownerUUID == null) ownerUUID = owner.getUUID();

        double distSqr = this.distanceToSqr(owner);
        if (distSqr > 400.0) {
            teleportToSafeSpot(owner);
        }
    }

    public void teleportToSafeSpot(LivingEntity owner) {
        // 全局禁止：未安装模块无法传送
        if (!this.hasTeleportModule()) {
            if (owner instanceof Player player) {
                player.displayClientMessage(Component.translatable("message.examplemod.teleport_module_missing"), true);
            }
            return;
        }

        for (int i = 0; i < 10; i++) {
            double angle = this.random.nextDouble() * 2.0 * Math.PI;
            double distance = 2.0 + this.random.nextDouble() * 2.0;

            double targetX = owner.getX() + Math.cos(angle) * distance;
            double targetZ = owner.getZ() + Math.sin(angle) * distance;
            double targetY = owner.getY();

            BlockPos pos = new BlockPos((int)targetX, (int)targetY, (int)targetZ);

            int safeY = findSafeY(pos);
            if (safeY != -999) {
                this.moveTo(targetX, safeY, targetZ, this.getYRot(), this.getXRot());
                this.getNavigation().stop();
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.PORTAL, targetX, safeY + 1, targetZ, 10, 0.5, 0.5, 0.5, 0.5);
                    // 注意看 SoundEvents.ENDERMAN_TELEPORT 后面多加了一段代码
                    sl.playSound(null, targetX, safeY, targetZ, SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.0f);
                }
                return;
            }
        }
        this.moveTo(owner.getX(), owner.getY(), owner.getZ());
    }

    private int findSafeY(BlockPos pos) {
        Level level = this.level();
        for (int i = 0; i < 3; i++) {
            BlockPos p = pos.above(i);
            BlockState state = level.getBlockState(p);
            BlockState stateUp = level.getBlockState(p.above());
            if (!state.blocksMotion() && !stateUp.blocksMotion()) {
                return p.getY();
            }
        }
        return -999;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        if (target != null && target != this) {
            TurretDialogue.trySpeak(this, TurretDialogue.Type.SPOT_ENEMY);
        }
        super.setTarget(target); // 别忘了保留这行
    }




    // 把原来 tick 里乱七八糟的逻辑都塞到这里面，保持 tick 清爽
    private void runNormalLogic() {
        // 智能战斗监控
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            if (target.getLastHurtByMob() == this) {
                if (this.tickCount - target.getLastHurtByMobTimestamp() < 10) {
                    this.consecutiveMisses = 0;
                }
            }
            if (this.consecutiveMisses >= 5) {
                this.setTarget(null);
                this.consecutiveMisses = 0;
            }
        }
// 在 runNormalLogic() 或 tick() 中
        if (this.tickCount % 200 == 0) { // 每10秒检查一次
            TurretDialogue.trySpeak(this, TurretDialogue.Type.IDLE);
        }

        // 自动吃东西
        if (this.getHealth() < this.getMaxHealth()) autoEat();

        // 更新书本
        if (this.tickCount % 20 == 0) updateInfoBookAndSlots();
        lockInfoBook();

        // 嘲讽怪物
        if (this.tickCount % 10 == 0) tauntNearbyMonsters();

        // 你的其他技能
        manageBrutalityAbility();
        manageTorchBehavior();
        handlePickupAndXp();
        checkOwnerDistanceAndTeleport();
    }

    private int hurtRecursionCounter = 0;

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.hurtRecursionCounter > 5) {
            System.err.println("[SkeletonTurret] Hurt loop detected for entity " + this.getId() + ", breaking loop.");
            return false;
        }
        
        this.hurtRecursionCounter++;
        try {
            // ✅ 传送无敌判定 (0.3s)
            if (this.invincibilityTimer > 0 && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return false;
            }

            // 1. 如果是无敌状态，直接免疫所有伤害 (除了虚空掉落)
            if (this.isInvulnerable()) {
                return source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD);
            }

            // 2. 玩家强制拆除逻辑 (Shift+左键) - 主人可以清理满血的塔
            if (source.getEntity() instanceof Player p) {
                if (this.ownerUUID != null && p.getUUID().equals(this.ownerUUID) && p.isShiftKeyDown()) {
                    return super.hurt(source, amount);
                }
                return false; // 普通左键免疫误伤
            }

            return super.hurt(source, amount);
        } finally {
            this.hurtRecursionCounter--;
        }
    }





    private void triggerLastStand() {
        if (this.level().isClientSide) return;

        float radius = 5.0f;
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius));
        for (LivingEntity target : targets) {
            if (target instanceof Player) continue;
            if (target instanceof SkeletonTurret) continue;
            if (target.hasCustomName() && target.getCustomName().getString().contains("感染体")) continue;

            target.hurt(this.level().damageSources().explosion(this, this), 200.0f);
            target.setSecondsOnFire(10);
        }

        ServerLevel sl = (ServerLevel) this.level();
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.HOSTILE, 4.0f, 1.0f);
    }

        // Duplicate hurt method removed


    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide) {
            System.out.println("[Turret] ☠️ SkeletonTurret #" + this.entityData.get(UNIT_ID) + " died. Source: " + source.getMsgId());

            if (TurretConfig.COMMON.enableDeathRecordDrop.get() && !this.deathRecordDropped) {
                int usedDrops = this.entityData.get(DROP_COUNT);
                if (usedDrops < 2) {
                    ItemStack record = this.createDeathRecordCard(usedDrops + 1);
                    ItemEntity drop = new ItemEntity(this.level(), this.getX(), this.getY() + 0.5D, this.getZ(), record);
                    drop.setDeltaMovement(0.0D, 0.2D, 0.0D);
                    this.level().addFreshEntity(drop);
                    this.deathRecordDropped = true;
                }
            }
        }
        super.die(source);
        
        // ✅ [Fix] 立即停止所有 AI 和物理运动，防止尸体抽搐或滑行
        this.setNoAi(true);
        this.getNavigation().stop();
        this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        
        if (!this.level().isClientSide) {
            // ✅ 新增：炸机芯片掉落 (仅在蓝屏状态下掉落，且只掉一次)
            if (this.getPrintState() == 2) {
                this.spawnAtLocation(ExampleMod.GLITCH_CHIP.get());
                this.playSound(ModSounds.PRINT_EXPLODE.get(), 1.0f, 1.0f);
            }

            // 1. 爆炸效果 (无方块破坏)
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 5.0F, Level.ExplosionInteraction.NONE);
            
            // 2. 对敌对生物造成真实伤害和击退
            List<LivingEntity> enemies = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(5.0), 
                e -> (e instanceof Monster || e instanceof Enemy) && e != this);
            
            for (LivingEntity enemy : enemies) {
                enemy.hurt(this.damageSources().magic(), 25.0F); 
                double d0 = enemy.getX() - this.getX();
                double d1 = enemy.getZ() - this.getZ();
                enemy.knockback(1.0F, -d0, -d1);
            }

            // ✅ 新增：末影珍珠独立掉落 (3% - 6% 随机)
            float pearlChance = 0.03f + this.random.nextFloat() * 0.03f;
            if (this.random.nextFloat() < pearlChance) {
                this.spawnAtLocation(Items.ENDER_PEARL);
            }

        // Death Record logic moved to ExampleMod.onLivingDrops for better compatibility and 100% chance configuration
        }
    }

    // ==========================================
    // ✅ 死亡记录卡数据生成器
    // ==========================================
    public CompoundTag createRecordTag(int nextDropCount) {
        CompoundTag dataTag = new CompoundTag();
        dataTag.putInt("UnitID", this.entityData.get(UNIT_ID));
        dataTag.putInt("RangeLevel", this.getRangeLevel()); 
        dataTag.putInt("Tier", getTier());
        dataTag.putInt("Level", getTier()); // Legacy support
        dataTag.putInt("XP", this.entityData.get(DATA_XP));
        dataTag.putInt("KillCount", this.entityData.get(KILL_COUNT));
        dataTag.putInt("UpgradeProgress", this.entityData.get(UPGRADE_PROGRESS));
        dataTag.putBoolean("IsBrutal", this.entityData.get(IS_BRUTAL));
        dataTag.putInt("Heat", this.getHeat());
        dataTag.putInt("DropCount", Math.max(0, nextDropCount));
        dataTag.putDouble("DeathX", this.getX());
        dataTag.putDouble("DeathY", this.getY());
        dataTag.putDouble("DeathZ", this.getZ());

        if (this.ownerUUID != null) {
            dataTag.putUUID("OwnerUUID", this.ownerUUID);
        }
        dataTag.putString("BaseName", this.entityData.get(SYNC_BASE_NAME));

        // Save Inventory
        ListTag invList = new ListTag();
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte)i);
                stack.save(itemTag);
                invList.add(itemTag);
            }
        }
        dataTag.put("Inventory", invList);

        // Save equipment
        ListTag equipmentList = new ListTag();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = this.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putString("SlotName", slot.getName());
                stack.save(itemTag);
                equipmentList.add(itemTag);
            }
        }
        dataTag.put("Equipment", equipmentList);
        
        return dataTag;
    }

    public CompoundTag createRecordTag() {
        return createRecordTag(this.entityData.get(DROP_COUNT));
    }

    public ItemStack createDeathRecordCard(int nextDropCount) {
        ItemStack record = new ItemStack(ExampleMod.DEATH_RECORD_ITEM.get());
        CompoundTag masterTag = new CompoundTag();
        masterTag.putString("Version", "2.0");

        CompoundTag dataTag = this.createRecordTag(nextDropCount);
        masterTag.put("Data", dataTag);
        masterTag.putString("Checksum", Integer.toHexString(dataTag.toString().hashCode()));
        masterTag.put("Backup", dataTag.copy());
        record.setTag(masterTag);
        return record;
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        // ✅ [Fix] 立即移除实体 (Immediate Removal)
        // 不再等待死亡动画，确保死亡后立刻消失
        if (this.deathTime >= 1 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte)60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    protected void registerGoals() {
        // ✅ 0. 浮水 (最高优先级)：保证掉水里会自己浮起来，而不是沉底
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        // ✅ 新增：紧急传送 (优先级 1) - 只有在被围殴且无法逃脱时触发
        this.goalSelector.addGoal(1, new TurretEmergencyTeleportGoal(this));
        
        // ✅ 新增：原地巡逻 (优先级 6，比打怪低，比发呆高)
        // 参数：速度 1.0
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 0, true, false, (e) -> {
            if (!this.isPurgeActive()) return false; // 没开模式就不生效
            // ✅ 修复核心：限制锁定距离！
            double range = this.getAttackRange();
            if (e.distanceToSqr(this) > range * range) return false; // ✅ 超过等级射程就不锁
            if (this.isFollowMode()) {
                // 执行跟随逻辑
            }
            if (e instanceof Player) return false;
            if (e instanceof SkeletonTurret) return false;
            if (e instanceof IronGolem) return false;
            if (e.getPersistentData().getBoolean("IsFriendlyZombie")) return false;
            if (e.getPersistentData().getBoolean("IsFriendlyCreeper")) return false;
            if (e instanceof net.minecraft.world.entity.decoration.ArmorStand) return false;
            return true;
        }));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, (entity) -> {
            if (entity instanceof SkeletonTurret) return false;
            if (entity instanceof Mob mob && mob.getTarget() instanceof Player) return true;
            return false;
        }));
        // ✅ 3. 普通怪物防御 (带等级射程限制)
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                (entity) -> {
                    if (entity instanceof SkeletonTurret) return false;
                    if (entity instanceof Player) return false;
                    if (entity.getPersistentData().getBoolean("IsFriendlyZombie")) return false;
                    if (entity.getPersistentData().getBoolean("IsFriendlyCreeper")) return false;
                    if (entity instanceof IronGolem) return false;
                    // ✅ 新增：距离检查 (防止 D级炮台去惹 100格外的苦力怕)
                    double range = this.getAttackRange();
                    if (entity.distanceToSqr(this) > range * range) return false;
                    return true;
                }));

        // ✅ 1. 落水逃生：如果在水里，优先往主人身边游，不准打架
        // ✅ 插入在这里 (优先级 1)：怪贴脸了先拉扯！
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, SkeletonTurret.class).setAlertOthers());
        // ❌ 删除：KeepDistanceGoal (已替换为传送)
        // ❌ 删除：MaintainSpaceGoal (已替换为传送)
        this.goalSelector.addGoal(1, new EscapeWaterGoal(this, 2.0)); // 速度 2.0 (游快点)
        
        // ✅ 攻击模式 (优先级 2)：站桩输出
        this.goalSelector.addGoal(2, new RampUpBowAttackGoal(this));
        
        // ✅ 修复：只有在“跟随模式”开启时，才允许移动 (优先级 4)
        this.goalSelector.addGoal(4, new TurretFollowGoal(this, 1.2, 10.0F, 2.0F));
        
        // ✅ 新增：清剿模式-地毯式搜索 (优先级 3)
        this.goalSelector.addGoal(3, new PurgeMoveGoal(this));
        
        this.goalSelector.addGoal(5, new TurretScavengeGoal(this, 1.15));
        // ✅ 新增 2：护主模式 (攻击主人的敌人)
        this.targetSelector.addGoal(2, new TurretDefendOwnerGoal(this));
        // ✅ 新增 3：协作模式 (攻击主人正在打的敌人)
        this.targetSelector.addGoal(3, new TurretAssistOwnerGoal(this));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        // ✅ 新增 4: 战术同步 (如果有队友在打架，我也加入)
        this.targetSelector.addGoal(4, new TurretPackAttackGoal(this));

        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    // ==========================================
    // ✅ 修复：添加 Shift+右键 交互逻辑
    // ==========================================



    private void lockInfoBook() {
        ItemStack stack = this.inventory.getItem(40);
        if (stack.isEmpty() || stack.getItem() != Items.WRITTEN_BOOK) {
            updateInfoBookAndSlots();
        }
    }

    private void tauntNearbyMonsters() {
        List<Monster> nearbyMonsters = this.level().getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(20.0));
        for (Monster monster : nearbyMonsters) {
            if (monster instanceof SkeletonTurret) continue;
            LivingEntity currentTarget = monster.getTarget();
            if (currentTarget == null || currentTarget instanceof Player) {
                monster.setTarget(this);
            }
        }
    }

    private void updateInfoBookAndSlots() {
        // 1. 同步装备槽位到背包前5格
        this.inventory.setItem(0, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
        this.inventory.setItem(1, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
        this.inventory.setItem(2, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST));
        this.inventory.setItem(3, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS));
        this.inventory.setItem(4, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET));

        // 2. 生成详细说明书
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.setHoverName(Component.literal("§e§l>>> 机体状态监控 <<<"));

        // --- 数据计算 ---
        int tier = getTier();

        float hp = this.getHealth();
        float maxHp = this.getMaxHealth();

        if (hp < maxHp * 0.2f) {
            TurretDialogue.trySpeak(this, TurretDialogue.Type.DYING);
        } else if (hp < maxHp * 0.5f) {
            TurretDialogue.trySpeak(this, TurretDialogue.Type.LOW_HP);
        }

        // 计算攻速 (用于显示)
        float speed = getFireRate();

        // 计算伤害
        double dmg = (4.0 + (tier * 5.0));

        String state = this.entityData.get(IS_FOLLOWING) ? "§a[机动模式]" : "§6[阵地模式]";
        boolean isBrutal = this.entityData.get(IS_BRUTAL);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§8===================="));

        // [A] 悲惨档案
        if (tier == 0) {
            lore.add(Component.literal("§8日记全是乱码... 只有一行字能看清:"));
            lore.add(Component.literal("§8“听从命令。指挥官就是神。”"));
        } else if (tier >= 1 && tier < 4) {
            lore.add(Component.literal("§b[ 记忆碎片: 编号 " + this.entityData.get(UNIT_ID) + " ]"));
            lore.add(Component.literal("§7“这里没有英雄，只有死不掉的鬼魂。”"));
            lore.add(Component.literal("§7“武器是从我尸体的手骨上硬生生掰下来的。”"));
        } else {
            lore.add(Component.literal("§4[ 觉醒记录: 错误 ]"));
            lore.add(Component.literal("§8“我看见了... 巨大的光标在天上划过。”"));
            lore.add(Component.literal("§8“我们只是游戏里的数据吗？回答我！指挥官！”"));
        }



        lore.add(Component.literal(" "));

// [B] 战斗遥测 (✅ 这里不会再报错了)
        lore.add(Component.literal("§c[战斗遥测]"));
        lore.add(Component.literal(String.format("  §c❤ 结构完整度: %.0f / %.0f", hp, maxHp)));
        lore.add(Component.literal(String.format("  §6⚔ 弹药破坏力: %.1f", dmg)));
        lore.add(Component.literal(String.format("  §b⚡ 射击频率: %.1f 发/秒", speed)));
        // 热度显示

        if (isBrutal) {
            lore.add(Component.literal("  §4🔥 引擎过载: 残暴模式已激活!"));
        } else {
            int heat = getHeat();
            String heatColor = heat > 80 ? "§c" : (heat > 40 ? "§6" : "§a");
            lore.add(Component.literal(String.format("  §d🔥 枪管热度: %s%d%%", heatColor, heat)));
        }

        lore.add(Component.literal(" "));

        // [C] 技能模块 (动态显示当前拥有的)
        lore.add(Component.literal("§d[已装载模块]"));
        getSkillList(tier).forEach(s -> lore.add(Component.literal("  " + s)));

        lore.add(Component.literal(" "));

        // [D] 进化指引 (动态显示下一级需求)
        if (tier < 5) {
            lore.add(Component.literal("§a[晋升方案 -> " + getTierName(tier + 1).replaceAll("§.", "").substring(0, 4) + "..§a]"));

            // 杀敌需求
            int kills = getKillCount();
            int target = getKillTarget(tier);
            String killColor = kills >= target ? "§a✔" : "§c✖";
            lore.add(Component.literal(String.format("  %s 击杀战绩: %d / %d", killColor, kills, target)));

            // 材料需求
            Item mat = getUpgradeMaterial(tier);
            int cost = getBaseMaterialCost(tier);
            boolean hasDiscount = ((float)kills / target) >= 0.5f;
            if (hasDiscount) cost = (int)Math.ceil(cost / 2.0); // 5折

            String costStr = hasDiscount ? ("§e(半价) " + cost) : ("" + cost);
            lore.add(Component.literal("  §7 材料注入: §f" + mat.getDescription().getString() + " x" + costStr));
            lore.add(Component.literal("  §8 (手持材料右键点击注入)"));
        } else {
            lore.add(Component.literal("§6★ 机体已进化至终极形态 ★"));
        }

        lore.add(Component.literal("§8===================="));
        lore.add(Component.literal("§8*此书仅为全息投影，无法取出"));

        // 打包写入 NBT
        CompoundTag display = new CompoundTag();
        ListTag loreTag = new ListTag();
        for (Component c : lore) {
            loreTag.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(c)));
        }
        display.put("Lore", loreTag);
        book.addTagElement("display", display);

        this.inventory.setItem(40, book);
    }



    // ==========================================
    // ✅ [冗余接口] UI 数据读取专用 (Getter)
    // ==========================================

    // 1. 获取基础名字 (不带前缀的)
    public String getBaseName() {
        return this.entityData.get(SYNC_BASE_NAME);
    }

    // 2. 获取枪管热度 (0-100)
    public int getHeat() {
        return this.entityData.get(DATA_HEAT);
    }

    // 3. 获取实时射速 (发/秒) - 逻辑与书本保持一致
    public float getFireRate() {
        return 20.0f / getFireDelay();
    }


    private List<String> getSkillList(int tier) {
        List<String> skills = new ArrayList<>();
        // 基础被动
        skills.add("§7➤ 动能穿透 (箭矢穿透)");

        if (tier >= 1) skills.add("§a➤ 极寒弹头 (减速 II)");
        if (tier >= 2) skills.add("§9➤ 电磁加速 (无视重力)");
        if (tier >= 3) {
            skills.add("§6➤ 智能引信 (安全爆破)");
            skills.add("§6➤ 神经毒素 (弱效策反)");
        }
        if (tier >= 4) {
            skills.add("§5➤ 聚变打击 (雷霆审判)");
            skills.add("§5➤ 纳米修复 (吸血光环)");
        }
        if (tier >= 5) {
            skills.add("§c➤ 终焉协议 (召唤援军)");
            skills.add("§c➤ 精神控制 (强效策反)");
        }

        // 动态技能
        if (this.entityData.get(IS_BRUTAL)) {
            skills.add("§4§k||§r §4[主动] 残暴模式 (400%攻速) §4§k||");
        } else if (checkFullBodyEnchanted()) {
            skills.add("§8[就绪] 残暴模式 (等待冷却)");
        }

        return skills;
    }

    private void autoEat() {
        if (eatCooldown > 0) {
            eatCooldown--;
            return;
        }
        for (int i = 12; i < 37; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEdible()) {
                FoodProperties food = stack.getItem().getFoodProperties(stack, this);
                if (food != null) {
                    this.heal((float) food.getNutrition());
                    this.playSound(SoundEvents.GENERIC_EAT, 1.0f, 1.0f);
                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack), this.getX(), this.getEyeY(), this.getZ(), 10, 0.1, 0.1, 0.1, 0.1);
                    }
                    stack.shrink(1);
                    eatCooldown = 40;
                    break;
                }
            }
        }
    }

    public void registerHit() {
        if (this.random.nextFloat() < 0.1f) this.playSound(SoundEvents.ARROW_HIT, 0.5f, 1.5f);
    }


    private int getEnchantLimit(int tier) {
        return switch(tier) {
            case 0 -> 2;
            case 1 -> 3;
            case 2 -> 4;
            case 3 -> 5;
            case 4 -> 6;
            case 5 -> 6;
            default -> 2;
        };
    }

    // =========================================================
    // ✅ 新增逻辑开始
    // =========================================================

    // 获取当前等级升级所需杀敌数
    private int getKillTarget(int tier) {
        return switch (tier) {
            case 0 -> 5;   // D -> C
            case 1 -> 80;  // C -> B
            case 2 -> 600; // B -> A
            case 3 -> 999; // A -> S
            case 4 -> 9999; // S -> L
            default -> 9999;
        };
    }

    // 获取升级所需材料类型
    private Item getUpgradeMaterial(int tier) {
        return switch (tier) {
            case 0 -> Items.COPPER_INGOT;      // D -> C
            case 1 -> Items.IRON_INGOT;        // C -> B
            case 2 -> Items.GOLD_INGOT;        // B -> A
            case 3 -> Items.DIAMOND;           // A -> S
            case 4 -> Items.NETHERITE_SCRAP;   // S -> L (此处为下界合金碎片)
            default -> Items.ANCIENT_DEBRIS;
        };
    }

    // 获取升级所需基础数量
    private int getBaseMaterialCost(int tier) {
        return (tier == 4) ? 5 : 25; // S->L需要5个，其他25个
    }

    // ==========================================
    // ✅ 新增：获取升级充能进度 (0-5)
    // ==========================================
    public int getUpgradeProgress() {
        return this.entityData.get(UPGRADE_PROGRESS);
    }
    // ==========================================
    // ✅ 新增：获取杀敌进度百分比 (用于 HUD 显示)
    // ==========================================
    public int getKillProgressPercent() {
        int tier = getTier();
        if (tier >= 5) return 100; // 满级了显示 100%

        // 调用内部的获取目标方法
        int target = getKillTarget(tier);
        if (target == 0) return 100; // 防止除以0

        int kills = getKillCount();

        // 计算百分比 (例如: 杀敌40 / 目标80 = 50%)
        int percent = (int)((float)kills / target * 100);

        return Math.min(percent, 100); // 封顶 100%
    }





    // 获取等级名称
    private String getTierName(int tier) {
        if (this.entityData.get(IS_BRUTAL)) return "§4§l☠ 终焉·魔神 (暴走)";
        return switch(tier) {
            case 0 -> "§7[D] 灰烬·哨兵";
            case 1 -> "§a[C] 森罗·游侠";
            case 2 -> "§9[B] 海渊·狙击手";
            case 3 -> "§6[A] 赤炎·毁灭者";
            case 4 -> "§5[S] 虚空·主宰";
            case 5 -> "§c§l[L] 终焉·魔神";
            default -> "未知";
        };
    }
    // ==========================================
    // ✅ 新增：获取或生成身份编号
    // ==========================================
    private String getUnitIdString() {
        int id = this.entityData.get(UNIT_ID);

        // 如果还没有编号 (是0)，就随机生成一个 (1-999)
        if (id == 0) {
            id = this.random.nextInt(999) + 1;
            this.entityData.set(UNIT_ID, id);
        }

        // 格式化为 3位数字 (例如 7 -> "007")
        return String.format("%03d", id);
    }

    // 更新名字显示
// 更新名字显示
// ==================== 📛 【第四步】 名字显示逻辑 ====================
    public void updateCustomName() {
        // 只在服务端运行，防止客户端用默认值覆盖
        if (this.level().isClientSide) return;

        // 1. 获取等级颜色
        String tierColor = switch(getTier()) {
            case 0 -> "§7"; case 1 -> "§a"; case 2 -> "§9";
            case 3 -> "§6"; case 4 -> "§5"; case 5 -> "§c";
            default -> "§f";
        };

        // 2. 获取编号 (例如 " #007")
        String idSuffix = " #" + getUnitIdString();

        // 3. ✅ 核心：读取全新的变量 SYNC_BASE_NAME
        String currentName = this.entityData.get(SYNC_BASE_NAME);

        // 4. 组装名字
        String finalName;
        if (this.entityData.get(IS_CAPTAIN)) {
            finalName = "§b[队伍] §6👑 " + tierColor + currentName + idSuffix;
        }
        else if (this.entityData.get(IS_SQUAD_MEMBER)) {
            finalName = "§b[队伍] " + tierColor + currentName + idSuffix;
        }
        else if (this.entityData.get(IS_FOLLOWING)) {
            finalName = "§8[后备] " + tierColor + currentName + idSuffix;
        }
        else {
            // 坚守或野生状态
            finalName = tierColor + currentName + idSuffix;
        }



        // 6. 应用到头顶
        this.setCustomName(Component.literal(finalName));
    }// 检查杀敌数
    private void checkKillUpgrade() {
        int tier = getTier();
        if (tier < 5) {
            int kills = getKillCount();
            int target = getKillTarget(tier);
            if (kills >= target) {
                performUpgrade(tier + 1);
                return;
            }
        }
        updateCustomName();
    }

    // 🚩 补上方法头，包住下面的逻辑
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);


        
        // ==================== 🏷️ 命名卡改名逻辑 (无须Shift) ====================
        
        // ==================== 🔷 青金石附魔逻辑 (Vanilla-Like) ====================
        if (item.getItem() == Items.LAPIS_LAZULI) {
            if (this.level().isClientSide) {
                return InteractionResult.SUCCESS;
            }

            // 1. 扫描可附魔装备
            EquipmentSlot[] slots = {
                net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET,
                net.minecraft.world.entity.EquipmentSlot.OFFHAND
            };

            ItemStack targetStack = ItemStack.EMPTY;
            EquipmentSlot targetSlot = null;

            for (EquipmentSlot slot : slots) {
                ItemStack s = this.getItemBySlot(slot);
                if (!s.isEmpty() && s.isEnchantable() && !s.isEnchanted()) {
                    targetStack = s;
                    targetSlot = slot;
                    break;
                }
            }

            if (targetStack.isEmpty()) {
                player.displayClientMessage(Component.literal("§c[错误] 无可附魔装备或装备已满附魔"), true);
                return InteractionResult.FAIL;
            }

            // 2. 确定附魔等级与消耗 (完全对标原版附魔台逻辑)
            // 逻辑: 检查玩家背包中的青金石数量 -> 决定附魔档位
            // 档位 1: 消耗 1 青金石 + 1 经验等级 (需要 10 级) -> 强度 10
            // 档位 2: 消耗 2 青金石 + 2 经验等级 (需要 20 级) -> 强度 20
            // 档位 3: 消耗 3 青金石 + 3 经验等级 (需要 30 级) -> 强度 30
            
            int lapisHeld = item.getCount();
            int tier = 0;
            int costLevels = 0;
            int requiredLevels = 0;
            int enchantPower = 0;

            // 优先匹配最高档位
            if (lapisHeld >= 3) {
                tier = 3;
                costLevels = 3;
                requiredLevels = 30;
                enchantPower = 30;
            } else if (lapisHeld == 2) {
                tier = 2;
                costLevels = 2;
                requiredLevels = 20;
                enchantPower = 20;
            } else {
                tier = 1;
                costLevels = 1;
                requiredLevels = 10;
                enchantPower = 10;
            }

            // 3. 校验玩家经验 (创造模式跳过)
            if (!player.getAbilities().instabuild) {
                if (player.experienceLevel < requiredLevels) {
                    player.displayClientMessage(Component.literal("§c[条件不足] 需要 " + requiredLevels + " 级经验 (当前: " + player.experienceLevel + ")"), true);
                    return InteractionResult.FAIL;
                }
            }

            // 4. 执行扣除
            if (!player.getAbilities().instabuild) {
                item.shrink(costLevels);
                player.giveExperienceLevels(-costLevels); // 扣除等级
            }

            // 5. 执行附魔 (使用原版 Helper)
            // ✅ 修正：使用玩家的附魔种子，确保与原版机制一致 (虽然没有预览，但保持底层逻辑一致)
            net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();
            random.setSeed(player.getEnchantmentSeed());
            
            EnchantmentHelper.enchantItem(random, targetStack, enchantPower, false);
            this.setItemSlot(targetSlot, targetStack);

            // ✅ 修正：更新玩家的附魔种子 (防止种子死锁)
            player.onEnchantmentPerformed(targetStack, costLevels);
            
            // 6. 反馈 (声音 + 粒子 + 提示)
            this.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
            if (this.level() instanceof ServerLevel serverLevel) {
                // 模拟附魔台周围飞向书本的字符粒子
                serverLevel.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 1.8, this.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
            }
            
            String msg = String.format("§a[附魔成功] 消耗 %d 青金石/%d 等级 -> %s (Lv.%d)", 
                costLevels, costLevels, targetStack.getHoverName().getString(), enchantPower);
            player.sendSystemMessage(Component.literal(msg));

            return InteractionResult.SUCCESS;
        }

        // ==================== 🛠️ 传送模块安装逻辑 ====================
        if (item.getItem() == ExampleMod.TELEPORT_UPGRADE_MODULE.get()) {
            if (!this.hasTeleportModule()) {
                if (!this.level().isClientSide) {
                    this.setHasTeleportModule(true);
                    this.playSound(SoundEvents.BEACON_ACTIVATE, 1.0f, 1.0f);
                    // 播放粒子效果
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.FIREWORK, this.getX(), this.getY() + 1.5, this.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
                    }
                    if (!player.getAbilities().instabuild) {
                        item.shrink(1);
                    }
                    player.sendSystemMessage(Component.translatable("message.examplemod.module_installed", this.getDisplayName()));
                }
                return InteractionResult.SUCCESS;
            } else {
                if (!this.level().isClientSide) {
                     player.sendSystemMessage(Component.translatable("message.examplemod.module_already_installed", this.getDisplayName()));
                }
                return InteractionResult.CONSUME;
            }
        }

// ==================== 🏷️ 【第三步】 命名牌改名逻辑 (强制拦截) ====================
        if (item.getItem() == Items.NAME_TAG) {
            // 只有当命名牌真的有名字时才生效
            if (item.hasCustomHoverName()) {
                String newName = item.getHoverName().getString();

                // 1. 修改全新的同步变量
                this.entityData.set(SYNC_BASE_NAME, newName);

                // 2. 打印一条日志到后台 (方便排查)
                System.out.println("DEBUG: 玩家修改炮台名字为 -> " + newName);

                // 3. 立即刷新显示
                updateCustomName();

                // 4. 消耗物品并播放音效
                this.playSound(SoundEvents.ANVIL_USE, 1.0f, 1.0f);
                if (!player.getAbilities().instabuild) item.shrink(1);

                return InteractionResult.SUCCESS; // 拦截原版逻辑
            }
            return InteractionResult.CONSUME;
        }

        // ==================== 🎮 普通右键 (打开菜单 / 切换模式) ====================
        // 迁移自 TurretInteractionHandler，实现逻辑内聚 (Entity-Centric Architecture)
        
        if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            
            // 1. Shift + 右键 (空手) -> 切换跟随/坚守模式
            if (player.isShiftKeyDown() && item.isEmpty()) {
                boolean newMode = !isFollowMode();
                setFollowMode(newMode);
                String status = newMode ? "§a[队伍] 已归队 (跟随)" : "§c[队伍] 已离队 (坚守)";
                player.sendSystemMessage(Component.literal(status));
                this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                return InteractionResult.SUCCESS;
            }

            // 2. 普通右键 (非潜行) -> 打开菜单
            if (!player.isShiftKeyDown()) {
                if (player instanceof ServerPlayer serverPlayer) {
                    updateInfoBookAndSlots();
                    NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                            (id, inv, p) -> new TurretMenu(id, inv, this, this.inventory),
                            this.getDisplayName()
                    ), (buf) -> buf.writeInt(this.getId()));
                }
                return InteractionResult.SUCCESS;
            }
        }
        
        return super.mobInteract(player, hand);
    }



    // 执行升级
    private void performUpgrade(int newTier) {
        setTier(newTier);

        this.entityData.set(UPGRADE_PROGRESS, 0);

        this.entityData.set(IS_BRUTAL, false);
        this.entityData.set(DATA_HEAT, 0);
        this.shotCounter = 0;
        this.overheatCooldown = 0;

        updateStatsAndEquip();

        if (this.level() instanceof ServerLevel level) {
            ItemStack fireworkItem = new ItemStack(Items.FIREWORK_ROCKET);
            CompoundTag tag = fireworkItem.getOrCreateTagElement("Fireworks");
            ListTag explosions = new ListTag();
            CompoundTag explosion = new CompoundTag();
            explosion.putByte("Type", (byte)4);
            explosion.putIntArray("Colors", new int[]{0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00});
            explosion.putBoolean("Trail", true);
            explosions.add(explosion);
            tag.put("Explosions", explosions);
            tag.putByte("Flight", (byte)1);

            FireworkRocketEntity rocket = new FireworkRocketEntity(level, this.getX(), this.getY(), this.getZ(), fireworkItem);
            level.addFreshEntity(rocket);

            this.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        String newAbility = getNewAbilityDesc(newTier);
        this.level().players().forEach(p -> {
            if (p.distanceToSqr(this) < 400) {
                p.sendSystemMessage(Component.literal("§6✦ 升级完成！§f" + this.getDisplayName().getString()));
                p.sendSystemMessage(Component.literal("§a  解锁能力: " + newAbility));
            }
        });
    }

    public String getNewAbilityDesc(int tier) {
        return switch (tier) {
            case 1 -> "§a寒冰射击 (攻击附带减速)";
            case 2 -> "§9直线狙击 (箭矢无重力)";
            case 3 -> "§6安全爆裂 (范围AOE不伤友军)";
            case 4 -> "§5雷霆审判 & 吸血光环";
            case 5 -> "§c终焉·魔神 (召唤暴走感染体)";
            default -> "未知力量";
        };
    }


    // 更新装备和属性
    public void updateStatsAndEquip() {
        int tier = getTier();
        updateCustomName();

        double maxHp = 20.0;
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, ItemStack.EMPTY);
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, ItemStack.EMPTY);
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, ItemStack.EMPTY);
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, ItemStack.EMPTY);

        switch (tier) {
            case 0: // D级 - 灰烬 (全套皮甲)
                maxHp = 20.0;
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
                break;

            case 1: // C级 - 森罗 (全套铁甲)
                maxHp = 50.0;
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                break;

            case 2: // B级 - 海渊 (全套金甲)
                maxHp = 100.0;
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Items.GOLDEN_LEGGINGS));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS));
                break;

            case 3: // A级 - 赤炎 (全套钻甲)
                maxHp = 150.0;
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
                break;

            case 4: // S级 - 虚空 (全套下界合金)
                maxHp = 250.0;
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
                break;

            case 5: // L级 - 终焉 (全套下界合金 + 可能的鞘翅或其他装饰)
                maxHp = 500.0;
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
                break;
        }

        // ✅ 继承旧弓附魔逻辑
        ItemStack oldBow = this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        ItemStack newBow = new ItemStack(Items.BOW);
        if (oldBow.isEnchanted()) {
            newBow.setTag(oldBow.getTag());
        }
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, newBow);

        // ✅ 同步属性到 Attribute 系统 (确保 UI 显示正确)
        // 1. 生命值
        Objects.requireNonNull(this.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(maxHp);
        this.setHealth((float) maxHp);

        // 2. 攻击伤害 (4 + tier * 5)
        double dmg = 4.0 + (tier * 5.0);
        if (this.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(dmg);
        }

        // 3. 搜索/攻击范围
        double range = getAttackRange();
        if (this.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(range);
        }
    }

    public void incrementKillCount() {
        this.entityData.set(KILL_COUNT, this.entityData.get(KILL_COUNT) + 1);

        // ✅ 新增：如果是清剿模式，单独记账
        if (this.isPurgeActive()) {
            this.purgeKillCount++;
        }

        checkKillUpgrade();
    }
    // 开启清剿模式 (由 ExampleMod 调用)
    public void startPurgeMode(float angle) {
        this.entityData.set(IS_PURGE_ACTIVE, true);
        this.purgeKillCount = 0; // 业绩归零
        this.purgeSearchAngle = angle; // 领受任务方向
        this.setCommandScavenging(false); // 停止捡垃圾


// 随机选一句台词
        String quote = PURGE_QUOTES[this.random.nextInt(PURGE_QUOTES.length)];

        // ✅ 修复：直接发给主人，不再在大范围内广播 (防止发不出来)
        if (!this.level().isClientSide && this.ownerUUID != null) {
            Player owner = this.level().getPlayerByUUID(this.ownerUUID);
            if (owner != null) {
                // 格式：<先锋小队> 收到指令，正在清场！
                owner.sendSystemMessage(Component.literal("§e<" + this.getDisplayName().getString() + "> §f" + quote));
            }
        }
        this.playSound(SoundEvents.RAVAGER_ROAR, 1.0f, 1.0f); // 吼叫音效
    }

    // 停止清剿模式
    public void stopPurgeMode() {
        if (!isPurgeActive()) return;

        this.entityData.set(IS_PURGE_ACTIVE, false);

        // 汇报战果
        if (this.ownerUUID != null && !this.level().isClientSide) {
            Player owner = this.level().getPlayerByUUID(this.ownerUUID);
            if (owner != null) {
                owner.sendSystemMessage(Component.literal("§a[报告] " + this.getDisplayName().getString() + " §a搜索结束，已击杀敌: §c" + this.purgeKillCount + "名"));
            }
        }
        this.purgeKillCount = 0;
    }



    public int getKillCount() { return this.entityData.get(KILL_COUNT); }
    // ==========================================
    // ⚙️ 射程配置表 (模拟服务器配置/接口)
    // ==========================================
    private static final Map<Integer, Double> RANGE_CONFIG = new HashMap<>();
    static {
        RANGE_CONFIG.put(1, 20.0);
        RANGE_CONFIG.put(2, 32.0);
        RANGE_CONFIG.put(3, 64.0);
        RANGE_CONFIG.put(4, 128.0);
        RANGE_CONFIG.put(5, 256.0);
    }

    // ✅ 射程控制
    public int getRangeLevel() {
        return getTier() + 1;
    }


    // ✅ 更新属性的具体实现
    public void updateRangeAttribute() {
        double range = getAttackRange();
        
        // 确保属性实例存在
        var attributeInstance = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (attributeInstance != null) {
            // 只有数值不同时才更新 (减少网络包)
            if (Math.abs(attributeInstance.getBaseValue() - range) > 0.01) {
                attributeInstance.setBaseValue(range);
            }
        }
    }
    
    public double getAttackRange() {
        int level = getRangeLevel();
        
        // 4. 异常处理：如果配置缺失，尝试回退
        if (!RANGE_CONFIG.containsKey(level)) {
            System.err.println("[Error] Missing range config for level " + level + ", using default.");
            return 20.0; // 默认值
        }
        
        return RANGE_CONFIG.get(level);
    }
    
    public int getTier() {
        return this.entityData.get(TIER);
    }
    public void setTier(int tier) { 
        this.entityData.set(TIER, tier); 
        this.updateRangeAttribute(); // Auto-update range
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {

        tag.putBoolean("FollowMode", this.isFollowMode());
        // ✅ 【第五步A】 保存新变量
        tag.putInt("UnitID", this.entityData.get(UNIT_ID));
        // RangeLevel not saved (derived)
        tag.putInt("DropCount", this.entityData.get(DROP_COUNT));
        tag.putString("CustomBaseName", this.entityData.get(SYNC_BASE_NAME));
        tag.putInt("XpBuffer", this.xpBuffer);
        tag.putInt("UpgradeProgress", this.entityData.get(UPGRADE_PROGRESS));
        tag.putBoolean("IsSquadMember", this.entityData.get(IS_SQUAD_MEMBER));
        // Save Teleport Module Data
        tag.putBoolean("HasTeleportModule", this.hasTeleportModule());
        tag.putInt("TeleportCooldown", this.teleportCooldown);
        
        super.addAdditionalSaveData(tag);
        tag.putInt("TurretTier", getTier());
        tag.putBoolean("IsFollowing", this.entityData.get(IS_FOLLOWING));
        tag.putInt("KillCount", getKillCount());
        tag.putBoolean("IsBrutal", this.entityData.get(IS_BRUTAL));
        tag.putInt("BrutalTimer", brutalityActiveTimer);
        tag.putInt("BrutalCD", brutalityCooldown);
        tag.putBoolean("IsCaptain", this.entityData.get(IS_CAPTAIN));

        if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID);

        ListTag itemList = new ListTag();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(itemTag);
                itemList.add(itemTag);
            }
        }
        tag.put("Inventory", itemList);
    }
    // ==================== ⚔ 清剿模式核心变量 ====================
    // 是否处于清剿模式
    public boolean isPurgeActive() {
        return this.entityData.get(IS_PURGE_ACTIVE);
    }
    // 本次清剿杀了多少个
    public int purgeKillCount = 0;
    // 我的搜索角度 (0-360度)
    private float purgeSearchAngle = 0.0f;

    // 敢死队台词库 (50句)
    private static final String[] PURGE_QUOTES = {
            "行动代号：焦土，执行中！", "收到指令，正在清场！", "一个都别想跑！", "区域净化程序已启动。",
            "为了指挥官的荣耀，杀！", "正在执行毁灭性打击！", "目视范围内，不允许存在活物。", "猎杀时刻到了！",
            "全弹发射，覆盖射击！", "正在执行最高级别清洗。", "杂碎们，迎接审判吧！", "不再仁慈，不再犹豫！",
            "地毯式搜索，不留死角！", "任何阻挡者，死！", "正在移除所有碳基生物。", "让这片土地重归寂静。",
            "清理害虫，就在此刻。", "敢死队，冲锋！", "把它们撕成碎片！", "火力全开，寸草不生！",
            "收割生命的时间到了。", "正在重写区域生态。", "恐惧吧，逃跑吧，然后死吧！", "没有任何东西能幸存。",
            "正在执行死刑判决。", "让火焰净化一切！", "这就是战争！", "没有人能逃脱我的准星。",
            "毁灭，只是开始。", "正在抹除所有敌对目标。", "为了绝对的秩序！", "障碍清除中...",
            "正在执行种族灭绝协议。", "这片区域将被鲜血染红。", "狩猎愉快，兄弟们！", "把它们全部送入虚空！",
            "正在执行第66号令。", "绝不留情，绝不手软！", "死亡如风，常伴吾身。", "正在清空弹夹...",
            "目标确认：所有活物。", "正在制造尸山血海。", "让它们见识真正的恐惧。", "正在执行强制拆除。",
            "为了主人的意志，杀戮！", "正在执行终极清理。", "无论是谁，格杀勿论！", "毁灭倒计时开始。",
            "正在执行焦土政策。", "任务：杀光一切。"
    };

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {

        if (tag.contains("FollowMode")) {
            this.setFollowMode(tag.getBoolean("FollowMode"));
        }
        // RangeLevel derived from Tier, ignored from tag

        if (tag.contains("UnitID")) {
            this.entityData.set(UNIT_ID, tag.getInt("UnitID"));
        }
        if (tag.contains("DropCount")) {
            this.entityData.set(DROP_COUNT, tag.getInt("DropCount"));
        }
        if (tag.contains("CustomBaseName")) {
            this.entityData.set(SYNC_BASE_NAME, tag.getString("CustomBaseName"));
        } else if (tag.contains("TurretBaseName")) {
            // 如果是旧存档，把旧名字迁移过来
            this.entityData.set(SYNC_BASE_NAME, tag.getString("TurretBaseName"));
        }
        if (tag.contains("TurretBaseName")) {
        }
        this.squadJoinTime = tag.getLong("SquadJoinTime");
        super.readAdditionalSaveData(tag);
        setTier(tag.getInt("TurretTier"));
        this.xpBuffer = tag.getInt("XpBuffer");
        this.entityData.set(IS_FOLLOWING, tag.getBoolean("IsFollowing"));
        this.entityData.set(KILL_COUNT, tag.getInt("KillCount"));
        this.entityData.set(IS_BRUTAL, tag.getBoolean("IsBrutal"));
        brutalityActiveTimer = tag.getInt("BrutalTimer");
        brutalityCooldown = tag.getInt("BrutalCD");
        this.entityData.set(IS_CAPTAIN, tag.getBoolean("IsCaptain"));
        this.entityData.set(IS_SQUAD_MEMBER, tag.getBoolean("IsSquadMember"));

        // Read Teleport Module Data
        if (tag.contains("HasTeleportModule")) {
            this.setHasTeleportModule(tag.getBoolean("HasTeleportModule"));
        }
        if (tag.contains("TeleportCooldown")) {
            this.teleportCooldown = tag.getInt("TeleportCooldown");
        }

        if (tag.hasUUID("OwnerUUID")) ownerUUID = tag.getUUID("OwnerUUID");

        if (tag.contains("Inventory")) {
            ListTag itemList = tag.getList("Inventory", 10);
            for (int i = 0; i < itemList.size(); i++) {
                CompoundTag itemTag = itemList.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot < inventory.getContainerSize()) {
                    inventory.setItem(slot, ItemStack.of(itemTag));
                }
            }
        }
        updateStatsAndEquip();
    }

    // ==========================================
    // ✅ 核心：从记录卡恢复完整数据 (Phase 3)
    // ==========================================
    public void restoreFromRecord(CompoundTag dataTag) {
        // 1. 基础属性
        if (dataTag.contains("UnitID")) this.entityData.set(UNIT_ID, dataTag.getInt("UnitID"));
        if (dataTag.contains("Tier")) this.setTier(dataTag.getInt("Tier"));
        if (dataTag.contains("Heat")) this.entityData.set(DATA_HEAT, dataTag.getInt("Heat"));
        // Level is derived from Tier
        if (dataTag.contains("XP")) this.entityData.set(DATA_XP, dataTag.getInt("XP"));
        if (dataTag.contains("IsBrutal")) this.entityData.set(IS_BRUTAL, dataTag.getBoolean("IsBrutal"));
        if (dataTag.contains("UpgradeProgress")) this.entityData.set(UPGRADE_PROGRESS, dataTag.getInt("UpgradeProgress"));
        if (dataTag.contains("KillCount")) this.entityData.set(KILL_COUNT, dataTag.getInt("KillCount"));
        if (dataTag.contains("DropCount")) this.entityData.set(DROP_COUNT, dataTag.getInt("DropCount"));

        // 1.1 恢复主人和名字
        if (dataTag.hasUUID("OwnerUUID")) {
            this.ownerUUID = dataTag.getUUID("OwnerUUID");
            this.entityData.set(OWNER_UUID_SYNC, Optional.of(this.ownerUUID));
        }
        if (dataTag.contains("BaseName")) {
            this.entityData.set(SYNC_BASE_NAME, dataTag.getString("BaseName"));
        }
        // 强制刷新一次名字
        updateCustomName();

        // 2. 恢复背包
        this.inventory.removeAllItems();
        if (dataTag.contains("Inventory")) {
            ListTag inventoryList = dataTag.getList("Inventory", 10);
            for (int i = 0; i < inventoryList.size(); i++) {
                CompoundTag itemTag = inventoryList.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255; // Use getByte to be safe
                if (slot >= 0 && slot < this.inventory.getContainerSize()) {
                    this.inventory.setItem(slot, ItemStack.of(itemTag));
                }
            }
        }

        // 3. 恢复装备槽位
        // 先清空现有装备 (以防万一)
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            this.setItemSlot(slot, ItemStack.EMPTY);
        }
        
        if (dataTag.contains("Equipment")) {
            ListTag equipmentList = dataTag.getList("Equipment", 10);
            for (int i = 0; i < equipmentList.size(); i++) {
                CompoundTag equipTag = equipmentList.getCompound(i);
                String slotName = equipTag.getString("SlotName");
                net.minecraft.world.entity.EquipmentSlot slot = net.minecraft.world.entity.EquipmentSlot.byName(slotName);
                if (slot != null) {
                    this.setItemSlot(slot, ItemStack.of(equipTag));
                }
            }
        }
        
        // 4. 刷新属性
        this.updateStatsAndEquip();
        
        // 5. [Fix] 物理状态重置 (防止复活后悬空/无重力)
        this.setNoGravity(false);
        this.resetFallDistance();
        this.setDeltaMovement(0, -0.1, 0); // 给予微小向下速度触发接地判断
        this.setOnGround(true); // 预设为接地状态，由tick逻辑修正
    }

    @Override protected boolean isSunBurnTick() { return false; }
    @Override
    public boolean isPushable() {
        // 打印中不能被推
        if (getPrintState() != 0) return false;
        return this.entityData.get(IS_FOLLOWING);
    }

    // ==========================================================
    // ✅ 强力驻守模式：防消失 + 区块强加载
    // ==========================================================



// ==========================================================
    // ✅ 强力驻守模式：防消失 + 区块强加载
    // ==========================================================

    // 记录上一次所在的区块位置 (这个变量必须定义在类里，如果你还没定义，请去文件最上面定义它)
    // private net.minecraft.world.level.ChunkPos keptChunkPos;
    // (如果你上面已经定义了 keptChunkPos，就不用管这行注释)

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // 永不消失
    }

    @Override
    public void aiStep() {
        super.aiStep();
        


        // 只在服务端执行
        if (!this.level().isClientSide && this.tickCount % 20 == 0) {
            net.minecraft.world.level.ChunkPos currentPos = this.chunkPosition();
            ServerLevel sl = (ServerLevel) this.level();

            if (keptChunkPos == null || !keptChunkPos.equals(currentPos)) {
                if (keptChunkPos != null) {
                    sl.setChunkForced(keptChunkPos.x, keptChunkPos.z, false);
                }
                sl.setChunkForced(currentPos.x, currentPos.z, true);
                keptChunkPos = currentPos;
            }
        }
    } // 🟢 这里的 } 必须有！结束 aiStep 方法

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && keptChunkPos != null) {
            ((ServerLevel) this.level()).setChunkForced(keptChunkPos.x, keptChunkPos.z, false);
            keptChunkPos = null;
        }
        super.remove(reason);
    } // 🟢 这里的 } 必须有！结束 remove 方法

    // 👇 下面应该是 TurretFollowGoal，千万不要把它包进上面的方法里！

    // ==========================================
    // ✅ 优化：智能跟随 AI (Smart Pathfinding & Decision Making)
    // ==========================================
    static class TurretFollowGoal extends Goal {
        private final SkeletonTurret turret;
        private LivingEntity owner;
        private final double speedModifier;
        private final float startDistance;
        private final float stopDistance;
        
        // AI State
        private int timeToRecalculatePath;
        private int thinkingTicks;      // 思考停顿计时
        private int stuckTimer;         // 卡死判定计时
        private net.minecraft.world.phys.Vec3 lastStuckCheckPos;
        private int pathFailures;       // 路径计算失败次数
        // private boolean isSprinting;    // Removed unused variable

        // Constants
        private static final int THINKING_DURATION = 15; // 0.75s 思考时间
        private static final int MAX_STUCK_TIME = 40;    // 2s 卡死则触发脱困
        
        public TurretFollowGoal(SkeletonTurret turret, double speed, float start, float stop) {
            this.turret = turret;
            this.speedModifier = speed;
            this.startDistance = start;
            this.stopDistance = stop;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public void stop() {
            this.turret.getNavigation().stop();
            this.turret.setSprinting(false);
            this.thinkingTicks = 0;
            this.stuckTimer = 0;
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = this.turret.getOwner();
            if (owner == null) return false;
            
            // 状态检查
            if (!this.turret.isFollowing()) return false;
            if (this.turret.isPurgeActive()) return false;
            if (this.turret.isCommandScavenging()) return false;
            if (owner.isSpectator()) return false;

            // 距离检查
            if (this.turret.distanceToSqr(owner) < (double)(this.startDistance * this.startDistance)) return false;

            this.owner = owner;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (!this.turret.isFollowing()) return false;
            if (this.turret.isPurgeActive()) return false;
            if (this.turret.isCommandScavenging()) return false;
            if (this.turret.getNavigation().isDone()) return false;
            
            return this.turret.distanceToSqr(this.owner) > (double)(this.stopDistance * this.stopDistance);
        }

        @Override
        public void start() {
            this.timeToRecalculatePath = 0;
            this.thinkingTicks = 0;
            this.stuckTimer = 0;
            this.pathFailures = 0;
            this.lastStuckCheckPos = this.turret.position();
        }

        @Override
        public void tick() {
            this.turret.getLookControl().setLookAt(this.owner, 10.0F, (float)this.turret.getMaxHeadXRot());

            // 0. 思考状态 (模拟停顿观察)
            if (this.thinkingTicks > 0) {
                this.thinkingTicks--;
                this.turret.getNavigation().stop();
                return; // 思考中，不移动
            }

            double distSqr = this.turret.distanceToSqr(this.owner);
            double dist = Math.sqrt(distSqr);
            double yDiff = Math.abs(this.owner.getY() - this.turret.getY());

            // 1. 传送决策 (Priority 1: Teleport)
            // 触发条件：有模块 & (卡死 OR 距离过远 OR 高度差过大)
            boolean needTeleport = (this.stuckTimer > MAX_STUCK_TIME) 
                                || (dist > 32.0) 
                                || (yDiff > 2.5 && dist < 8.0 && !this.turret.getNavigation().isInProgress());
                                
            if (needTeleport && tryTeleport()) {
                return; // 传送成功，本 tick 结束
            }

            // 2. 移动模式切换 (Walk/Sprint)
            // 距离 > 8 格且路径畅通时疾跑
            if (dist > 8.0 && this.turret.getNavigation().getPath() != null) {
                this.turret.setSprinting(true);
            } else {
                this.turret.setSprinting(false);
            }

            // 3. 路径规划 (LOD & Adaptive Frequency)
            if (--this.timeToRecalculatePath <= 0) {
                // 根据距离调整计算频率 (LOD)
                // 远距离(>20): 40 ticks (2s)
                // 中距离(>10): 20 ticks (1s)
                // 近距离(<10): 10 ticks (0.5s)
                if (dist > 20) this.timeToRecalculatePath = 40;
                else if (dist > 10) this.timeToRecalculatePath = 20;
                else this.timeToRecalculatePath = 10;

                // 尝试移动
                if (!this.turret.getNavigation().moveTo(this.owner, this.speedModifier)) {
                    // 路径计算失败
                    this.pathFailures++;
                    
                    // 连续失败 2 次 -> 进入思考状态 (模拟观察地形)
                    if (this.pathFailures >= 2) {
                        this.thinkingTicks = THINKING_DURATION + this.turret.getRandom().nextInt(10); // 0.75s - 1.25s
                        this.pathFailures = 0; // 重置计数
                    }
                } else {
                    // 路径计算成功
                    this.pathFailures = 0;
                    
                    // 模拟"非最优路径"决策 (Randomness 15-25%)
                    // 偶尔故意停顿一下，显得像人在犹豫
                    if (this.turret.getRandom().nextFloat() < 0.02f) { // 2% 概率每 tick (实际在路径更新时判定)
                         this.thinkingTicks = 10; 
                    }
                }
            }

            // 4. 卡死检测与智能跳跃
            checkStuckAndJump();
        }

        private boolean tryTeleport() {
            if (!this.turret.hasTeleportModule()) return false;
            // 检查冷却 (假设 SkeletonTurret 有 public int teleportCooldown 或者 getter)
            // 这里我们使用反射出来的字段或假设已修复访问权限
            // 根据之前的 grep，teleportCooldown 是 private 且没有 getter，但有 setTeleportCooldown
            // 我们需要修改 SkeletonTurret 添加 getTeleportCooldown() 或者将字段改为 public
            // *为了稳妥，这里先用反射或者假设我能修改 SkeletonTurret*
            // 实际上我可以直接修改 SkeletonTurret 来添加访问器。
            
            // 暂时假设: 我会添加一个 public int getTeleportCooldown() 到 SkeletonTurret
            if (this.turret.getTeleportCooldown() > 0) return false;

            // 执行传送
            double targetX = this.owner.getX() + (this.turret.getRandom().nextDouble() - 0.5) * 2.0;
            double targetY = this.owner.getY();
            double targetZ = this.owner.getZ() + (this.turret.getRandom().nextDouble() - 0.5) * 2.0;
            
            // 简单的传送逻辑 (调用原版 randomTeleport 变体)
            if (this.turret.randomTeleport(targetX, targetY, targetZ, true)) {
                this.turret.notifyTeleport(); // 播放特效和声音
                
                // 计算新冷却: 60 - Tier * 10 (Min 10)
                int tier = this.turret.getTier();
                int cooldown = Math.max(10, 60 - tier * 10);
                this.turret.setTeleportCooldown(cooldown);
                
                this.stuckTimer = 0; // 重置卡死
                this.turret.getNavigation().stop();
                return true;
            }
            return false;
        }

        private void checkStuckAndJump() {
            // 每 4 tick 检查一次
            if (this.turret.tickCount % 4 != 0) return;

            net.minecraft.world.phys.Vec3 currentPos = this.turret.position();
            if (this.lastStuckCheckPos != null && currentPos.distanceToSqr(this.lastStuckCheckPos) < 0.01) { // 移动非常微小
                if (this.turret.getNavigation().isInProgress()) {
                    this.stuckTimer += 4;
                }
            } else {
                this.stuckTimer = 0;
                this.lastStuckCheckPos = currentPos;
            }

            // 智能跳跃：水平碰撞且在地面 -> 跳
            if (this.turret.horizontalCollision && this.turret.onGround()) {
                 this.turret.getJumpControl().jump();
                 // 如果卡住时间较长，尝试加大跳跃力度 (通过给予向上的速度)
                 if (this.stuckTimer > 20) {
                     this.turret.setDeltaMovement(this.turret.getDeltaMovement().add(0, 0.1, 0));
                 }
            }
            
            // 沟壑跳跃检测 (简单的)
            // 检测前方是否是空气，且远处有方块
            // 这部分比较复杂，Vanilla AI 通常靠 PathNavigation 处理跳跃
            // 这里我们主要依赖 PathNavigation，但在 Stuck 时辅助跳跃
        }
    }

    // ==========================================
    // ✅ 修复：射击执行逻辑 (带射程锁 & 完整定义)
    // ==========================================
    static class RampUpBowAttackGoal extends Goal {
        // 1. 补回丢失的变量
        private final SkeletonTurret mob;
        private int attackTime = -1;

        // 2. 补回丢失的构造函数 (现在可以接收参数了)
        public RampUpBowAttackGoal(SkeletonTurret mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        // 3. 补回丢失的 canUse 方法 (没有它 AI 跑不起来)
        @Override
        public boolean canUse() {
            if (mob.overheatCooldown > 0) return false;      // 过热不能射
            if (mob.postTeleportAttackDelay > 0) return false; // ✅ 传送后延迟 (0.2s)
            return this.mob.getTarget() != null;             // 有目标才能射
        }

        @Override
        public void start() {
            super.start();
            this.attackTime = -1;
            this.mob.getNavigation().stop(); // ✅ 立即停车
        }

        // 4. 核心逻辑 (带射程检查)
        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null) return;

            // ✅ 强制站桩 (每帧都停，防止被其他因素推动)
            this.mob.getNavigation().stop();

            // --- 🛑 射程锁 (新增) ---
            double distSqr = this.mob.distanceToSqr(target);
            double attackRange = this.mob.getAttackRange();

            // 如果目标跑出了射程，立刻放弃治疗
            if (distSqr > attackRange * attackRange) {
                this.mob.setTarget(null);
                return;
            }
            // -----------------------

            // 瞄准
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

            // 计算冷却
            // 计算冷却
            int minCd = (int) mob.getFireDelay();

            // 开火逻辑
            if (--this.attackTime <= 0) {
                boolean isClose = distSqr < 36.0;
                boolean isSmart = mob.getTier() >= 2;
                boolean canSee = this.mob.getSensing().hasLineOfSight(target);

                // 只有在射程内(前面已查) 且 (能看见/聪明/贴脸) 时才开火
                if (canSee || isSmart || isClose) {
                    this.mob.performRangedAttack(target, 1.0F);
                    this.attackTime = minCd;
                }
            }
        }
    }    // ==========================================
    // ✅ 新增：火把照明系统 (自动副手装备)
    // ==========================================
    private void manageTorchBehavior() {
        // 1. 扫描背包，看有没有火把
        boolean hasTorch = false;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(Items.TORCH)) {
                hasTorch = true;
                break;
            }
        }

        ItemStack currentOffhand = this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND);

        // 2. 如果有火把
        if (hasTorch) {
            // 如果手上拿的不是火把，赶紧换成火把
            if (!currentOffhand.is(Items.TORCH)) {
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH));
            }

            // 视觉特效：每隔几秒冒一点火星，增加氛围感
            if (this.level().isClientSide && this.tickCount % 10 == 0) {
                double offsetX = -Math.sin(this.getYRot() * ((float)Math.PI / 180F)) * 0.4;
                double offsetZ = Math.cos(this.getYRot() * ((float)Math.PI / 180F)) * 0.4;
                this.level().addParticle(ParticleTypes.FLAME, this.getX() + offsetX, this.getY() + 1.5, this.getZ() + offsetZ, 0, 0, 0);
            }
        }
        // 3. 如果没火把 (或者火把被拿走了)
        else {
            // 如果手上还傻傻拿着火把，赶紧放下
            if (currentOffhand.is(Items.TORCH)) {
                // 恢复原有的装备：B级(2)以上应该拿盾牌，否则空手
                if (getTier() >= 2) {
                    this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                } else {
                    this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                }
            }
        }
    }
    // ==========================================
    // ✅ 新增：RPG 附魔系统辅助方法
    // ==========================================

    // 辅助：给指定物品增加 1 个随机新附魔
    private void addRandomEnchantment(ItemStack stack) {
        if (stack.isEmpty()) return;

        // 1. 获取当前已有的附魔
        Map<net.minecraft.world.item.enchantment.Enchantment, Integer> currentEnchants = EnchantmentHelper.getEnchantments(stack);

        // 2. 从游戏所有附魔中筛选出能用的
        List<net.minecraft.world.item.enchantment.Enchantment> possible = new ArrayList<>();
        for (net.minecraft.world.item.enchantment.Enchantment ench : net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS) {
            // 条件：物品支持这个附魔 && 当前没有这个附魔 && 不与现有附魔冲突
            if (ench.canEnchant(stack) && !currentEnchants.containsKey(ench)) {
                boolean compatible = true;
                for (net.minecraft.world.item.enchantment.Enchantment existing : currentEnchants.keySet()) {
                    if (!ench.isCompatibleWith(existing)) {
                        compatible = false;
                        break;
                    }
                }
                if (compatible) possible.add(ench);
            }
        }

        // 3. 随机挑一个加上去
        if (!possible.isEmpty()) {
            net.minecraft.world.item.enchantment.Enchantment pick = possible.get(this.random.nextInt(possible.size()));
            stack.enchant(pick, 1); // 初始等级 1
        }
    }

    // 辅助：升级全身所有装备的附魔等级
    private void upgradeAllEquipmentLevels() {
        net.minecraft.world.entity.EquipmentSlot[] slots = {
                net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET
        };

        for (EquipmentSlot slot : slots) {
            ItemStack stack = this.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isEnchanted()) continue;

            // 获取附魔列表 -> 等级+1 -> 写回物品
            Map<net.minecraft.world.item.enchantment.Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
            for (Map.Entry<net.minecraft.world.item.enchantment.Enchantment, Integer> entry : enchants.entrySet()) {
                int newLevel = entry.getValue() + 1;
                // 这里可以设置个上限，比如 10级，防止溢出崩服，或者不设上限爽就完事了
                if (newLevel <= 10) {
                    entry.setValue(newLevel);
                }
            }
            EnchantmentHelper.setEnchantments(enchants, stack);
        }
    }

    // 辅助：检查是否全身装备都有附魔（不需要满3个，只要有就行）
    private boolean checkFullBodyEnchanted() {
        net.minecraft.world.entity.EquipmentSlot[] slots = {
                net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET
        };
        for (EquipmentSlot slot : slots) {
            ItemStack stack = this.getItemBySlot(slot);
            // 如果某个部位没东西，或者没附魔，就不算觉醒
            if (stack.isEmpty() || !stack.isEnchanted()) return false;
        }
        return true;
    }
    // ✅ 补充这个方法，允许外部读取主人UUID
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }
    @javax.annotation.Nullable
    public LivingEntity getOwner() {
        if (this.ownerUUID == null) return null;
        return this.level().getPlayerByUUID(this.ownerUUID);
    }

    // ==========================================
    // ✅ 新增：倒车雷达 (自动保持距离)
    // ==========================================
    static class MaintainSpaceGoal extends Goal {
        private final SkeletonTurret turret;
        private final double speed;
        private final float minDistance; // 最小允许距离 (3格)
        private LivingEntity owner;

        public MaintainSpaceGoal(SkeletonTurret turret, double speed, float minDistance) {
            this.turret = turret;
            this.speed = speed;
            this.minDistance = minDistance;
            // 这是一个移动类任务，所以要加 MOVE 标记
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // ✅ 修复 1：如果在水里，禁止触发“后退”逻辑！防止把它推回水里，或者在水里跟逃生逻辑打架。
            if (turret.isInWaterOrBubble()) return false;

            // 原有的判断
            if (!turret.entityData.get(IS_FOLLOWING)) return false;
            // ✅ 修复：清剿模式下，不需要保持社交距离
            if (turret.isPurgeActive()) return false;
            if (turret.ownerUUID == null) return false;
            Player p = turret.level().getPlayerByUUID(turret.ownerUUID);
            if (p == null) return false;
            this.owner = p;

            return turret.distanceToSqr(p) < (minDistance * minDistance);
        }

        @Override
        public boolean canContinueToUse() {
            if (turret.isPurgeActive()) return false;
            // ✅ 修复 2：如果倒车倒着倒着掉水里了，立刻停止！把控制权交给“逃生逻辑”。
            if (turret.isInWaterOrBubble()) return false;

            return !turret.getNavigation().isDone() &&
                    owner != null &&
                    turret.distanceToSqr(owner) < (minDistance * minDistance);
        }

        @Override
        public void start() {
            // 寻找一个“远离”主人的位置
            // 参数解释: turret, 向外找4格, 向上找2格, 远离owner的坐标
            net.minecraft.world.phys.Vec3 awayPos = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPosAway(turret, 4, 2, owner.position());

            if (awayPos != null) {
                // 开始移动到那个远离点，速度稍微快一点 (1.0)
                turret.getNavigation().moveTo(awayPos.x, awayPos.y, awayPos.z, speed);
            }
        }

        @Override
        public void stop() {
            // 倒车结束，停下来
            turret.getNavigation().stop();
        }
    }
    // ==========================================
    // ✅ 新增：拾取食物 & 经验转化系统
    // ==========================================
    private void handlePickupAndXp() {
        if (this.level().isClientSide) return; // 只在服务端运行

        // 设定拾取范围：以炮台为中心，向外扩 3.5 格
        // getBoundingBox() 是炮台的碰撞箱，inflate(1.5) 是把箱子变大
        List<Entity> targets = this.level().getEntities(this, this.getBoundingBox().inflate(3.5));

        for (Entity target : targets) {

            // --- 逻辑 A: 拾取物品 (食物 & 杂物) ---
            if (target instanceof ItemEntity itemEntity) {
                ItemStack stack = itemEntity.getItem();

                // 1. 必须是没被捡过的
                if (itemEntity.hasPickUpDelay()) continue;

                // 2. 判定是否拾取：
                // - 如果是食物：总是拾取 (为了回血)
                // - 如果开启了拾荒模式：拾取所有东西
                boolean shouldPickup = stack.isEdible() || isCommandScavenging();

                if (shouldPickup) {
                    ItemStack remainder = addItemToInventory(stack);

                    // 如果全部捡起来了
                    if (remainder.isEmpty()) {
                        itemEntity.discard(); // 删除地上的掉落物
                        this.playSound(SoundEvents.ITEM_PICKUP, 0.2F, 1.5F);
                    }
                    // 如果只捡了一部分 (背包满了)
                    else {
                        itemEntity.setItem(remainder);
                        
                        // 3. 背包满提示 (每3秒一次)
                        if (this.tickCount % 60 == 0) {
                             if (this.ownerUUID != null) {
                                 Player owner = this.level().getPlayerByUUID(this.ownerUUID);
                                 if (owner != null && this.distanceTo(owner) < 12) {
                                     owner.displayClientMessage(Component.literal("§c[炮台] 背包已满！"), true);
                                 }
                             }
                             // 同步状态给 HUD (如果有)
                             this.entityData.set(DATA_STATUS_OVERLAY, "§c🎒 FULL");
                        }
                    }
                }
            }

            // --- 逻辑 B: 吸收经验球并转化为瓶子 ---
            if (target instanceof ExperienceOrb orb) {
                // 1. 获取经验值
                int amount = orb.getValue();

                // 2. 存入缓存
                this.xpBuffer += amount;

                // 3. 吸收掉经验球
                orb.discard();
                this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.1F, 1.0F);

                // 4. 尝试转化：每 10 点经验 = 1 个附魔之瓶
                while (this.xpBuffer >= 10) {
                    ItemStack bottle = new ItemStack(Items.EXPERIENCE_BOTTLE);
                    ItemStack left = addItemToInventory(bottle);

                    if (left.isEmpty()) {
                        // 成功放入背包，扣除缓存
                        this.xpBuffer -= 10;
                    } else {
                        // 背包满了！停止转化，剩下的经验留着下次再说
                        // 或者：把瓶子吐出来掉地上 (防止吞经验)
                        // 这里我们选择保留在缓存里，等背包有空位再转
                        break;
                    }
                }
            }
        }
    }

    // 辅助工具：尝试把物品放入背包 (仅限普通储物格 10-36)
    // 返回值：没能放进去的剩余物品 (如果为空，说明全放进去了)
    private ItemStack addItemToInventory(ItemStack stack) {
        ItemStack toAdd = stack.copy();

        // 定义普通背包的范围 (索引 10 ~ 36)
        // 0-4: 装备栏, 5-9: 升级模块, 10-36: 储物箱
        int startSlot = 10;
        int endSlot = 36;

        // 1. 先尝试堆叠到已有的格子里
        for (int i = startSlot; i <= endSlot; i++) {
            ItemStack slotStack = inventory.getItem(i);

            // 如果是同一种物品，且还能堆叠
            if (ItemStack.isSameItemSameTags(slotStack, toAdd) && slotStack.getCount() < slotStack.getMaxStackSize()) {
                int space = slotStack.getMaxStackSize() - slotStack.getCount();
                int moveCount = Math.min(space, toAdd.getCount());

                slotStack.grow(moveCount);
                toAdd.shrink(moveCount);

                if (toAdd.isEmpty()) return ItemStack.EMPTY; // 完了
            }
        }

        // 2. 如果还有剩的，找空格子放
        for (int i = startSlot; i <= endSlot; i++) {
            if (inventory.getItem(i).isEmpty()) {
                inventory.setItem(i, toAdd);
                return ItemStack.EMPTY; // 完了
            }
        }

        return toAdd; // 返回剩下的 (背包满了)
    }
    // ==========================================
    // ✅ 新增：护主逻辑 (主人挨打，我帮忙)
    // ==========================================
    static class TurretDefendOwnerGoal extends net.minecraft.world.entity.ai.goal.target.TargetGoal {
        private final SkeletonTurret turret;
        private LivingEntity attacker;
        private int timestamp;

        public TurretDefendOwnerGoal(SkeletonTurret turret) {
            super(turret, false);
            this.turret = turret;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            // 1. 必须有主人
            if (this.turret.ownerUUID == null) return false;
            Player owner = this.turret.level().getPlayerByUUID(this.turret.ownerUUID);
            if (owner == null) return false;

            // 2.以此判定：主人是否刚刚受过伤？
            this.attacker = owner.getLastHurtByMob();
            int i = owner.getLastHurtByMobTimestamp();

            // 3. 检查时间戳，防止翻旧账
            return i != this.timestamp && this.canAttack(this.attacker, net.minecraft.world.entity.ai.targeting.TargetingConditions.DEFAULT);
        }




        @Override
        public void start() {
            this.mob.setTarget(this.attacker);
            Player owner = this.turret.level().getPlayerByUUID(this.turret.ownerUUID);
            if (owner != null) {
                this.timestamp = owner.getLastHurtByMobTimestamp();
            }
            super.start();
        }

        // 排除友军 (非常重要，防止误伤队友)
        @Override
        protected boolean canAttack(@Nullable LivingEntity target, net.minecraft.world.entity.ai.targeting.TargetingConditions targetPredicate) {
            if (target == null) return false;
            if (target instanceof Player && target.getUUID().equals(this.turret.ownerUUID)) return false; // 别打主人
            if (target instanceof SkeletonTurret) return false; // 别打友军塔
            if (target instanceof IronGolem) return false;      // 别打铁傀儡
            // 别打我们认证过的友军怪物
            if (target.getPersistentData().getBoolean("IsFriendlyZombie")) return false;
            if (target.getPersistentData().getBoolean("IsFriendlyCreeper")) return false;

            return super.canAttack(target, targetPredicate);
        }
    }

    // ==========================================
    // ✅ 新增：协作逻辑 (主人打谁，我打谁)
    // ==========================================
    static class TurretAssistOwnerGoal extends net.minecraft.world.entity.ai.goal.target.TargetGoal {
        private final SkeletonTurret turret;
        private LivingEntity target;
        private int timestamp;

        public TurretAssistOwnerGoal(SkeletonTurret turret) {
            super(turret, false);
            this.turret = turret;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            // 1. 基础检查
            if (this.turret.ownerUUID == null) return false;
            Player owner = this.turret.level().getPlayerByUUID(this.turret.ownerUUID);
            if (owner == null) return false;

            // 2. 获取主人攻击的目标
            this.target = owner.getLastHurtMob();
            int i = owner.getLastHurtMobTimestamp();
            if (i == this.timestamp) return false;

            // 🛑 【核心修复】主人打的怪如果太远，我也不管！
            if (this.target != null) {
                double maxRange = this.turret.getAttackRange();
                if (this.target.distanceToSqr(this.turret) > maxRange * maxRange) {
                    return false; // 超出射程，不予协助
                }
            }

            return this.canAttack(this.target, net.minecraft.world.entity.ai.targeting.TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            this.mob.setTarget(this.target);
            Player owner = this.turret.level().getPlayerByUUID(this.turret.ownerUUID);
            if (owner != null) {
                this.timestamp = owner.getLastHurtMobTimestamp();
            }
            super.start();
        }

        @Override
        protected boolean canAttack(@Nullable LivingEntity target, net.minecraft.world.entity.ai.targeting.TargetingConditions targetPredicate) {
            // 同样的友军排除逻辑
            if (target == null) return false;
            if (target instanceof Player && target.getUUID().equals(this.turret.ownerUUID)) return false;
            if (target instanceof SkeletonTurret) return false;
            if (target instanceof IronGolem) return false;
            if (target.getPersistentData().getBoolean("IsFriendlyZombie")) return false;
            if (target.getPersistentData().getBoolean("IsFriendlyCreeper")) return false;

            return super.canAttack(target, targetPredicate);
        }
    }
    // ==========================================
// ==========================================
    // ✅ 修复：拾荒逻辑 (队长不动 + 无限距离)
    // ==========================================
    static class TurretScavengeGoal extends Goal {
        private final SkeletonTurret turret;
        private final double speed;
        private Entity targetItem;

        public TurretScavengeGoal(SkeletonTurret turret, double speed) {
            this.turret = turret;
            this.speed = speed;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // 1. 基础检查
            if (!turret.isFollowing()) return false;
            if (turret.getTarget() != null) return false; // 有怪先打怪

            boolean isCaptain = turret.isCaptain();
            boolean isCommandMode = turret.isCommandScavenging();

            // ✅ 关键修改 1：如果是队长，且处于指令模式 -> 队长无视指令，不动！
            // 这样队长就会被 FollowGoal 接管，乖乖留在主人身边
            if (isCaptain && isCommandMode) return false;

            // ✅ 关键修改 2：设定范围
            double searchRange = 10.0; // 默认普通拾取 (范围小)

            if (isCommandMode && !isCaptain) {
                // 如果是队员收到指令 -> 范围极大 (100格)
                searchRange = 100.0;
            }

            // 搜索物品
            List<Entity> loot = turret.level().getEntities(turret, turret.getBoundingBox().inflate(searchRange), e -> {
                if (e instanceof net.minecraft.world.entity.item.ItemEntity ie) return ie.getItem().isEdible();
                if (e instanceof net.minecraft.world.entity.ExperienceOrb) return true;
                return false;
            });

            if (loot.isEmpty()) return false;

            // 找最近的
            loot.sort(java.util.Comparator.comparingDouble(turret::distanceToSqr));
            this.targetItem = loot.get(0);
            return true;
        }

        @Override
        public void start() {
            turret.getNavigation().moveTo(targetItem, speed);
        }

        @Override
        public boolean canContinueToUse() {
            // ✅ 关键修改 3：移除了所有距离检查 (虚拟围栏)
            // 只要物品还在，就一直追过去，不论多远！
            // 安全性由 SkeletonTurret.tick 里的 "hurtTime" 传送负责
            return targetItem != null && targetItem.isAlive() && !turret.getNavigation().isDone();
        }

        // tick 方法可以删除，因为不再需要计算距离强制停止了
    }    // ==========================================
    // ✅ 新增：战术同步 (蜂群思维 - 攻击队友的目标)
    // ==========================================
    static class TurretPackAttackGoal extends net.minecraft.world.entity.ai.goal.target.TargetGoal {
        private final SkeletonTurret turret;
        private LivingEntity potentialTarget;

        public TurretPackAttackGoal(SkeletonTurret turret) {
            super(turret, false); // false 表示不需要直接视线也能锁定(先锁了再说)
            this.turret = turret;
            this.setFlags(EnumSet.of(Flag.TARGET)); // 这是一个“设定目标”的任务
        }

        @Override
        public boolean canUse() {
            // 1. 如果我已经有目标了，就专心打，不需要抄作业
            if (this.turret.getTarget() != null) return false;

            // 2. 只有“跟随模式”或“防御模式”都生效，这无所谓，只要是炮台就行

            // 3. 搜索周围 20 格内的其他炮台
            // getEntitiesOfClass 用于获取指定类型的实体
            List<SkeletonTurret> allies = this.turret.level().getEntitiesOfClass(
                    SkeletonTurret.class,
                    this.turret.getBoundingBox().inflate(20.0),
                    // 筛选条件：必须是活的，必须有目标，必须是同一个主人的(如果有主人)
                    other -> other != this.turret && other.isAlive() && other.getTarget() != null
            );

            if (allies.isEmpty()) return false;

            // 4. 遍历队友，看看它们在打谁
            for (SkeletonTurret ally : allies) {
                // 确保是自己人 (防止PVP时炮台互殴的逻辑干扰，虽然前面有排除了)
                if (this.turret.ownerUUID != null && ally.ownerUUID != null && !this.turret.ownerUUID.equals(ally.ownerUUID)) {
                    continue; // 不是一家人，不帮忙
                }

                LivingEntity allyTarget = ally.getTarget();

                // 5. 再次确认这个目标是不是合法的敌人 (防止队友发疯打自己人)
                if (this.canAttack(allyTarget, net.minecraft.world.entity.ai.targeting.TargetingConditions.DEFAULT)) {
                    this.potentialTarget = allyTarget;
                    return true; // 找到了！兄弟在打它，我也要打！
                }
            }

            return false;
        }

        @Override
        public void start() {
            // 锁定目标！
            this.turret.setTarget(this.potentialTarget);
            super.start();
        }

        // 再次封装安全检查，确保不会误伤友军
        @Override
        protected boolean canAttack(@Nullable LivingEntity target, net.minecraft.world.entity.ai.targeting.TargetingConditions targetPredicate) {
            if (target == null) return false;
            // 绝对不能攻击的名单：
            if (target instanceof Player && target.getUUID().equals(this.turret.ownerUUID)) return false;
            if (target instanceof SkeletonTurret) return false;
            if (target instanceof IronGolem) return false;
            if (target.getPersistentData().getBoolean("IsFriendlyZombie")) return false;
            if (target.getPersistentData().getBoolean("IsFriendlyCreeper")) return false;

            return super.canAttack(target, targetPredicate);
        }
    }


    // ==========================================
    // ✅ 新增：落水逃生 (遇水先跑，上岸再打)
    // ==========================================
    static class EscapeWaterGoal extends Goal {
        private final SkeletonTurret turret;
        private final double speed;

        public EscapeWaterGoal(SkeletonTurret turret, double speed) {
            this.turret = turret;
            this.speed = speed;
            // 这个任务需要接管移动和跳跃，优先级很高
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            // 1. 只有在水里才触发 (且水深到足以淹没脚踝)
            // isInWater() 是原版判断
            if (!turret.isInWater()) return false;

            // 2. 必须有主人 (往主人那里跑最安全)
            if (turret.ownerUUID == null) return false;
            Player owner = turret.level().getPlayerByUUID(turret.ownerUUID);

            // 如果主人就在附近，就启用这个逃生逻辑
            return owner != null;
        }

        @Override
        public void start() {
            // 告诉炮台：别打架了，先跑路！
            turret.setTarget(null);
        }

        @Override
        public void tick() {
            Player owner = turret.level().getPlayerByUUID(turret.ownerUUID);
            if (owner != null) {
                // 1. 努力往上游 (FloatGoal 会辅助，这里双重保险)
                if (turret.getRandom().nextFloat() < 0.8f) {
                    turret.getJumpControl().jump();
                }

                // 2. 往主人方向游
                turret.getNavigation().moveTo(owner, speed);
            }
        }

        @Override
        public boolean canContinueToUse() {
            // 只要还在水里，就一直跑，直到上岸
            return turret.isInWater();
        }

    }
    // ==========================================
    // ✅ 新增：小队系统支持方法
    // ==========================================

    public void setCaptain(boolean isCaptain) {
        this.entityData.set(IS_CAPTAIN, isCaptain);
        updateCustomName(); // 状态改变时立刻刷新名字
    }

    public boolean isCaptain() {
        return this.entityData.get(IS_CAPTAIN);
    }

    // 计算“战斗力评分”，分数越高越有资格当队长
    public double getSquadScore() {
        // 1. 等级权重最大 (每级 10000 分)
        double score = getTier() * 10000.0;

        // 2. 即将升级的权重第二 (杀敌比例 * 5000 分)
        // 比如杀了 90/100，就是 0.9 * 5000 = 4500 分
        int kills = getKillCount();
        int target = getKillTarget(getTier());
        if (target > 0) {
            score += ((double)kills / target) * 5000.0;
        }

        // 3. 伤害值 (作为辅助参考，虽然跟等级挂钩，但也加上)
        score += this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 100.0;

        // 4. 当前血量 (同等级下，血多的当队长)
        score += this.getHealth();

        return score;
    }
    // ✅ 新增：允许外部查询跟随状态
    public boolean isFollowing() {
        return this.entityData.get(IS_FOLLOWING);
    }
    // ✅ 新增：允许外部修改跟随状态 (解决报错的核心)
    public void setFollowing(boolean isFollowing) {
        this.entityData.set(IS_FOLLOWING, isFollowing);

        // ✅ 新增：如果是切换到 [坚守模式] (false)，立刻强制刹车！
        if (!isFollowing) {
            this.getNavigation().stop(); // 停下脚步
            this.setTarget(null);        // (可选) 停止当前攻击目标，重新索敌
        }
    }

    // ==========================================
    static class TurretRescueGoal extends Goal {
        private final SkeletonTurret turret;
        private final double speed;
        private Player owner;
        private int rescueTime;

        public TurretRescueGoal(SkeletonTurret turret, double speed) {
            this.turret = turret;
            this.speed = speed;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }


        @Override
        public boolean canUse() {
            if (turret.ownerUUID == null) return false;
            this.owner = turret.level().getPlayerByUUID(turret.ownerUUID);
            if (this.owner == null) return false;

            // 只要血量不对，或者处于特殊姿态，就判定为倒地
            boolean isDowned = (this.owner.hasPose(net.minecraft.world.entity.Pose.SWIMMING) && !this.owner.isInWater())
                    || (this.owner.getHealth() <= 4.0f && this.owner.hasPose(net.minecraft.world.entity.Pose.SLEEPING))
                    || this.owner.getHealth() <= 1.0f; // 兼容锁血模组

            return (isDowned || turret.isCommandRescue()) && this.owner.isAlive();
        }

        @Override
        public void start() { this.rescueTime = 0; }


        @Override
        public void stop() {
            turret.setShiftKeyDown(false);
            this.rescueTime = 0;
            turret.setCommandRescue(false);
        }

        private void performRevive() {
            // 1. 物理治疗
            this.owner.setHealth(this.owner.getMaxHealth());
            this.owner.removeAllEffects();
            this.owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2));
            this.owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));

            // 2. 上帝权限指令轰炸 (最高优先级)
            if (turret.level() instanceof ServerLevel sl) {
                String name = this.owner.getGameProfile().getName();

                // 获取服务器控制台权限 (Level 4, bypass everything)
                var consoleSource = sl.getServer().createCommandSourceStack();
                var commands = sl.getServer().getCommands();

                try {
                    // 针对 PlayerRevive / Hardcore Revival
                    commands.performPrefixedCommand(consoleSource, "pr revive " + name);
                    commands.performPrefixedCommand(consoleSource, "playerrevive revive " + name);
                    commands.performPrefixedCommand(consoleSource, "hardcorerevival revive " + name);
                    commands.performPrefixedCommand(consoleSource, "hcr revive " + name);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // 3. 尝试暴力清除 NBT (针对顽固模组)
                try {
                    CompoundTag data = this.owner.getPersistentData();
                    data.remove("PlayerRevive");
                    data.remove("is_downed");
                    data.remove("revive_timer");
                } catch (Exception ignored) {}

                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, owner.getX(), owner.getY() + 1.0, owner.getZ(), 30, 0.5, 0.5, 0.5, 0.5);
                turret.playSound(SoundEvents.TOTEM_USE, 1.0f, 1.0f);
            }

            this.owner.sendSystemMessage(Component.literal("§a[系统] §e" + turret.getDisplayName().getString() + "§a 使用了高阶复苏指令！"));

            this.rescueTime = -100;
            turret.setCommandRescue(false);
        }
    }

    public void setSquadMember(boolean isMember) {
        // ✅ [Fix] 记录卡召唤的实体禁止加入小队
        if (isMember && this.getPersistentData().getBoolean("RecordSummoned")) {
            return;
        }
        this.entityData.set(IS_SQUAD_MEMBER, isMember);
        updateCustomName(); // 状态变了要刷新名字
    }

    public boolean isSquadMember() {
        return this.entityData.get(IS_SQUAD_MEMBER);
    }
    // ==========================================
    // ✅ 新增：坚守模式下的原地巡逻 AI
    // ==========================================
    static class StationaryWanderGoal extends net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal {
        private final SkeletonTurret turret;

        public StationaryWanderGoal(SkeletonTurret turret, double speed) {
            super(turret, speed);
            this.turret = turret;
        }

        // 1. 只有在 [坚守模式] 且 [没有目标] 时才溜达
        @Override
        public boolean canUse() {
            if (this.turret.isFollowing()) return false; // 跟随模式下禁止乱跑
            if (this.turret.getTarget() != null) return false; // 有敌人先打敌人
            if (this.turret.isCommandScavenging()) return false; // 捡垃圾时别乱跑

            return super.canUse();
        }

        // 2. 限制移动范围 (3格)
        @Override
        @Nullable
        protected net.minecraft.world.phys.Vec3 getPosition() {
            // 在当前位置 (this.mob) 周围找一个点
            // 参数：水平范围 3，垂直范围 2
            return net.minecraft.world.entity.ai.util.DefaultRandomPos.getPos(this.mob, 3, 2);
        }
    }
    // ==========================================
    // ✅ 新增：清剿模式移动 AI (已优化：150格 + 屏蔽干扰)
    // ==========================================
    // ==========================================
    // ✅ 新增：清剿模式移动 AI (已修复：分段导航 + 150格)
    // ==========================================
// ==========================================
    // ✅ 新增：清剿模式 AI (猎杀版：主动索敌 + 150格边界)
    // ==========================================
    static class PurgeMoveGoal extends Goal {
        private final SkeletonTurret turret;

        public PurgeMoveGoal(SkeletonTurret turret) {
            this.turret = turret;
            this.setFlags(EnumSet.of(Flag.MOVE)); // 接管移动
        }

        @Override
        public boolean canUse() {
            // 只要开了模式，就必须干活
            return turret.isPurgeActive();
        }

        @Override
        public boolean canContinueToUse() {
            return turret.isPurgeActive();
        }

        @Override
        public void tick() {
            LivingEntity owner = turret.getOwner();
            if (owner == null) {
                turret.stopPurgeMode();
                return;
            }

            // 1. 检查最大活动范围 (150格 = 22500)
            // 如果离主人太远了，哪怕前面有怪也不能追了，必须回来
            if (turret.distanceToSqr(owner) > 22500.0) {
                turret.teleportToSafeSpot(owner);
                turret.stopPurgeMode();
                return;
            }

            // 2. 如果已经锁定了攻击目标，就交给战斗 AI 处理
            if (turret.getTarget() != null && turret.getTarget().isAlive()) {
                return;
            }

// ==================== ⚔ 猎杀雷达 (已修复) ====================

            // ✅ 第一步：先算出我要扫描多远 (提出来写)
            double scanRange = Math.max(32.0, turret.getAttackRange());

            // ✅ 第二步：再把算出来的距离放进去用
            List<LivingEntity> enemies = turret.level().getEntitiesOfClass(LivingEntity.class,
                    turret.getBoundingBox().inflate(scanRange),
                    e -> isValidTarget(e)
            );

            // 如果发现了敌人
            if (!enemies.isEmpty()) {
                // 找最近的一个
                enemies.sort(Comparator.comparingDouble(turret::distanceToSqr));
                LivingEntity prey = enemies.get(0);

                // 冲过去！(速度 1.4，比平时快)
                turret.getNavigation().moveTo(prey, 1.4);
                return;
            }
            // ====================================================================

            // 3. 附近没怪了？继续执行地毯式搜索 (往 150 格边界走)
            if (turret.getNavigation().isDone()) {
                double rad = Math.toRadians(turret.purgeSearchAngle); // 之前分配的角度

                // 计算 150 格远处的终点
                double finalX = owner.getX() + Math.cos(rad) * 150.0;
                double finalZ = owner.getZ() + Math.sin(rad) * 150.0;

                // 计算我现在离终点还有多远
                double dx = finalX - turret.getX();
                double dz = finalZ - turret.getZ();
                double distToFinal = Math.sqrt(dx * dx + dz * dz);

                // 每次只往前推进 16 格 (分段导航，防止寻路失败)
                double step = Math.min(distToFinal, 16.0);
                double nextX = turret.getX() + (dx / distToFinal) * step;
                double nextZ = turret.getZ() + (dz / distToFinal) * step;

                turret.getNavigation().moveTo(nextX, owner.getY(), nextZ, 1.3);
            }
        }

        // 🛡️ 敌我识别过滤器 (把朋友排除掉)
        private boolean isValidTarget(LivingEntity e) {
            if (e == turret) return false; // 别打自己
            if (!e.isAlive()) return false; // 别鞭尸
            if (e instanceof Player) return false; // 别打人
            if (e instanceof SkeletonTurret) return false; // 别打队友
            if (e instanceof net.minecraft.world.entity.decoration.ArmorStand) return false; // 别打架子

            // 别打我们认证过的友军僵尸/苦力怕
            if (e.getPersistentData().getBoolean("IsFriendlyZombie")) return false;
            if (e.getPersistentData().getBoolean("IsFriendlyCreeper")) return false;

            // 其他所有能动的东西 (僵尸、骷髅、猪、羊、村民...)，全部视为猎物！
            return true;
        }

    }

    // ==========================================
    // ✅ 新增：战术拉扯 AI (拒绝贴脸，保持 3.5 格距离)
    // ==========================================
    static class KeepDistanceGoal extends Goal {
        private final SkeletonTurret turret;
        private final double speed;
        private final float range; // 警戒距离 (3.5)
        private LivingEntity toAvoid;

        public KeepDistanceGoal(SkeletonTurret turret, double speed, float range) {
            this.turret = turret;
            this.speed = speed;
            this.range = range;
            this.setFlags(EnumSet.of(Flag.MOVE)); // 接管移动控制权
        }

        @Override
        public boolean canUse() {
            // 🛑 1. 坚守模式 (Guard Mode) 检查
            // 如果不是跟随状态 (即坚守)，绝对不动！死守原地！
            if (!turret.isFollowing()) return false;

            // 🛑 2. 如果正在被玩家强制救援，也不要乱跑
            if (turret.isCommandRescue()) return false;

            // 3. 扫描周围 (range) 范围内的怪物
            List<Monster> enemies = turret.level().getEntitiesOfClass(Monster.class,
                    turret.getBoundingBox().inflate(range, 2.0, range),
                    e -> e != turret && e.isAlive() && !isFriendly(e)
            );

            if (enemies.isEmpty()) return false;

            // 4. 找到最近的一个，确立为躲避目标
            // (简单的排序，找最近的)
            enemies.sort(Comparator.comparingDouble(turret::distanceToSqr));
            this.toAvoid = enemies.get(0);

            return true;
        }

        @Override
        public void start() {
            if (this.toAvoid == null) return;

            // 5. 计算撤退路径 (向反方向跑 6 格)
            net.minecraft.world.phys.Vec3 awayPos = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPosAway(turret, 6, 4, this.toAvoid.position());

            if (awayPos != null) {
                // 速度 1.3 (稍微快一点，确保存活)
                turret.getNavigation().moveTo(awayPos.x, awayPos.y, awayPos.z, speed);
            }
        }

        @Override
        public boolean canContinueToUse() {
            // 只要没跑到终点，且还在跟随模式，就继续跑
            return !turret.getNavigation().isDone() && turret.isFollowing();
        }

        // 辅助：判断是否为友军 (别躲开队友或召唤物)
        private boolean isFriendly(Entity e) {
            if (e instanceof SkeletonTurret) return true;
            if (e instanceof IronGolem) return true;
            if (e.getPersistentData().getBoolean("IsFriendlyZombie")) return true;
            if (e.getPersistentData().getBoolean("IsFriendlyCreeper")) return true;
            return false;
        }

    }
    // ==========================================
    // 🛡️ 冗余设计：排斥力场白名单
    // ==========================================
    private boolean isImmuneToPush(LivingEntity e) {
        // 1. 绝对排除：我自己
        if (e == this) return true;

        // 2. 绝对排除：玩家 (哪怕是敌人也不要乱推，体验不好，除非你想做PVP)
        if (e instanceof Player) return true;

        // 3. 绝对排除：同类 (所有炮台)
        if (e instanceof SkeletonTurret) return true;

        // 4. 【针对你的需求】：排除所有骷髅家族成员
        // AbstractSkeleton 包含了：普通骷髅、流浪者、凋灵骷髅
        // ✅ 只要你未来的“近战骷髅”继承自 Skeleton 或 AbstractSkeleton，这里自动生效！
        if (e instanceof net.minecraft.world.entity.monster.AbstractSkeleton) return true;

        // 5. 排除铁傀儡和其他已知友军 (之前逻辑里的)
        if (e instanceof net.minecraft.world.entity.animal.IronGolem) return true;
        if (e.getPersistentData().getBoolean("IsFriendlyZombie")) return true;
        if (e.getPersistentData().getBoolean("IsFriendlyCreeper")) return true;

        // 6. 【冗余接口 - NBT标签】：终极扩展方案
        // 如果你以后做了一个“地狱火恶魔”，它不是骷髅类，但你也不想推它
        // 只需要在那只怪生成时写一句：entity.getPersistentData().putBoolean("TurretAlly", true);
        if (e.getPersistentData().getBoolean("TurretAlly")) return true;

        // 7. 【冗余接口 - 骑乘判断】
        // 如果这个怪骑着我，或者我骑着它，别推
        if (this.hasPassenger(e) || e.hasPassenger(this)) return true;

        // 如果以上都不是，那就是可以推开的杂鱼
        return false;
    }
    // (registerControllers removed)



    // --- GUI 数据接口 ---

    public int getLevel() {
        return getTier() + 1;
    }

    public int getXp() {
        return this.entityData.get(DATA_XP);
    }

    // 如果你需要设置 XP 的方法：
    public void setXp(int amount) {
        this.entityData.set(DATA_XP, amount);
    }



    // 3. 获取射击延迟 (Tick) - 核心算法
    public float getFireDelay() {
        int tier = getTier();
        
        // 基础冷却: 随着等级提升而降低 (20 -> 17 -> 14 -> 11 -> 8 -> 5)
        double cooldown = Math.max(5.0, 20.0 - (tier * 3.0));

        // 攻速叠加层数 (0.075 -> 7.5% per stack)
        // 满级 120层 -> +900% (10倍速)
        double stackMultiplier = 1.0 + (this.entityData.get(DATA_HEAT) * 0.075);
        cooldown /= stackMultiplier;

        // 狂暴模式 4倍速
        if (this.entityData.get(IS_BRUTAL)) {
            cooldown /= 4.0;
        }

        // ✅ 应用攻速属性加成 (Attribute Modifier)
        double attrSpeed = this.getAttributeValue(Attributes.ATTACK_SPEED);
        // 如果攻速属性 > 1.0 (比如有加速buff)，则冷却时间缩短
        if (attrSpeed > 0) {
            cooldown /= attrSpeed;
        }

        return (float) Math.max(1.0, cooldown);
    }

    // ==========================================
    // 🧠 自定义跟随 AI (适配 Skeleton)
    // ==========================================
    // (已移除重复且错误的 Goal 代码块)



    

    // (Method removed)





}



