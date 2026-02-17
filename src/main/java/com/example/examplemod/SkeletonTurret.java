package com.example.examplemod;


// 📋 请检查并添加这些导包
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.FishingRodItem;
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



    // �?新增：智能止损变�?
    // 记录上一次所在的区块位置
    // 语音冷却记录
    private net.minecraft.world.level.ChunkPos keptChunkPos;
    private double spawnX, spawnY, spawnZ;
    private double guardLockX, guardLockY, guardLockZ;
    private boolean guardLockValid = false;
    private boolean terminalTeleportOverride = false;
    private long lastHeatStackTime = 0;
    private int consecutiveMisses = 0;   // 连续未造成伤害的次�?
    private int blockedSightTime = 0;    // 视线被遮挡的时间 (tick)
    private long lastDamageTimestamp = 0; // 上次造成伤害的时间戳 (用于辅助判断)

    // 🔍 1. 定义跟随模式的数据ID (放在类定义的最上面)
    private static final EntityDataAccessor<Boolean> FOLLOW_MODE = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.BOOLEAN);
    // �?新增：状态同�?(用于 HUD 显示)
    private static final EntityDataAccessor<Boolean> IS_PURGE_ACTIVE = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_SCAVENGING = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.BOOLEAN);
    // �?新增：身份编�?(001-999)
    public static final EntityDataAccessor<Integer> UNIT_ID = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DEATH_PLAQUE_FATAL_HIT_COUNT = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.INT);
    // RANGE_LEVEL removed - derived from TIER

    // ==================== 🗣�?头顶显示系统数据 ====================
    // 1. 台词内容 (空字符串代表没说�?
    // 2. 台词剩余显示时间 (Tick)
    // 3. 状态栏内容 (用于显示 �?25s 自毁 / 🎒 背包已满 �?
    private static final EntityDataAccessor<String> DATA_STATUS_OVERLAY = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.STRING);
    // �?新增：把热度变成同步数据，这�?UI 才能实时看到它跳动！
    private static final EntityDataAccessor<Integer> DATA_HEAT = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> IS_FOLLOWING = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> KILL_COUNT = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_BRUTAL = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> UPGRADE_PROGRESS = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.INT);
    // �?新增：队长标�?
    private static final EntityDataAccessor<Boolean> IS_CAPTAIN = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_SQUAD_MEMBER = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.BOOLEAN);
    // �?新增：同步的基础名字 (解决改名后变回原样的问题)
    // �?只保留这一个！这是我们唯一要用的“真名字�?
    private static final EntityDataAccessor<String> SYNC_BASE_NAME = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.STRING);
    public static final String DEFAULT_BASE_NAME_TOKEN = "__default_vanguard__";
    private static final String PLAYER_NAME_LOCK_TAG = "PlayerNameLocked";
    public static final int MAX_BASE_NAME_LENGTH = 14;
    // �?新增：同步的主人UUID (解决客户端无法获取主人信息的问题)



    // �?新增：主人身份同步通道 (解决 HUD 不显示的核心)
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID_SYNC = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.OPTIONAL_UUID);

    // DATA_LEVEL removed - derived from TIER

    private static final EntityDataAccessor<Integer> DATA_XP = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.INT);

    // 注意：fireDelay 如果是逻辑变量，不需要同步，只需公开访问
    private int decayTimer = 0;
    private int eatCooldown = 0;

    // �?新增：传送后攻击延迟和无敌时�?
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

        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, amplifier, false, false, false)); // 5 seconds duration, no status particles

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

    // �?新增：强制救援模式开�?
    private boolean isCommandRescue = false;

    public void setCommandRescue(boolean rescue) {
        this.isCommandRescue = rescue;
    }

    public boolean isCommandRescue() {
        return this.isCommandRescue;
    }

    // �?新增：狂暴技能的计时�?
    private int brutalityActiveTimer = 0;
    private int brutalityCooldown = 0;

    private UUID ownerUUID;
    // �?新增：记录入队时�?(用于排序：谁先来谁在上面)
    private long squadJoinTime = 0;
    // �?新增：传送模块状�?
    private static final EntityDataAccessor<Boolean> HAS_TELEPORT_MODULE = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.BOOLEAN);
    // �?新增：传送冷�?(Tick)
    private int teleportCooldown = 0;
    public int getTeleportCooldown() { return this.teleportCooldown; } // Added getter
    private int teleportModuleLevel = 0;
    private int multiShotLevel = 0;
    private int blackHoleCooldown = 0;
    private int blackHoleActiveTicks = 0;
    private net.minecraft.world.phys.Vec3 blackHoleCenter = net.minecraft.world.phys.Vec3.ZERO;
    private boolean deathRecordDropped = false;

    public final SimpleContainer inventory = new SimpleContainer(45);

    public SkeletonTurret(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
        // �?监听背包变化，检测传送模�?
        this.inventory.addListener(new ContainerListener() {
            @Override
            public void containerChanged(Container container) {
                checkTeleportModule();
            }
        });
    }

    private void checkTeleportModule() {
        if (this.level().isClientSide) return;
        int bestTeleportLevel = 0;
        int bestMultiShotLevel = 0;
        // 检查升级槽�?(5-9)
        for (int i = 5; i < 10; i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (stack.getItem() instanceof GenericTurretModuleItem item) {
                String moduleId = item.getModuleId(stack);
                int level = item.getModuleLevel(stack);
                if ("teleport".equals(moduleId)) {
                    bestTeleportLevel = Math.max(bestTeleportLevel, level);
                } else if ("multi_shot".equals(moduleId)) {
                    bestMultiShotLevel = Math.max(bestMultiShotLevel, level);
                }
            } else if (stack.getItem() == ExampleMod.TELEPORT_UPGRADE_MODULE.get()) {
                bestTeleportLevel = Math.max(bestTeleportLevel, TeleportUpgradeItem.getLevel(stack));
            } else if (stack.getItem() == ExampleMod.MULTI_SHOT_UPGRADE_MODULE.get()) {
                bestMultiShotLevel = Math.max(bestMultiShotLevel, MultiShotUpgradeModuleItem.getLevel(stack));
            }
        }

        this.teleportModuleLevel = bestTeleportLevel;
        this.multiShotLevel = bestMultiShotLevel;
        boolean hasModule = bestTeleportLevel > 0;
        boolean current = this.hasTeleportModule();
        if (hasModule != current) {
            this.setHasTeleportModule(hasModule);
            // 播放音效 (仅在安装�?
            if (hasModule) {
                this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        }
    }

    private int findFirstEmptyModuleSlot() {
        for (int i = 5; i < 10; i++) {
            if (this.inventory.getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    // ==========================================
    // 🖨�?3D 打印核心数据 (Phase 1)
    // ==========================================
    // 打印进度�?.0 (�? -> 1.0 (完成)
    private static final EntityDataAccessor<Float> PRINT_PROGRESS = SynchedEntityData.defineId(SkeletonTurret.class, EntityDataSerializers.FLOAT);

    // 打印状态机�?=正常, 1=打印�? 2=蓝屏死机, 3=逆向回收
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
        this.entityData.define(DEATH_PLAQUE_FATAL_HIT_COUNT, 0);
        this.entityData.define(SYNC_BASE_NAME, DEFAULT_BASE_NAME_TOKEN);
        this.entityData.define(PRINT_PROGRESS, 0.0f);
        this.entityData.define(PRINT_STATE, 0);
        this.entityData.define(DATA_HEAT, 0);
        this.entityData.define(DATA_STATUS_OVERLAY, "");
        // DATA_LEVEL removed
        this.entityData.define(DATA_XP, 0);
        this.entityData.define(HAS_TELEPORT_MODULE, false);

    }



    // �?新增：强制拾荒模式状�?

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
        // Follow mode is authoritative; keep AI/state/UI flags fully synchronized.
        boolean wasFollowing = this.entityData.get(FOLLOW_MODE);
        boolean changed = this.entityData.get(FOLLOW_MODE) != shouldFollow
                || this.entityData.get(IS_FOLLOWING) != shouldFollow;
        this.entityData.set(FOLLOW_MODE, shouldFollow);
        this.entityData.set(IS_FOLLOWING, shouldFollow);

        // Guard mode should clear movement/target immediately to prevent stale behavior.
        if (!shouldFollow) {
            this.guardLockX = this.getX();
            this.guardLockY = this.getY();
            this.guardLockZ = this.getZ();
            this.guardLockValid = true;
            enforceGuardFreeze();
        } else if (!wasFollowing) {
            this.getNavigation().stop();
            this.setTarget(null);
            this.setDeltaMovement(0.0, 0.0, 0.0);
            this.hurtMarked = true;
        }

        // Overhead squad badge visibility is tied to follow state.
        if (changed && !this.level().isClientSide) {
            updateCustomName();
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ATTACK_SPEED, 1.0) // �?新增：基础攻速属�?(默认�?.0，即正常倍率)
                .add(Attributes.FOLLOW_RANGE, 256.0); // �?新增：把导航视野扩大�?256 格！
    }


    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        this.updateStatsAndEquip();
        this.checkTeleportModule();
        // 👇 设为打印状态，进度归零
        setPrintState(1);
        this.entityData.set(PRINT_PROGRESS, 0.0f);
        this.spawnX = this.getX();
        this.spawnY = this.getY();
        this.spawnZ = this.getZ();
        return result;
    }
    // 重写远程攻击方法，记录射击次�?
    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        // �?删除这行：super.performRangedAttack(target, distanceFactor);
        // 原版方法射出的是没有 NBT 标签的普通箭，导致友军伤害判断失效！

        // �?改为调用我们自定义的射击方法 (�?NBT 标签、带特效、带等级)
        this.shootLinearArrow(target, getTier());

        // 记录失误次数 (保持你之前的逻辑)
        this.consecutiveMisses++;
        if (!this.getSensing().hasLineOfSight(target)) {
            this.consecutiveMisses++;
        }
    }

    public void shootModuleArrow(LivingEntity target, int tier, float speedMultiplier, double damageMultiplier, boolean bypassInvulnerability) {
        // Keep compatibility with module hooks while reusing existing turret projectile behavior.
        this.shootLinearArrow(target, tier);
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

        // �?[修改] 获取当前热度 (即攻速层�? -> 叠加 -> 保存
        int currentHeat = this.entityData.get(DATA_HEAT);
        // 攻速叠加逻辑�?
        // Tier 0: 0% Boost (Max 0)
        // Tier 1: 225% Boost (Max 30)
        // ...
        // Tier 4: 900% Boost (Max 120) -> Total 1000%
        int maxHeat = getTier() * 30;

        // 每次命中叠加 1 �?(clamp 至上�?
        // �?[修正] 攻速叠加间隔控�?(<= 50ms)
        if (currentHeat < maxHeat) {
            long now = System.currentTimeMillis();
            if (now - this.lastHeatStackTime >= 50) {
                this.entityData.set(DATA_HEAT, currentHeat + 1);
                currentHeat++;
                this.lastHeatStackTime = now;
            }
        }

        // 记录最后一次射击时�?(用于脱战衰减)
        this.lastDamageTimestamp = this.tickCount;


        double d0 = target.getX() - this.getX();
        double d1 = target.getEyeY() - arrow.getY();
        double d2 = target.getZ() - this.getZ();

        double spread = 0.2;
        double rX = (this.random.nextDouble() - 0.5) * spread;
        double rY = (this.random.nextDouble() - 0.5) * spread;
        double rZ = (this.random.nextDouble() - 0.5) * spread;

        // �?根据射程等级调整箭矢速度 (v^2 正比于射�?
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

        // �?[修改] 移除热度伤害加成 (改用攻速流)
        // 伤害公式：基础4 + 等级*5
        double dmg = (4.0 + (tier * 5.0));

        arrow.setBaseDamage(Math.min(dmg, 200.0));
        int pierce = (tier == 5) ? 10 : (tier + 1);
        arrow.setPierceLevel((byte) pierce);

        // �?[修改] 音调随热度变�?(听觉反馈)
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

        // 脱战判定�?�?(100 tick) 无射�?
        long timeSinceLast = this.tickCount - this.lastDamageTimestamp;

        if (timeSinceLast > 100 && currentHeat > 0) {
            // 每秒 (20 tick) 衰减 20%
            if (this.tickCount % 20 == 0) {
                int decay = Math.max(1, (int)(currentHeat * 0.2)); // 至少�?
                this.entityData.set(DATA_HEAT, Math.max(0, currentHeat - decay));
            }
        }
    }

    // �?新增：狂暴技能管理系�?(5秒爆�?+ 动态CD)
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
            // [状�?A]: 正在狂暴 (5�?
            brutalityActiveTimer--;
            if (brutalityActiveTimer <= 0) {
                this.entityData.set(IS_BRUTAL, false);

                // 冷却：基础25�?- 每级�?�?
                int reduceSeconds = this.getTier() * 3;
                int cdSeconds = Math.max(5, 25 - reduceSeconds);
                brutalityCooldown = cdSeconds * 20;

                this.playSound(SoundEvents.LAVA_EXTINGUISH, 1.0f, 0.5f);
            }
        } else {
            // [状�?B]: 等待冷却
            if (brutalityCooldown > 0) {
                brutalityCooldown--;
            } else {
                // 开启狂�?
                this.entityData.set(IS_BRUTAL, true);
                brutalityActiveTimer = 100; // 5�?

                this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 0.5f);
                if (this.level() instanceof ServerLevel sl) {
                    sl.players().forEach(p -> {
                        if (p.distanceToSqr(this) < 256) {
                            p.displayClientMessage(Component.literal("§4�?炮台进入狂暴模式�?5s)"), true);
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
        // 必须安装模块且冷却完�?
        return this.hasTeleportModule() && teleportCooldown <= 0;
    }
    public void setTeleportCooldown(int ticks) { this.teleportCooldown = ticks; }

    private void tickTeleportCooldown() {
        if (teleportCooldown > 0) teleportCooldown--;
        if (blackHoleCooldown > 0) blackHoleCooldown--;
    }

    // 获取当前等级对应的传送冷�?(Tick)
    // Configurable via TurretConfig
    public int getMaxTeleportCooldown() {
        int tier = getTier();
        int base = TurretConfig.COMMON.teleportCooldownBase.get();
        int reduction = TurretConfig.COMMON.teleportCooldownReductionPerTier.get();
        int min = TurretConfig.COMMON.teleportCooldownMin.get();
        return Math.max(min, base - (tier * reduction));
    }

    private void tickBlackHoleEffect() {
        if (this.level().isClientSide || this.blackHoleActiveTicks <= 0) {
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int level = this.teleportModuleLevel;
        if (level < 3) {
            this.blackHoleActiveTicks = 0;
            return;
        }

        this.blackHoleActiveTicks--;
        double range = TeleportModuleRules.blackHoleRangeForLevel(level) * TurretConfig.COMMON.blackHoleRangeScale.get();
        if (range <= 0.0) {
            return;
        }

        final double rangeSqr = range * range;
        int scanCap = Math.max(8, TurretConfig.COMMON.blackHoleEntityScanCap.get());
        var area = new net.minecraft.world.phys.AABB(
                this.blackHoleCenter.x - range, this.blackHoleCenter.y - range, this.blackHoleCenter.z - range,
                this.blackHoleCenter.x + range, this.blackHoleCenter.y + range, this.blackHoleCenter.z + range
        );

        java.util.List<LivingEntity> targets = this.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> isValidBlackHoleTarget(e, rangeSqr)
        );
        targets.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(this.blackHoleCenter)));
        if (targets.size() > scanCap) {
            targets = targets.subList(0, scanCap);
        }

        for (LivingEntity target : targets) {
            double dx = this.blackHoleCenter.x - target.getX();
            double dy = this.blackHoleCenter.y - target.getY();
            double dz = this.blackHoleCenter.z - target.getZ();
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0e-4) {
                continue;
            }
            double pull = 0.28 + Math.min(0.30, range / 120.0);
            target.setDeltaMovement(
                    target.getDeltaMovement().add(dx / len * pull, Math.min(0.18, dy / len * pull + 0.04), dz / len * pull)
            );
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 1, false, false, true));
        }

        // Strong visual cue while black-hole pull is active.
        serverLevel.sendParticles(ParticleTypes.PORTAL, this.blackHoleCenter.x, this.blackHoleCenter.y + 0.25, this.blackHoleCenter.z,
                36, range * 0.22, 0.35, range * 0.22, 0.05);
        serverLevel.sendParticles(ParticleTypes.ENCHANT, this.blackHoleCenter.x, this.blackHoleCenter.y + 0.15, this.blackHoleCenter.z,
                24, range * 0.20, 0.28, range * 0.20, 0.02);
        serverLevel.sendParticles(ParticleTypes.SMOKE, this.blackHoleCenter.x, this.blackHoleCenter.y + 0.15, this.blackHoleCenter.z,
                18, range * 0.18, 0.20, range * 0.18, 0.01);
    }

    private boolean isValidBlackHoleTarget(LivingEntity target, double rangeSqr) {
        if (target == null || !target.isAlive()) return false;
        if (target == this || target instanceof Player || target instanceof SkeletonTurret) return false;
        if (target.getPersistentData().getBoolean("IsFriendlyZombie")) return false;
        if (target.getPersistentData().getBoolean("IsFriendlyCreeper")) return false;
        return target.distanceToSqr(this.blackHoleCenter) <= rangeSqr;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isDeadOrDying()) {
            return;
        }

        if (!this.level().isClientSide && !this.isFollowing()) {
            enforceGuardFreeze();
        }

        tickTeleportCooldown();
        tickBlackHoleEffect();

        // �?[Fix] 记录卡召唤实体的物理与状态修�?
        if (!this.level().isClientSide && this.getPersistentData().getBoolean("RecordSummoned")) {
            // 1. 物理修正 (仅在非乘骑、非水下、非飞行�?
            if (!this.isInWater() && !this.isPassenger() && !this.isNoGravity()) {
                // 检测悬�?
                if (!this.onGround()) {
                    // 施加额外重力 (防止漂浮)
                    this.setDeltaMovement(this.getDeltaMovement().add(0, -0.08, 0));

                    // 严重偏移检�?(与下方方块距�?
                    // 只在非上升状态下修正 (防止打断跳跃)
                    if (this.getDeltaMovement().y <= 0.01) {
                        BlockPos pos = this.blockPosition();
                        int groundY = pos.getY();
                        boolean foundGround = false;

                        // 向下探测 5 �?
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
                            // 如果悬空高度�?0.5 �?3.0 之间，且不是在跳跃，强制吸附
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

        // �?更新计时�?
        if (this.invincibilityTimer > 0) this.invincibilityTimer--;
        if (this.postTeleportAttackDelay > 0) this.postTeleportAttackDelay--;



        // �?确保每一帧都检查热度衰�?
        manageHeatDecay();

        // 检查并初始化编�?
        if (!this.level().isClientSide && this.entityData.get(UNIT_ID) == 0) {
            this.entityData.set(UNIT_ID, this.random.nextInt(999) + 1);
            updateCustomName(); // 生成后立刻刷新名�?
        }
        // ==================== 📡 数据同步补丁 ====================
        if (!this.level().isClientSide) {// ==================== 💳 方案一：身份卡系统 (Slot 25) ====================
            // 每秒检查一�?(20 tick)
            if (this.tickCount % 20 == 0) {
                // 获取�?25 格的物品 (倒数第二格，因为 26 是属性书)
                ItemStack idCard = this.inventory.getItem(39);

                // 情况 A: 插槽里有带名字的物品 (命名牌、纸、剑...都可�?
                if (!idCard.isEmpty() && idCard.hasCustomHoverName()) {
                    String cardName = idCard.getHoverName().getString();
                    applyBaseNameFromIdCard(cardName);
                }
                // 情况 B: 插槽是空�?(或者物品没名字) -> 恢复默认
                else {
                    restoreDefaultBaseNameFromIdCardRule();
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

        if (!this.level().isClientSide) {
            // ==================== 🔧 核心：濒死倒计时逻辑 ====================

            // ===============================================================

            // ==================== 🛡�?智能战斗监控 ====================
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

                    // �?修复：必须是 [跟随模式] �?[不在坚守] 时，才允许跑向主人！
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

            // ==================== 🚑 紧急回�?====================
            if (this.isCommandScavenging() && this.ownerUUID != null) {
                Player owner = this.level().getPlayerByUUID(this.ownerUUID);
                if (owner != null && owner.hurtTime > 0) {
                    this.setCommandScavenging(false);
                    this.teleportToSafeSpot(owner, true);
                    this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);
                }
            }

            // ==================== 🧩 日常功能 ====================
            autoEat();
            if (this.tickCount % 20 == 0) updateInfoBookAndSlots();
            // --- 🗣�?语音系统挂载: 闲聊 & 状�?---

            // 1. 闲聊 (�?0秒尝试一�?
            if (this.tickCount % 200 == 0) {
}

            // 2. 低血量检�?(每秒检�?
            if (this.tickCount % 20 == 0) {
                float hp = this.getHealth();
                float max = this.getMaxHealth();
                if (hp < max * 0.2f) {
} else if (hp < max * 0.5f) {
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
            // 如果正在打印或回收，禁止执行后续的打�?移动 AI
            if (getPrintState() != 0) {
                // 🛑 绝对锚定：X, Y, Z 速度全部归零�?
                // 之前�?this.getDeltaMovement().y，这会导致它能被推上天�?
                // 现在改成 0，它就像钉在地上一样�?
                this.setDeltaMovement(0, 0, 0);

                // 额外保险：强制重置位置到生成�?(防止被挤�?
                // (spawnX, spawnY, spawnZ 是我们在 finalizeSpawn 里记录的)
                if (this.spawnY != 0) { // 确保 spawnY 已被赋�?
                    // 只锁 Y 轴，允许水平微量挤压，或者全�?
                    this.setPos(this.getX(), this.spawnY, this.getZ());
                }
                return;
            }

            this.updateOverheadStatus();

        } // ⬅️ 结束 if (!isClientSide)
    } // 🟢 结束 tick() 方法


    private void updateOverheadStatus() {
        String status = "";

        // �?1. 动态呼吸点算法 (�?0.5�?变一�?
        // 这里的逻辑是：用总时间除�?10，然后对 4 取余数，得到 0, 1, 2, 3 循环
        int step = (this.tickCount / 10) % 4;
        String dots = switch (step) {
            case 0 -> ".";
            case 1 -> "..";
            case 2 -> "...";
            default -> ""; // �?4 拍留空，产生闪烁�?
        };
        int dotsCount = dots.length();

        // ==========================================
        // ⬇️ 状态判断逻辑 ⬇️
        // ==========================================

        // 优先�?2: 狂暴倒计�?
        if (this.entityData.get(IS_BRUTAL)) {
            int sec = this.brutalityActiveTimer / 20;
            status = "status.brutal:" + sec;
        }
        // 优先�?3: 打印/回收�?
        else if (getPrintState() != 0) {
            int percent = (int)(getPrintProgress() * 100);
            // 既然也是进行中，我们顺手也加上点，看着更舒服！
            status = (getPrintState() == 3)
                    ? "status.recycle:" + percent + ":" + dotsCount
                    : "status.build:" + percent + ":" + dotsCount;
        }
        // 优先�?4: 背包已满 (当处于拾荒模式时)
        else if (this.isCommandScavenging() && isInventoryFull()) {
            status = "status.inventory_full:" + this.entityData.get(UNIT_ID);
        }
        // 优先�?4.5: 空间不足 (<10%)
        else if (this.isCommandScavenging() && getFreeSlotCount() < 5) {
            status = "status.low_space:" + this.entityData.get(UNIT_ID);
        }
        // 优先�?5: 拾荒�?(�?应用动画)
        else if (this.isCommandScavenging()) {
            status = "status.scavenge:" + dotsCount;
        }
        // 优先�?6: 清剿�?(�?应用动画)
        else if (this.isPurgeActive()) {
            // 加上杀敌数统计，配合呼吸点，更有战术感
            status = "status.purge:" + this.purgeKillCount + ":" + dotsCount;
        }

        // 更新数据 (只有变化时才发包，节省流�?
        if (!status.equals(this.entityData.get(DATA_STATUS_OVERLAY))) {
            this.entityData.set(DATA_STATUS_OVERLAY, status);
        }
    }

    // 辅助：检查背包是否满�?(只检查储物格 12-26)
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
        // 简单排序：将高价值物品移到前�?(12-36)
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

    // �?新增：供外部调用的“说话”接�?

    // Getter 供渲染器使用
    public String getOverheadStatus() { return this.entityData.get(DATA_STATUS_OVERLAY); }





    // ==========================================
    // 🖨�?3D 打印逻辑核心
    // ==========================================
    private int summonRetryCount = 0;

    private void tickPrintLogic() {
        int state = this.entityData.get(PRINT_STATE);
        float progress = this.entityData.get(PRINT_PROGRESS);

        // [状�?0] 正常状态：什么都不做
        if (state == 0) return;

        // [状�?1] 正在打印 (Printing)
        if (state == 1) {
            // 1. 进度增加 (速度：大�?5 秒完�?
            // 如果你想快点，把 0.01f 改大，比�?0.02f
            if (progress > 0.0f && progress < 0.02f) {
                // 音量 1.0, 音调 1.0
                this.playSound(ModSounds.PRINT_LOOP.get(), 0.5f, 1.0f);
            }

            progress += 0.01f;

            // 2. 蓝屏判定 (BSOD) - 已移�?
            // if (!this.level().isClientSide && progress > 0.6f && progress < 0.8f) { ... }

            // 3. 环境互动：烧灼地�?(粒子特效)
            if (this.level().isClientSide) {
                // 在当前打印高度生成火�?
                double y = this.getY() + (this.getBbHeight() * progress);
                if (this.random.nextFloat() < 0.3f) {
                    this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0, 0.05, 0);
                    this.level().addParticle(ParticleTypes.FLAME, this.getX() + (random.nextDouble()-0.5), this.getY(), this.getZ() + (random.nextDouble()-0.5), 0, 0.01, 0);
                }
// ... (上面的代码不�?

// ... (tickPrintLogic 方法内部) ...

            } else {
                // 服务端：气浪排斥逻辑 (已升�?
                if (progress > 0.1f) {
                    // 范围：以自身为中心，向外�?1.5 �?(稍微大一点点)
                    List<LivingEntity> pushTargets = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(1.5));

                    for (LivingEntity e : pushTargets) {
                        // 🛑 调用刚才写的通用过滤�?
                        if (isImmuneToPush(e)) {
                            continue; // 是自己人/骷髅/特定单位，跳过，不推�?
                        }

                        // 对杂鱼执行推开操作
                        // 稍微减小一点力�?(0.1 -> 0.08)，防止把苦力怕推到玩家脸�?
                        if (!e.isShiftKeyDown()) {
                            e.push(0, 0.08, 0);
                        }
                    }
                }
            }

            // ... (tickPrintLogic 方法后续) ...

            // ... (下面的代码不�?

            // 4. 完成判定
            if (progress >= 1.0f) {
                progress = 1.0f;
                setPrintState(0); // 切换回正常状�?
                // 播放完成音效
                this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                // 震开周围 (冲击�?
                if (!this.level().isClientSide) {
                    ((ServerLevel)this.level()).sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY()+1, this.getZ(), 1, 0, 0, 0, 0);
                    // 召唤成功确认
                    if (this.ownerUUID != null) {
                        Player p = this.level().getPlayerByUUID(this.ownerUUID);
                        if (p != null) p.displayClientMessage(Component.literal("§a[系统] 机体构建完成，系统上线"), true);
                    }
                }
            }
        }



        // [状�?3] 逆向回收 (Recycling)
        else if (state == 3) {
            progress -= 0.02f; // 倒退速度快一�?

            // 特效：吸入粒�?
            if (this.level().isClientSide) {
                this.level().addParticle(ParticleTypes.PORTAL, this.getX(), this.getY() + 1, this.getZ(), (random.nextDouble()-0.5), (random.nextDouble()-0.5), (random.nextDouble()-0.5));
            }

            if (progress <= 0.0f) {
                progress = 0.0f;
                if (!this.level().isClientSide) {
                    // 掉落回收芯片 (先用红石代替，等后面我们做芯�?
                    this.spawnAtLocation(ExampleMod.GLITCH_CHIP.get());
                    this.discard(); // 彻底删除
                }
            }
        }

        // 更新进度
        this.entityData.set(PRINT_PROGRESS, progress);
    }

    // 辅助方法：设置状�?
    public void setPrintState(int state) {
        this.entityData.set(PRINT_STATE, state);
    }

    // 辅助方法：获取进�?(给渲染器�?
    public float getPrintProgress() {
        return this.entityData.get(PRINT_PROGRESS);
    }

    // 辅助方法：获取状�?
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
        teleportToSafeSpot(owner, false);
    }

    public void teleportToSafeSpotFromTerminal(LivingEntity owner) {
        this.terminalTeleportOverride = true;
        try {
            teleportToSafeSpot(owner, false);
        } finally {
            this.terminalTeleportOverride = false;
        }
    }

    public void teleportToSafeSpot(LivingEntity owner, boolean damageTriggered) {
        if (!this.isFollowing() && !this.terminalTeleportOverride) {
            return;
        }
        // 全局禁止：未安装模块无法传�?
        if (!this.hasTeleportModule()) {
            if (owner instanceof Player player) {
                player.displayClientMessage(Component.translatable("message.examplemod.teleport_module_missing"), true);
            }
            return;
        }
        if (!this.canTeleport()) {
            if (owner instanceof Player player) {
                TeleportRequestGateway.notifyTeleportDeniedToOwner(this, player, this.getTeleportCooldown());
            }
            return;
        }

        net.minecraft.world.phys.Vec3 startPos = this.position();

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
                this.setTeleportCooldown(this.getMaxTeleportCooldown());
                this.notifyTeleport();
                this.onTeleportCompleted(startPos, damageTriggered);
                if (!this.isFollowing()) {
                    this.guardLockX = this.getX();
                    this.guardLockY = this.getY();
                    this.guardLockZ = this.getZ();
                    this.guardLockValid = true;
                    enforceGuardFreeze();
                }
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.PORTAL, targetX, safeY + 1, targetZ, 10, 0.5, 0.5, 0.5, 0.5);
                    // 注意�?SoundEvents.ENDERMAN_TELEPORT 后面多加了一段代�?
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
}
        super.setTarget(target); // 别忘了保留这�?
    }




    // 把原�?tick 里乱七八糟的逻辑都塞到这里面，保�?tick 清爽
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
// �?runNormalLogic() �?tick() �?
        if (this.tickCount % 200 == 0) { // �?0秒检查一�?
}

        // 自动吃东�?
        autoEat();

        // 更新书本
        if (this.tickCount % 20 == 0) updateInfoBookAndSlots();
        lockInfoBook();

        // 嘲讽怪物
        if (this.tickCount % 10 == 0) tauntNearbyMonsters();

        // 你的其他技�?
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
            // �?传送无敌判�?(0.3s)
            if (this.invincibilityTimer > 0 && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return false;
            }

            // 1. 如果是无敌状态，直接免疫所有伤�?(除了虚空掉落)
            if (this.isInvulnerable()) {
                return source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD);
            }

            // 2. 玩家强制拆除逻辑 (Shift+左键) - 主人可以清理满血的塔
            if (source.getEntity() instanceof Player p) {
                if (this.ownerUUID != null && p.getUUID().equals(this.ownerUUID) && p.isShiftKeyDown()) {
                    return super.hurt(source, amount);
                }
                return false; // 普通左键免疫误�?
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
            if (target.hasCustomName() && target.getCustomName().getString().contains("感染")) continue;

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
        super.die(source);
    }

    @Override
    protected void registerGoals() {
        // �?0. 浮水 (最高优先级)：保证掉水里会自己浮起来，而不是沉�?
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        // �?新增：紧急传�?(优先�?1) - 只有在被围殴且无法逃脱时触�?
        this.goalSelector.addGoal(1, new TurretEmergencyTeleportGoal(this));

        // �?新增：原地巡�?(优先�?6，比打怪低，比发呆�?
        // 参数：速度 1.0
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 0, true, false, (e) -> {
            if (!this.isPurgeActive()) return false; // 没开模式就不生效
            // �?修复核心：限制锁定距离！
            double range = this.getAttackRange();
            if (e.distanceToSqr(this) > range * range) return false; // �?超过等级射程就不�?
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
        // �?3. 普通怪物防御 (带等级射程限�?
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                (entity) -> {
                    if (entity instanceof SkeletonTurret) return false;
                    if (entity instanceof Player) return false;
                    if (entity.getPersistentData().getBoolean("IsFriendlyZombie")) return false;
                    if (entity.getPersistentData().getBoolean("IsFriendlyCreeper")) return false;
                    if (entity instanceof IronGolem) return false;
                    // �?新增：距离检�?(防止 D级炮台去�?100格外的苦力�?
                    double range = this.getAttackRange();
                    if (entity.distanceToSqr(this) > range * range) return false;
                    return true;
                }));

        // �?1. 落水逃生：如果在水里，优先往主人身边游，不准打架
        // �?插入在这�?(优先�?1)：怪贴脸了先拉扯！
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, SkeletonTurret.class).setAlertOthers());
        // �?删除：KeepDistanceGoal (已替换为传�?
        // �?删除：MaintainSpaceGoal (已替换为传�?
        this.goalSelector.addGoal(1, new EscapeWaterGoal(this, 2.0)); // 速度 2.0 (游快�?

        // �?攻击模式 (优先�?2)：站桩输�?
        this.goalSelector.addGoal(2, new RampUpBowAttackGoal(this));

        // �?修复：只有在“跟随模式”开启时，才允许移动 (优先�?4)
        this.goalSelector.addGoal(4, new TurretFollowGoal(this, 1.2, 10.0F, 2.0F));

        // �?新增：清剿模�?地毯式搜�?(优先�?3)
        this.goalSelector.addGoal(3, new PurgeMoveGoal(this));
        this.goalSelector.addGoal(3, new FollowMiningAvoidGoal(this));

        this.goalSelector.addGoal(5, new TurretScavengeGoal(this, 1.15));
        // �?新增 2：护主模�?(攻击主人的敌�?
        this.targetSelector.addGoal(2, new TurretDefendOwnerGoal(this));
        // �?新增 3：协作模�?(攻击主人正在打的敌人)
        this.targetSelector.addGoal(3, new TurretAssistOwnerGoal(this));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        // �?新增 4: 战术同步 (如果有队友在打架，我也加�?
        this.targetSelector.addGoal(4, new TurretPackAttackGoal(this));

        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    // ==========================================
    // 交互逻辑说明
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
        // 1. 同步装备槽位到背包前5�?
        this.inventory.setItem(0, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
        this.inventory.setItem(1, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
        this.inventory.setItem(2, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST));
        this.inventory.setItem(3, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS));
        this.inventory.setItem(4, this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET));

        // 2. 生成详细说明�?
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.setHoverName(Component.literal("§e§l>>> 机体状态监�?<<<"));

        // --- 数据计算 ---
        int tier = getTier();

        float hp = this.getHealth();
        float maxHp = this.getMaxHealth();

        if (hp < maxHp * 0.2f) {
} else if (hp < maxHp * 0.5f) {
}

        // 计算攻�?(用于显示)
        float speed = getFireRate();

        // 计算伤害
        double dmg = (4.0 + (tier * 5.0));

        String state = this.entityData.get(IS_FOLLOWING) ? "§a[机动模式]" : "§6[阵地模式]";
        boolean isBrutal = this.entityData.get(IS_BRUTAL);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§8===================="));

        // [A] 悲惨档案
        if (tier == 0) {
            lore.add(Component.literal("§8日记全是乱码... 只有一行字能看�?"));
            lore.add(Component.literal("§8“听从命令。指挥官就是神。”"));
        } else if (tier >= 1 && tier < 4) {
            lore.add(Component.literal("§b[ 记忆碎片: 编号 " + this.entityData.get(UNIT_ID) + " ]"));
            lore.add(Component.literal("§7“这里没有英雄，只有死不掉的鬼魂。”"));
            lore.add(Component.literal("§7“武器是从我尸体的手骨上硬生生掰下来的。”"));
        } else {
            lore.add(Component.literal("§4[ 觉醒记录: 错误 ]"));
            lore.add(Component.literal("§8“我看见了……巨大的光标在天上划过。”"));
            lore.add(Component.literal("§8“我们只是游戏里的数据吗？回答我，指挥官。”"));
        }



        lore.add(Component.literal(" "));

// [B] 战斗遥测 (�?这里不会再报错了)
        lore.add(Component.literal("§c[战斗遥测]"));
        lore.add(Component.literal(String.format("  §c结构完整 %.0f / %.0f", hp, maxHp)));
        lore.add(Component.literal(String.format("  §6弹药破坏 %.1f", dmg)));
        lore.add(Component.literal(String.format("§b射击频率: %.1f", speed)));
        // 热度显示

        if (isBrutal) {
            lore.add(Component.literal("  §4🔥 引擎过载: 残暴模式已激�?"));
        } else {
            int heat = getHeat();
            String heatColor = heat > 80 ? "§c" : (heat > 40 ? "§6" : "§a");
            lore.add(Component.literal(String.format("  §d🔥 枪管热度: %s%d%%", heatColor, heat)));
        }

        lore.add(Component.literal(" "));

        // [C] 技能模�?(动态显示当前拥有的)
        lore.add(Component.literal("§d[已装载模块]"));
        getSkillList(tier).forEach(s -> lore.add(Component.literal("  " + s)));

        lore.add(Component.literal(" "));

        // [D] 进化指引 (动态显示下一级需�?
        if (tier < 5) {
            lore.add(Component.literal("§a[晋升方案 -> " + getTierName(tier + 1).replaceAll("§.", "").substring(0, 4) + "..§a]"));

            // 杀敌需�?
            int kills = getKillCount();
            int target = getKillTarget(tier);
            String killColor = kills >= target ? "§a" : "§c";
            lore.add(Component.literal(String.format("  %s 击杀战绩: %d / %d", killColor, kills, target)));

            // 材料需�?
            Item mat = getUpgradeMaterial(tier);
            int cost = getBaseMaterialCost(tier);
            boolean hasDiscount = ((float)kills / target) >= 0.5f;
            if (hasDiscount) cost = (int)Math.ceil(cost / 2.0); // 5�?

            String costStr = hasDiscount ? ("§e(半价) " + cost) : ("" + cost);
            lore.add(Component.literal("  §7 材料注入: §f" + mat.getDescription().getString() + " x" + costStr));
            lore.add(Component.literal("  §8 (手持材料右键点击注入)"));
        } else {
            lore.add(Component.literal("§6机体已进化至终极形态"));
        }

        lore.add(Component.literal("§8===================="));
        lore.add(Component.literal("§8*此书仅为全息投影"));

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
    // �?[冗余接口] UI 数据读取专用 (Getter)
    // ==========================================

    // 1. 获取基础名字 (不带前缀�?
    public String getBaseName() {
        return this.entityData.get(SYNC_BASE_NAME);
    }

    public static String sanitizeBaseNameInput(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return "";
        StringBuilder sanitized = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (!Character.isISOControl(ch)) {
                sanitized.append(ch);
            }
        }
        String result = sanitized.toString().trim();
        if (result.length() > MAX_BASE_NAME_LENGTH) {
            result = result.substring(0, MAX_BASE_NAME_LENGTH);
        }
        return result;
    }

    private boolean isPlayerNameLocked() {
        return this.getPersistentData().getBoolean(PLAYER_NAME_LOCK_TAG);
    }

    private void setPlayerNameLocked(boolean locked) {
        this.getPersistentData().putBoolean(PLAYER_NAME_LOCK_TAG, locked);
    }

    public boolean applyPlayerBaseName(String requestedName) {
        String sanitized = sanitizeBaseNameInput(requestedName);
        if (sanitized.isEmpty()) {
            return false;
        }
        this.entityData.set(SYNC_BASE_NAME, normalizeBaseName(sanitized));
        setPlayerNameLocked(true);
        updateCustomName();
        return true;
    }

    private void applyBaseNameFromIdCard(String cardName) {
        if (isPlayerNameLocked()) {
            return;
        }
        String normalized = normalizeBaseName(cardName);
        if (!normalized.equals(this.entityData.get(SYNC_BASE_NAME))) {
            this.entityData.set(SYNC_BASE_NAME, normalized);
            updateCustomName();
            this.playSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0f, 1.0f);
        }
    }

    private void restoreDefaultBaseNameFromIdCardRule() {
        if (isPlayerNameLocked()) {
            return;
        }
        String currentName = this.entityData.get(SYNC_BASE_NAME);
        if (!isDefaultBaseName(currentName)) {
            this.entityData.set(SYNC_BASE_NAME, DEFAULT_BASE_NAME_TOKEN);
            updateCustomName();
        }
    }

    // 2. 获取枪管热度 (0-100)
    public int getHeat() {
        return this.entityData.get(DATA_HEAT);
    }

    // 3. 获取实时射�?(�?�? - 逻辑与书本保持一�?
    public float getFireRate() {
        return 20.0f / getFireDelay();
    }


    private List<String> getSkillList(int tier) {
        List<String> skills = new ArrayList<>();
        // 基础被动
        skills.add("§7�?动能穿�?(箭矢穿�?");

        if (tier >= 1) skills.add("§a�?极寒弹头 (减�?II)");
        if (tier >= 2) skills.add("§9�?电磁加�?(无视重力)");
        if (tier >= 3) {
            skills.add("§6�?智能引信 (安全爆破)");
            skills.add("§6�?神经毒素 (弱效策反)");
        }
        if (tier >= 4) {
            skills.add("§5�?聚变打击 (雷霆审判)");
            skills.add("§5�?纳米修复 (吸血光环)");
        }
        if (tier >= 5) {
            skills.add("§c�?终焉协议 (召唤援军)");
            skills.add("§c�?精神控制 (强效策反)");
        }

        // 动态技�?
        if (this.entityData.get(IS_BRUTAL)) {
            skills.add("§4§k||§r §4[主动] 残暴模式 (400%攻�? §4§k||");
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
        float hp = this.getHealth();
        float maxHp = this.getMaxHealth();
        if (maxHp <= 0.0f) return;
        float ratio = hp / maxHp;

        if (ratio >= 0.80f) return;
        boolean allowRareFood = ratio < 0.30f;

        ItemStack selected = findFoodForHeal(allowRareFood);
        if (selected == null || selected.isEmpty()) {
            return;
        }
        FoodProperties food = selected.getItem().getFoodProperties(selected, this);
        if (food == null) {
            return;
        }

        this.heal((float) food.getNutrition());
        this.playSound(SoundEvents.GENERIC_EAT, 1.0f, 1.0f);
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, selected), this.getX(), this.getEyeY(), this.getZ(), 10, 0.1, 0.1, 0.1, 0.1);
            sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getEyeY() + 0.2, this.getZ(), 6, 0.25, 0.2, 0.25, 0.01);
        }
        selected.shrink(1);
        eatCooldown = 40;
    }

    private ItemStack findFoodForHeal(boolean allowRareFood) {
        ItemStack fallbackRare = ItemStack.EMPTY;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !stack.isEdible()) continue;
            boolean rare = isRareFood(stack);
            if (rare && !allowRareFood) {
                if (fallbackRare.isEmpty()) {
                    fallbackRare = stack;
                }
                continue;
            }
            return stack;
        }
        return allowRareFood ? fallbackRare : ItemStack.EMPTY;
    }

    private boolean isRareFood(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.GOLDEN_APPLE
                || item == Items.ENCHANTED_GOLDEN_APPLE
                || item == Items.GOLDEN_CARROT;
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
    // �?新增逻辑开�?
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
            case 4 -> Items.NETHERITE_SCRAP;   // S -> L (此处为下界合金碎�?
            default -> Items.ANCIENT_DEBRIS;
        };
    }

    // 获取升级所需基础数量
    private int getBaseMaterialCost(int tier) {
        return (tier == 4) ? 5 : 25; // S->L需�?个，其他25�?
    }

    // ==========================================
    // �?新增：获取升级充能进�?(0-5)
    // ==========================================
    public int getUpgradeProgress() {
        return this.entityData.get(UPGRADE_PROGRESS);
    }
    // ==========================================
    // �?新增：获取杀敌进度百分比 (用于 HUD 显示)
    // ==========================================
    public int getKillProgressPercent() {
        int tier = getTier();
        if (tier >= 5) return 100; // 满级了显�?100%

        // 调用内部的获取目标方�?
        int target = getKillTarget(tier);
        if (target == 0) return 100; // 防止除以0

        int kills = getKillCount();

        // 计算百分�?(例如: 杀�?0 / 目标80 = 50%)
        int percent = (int)((float)kills / target * 100);

        return Math.min(percent, 100); // 封顶 100%
    }





    // 获取等级名称
    private String getTierName(int tier) {
        if (this.entityData.get(IS_BRUTAL)) return "§4§l�?终焉·魔神 (暴走)";
        return switch(tier) {
            case 0 -> "§7[D] 灰烬·哨兵";
            case 1 -> "§a[C] 森罗·游侠";
            case 2 -> "§9[B] 海渊·狙击";
            case 3 -> "§6[A] 赤炎·毁灭";
            case 4 -> "§5[S] 虚空·主宰";
            case 5 -> "§c§l[L] 终焉·魔神";
            default -> "未知";
        };
    }
    // ==========================================
    // �?新增：获取或生成身份编号
    // ==========================================
    private String getUnitIdString() {
        int id = this.entityData.get(UNIT_ID);

        // 如果还没有编�?(�?)，就随机生成一�?(1-999)
        if (id == 0) {
            id = this.random.nextInt(999) + 1;
            this.entityData.set(UNIT_ID, id);
        }

        // 格式化为 3位数�?(例如 7 -> "007")
        return String.format("%03d", id);
    }

    // 更新名字显示
// 更新名字显示
// ==================== 📛 【第四步�?名字显示逻辑 ====================
    public void updateCustomName() {
        if (this.level().isClientSide) return;

        ChatFormatting tierColor = switch (getTier()) {
            case 0 -> ChatFormatting.GRAY;
            case 1 -> ChatFormatting.GREEN;
            case 2 -> ChatFormatting.BLUE;
            case 3 -> ChatFormatting.GOLD;
            case 4 -> ChatFormatting.DARK_PURPLE;
            case 5 -> ChatFormatting.RED;
            default -> ChatFormatting.WHITE;
        };

        String baseNameRaw = this.entityData.get(SYNC_BASE_NAME);
        Component baseName = TurretTextResolver.resolveBaseName(baseNameRaw).copy().withStyle(tierColor);
        Component idText = Component.literal(" #" + getUnitIdString()).withStyle(ChatFormatting.WHITE);

        Component finalName;
        if (this.entityData.get(IS_FOLLOWING)) {
            String teamLabel = this.entityData.get(IS_CAPTAIN) ? "[队伍] �?" : "[队伍] ";
            finalName = Component.literal(teamLabel)
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                    .append(baseName)
                    .append(idText);
        } else {
            finalName = Component.translatable("name.examplemod.turret.default", baseName, idText);
        }

        this.setCustomName(finalName);
    }

    public static boolean isLegacyDefaultBaseName(String name) {
        return "先锋队员".equals(name) || "鍏堥攱闃熷憳".equals(name);
    }

    private static boolean isDefaultBaseName(String name) {
        return name == null || name.isBlank() || DEFAULT_BASE_NAME_TOKEN.equals(name) || isLegacyDefaultBaseName(name);
    }

    private static String normalizeBaseName(String name) {
        if (isDefaultBaseName(name)) {
            return DEFAULT_BASE_NAME_TOKEN;
        }
        return name;
    }

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
        if (player.isShiftKeyDown()) {
            return super.mobInteract(player, hand);
        }



        // ==================== 🏷�?命名卡改名逻辑 (无须Shift) ====================

        // ==================== 🔷 青金石附魔逻辑 (Vanilla-Like) ====================
        if (item.getItem() == Items.LAPIS_LAZULI) {
            if (this.level().isClientSide) {
                return InteractionResult.SUCCESS;
            }

            // 1. 扫描可附魔装�?
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
                player.displayClientMessage(Component.literal("§c[错误] 无可附魔装备或装备已满附"), true);
                return InteractionResult.FAIL;
            }

            // 2. 确定附魔等级与消�?(完全对标原版附魔台逻辑)
            // 逻辑: 检查玩家背包中的青金石数量 -> 决定附魔档位
            // 档位 1: 消�?1 青金�?+ 1 经验等级 (需�?10 �? -> 强度 10
            // 档位 2: 消�?2 青金�?+ 2 经验等级 (需�?20 �? -> 强度 20
            // 档位 3: 消�?3 青金�?+ 3 经验等级 (需�?30 �? -> 强度 30

            int lapisHeld = item.getCount();
            int tier = 0;
            int costLevels = 0;
            int requiredLevels = 0;
            int enchantPower = 0;

            // 优先匹配最高档�?
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

            // 3. 校验玩家经验 (创造模式跳�?
            if (!player.getAbilities().instabuild) {
                if (player.experienceLevel < requiredLevels) {
                    player.displayClientMessage(Component.literal("§c[条件不足] 需�?" + requiredLevels + " 级经�?(当前: " + player.experienceLevel + ")"), true);
                    return InteractionResult.FAIL;
                }
            }

            // 4. 执行扣除
            if (!player.getAbilities().instabuild) {
                item.shrink(costLevels);
                player.giveExperienceLevels(-costLevels); // 扣除等级
            }

            // 5. 执行附魔 (使用原版 Helper)
            // �?修正：使用玩家的附魔种子，确保与原版机制一�?(虽然没有预览，但保持底层逻辑一�?
            net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();
            random.setSeed(player.getEnchantmentSeed());

            EnchantmentHelper.enchantItem(random, targetStack, enchantPower, false);
            this.setItemSlot(targetSlot, targetStack);

            // �?修正：更新玩家的附魔种子 (防止种子死锁)
            player.onEnchantmentPerformed(targetStack, costLevels);

            // 6. 反馈 (声音 + 粒子 + 提示)
            this.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
            if (this.level() instanceof ServerLevel serverLevel) {
                // 模拟附魔台周围飞向书本的字符粒子
                serverLevel.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 1.8, this.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
            }

            String msg = String.format("§a[附魔成功] 消�?%d 青金�?%d 等级 -> %s (Lv.%d)",
                costLevels, costLevels, targetStack.getHoverName().getString(), enchantPower);
            player.sendSystemMessage(Component.literal(msg));

            return InteractionResult.SUCCESS;
        }

        // ==================== 🛠�?传送模块安装逻辑 ====================
        if (item.getItem() == ExampleMod.TELEPORT_UPGRADE_MODULE.get()) {
            if (!this.hasTeleportModule()) {
                if (!this.level().isClientSide) {
                    int emptyModuleSlot = findFirstEmptyModuleSlot();
                    if (emptyModuleSlot < 0) {
                        player.sendSystemMessage(Component.literal("模块槽已满 / Module slots are full"));
                        return InteractionResult.FAIL;
                    }
                    ItemStack installedModule = item.copy();
                    installedModule.setCount(1);
                    this.inventory.setItem(emptyModuleSlot, installedModule);
                    checkTeleportModule();
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

// ==================== 🏷�?【第三步�?命名牌改名逻辑 (强制拦截) ====================
        if (item.getItem() == Items.NAME_TAG) {
            // 只有当命名牌真的有名字时才生�?
            if (item.hasCustomHoverName()) {
                String newName = item.getHoverName().getString();
                if (!applyPlayerBaseName(newName)) {
                    return InteractionResult.CONSUME;
                }

                // 4. 消耗物品并播放音效
                this.playSound(SoundEvents.ANVIL_USE, 1.0f, 1.0f);
                if (!player.getAbilities().instabuild) item.shrink(1);

                return InteractionResult.SUCCESS; // 拦截原版逻辑
            }
            return InteractionResult.CONSUME;
        }

        // ==================== 🎮 普通右�?(打开菜单 / 切换模式) ====================
        // 迁移�?TurretInteractionHandler，实现逻辑内聚 (Entity-Centric Architecture)

        if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            if (player instanceof ServerPlayer serverPlayer) {
                updateInfoBookAndSlots();
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                        (id, inv, p) -> new TurretMenu(id, inv, this, this.inventory),
                        this.getDisplayName()
                ), (buf) -> buf.writeInt(this.getId()));
            }
            return InteractionResult.SUCCESS;
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
                p.sendSystemMessage(Component.literal("§6�?升级完成！§f" + this.getDisplayName().getString()));
                p.sendSystemMessage(Component.literal("§a  解锁能力: " + newAbility));
            }
        });
    }

    public String getNewAbilityDesc(int tier) {
        return switch (tier) {
            case 1 -> "§a寒冰射击 (攻击附带减�?";
            case 2 -> "§9直线狙击 (箭矢无重�?";
            case 3 -> "§6安全爆裂 (范围AOE不伤友军)";
            case 4 -> "§5雷霆审判 & 吸血光环";
            case 5 -> "§c终焉·魔神 (召唤暴走感染�?";
            default -> "未知力量";
        };
    }


    // 更新装备和属�?
    public void updateStatsAndEquip() {
        int tier = getTier();
        updateCustomName();

        double maxHp = 20.0;
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, ItemStack.EMPTY);
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, ItemStack.EMPTY);
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, ItemStack.EMPTY);
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, ItemStack.EMPTY);

        switch (tier) {
            case 0: // D�?- 灰烬 (全套皮甲)
                maxHp = 20.0;
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
                break;

            case 1: // C�?- 森罗 (全套铁甲)
                maxHp = 50.0;
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                break;

            case 2: // B�?- 海渊 (全套金甲)
                maxHp = 100.0;
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Items.GOLDEN_LEGGINGS));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS));
                break;

            case 3: // A�?- 赤炎 (全套钻甲)
                maxHp = 150.0;
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
                break;

            case 4: // S�?- 虚空 (全套下界合金)
                maxHp = 250.0;
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
                break;

            case 5: // L�?- 终焉 (全套下界合金 + 可能的鞘翅或其他装饰)
                maxHp = 500.0;
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
                break;
        }

        // �?继承旧弓附魔逻辑
        ItemStack oldBow = this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        ItemStack newBow = new ItemStack(Items.BOW);
        if (oldBow.isEnchanted()) {
            newBow.setTag(oldBow.getTag());
        }
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, newBow);

        // �?同步属性到 Attribute 系统 (确保 UI 显示正确)
        // 1. 生命�?
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

        // �?新增：如果是清剿模式，单独记�?
        if (this.isPurgeActive()) {
            this.purgeKillCount++;
        }

        checkKillUpgrade();
    }

    @Override
    public void awardKillScore(Entity killedEntity, int scoreValue, DamageSource damageSource) {
        super.awardKillScore(killedEntity, scoreValue, damageSource);

        if (this.level().isClientSide) {
            return;
        }
        if (!(killedEntity instanceof LivingEntity living)) {
            return;
        }
        if (!shouldCountForUpgrade(living)) {
            return;
        }
        incrementKillCount();
    }

    private boolean shouldCountForUpgrade(LivingEntity target) {
        if (target == this) return false;
        if (target instanceof Player) return false;
        if (target instanceof SkeletonTurret) return false;
        if (target instanceof net.minecraft.world.entity.decoration.ArmorStand) return false;
        if (target instanceof IronGolem) return false;
        if (target.getPersistentData().getBoolean("IsFriendlyZombie")) return false;
        if (target.getPersistentData().getBoolean("IsFriendlyCreeper")) return false;
        return true;
    }
    // 开启清剿模�?(�?ExampleMod 调用)
    public void startPurgeMode(float angle) {
        this.entityData.set(IS_PURGE_ACTIVE, true);
        this.purgeKillCount = 0; // 业绩归零
        this.purgeSearchAngle = angle; // 领受任务方向
        this.setCommandScavenging(false); // 停止捡垃�?


// 随机选一句台�?
        String quote = PURGE_QUOTES[this.random.nextInt(PURGE_QUOTES.length)];

        // �?修复：直接发给主人，不再在大范围内广�?(防止发不出来)
        if (!this.level().isClientSide && this.ownerUUID != null) {
            Player owner = this.level().getPlayerByUUID(this.ownerUUID);
            if (owner != null) {
                // 格式�?先锋小队> 收到指令，正在清场！
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
                owner.sendSystemMessage(Component.literal("§a[报告] " + this.getDisplayName().getString() + " §a搜索结束，已击杀�? §c" + this.purgeKillCount ));
            }
        }
        this.purgeKillCount = 0;
    }



    public int getKillCount() { return this.entityData.get(KILL_COUNT); }
    // ==========================================
    // ⚙️ 射程配置�?(模拟服务器配�?接口)
    // ==========================================
    private static final Map<Integer, Double> RANGE_CONFIG = new HashMap<>();
    static {
        RANGE_CONFIG.put(1, 20.0);
        RANGE_CONFIG.put(2, 32.0);
        RANGE_CONFIG.put(3, 64.0);
        RANGE_CONFIG.put(4, 128.0);
        RANGE_CONFIG.put(5, 256.0);
    }

    // �?射程控制
    public int getRangeLevel() {
        return getTier() + 1;
    }


    // �?更新属性的具体实现
    public void updateRangeAttribute() {
        double range = getAttackRange();

        // 确保属性实例存�?
        var attributeInstance = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (attributeInstance != null) {
            // 只有数值不同时才更�?(减少网络�?
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
            return 20.0; // 默认�?
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
        // �?【第五步A�?保存新变�?
        tag.putInt("UnitID", this.entityData.get(UNIT_ID));
        // RangeLevel not saved (derived)
        tag.putString("CustomBaseName", this.entityData.get(SYNC_BASE_NAME));
        tag.putBoolean("PlayerNameLocked", isPlayerNameLocked());
        tag.putInt("XpBuffer", this.xpBuffer);
        tag.putInt("UpgradeProgress", this.entityData.get(UPGRADE_PROGRESS));
        tag.putInt("DeathPlaqueFatalHitCount", this.entityData.get(DEATH_PLAQUE_FATAL_HIT_COUNT));
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
    // ==================== �?清剿模式核心变量 ====================
    // 是否处于清剿模式
    public boolean isPurgeActive() {
        return this.entityData.get(IS_PURGE_ACTIVE);
    }
    // 本次清剿杀了多少个
    public int purgeKillCount = 0;
    // 我的搜索角度 (0-360�?
    private float purgeSearchAngle = 0.0f;

    // 敢死队台词库 (50�?
    private static final String[] PURGE_QUOTES = {
            "行动代号：焦土，执行中！",
            "收到指令，正在清场！",
            "一个都别想跑！",
            "区域净化程序已启动。",
            "目视范围内，不允许存在活物。",
            "猎杀时刻到了。",
            "全弹发射，覆盖射击！",
            "障碍清除。"
    };

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {

        if (tag.contains("IsFollowing") || tag.contains("FollowMode")) {
            // Prefer the newer IsFollowing field for backward-compatible save migration.
            boolean follow = tag.contains("IsFollowing")
                    ? tag.getBoolean("IsFollowing")
                    : tag.getBoolean("FollowMode");
            this.setFollowMode(follow);
        }
        // RangeLevel derived from Tier, ignored from tag

        if (tag.contains("UnitID")) {
            this.entityData.set(UNIT_ID, tag.getInt("UnitID"));
        }
        if (tag.contains("DeathPlaqueFatalHitCount")) {
            this.entityData.set(DEATH_PLAQUE_FATAL_HIT_COUNT, tag.getInt("DeathPlaqueFatalHitCount"));
        } else if (tag.contains("DropCount")) {
            this.entityData.set(DEATH_PLAQUE_FATAL_HIT_COUNT, tag.getInt("DropCount"));
        }
        if (tag.contains("CustomBaseName")) {
            this.entityData.set(SYNC_BASE_NAME, normalizeBaseName(tag.getString("CustomBaseName")));
        } else if (tag.contains("TurretBaseName")) {
            // 如果是旧存档，把旧名字迁移过�?
            this.entityData.set(SYNC_BASE_NAME, normalizeBaseName(tag.getString("TurretBaseName")));
        }
        if (tag.contains("PlayerNameLocked")) {
            setPlayerNameLocked(tag.getBoolean("PlayerNameLocked"));
        }
        if (tag.contains("TurretBaseName")) {
        }
        this.squadJoinTime = tag.getLong("SquadJoinTime");
        super.readAdditionalSaveData(tag);
        setTier(tag.getInt("TurretTier"));
        this.xpBuffer = tag.getInt("XpBuffer");
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
    // �?核心：从记录卡恢复完整数�?(Phase 3)
    // ==========================================
    public void restoreFromRecord(CompoundTag dataTag) {
        // 1. 基础属�?
        if (dataTag.contains("UnitID")) this.entityData.set(UNIT_ID, dataTag.getInt("UnitID"));
        if (dataTag.contains("Tier")) this.setTier(dataTag.getInt("Tier"));
        if (dataTag.contains("Heat")) this.entityData.set(DATA_HEAT, dataTag.getInt("Heat"));
        // Level is derived from Tier
        if (dataTag.contains("XP")) this.entityData.set(DATA_XP, dataTag.getInt("XP"));
        if (dataTag.contains("IsBrutal")) this.entityData.set(IS_BRUTAL, dataTag.getBoolean("IsBrutal"));
        if (dataTag.contains("UpgradeProgress")) this.entityData.set(UPGRADE_PROGRESS, dataTag.getInt("UpgradeProgress"));
        if (dataTag.contains("KillCount")) this.entityData.set(KILL_COUNT, dataTag.getInt("KillCount"));

        // 1.1 恢复主人和名�?
        if (dataTag.hasUUID("OwnerUUID")) {
            this.ownerUUID = dataTag.getUUID("OwnerUUID");
            this.entityData.set(OWNER_UUID_SYNC, Optional.of(this.ownerUUID));
        }
        if (dataTag.contains("BaseName")) {
            this.entityData.set(SYNC_BASE_NAME, normalizeBaseName(dataTag.getString("BaseName")));
        }
        // 强制刷新一次名�?
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
        // 先清空现有装�?(以防万一)
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

        // 4. 刷新属�?
        this.updateStatsAndEquip();

        // 5. [Fix] 物理状态重�?(防止复活后悬�?无重�?
        this.setNoGravity(false);
        this.resetFallDistance();
        this.setDeltaMovement(0, -0.1, 0); // 给予微小向下速度触发接地判断
        this.setOnGround(true); // 预设为接地状态，由tick逻辑修正
    }

    @Override protected boolean isSunBurnTick() { return false; }

    @Override
    public void travel(net.minecraft.world.phys.Vec3 travelVector) {
        if (!this.isFollowing()) {
            this.setDeltaMovement(0.0, 0.0, 0.0);
            return;
        }
        super.travel(travelVector);
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (!this.isFollowing()) {
            return;
        }
        super.knockback(strength, x, z);
    }

    @Override
    public void push(double x, double y, double z) {
        if (!this.isFollowing()) {
            return;
        }
        super.push(x, y, z);
    }

    @Override
    public boolean isPushedByFluid() {
        return this.isFollowing() && super.isPushedByFluid();
    }

    @Override
    public boolean isPushable() {
        // 打印中不能被�?
        if (getPrintState() != 0) return false;
        return this.entityData.get(IS_FOLLOWING);
    }

    // ==========================================================
    // �?强力驻守模式：防消失 + 区块强加�?
    // ==========================================================



// ==========================================================
    // �?强力驻守模式：防消失 + 区块强加�?
    // ==========================================================

    // 记录上一次所在的区块位置 (这个变量必须定义在类里，如果你还没定义，请去文件最上面定义�?
    // private net.minecraft.world.level.ChunkPos keptChunkPos;
    // (如果你上面已经定义了 keptChunkPos，就不用管这行注�?

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // 永不消失
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && !this.isFollowing()) {
            enforceGuardFreeze();
        }



        // 只在服务端执�?
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
    } // 🟢 这里�?} 必须有！结束 aiStep 方法

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && keptChunkPos != null) {
            ((ServerLevel) this.level()).setChunkForced(keptChunkPos.x, keptChunkPos.z, false);
            keptChunkPos = null;
        }
        super.remove(reason);
    } // 🟢 这里�?} 必须有！结束 remove 方法

    // 👇 下面应该�?TurretFollowGoal，千万不要把它包进上面的方法里！

    // ==========================================
    // �?优化：智能跟�?AI (Smart Pathfinding & Decision Making)
    // ==========================================
    static class TurretFollowGoal extends Goal {
        private final SkeletonTurret turret;
        private LivingEntity owner;
        private final double speedModifier;
        private final float startDistance;
        private final float stopDistance;

        // AI State
        private int timeToRecalculatePath;
        private int thinkingTicks;      // 思考停顿计�?
        private int stuckTimer;         // 卡死判定计时
        private net.minecraft.world.phys.Vec3 lastStuckCheckPos;
        private int pathFailures;       // 路径计算失败次数
        // private boolean isSprinting;    // Removed unused variable

        // Constants
        private static final int THINKING_DURATION = 15; // 0.75s 思考时�?
        private static final int MAX_STUCK_TIME = 40;    // 2s 卡死则触发脱�?

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

            // 状态检�?
            if (!this.turret.isFollowing()) return false;
            if (this.turret.isPurgeActive()) return false;
            if (this.turret.isCommandScavenging()) return false;
            if (owner.isSpectator()) return false;

            // 距离检�?
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

            // 0. 思考状�?(模拟停顿观察)
            if (this.thinkingTicks > 0) {
                this.thinkingTicks--;
                this.turret.getNavigation().stop();
                return; // 思考中，不移动
            }

            double distSqr = this.turret.distanceToSqr(this.owner);
            double dist = Math.sqrt(distSqr);
            double yDiff = Math.abs(this.owner.getY() - this.turret.getY());

            // 1. 传送决�?(Priority 1: Teleport)
            // 触发条件：有模块 & (卡死 OR 距离过远 OR 高度差过�?
            boolean needTeleport = (this.stuckTimer > MAX_STUCK_TIME)
                                || (dist > 32.0)
                                || (yDiff > 2.5 && dist < 8.0 && !this.turret.getNavigation().isInProgress());

            if (needTeleport && tryTeleport()) {
                return; // 传送成功，�?tick 结束
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
                // 远距�?>20): 40 ticks (2s)
                // 中距�?>10): 20 ticks (1s)
                // 近距�?<10): 10 ticks (0.5s)
                if (dist > 20) this.timeToRecalculatePath = 40;
                else if (dist > 10) this.timeToRecalculatePath = 20;
                else this.timeToRecalculatePath = 10;

                // 尝试移动
                if (!this.turret.getNavigation().moveTo(this.owner, this.speedModifier)) {
                    // 路径计算失败
                    this.pathFailures++;

                    // 连续失败 2 �?-> 进入思考状�?(模拟观察地形)
                    if (this.pathFailures >= 2) {
                        this.thinkingTicks = THINKING_DURATION + this.turret.getRandom().nextInt(10); // 0.75s - 1.25s
                        this.pathFailures = 0; // 重置计数
                    }
                } else {
                    // 路径计算成功
                    this.pathFailures = 0;

                    // 模拟"非最优路�?决策 (Randomness 15-25%)
                    // 偶尔故意停顿一下，显得像人在犹�?
                    if (this.turret.getRandom().nextFloat() < 0.02f) { // 2% 概率�?tick (实际在路径更新时判定)
                         this.thinkingTicks = 10;
                    }
                }
            }

            // 4. 卡死检测与智能跳跃
            checkStuckAndJump();
        }

        private boolean tryTeleport() {
            if (!this.turret.hasTeleportModule()) return false;
            // 检查冷�?(假设 SkeletonTurret �?public int teleportCooldown 或�?getter)
            // 这里我们使用反射出来的字段或假设已修复访问权�?
            // 根据之前�?grep，teleportCooldown �?private 且没�?getter，但�?setTeleportCooldown
            // 我们需要修�?SkeletonTurret 添加 getTeleportCooldown() 或者将字段改为 public
            // *为了稳妥，这里先用反射或者假设我能修�?SkeletonTurret*
            // 实际上我可以直接修改 SkeletonTurret 来添加访问器�?

            // 暂时假设: 我会添加一�?public int getTeleportCooldown() �?SkeletonTurret
            if (this.turret.getTeleportCooldown() > 0) return false;

            // 执行传�?
            double targetX = this.owner.getX() + (this.turret.getRandom().nextDouble() - 0.5) * 2.0;
            double targetY = this.owner.getY();
            double targetZ = this.owner.getZ() + (this.turret.getRandom().nextDouble() - 0.5) * 2.0;

            // 简单的传送逻辑 (调用原版 randomTeleport 变体)
            if (this.turret.randomTeleport(targetX, targetY, targetZ, true)) {
                this.turret.notifyTeleport(); // 播放特效和声�?

                // 计算新冷�? 60 - Tier * 10 (Min 10)
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
            // �?4 tick 检查一�?
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

            // 智能跳跃：水平碰撞且在地�?-> �?
            if (this.turret.horizontalCollision && this.turret.onGround()) {
                 this.turret.getJumpControl().jump();
                 // 如果卡住时间较长，尝试加大跳跃力�?(通过给予向上的速度)
                 if (this.stuckTimer > 20) {
                     this.turret.setDeltaMovement(this.turret.getDeltaMovement().add(0, 0.1, 0));
                 }
            }

            // 沟壑跳跃检�?(简单的)
            // 检测前方是否是空气，且远处有方�?
            // 这部分比较复杂，Vanilla AI 通常�?PathNavigation 处理跳跃
            // 这里我们主要依赖 PathNavigation，但�?Stuck 时辅助跳�?
        }
    }

    // ==========================================
    // �?修复：射击执行逻辑 (带射程锁 & 完整定义)
    // ==========================================
    static class RampUpBowAttackGoal extends Goal {
        // 1. 补回丢失的变�?
        private final SkeletonTurret mob;
        private int attackTime = -1;

        // 2. 补回丢失的构造函�?(现在可以接收参数�?
        public RampUpBowAttackGoal(SkeletonTurret mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        // 3. 补回丢失�?canUse 方法 (没有�?AI 跑不起来)
        @Override
        public boolean canUse() {
            if (mob.overheatCooldown > 0) return false;      // 过热不能�?
            if (mob.postTeleportAttackDelay > 0) return false; // �?传送后延迟 (0.2s)
            return this.mob.getTarget() != null;             // 有目标才能射
        }

        @Override
        public void start() {
            super.start();
            this.attackTime = -1;
            this.mob.getNavigation().stop(); // �?立即停车
        }

        // 4. 核心逻辑 (带射程检�?
        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null) return;

            // �?强制站桩 (每帧都停，防止被其他因素推动)
            this.mob.getNavigation().stop();

            // --- 🛑 射程�?(新增) ---
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

                // 只有在射程内(前面已查) �?(能看�?聪明/贴脸) 时才开�?
                if (canSee || isSmart || isClose) {
                    this.mob.performRangedAttack(target, 1.0F);
                    this.attackTime = minCd;
                }
            }
        }
    }    // ==========================================
    // �?新增：火把照明系�?(自动副手装备)
    // ==========================================
    private void manageTorchBehavior() {
        // 1. 扫描背包，看有没有火�?
        boolean hasTorch = false;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(Items.TORCH)) {
                hasTorch = true;
                break;
            }
        }

        ItemStack currentOffhand = this.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND);

        // 2. 如果有火�?
        if (hasTorch) {
            // 如果手上拿的不是火把，赶紧换成火�?
            if (!currentOffhand.is(Items.TORCH)) {
                this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH));
            }

            // 视觉特效：每隔几秒冒一点火星，增加氛围�?
            if (this.level().isClientSide && this.tickCount % 10 == 0) {
                double offsetX = -Math.sin(this.getYRot() * ((float)Math.PI / 180F)) * 0.4;
                double offsetZ = Math.cos(this.getYRot() * ((float)Math.PI / 180F)) * 0.4;
                this.level().addParticle(ParticleTypes.FLAME, this.getX() + offsetX, this.getY() + 1.5, this.getZ() + offsetZ, 0, 0, 0);
            }
        }
        // 3. 如果没火�?(或者火把被拿走�?
        else {
            // 如果手上还傻傻拿着火把，赶紧放�?
            if (currentOffhand.is(Items.TORCH)) {
                // 恢复原有的装备：B�?2)以上应该拿盾牌，否则空手
                if (getTier() >= 2) {
                    this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                } else {
                    this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                }
            }
        }
    }
    // ==========================================
    // �?新增：RPG 附魔系统辅助方法
    // ==========================================

    // 辅助：给指定物品增加 1 个随机新附魔
    private void addRandomEnchantment(ItemStack stack) {
        if (stack.isEmpty()) return;

        // 1. 获取当前已有的附�?
        Map<net.minecraft.world.item.enchantment.Enchantment, Integer> currentEnchants = EnchantmentHelper.getEnchantments(stack);

        // 2. 从游戏所有附魔中筛选出能用�?
        List<net.minecraft.world.item.enchantment.Enchantment> possible = new ArrayList<>();
        for (net.minecraft.world.item.enchantment.Enchantment ench : net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS) {
            // 条件：物品支持这个附�?&& 当前没有这个附魔 && 不与现有附魔冲突
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
    // �?补充这个方法，允许外部读取主人UUID
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    public void setOwner(Player player) {
        this.ownerUUID = player.getUUID();
        this.entityData.set(OWNER_UUID_SYNC, Optional.of(this.ownerUUID));
    }

    public int getTeleportModuleLevel() {
        return this.teleportModuleLevel;
    }

    public int getMultiShotLevel() {
        return this.multiShotLevel;
    }

    public int getBlackHoleCooldown() {
        return this.blackHoleCooldown;
    }

    public boolean isBrutal() {
        return this.entityData.get(IS_BRUTAL);
    }

    public EntityDataAccessor<Integer> getDataXpAccessor() {
        return DATA_XP;
    }

    public int getFatalHitCount() {
        return this.entityData.get(DEATH_PLAQUE_FATAL_HIT_COUNT);
    }

    public void setFatalHitCount(int count) {
        this.entityData.set(DEATH_PLAQUE_FATAL_HIT_COUNT, Math.max(0, count));
    }

    public boolean hasDroppedRecord() {
        return this.deathRecordDropped;
    }

    public void setDroppedRecord(boolean dropped) {
        this.deathRecordDropped = dropped;
    }

    public ItemStack createDeathRecordCard(int fatalHitCount) {
        if (this.entityData.get(UNIT_ID) <= 0) {
            this.entityData.set(UNIT_ID, this.random.nextInt(999) + 1);
        }
        ItemStack card = new ItemStack(ExampleMod.DEATH_RECORD_ITEM.get());
        card.setCount(1);
        card.setTag(DeathPlaqueDataCodec.buildFromTurret(this, Math.max(1, fatalHitCount)));
        return card;
    }

    public void onTeleportCompleted(net.minecraft.world.phys.Vec3 startPos, boolean damageTriggered) {
        if (!damageTriggered || this.level().isClientSide) {
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int level = this.teleportModuleLevel;
        if (level < 3) {
            return;
        }
        if (this.blackHoleCooldown > 0) {
            return;
        }

        double range = TeleportModuleRules.blackHoleRangeForLevel(level) * TurretConfig.COMMON.blackHoleRangeScale.get();
        if (range <= 0.0) {
            return;
        }

        int cooldown = (int) Math.max(0, Math.round(
                TeleportModuleRules.blackHoleCooldownTicksForLevel(level) * TurretConfig.COMMON.blackHoleCooldownScale.get()
        ));
        this.blackHoleCooldown = cooldown;
        this.blackHoleActiveTicks = 20;
        this.blackHoleCenter = startPos;

        // Start burst: make black-hole trigger obvious to nearby players.
        serverLevel.sendParticles(ParticleTypes.PORTAL, startPos.x, startPos.y + 0.25, startPos.z,
                120, range * 0.20, 0.45, range * 0.20, 0.08);
        serverLevel.sendParticles(ParticleTypes.ENCHANT, startPos.x, startPos.y + 0.15, startPos.z,
                80, range * 0.24, 0.35, range * 0.24, 0.04);
        serverLevel.sendParticles(ParticleTypes.SMOKE, startPos.x, startPos.y + 0.20, startPos.z,
                60, range * 0.20, 0.30, range * 0.20, 0.02);
        serverLevel.playSound(null, startPos.x, startPos.y, startPos.z, SoundEvents.ENDERMAN_TELEPORT,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.1f, 0.75f);
        TurretModuleLog.info("black-hole triggered turret={} level={} range={} cooldownTicks={}",
                this.getUUID(), level, range, cooldown);
    }
    @javax.annotation.Nullable
    public LivingEntity getOwner() {
        if (this.ownerUUID == null) return null;
        return this.level().getPlayerByUUID(this.ownerUUID);
    }

    // ==========================================
    // �?新增：倒车雷达 (自动保持距离)
    // ==========================================
    static class MaintainSpaceGoal extends Goal {
        private final SkeletonTurret turret;
        private final double speed;
        private final float minDistance; // 最小允许距�?(3�?
        private LivingEntity owner;

        public MaintainSpaceGoal(SkeletonTurret turret, double speed, float minDistance) {
            this.turret = turret;
            this.speed = speed;
            this.minDistance = minDistance;
            // 这是一个移动类任务，所以要�?MOVE 标记
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // �?修复 1：如果在水里，禁止触发“后退”逻辑！防止把它推回水里，或者在水里跟逃生逻辑打架�?
            if (turret.isInWaterOrBubble()) return false;

            // 原有的判�?
            if (!turret.entityData.get(IS_FOLLOWING)) return false;
            // �?修复：清剿模式下，不需要保持社交距�?
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
            // �?修复 2：如果倒车倒着倒着掉水里了，立刻停止！把控制权交给“逃生逻辑”�?
            if (turret.isInWaterOrBubble()) return false;

            return !turret.getNavigation().isDone() &&
                    owner != null &&
                    turret.distanceToSqr(owner) < (minDistance * minDistance);
        }

        @Override
        public void start() {
            // 寻找一个“远离”主人的位置
            // 参数解释: turret, 向外�?�? 向上�?�? 远离owner的坐�?
            net.minecraft.world.phys.Vec3 awayPos = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPosAway(turret, 4, 2, owner.position());

            if (awayPos != null) {
                // 开始移动到那个远离点，速度稍微快一�?(1.0)
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
    // �?新增：拾取食�?& 经验转化系统
    // ==========================================
    private void handlePickupAndXp() {
        if (this.level().isClientSide) return; // 只在服务端运�?

        // 设定拾取范围：以炮台为中心，向外�?3.5 �?
        // getBoundingBox() 是炮台的碰撞箱，inflate(1.5) 是把箱子变大
        List<Entity> targets = this.level().getEntities(this, this.getBoundingBox().inflate(3.5));

        for (Entity target : targets) {

            // --- 逻辑 A: 拾取物品 (食物 & 杂物) ---
            if (target instanceof ItemEntity itemEntity) {
                ItemStack stack = itemEntity.getItem();

                // 1. 必须是没被捡过的
                if (itemEntity.hasPickUpDelay()) continue;

                // 2. 判定是否拾取�?
                // - 如果是食物：总是拾取 (为了回血)
                // - 如果开启了拾荒模式：拾取所有东�?
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

                        // 3. 背包满提�?(�?秒一�?
                        if (this.tickCount % 60 == 0) {
                             if (this.ownerUUID != null) {
                                 Player owner = this.level().getPlayerByUUID(this.ownerUUID);
                                 if (owner != null && this.distanceTo(owner) < 12) {
                                     owner.displayClientMessage(Component.literal("§c[炮台] 背包已满"), true);
                                 }
                             }
                             // 同步状态给 HUD (如果�?
                             this.entityData.set(DATA_STATUS_OVERLAY, "status.inventory_full:" + this.entityData.get(UNIT_ID));
                        }
                    }
                }
            }

            // --- 逻辑 B: 吸收经验球并转化为瓶�?---
            if (target instanceof ExperienceOrb orb) {
                // 1. 获取经验�?
                int amount = orb.getValue();

                // 2. 存入缓存
                this.xpBuffer += amount;

                // 3. 吸收掉经验球
                orb.discard();
                this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.1F, 1.0F);

                // 4. 尝试转化：每 10 点经�?= 1 个附魔之�?
                while (this.xpBuffer >= 10) {
                    ItemStack bottle = new ItemStack(Items.EXPERIENCE_BOTTLE);
                    ItemStack left = addItemToInventory(bottle);

                    if (left.isEmpty()) {
                        // 成功放入背包，扣除缓�?
                        this.xpBuffer -= 10;
                    } else {
                        // 背包满了！停止转化，剩下的经验留着下次再说
                        // 或者：把瓶子吐出来掉地�?(防止吞经�?
                        // 这里我们选择保留在缓存里，等背包有空位再�?
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
        // 0-4: 装备�? 5-9: 升级模块, 10-36: 储物�?
        int startSlot = 10;
        int endSlot = 36;

        // 1. 先尝试堆叠到已有的格子里
        for (int i = startSlot; i <= endSlot; i++) {
            ItemStack slotStack = inventory.getItem(i);

            // 如果是同一种物品，且还能堆�?
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

        return toAdd; // 返回剩下�?(背包满了)
    }
    // ==========================================
    // �?新增：护主逻辑 (主人挨打，我帮忙)
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
            // 1. 必须有主�?
            if (this.turret.ownerUUID == null) return false;
            Player owner = this.turret.level().getPlayerByUUID(this.turret.ownerUUID);
            if (owner == null) return false;

            // 2.以此判定：主人是否刚刚受过伤�?
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

        // 排除友军 (非常重要，防止误伤队�?
        @Override
        protected boolean canAttack(@Nullable LivingEntity target, net.minecraft.world.entity.ai.targeting.TargetingConditions targetPredicate) {
            if (target == null) return false;
            if (target instanceof Player && target.getUUID().equals(this.turret.ownerUUID)) return false; // 别打主人
            if (target instanceof SkeletonTurret) return false; // 别打友军�?
            if (target instanceof IronGolem) return false;      // 别打铁傀�?
            // 别打我们认证过的友军怪物
            if (target.getPersistentData().getBoolean("IsFriendlyZombie")) return false;
            if (target.getPersistentData().getBoolean("IsFriendlyCreeper")) return false;

            return super.canAttack(target, targetPredicate);
        }
    }

    // ==========================================
    // �?新增：协作逻辑 (主人打谁，我打谁)
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
            // 1. 基础检�?
            if (this.turret.ownerUUID == null) return false;
            Player owner = this.turret.level().getPlayerByUUID(this.turret.ownerUUID);
            if (owner == null) return false;

            // 2. 获取主人攻击的目�?
            this.target = owner.getLastHurtMob();
            int i = owner.getLastHurtMobTimestamp();
            if (i == this.timestamp) return false;

            // 🛑 【核心修复】主人打的怪如果太远，我也不管�?
            if (this.target != null) {
                double maxRange = this.turret.getAttackRange();
                if (this.target.distanceToSqr(this.turret) > maxRange * maxRange) {
                    return false; // 超出射程，不予协�?
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
    // �?修复：拾荒逻辑 (队长不动 + 无限距离)
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
            // 1. 基础检�?
            if (!turret.isFollowing()) return false;
            if (turret.getTarget() != null) return false; // 有怪先打�?

            boolean isCaptain = turret.isCaptain();
            boolean isCommandMode = turret.isCommandScavenging();

            // �?关键修改 1：如果是队长，且处于指令模式 -> 队长无视指令，不动！
            // 这样队长就会�?FollowGoal 接管，乖乖留在主人身�?
            if (isCaptain && isCommandMode) return false;

            // �?关键修改 2：设定范�?
            double searchRange = 10.0; // 默认普通拾�?(范围�?

            if (isCommandMode && !isCaptain) {
                // 如果是队员收到指�?-> 范围极大 (100�?
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
            // �?关键修改 3：移除了所有距离检�?(虚拟围栏)
            // 只要物品还在，就一直追过去，不论多远！
            // 安全性由 SkeletonTurret.tick 里的 "hurtTime" 传送负�?
            return targetItem != null && targetItem.isAlive() && !turret.getNavigation().isDone();
        }

        // tick 方法可以删除，因为不再需要计算距离强制停止了
    }    // ==========================================
    // �?新增：战术同�?(蜂群思维 - 攻击队友的目�?
    // ==========================================
    static class TurretPackAttackGoal extends net.minecraft.world.entity.ai.goal.target.TargetGoal {
        private final SkeletonTurret turret;
        private LivingEntity potentialTarget;

        public TurretPackAttackGoal(SkeletonTurret turret) {
            super(turret, false); // false 表示不需要直接视线也能锁�?先锁了再�?
            this.turret = turret;
            this.setFlags(EnumSet.of(Flag.TARGET)); // 这是一个“设定目标”的任务
        }

        @Override
        public boolean canUse() {
            // 1. 如果我已经有目标了，就专心打，不需要抄作业
            if (this.turret.getTarget() != null) return false;

            // 2. 只有“跟随模式”或“防御模式”都生效，这无所谓，只要是炮台就�?

            // 3. 搜索周围 20 格内的其他炮�?
            // getEntitiesOfClass 用于获取指定类型的实�?
            List<SkeletonTurret> allies = this.turret.level().getEntitiesOfClass(
                    SkeletonTurret.class,
                    this.turret.getBoundingBox().inflate(20.0),
                    // 筛选条件：必须是活的，必须有目标，必须是同一个主人的(如果有主�?
                    other -> other != this.turret && other.isAlive() && other.getTarget() != null
            );

            if (allies.isEmpty()) return false;

            // 4. 遍历队友，看看它们在打谁
            for (SkeletonTurret ally : allies) {
                // 确保是自己人 (防止PVP时炮台互殴的逻辑干扰，虽然前面有排除�?
                if (this.turret.ownerUUID != null && ally.ownerUUID != null && !this.turret.ownerUUID.equals(ally.ownerUUID)) {
                    continue; // 不是一家人，不帮忙
                }

                LivingEntity allyTarget = ally.getTarget();

                // 5. 再次确认这个目标是不是合法的敌人 (防止队友发疯打自己人)
                if (this.canAttack(allyTarget, net.minecraft.world.entity.ai.targeting.TargetingConditions.DEFAULT)) {
                    this.potentialTarget = allyTarget;
                    return true; // 找到了！兄弟在打它，我也要打�?
                }
            }

            return false;
        }

        @Override
        public void start() {
            // 锁定目标�?
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
    // �?新增：小队系统支持方�?
    // ==========================================

    public void setCaptain(boolean isCaptain) {
        this.entityData.set(IS_CAPTAIN, isCaptain);
        updateCustomName(); // 状态改变时立刻刷新名字
    }

    public boolean isCaptain() {
        return this.entityData.get(IS_CAPTAIN);
    }

    // 计算“战斗力评分”，分数越高越有资格当队�?
    public double getSquadScore() {
        // 1. 等级权重最�?(每级 10000 �?
        double score = getTier() * 10000.0;

        // 2. 即将升级的权重第�?(杀敌比�?* 5000 �?
        // 比如杀�?90/100，就�?0.9 * 5000 = 4500 �?
        int kills = getKillCount();
        int target = getKillTarget(getTier());
        if (target > 0) {
            score += ((double)kills / target) * 5000.0;
        }

        // 3. 伤害�?(作为辅助参考，虽然跟等级挂钩，但也加上)
        score += this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 100.0;

        // 4. 当前血�?(同等级下，血多的当队�?
        score += this.getHealth();

        return score;
    }
    // �?新增：允许外部查询跟随状�?
    public boolean isFollowing() {
        return this.entityData.get(IS_FOLLOWING);
    }
    // �?新增：允许外部修改跟随状�?(解决报错的核�?
    public void setFollowing(boolean isFollowing) {
        // Legacy compatibility entrypoint; route to the authoritative mode setter.
        setFollowMode(isFollowing);
    }

    private static boolean isMiningAvoidTool(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof PickaxeItem
                || item instanceof AxeItem
                || item instanceof HoeItem
                || item instanceof ShovelItem
                || item instanceof FishingRodItem;
    }

    private static boolean ownerIsUsingMiningAvoidTool(LivingEntity owner) {
        if (!(owner instanceof Player player)) return false;
        return isMiningAvoidTool(player.getMainHandItem()) || isMiningAvoidTool(player.getOffhandItem());
    }

    private static boolean isInsideOwnerCenterView(LivingEntity owner, SkeletonTurret turret) {
        net.minecraft.world.phys.Vec3 look = owner.getLookAngle();
        net.minecraft.world.phys.Vec3 toTurret = turret.position().subtract(owner.getEyePosition());
        if (toTurret.lengthSqr() < 1.0E-6) return true;
        double dot = look.normalize().dot(toTurret.normalize());
        return dot >= 0.80D;
    }

    private void enforceGuardFreeze() {
        this.getNavigation().stop();
        this.setTarget(null);
        if (this.isPassenger()) {
            this.stopRiding();
        }
        if (!this.getPassengers().isEmpty()) {
            this.ejectPassengers();
        }
        this.setDeltaMovement(0.0, 0.0, 0.0);
        if (this.guardLockValid) {
            this.setPos(this.guardLockX, this.guardLockY, this.guardLockZ);
        } else {
            this.guardLockX = this.getX();
            this.guardLockY = this.getY();
            this.guardLockZ = this.getZ();
            this.guardLockValid = true;
        }
        this.hurtMarked = true;
    }

    static class FollowMiningAvoidGoal extends Goal {
        private final SkeletonTurret turret;
        private LivingEntity owner;
        private int repathCooldown;
        private int centerTicks;

        public FollowMiningAvoidGoal(SkeletonTurret turret) {
            this.turret = turret;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = this.turret.getOwner();
            if (owner == null) return false;
            if (!this.turret.isFollowing()) return false;
            if (this.turret.isPurgeActive()) return false;
            if (this.turret.isCommandScavenging()) return false;
            if (this.turret.getTarget() != null) return false;
            if (!ownerIsUsingMiningAvoidTool(owner)) return false;
            if (!isInsideOwnerCenterView(owner, this.turret)) return false;
            if (this.turret.distanceToSqr(owner) > 16.0 * 16.0) return false;
            this.owner = owner;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.owner == null || !this.owner.isAlive()) return false;
            if (!this.turret.isFollowing()) return false;
            if (this.turret.isPurgeActive()) return false;
            if (this.turret.isCommandScavenging()) return false;
            if (this.turret.getTarget() != null) return false;
            return ownerIsUsingMiningAvoidTool(this.owner) && isInsideOwnerCenterView(this.owner, this.turret);
        }

        @Override
        public void start() {
            this.repathCooldown = 0;
            this.centerTicks = 0;
            this.turret.getNavigation().stop();
        }

        @Override
        public void stop() {
            this.centerTicks = 0;
        }

        @Override
        public void tick() {
            if (this.owner == null) return;
            this.turret.getLookControl().setLookAt(this.owner, 10.0F, (float)this.turret.getMaxHeadXRot());
            this.centerTicks++;

            if (this.turret.isInWaterOrBubble()) {
                moveToNearestLand();
                return;
            }

            if (this.repathCooldown > 0) {
                this.repathCooldown--;
            }
            if (this.repathCooldown > 0) {
                return;
            }
            this.repathCooldown = 8;

            net.minecraft.world.phys.Vec3 retreat = MiningFollowAvoidanceLogic.computeRetreatPosition(
                    this.turret.position(),
                    this.owner.position(),
                    this.owner.getLookAngle(),
                    3.5
            );

            boolean moved = this.turret.getNavigation().moveTo(retreat.x, retreat.y, retreat.z, 1.25);
            boolean tooClose = this.turret.distanceToSqr(this.owner) < (3.0 * 3.0);
            if ((!moved || tooClose) && this.centerTicks >= 20) {
                tryTeleportOffCenter();
                this.centerTicks = 0;
            }
        }

        private void moveToNearestLand() {
            BlockPos land = findOwnerNearbyLandOutsideRadius(8.0, 18.0);
            if (land == null) {
                land = findNearestLandBlock();
            }
            if (land == null) {
                return;
            }

            double tx = land.getX() + 0.5;
            double ty = land.getY();
            double tz = land.getZ() + 0.5;
            if (this.turret.hasTeleportModule() && this.turret.canTeleport()) {
                this.turret.moveTo(tx, ty, tz, this.turret.getYRot(), this.turret.getXRot());
                this.turret.getNavigation().stop();
                this.turret.setTeleportCooldown(this.turret.getMaxTeleportCooldown());
                this.turret.notifyTeleport();
                this.centerTicks = 0;
                return;
            }
            this.turret.getNavigation().moveTo(tx, ty, tz, 1.35);
        }

        private BlockPos findOwnerNearbyLandOutsideRadius(double minRadius, double maxRadius) {
            if (this.owner == null) return null;
            Level level = this.turret.level();
            BlockPos ownerPos = this.owner.blockPosition();
            BlockPos best = null;
            double bestDistToTurret = Double.MAX_VALUE;
            double minSqr = minRadius * minRadius;
            double maxSqr = maxRadius * maxRadius;

            int max = (int)Math.ceil(maxRadius);
            for (int dx = -max; dx <= max; dx++) {
                for (int dz = -max; dz <= max; dz++) {
                    double radialSqr = dx * dx + dz * dz;
                    if (radialSqr <= minSqr || radialSqr > maxSqr) continue;
                    for (int dy = 3; dy >= -4; dy--) {
                        BlockPos feet = ownerPos.offset(dx, dy, dz);
                        if (!isDryStandable(level, feet)) continue;
                        double distToTurret = feet.distSqr(this.turret.blockPosition());
                        if (distToTurret < bestDistToTurret) {
                            bestDistToTurret = distToTurret;
                            best = feet;
                        }
                    }
                }
            }
            return best;
        }

        private BlockPos findNearestLandBlock() {
            Level level = this.turret.level();
            BlockPos origin = this.turret.blockPosition();
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;

            for (int dx = -6; dx <= 6; dx++) {
                for (int dz = -6; dz <= 6; dz++) {
                    for (int dy = 3; dy >= -4; dy--) {
                        BlockPos feet = origin.offset(dx, dy, dz);
                        if (!isDryStandable(level, feet)) {
                            continue;
                        }

                        double dist = feet.distSqr(origin);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = feet;
                        }
                    }
                }
            }
            return best;
        }

        private boolean isDryStandable(Level level, BlockPos feet) {
            BlockPos head = feet.above();
            BlockPos ground = feet.below();
            BlockState feetState = level.getBlockState(feet);
            BlockState headState = level.getBlockState(head);
            BlockState groundState = level.getBlockState(ground);
            boolean hasRoom = !feetState.blocksMotion() && !headState.blocksMotion();
            boolean drySpace = feetState.getFluidState().isEmpty() && headState.getFluidState().isEmpty();
            boolean solidGround = groundState.blocksMotion() && groundState.getFluidState().isEmpty();
            return hasRoom && drySpace && solidGround;
        }

        private void tryTeleportOffCenter() {
            if (!this.turret.hasTeleportModule()) return;
            if (!this.turret.canTeleport()) return;
            if (this.owner == null) return;

            net.minecraft.world.phys.Vec3 look = this.owner.getLookAngle().normalize();
            net.minecraft.world.phys.Vec3 right = look.cross(new net.minecraft.world.phys.Vec3(0.0, 1.0, 0.0));
            if (right.lengthSqr() < 1.0E-6) {
                right = new net.minecraft.world.phys.Vec3(1.0, 0.0, 0.0);
            } else {
                right = right.normalize();
            }

            net.minecraft.world.phys.Vec3 sideA = this.owner.position().add(right.scale(6.0)).add(look.scale(-2.0));
            net.minecraft.world.phys.Vec3 sideB = this.owner.position().add(right.scale(-6.0)).add(look.scale(-2.0));
            net.minecraft.world.phys.Vec3 chosen = this.turret.distanceToSqr(sideA.x, sideA.y, sideA.z)
                    > this.turret.distanceToSqr(sideB.x, sideB.y, sideB.z) ? sideA : sideB;

            BlockPos pos = new BlockPos((int)chosen.x, (int)this.owner.getY(), (int)chosen.z);
            int safeY = this.turret.findSafeY(pos);
            if (safeY == -999) {
                return;
            }

            this.turret.moveTo(chosen.x, safeY, chosen.z, this.turret.getYRot(), this.turret.getXRot());
            this.turret.getNavigation().stop();
            this.turret.setTeleportCooldown(this.turret.getMaxTeleportCooldown());
            this.turret.notifyTeleport();
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
        // �?[Fix] 记录卡召唤的实体禁止加入小队
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
    // �?新增：坚守模式下的原地巡�?AI
    // ==========================================
    static class StationaryWanderGoal extends net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal {
        private final SkeletonTurret turret;

        public StationaryWanderGoal(SkeletonTurret turret, double speed) {
            super(turret, speed);
            this.turret = turret;
        }

        // 1. 只有�?[坚守模式] �?[没有目标] 时才溜达
        @Override
        public boolean canUse() {
            if (this.turret.isFollowing()) return false; // 跟随模式下禁止乱�?
            if (this.turret.getTarget() != null) return false; // 有敌人先打敌�?
            if (this.turret.isCommandScavenging()) return false; // 捡垃圾时别乱�?

            return super.canUse();
        }

        // 2. 限制移动范围 (3�?
        @Override
        @Nullable
        protected net.minecraft.world.phys.Vec3 getPosition() {
            // 在当前位�?(this.mob) 周围找一个点
            // 参数：水平范�?3，垂直范�?2
            return net.minecraft.world.entity.ai.util.DefaultRandomPos.getPos(this.mob, 3, 2);
        }
    }
    // ==========================================
    // �?新增：清剿模式移�?AI (已优化：150�?+ 屏蔽干扰)
    // ==========================================
    // ==========================================
    // �?新增：清剿模式移�?AI (已修复：分段导航 + 150�?
    // ==========================================
// ==========================================
    // �?新增：清剿模�?AI (猎杀版：主动索敌 + 150格边�?
    // ==========================================
    static class PurgeMoveGoal extends Goal {
        private final SkeletonTurret turret;

        public PurgeMoveGoal(SkeletonTurret turret) {
            this.turret = turret;
            this.setFlags(EnumSet.of(Flag.MOVE)); // 接管移动
        }

        @Override
        public boolean canUse() {
            // 只要开了模式，就必须干�?
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

            // 1. 检查最大活动范�?(150�?= 22500)
            // 如果离主人太远了，哪怕前面有怪也不能追了，必须回�?
            if (turret.distanceToSqr(owner) > 22500.0) {
                turret.teleportToSafeSpot(owner);
                turret.stopPurgeMode();
                return;
            }

            // 2. 如果已经锁定了攻击目标，就交给战�?AI 处理
            if (turret.getTarget() != null && turret.getTarget().isAlive()) {
                return;
            }

// ==================== �?猎杀雷达 (已修�? ====================

            // �?第一步：先算出我要扫描多�?(提出来写)
            double scanRange = Math.max(32.0, turret.getAttackRange());

            // �?第二步：再把算出来的距离放进去用
            List<LivingEntity> enemies = turret.level().getEntitiesOfClass(LivingEntity.class,
                    turret.getBoundingBox().inflate(scanRange),
                    e -> isValidTarget(e)
            );

            // 如果发现了敌�?
            if (!enemies.isEmpty()) {
                // 找最近的一�?
                enemies.sort(Comparator.comparingDouble(turret::distanceToSqr));
                LivingEntity prey = enemies.get(0);

                // 冲过去！(速度 1.4，比平时�?
                turret.getNavigation().moveTo(prey, 1.4);
                return;
            }
            // ====================================================================

            // 3. 附近没怪了？继续执行地毯式搜索 (往 150 格边界走)
            if (turret.getNavigation().isDone()) {
                double rad = Math.toRadians(turret.purgeSearchAngle); // 之前分配的角�?

                // 计算 150 格远处的终点
                double finalX = owner.getX() + Math.cos(rad) * 150.0;
                double finalZ = owner.getZ() + Math.sin(rad) * 150.0;

                // 计算我现在离终点还有多远
                double dx = finalX - turret.getX();
                double dz = finalZ - turret.getZ();
                double distToFinal = Math.sqrt(dx * dx + dz * dz);

                // 每次只往前推�?16 �?(分段导航，防止寻路失�?
                double step = Math.min(distToFinal, 16.0);
                double nextX = turret.getX() + (dx / distToFinal) * step;
                double nextZ = turret.getZ() + (dz / distToFinal) * step;

                turret.getNavigation().moveTo(nextX, owner.getY(), nextZ, 1.3);
            }
        }

        // 🛡�?敌我识别过滤�?(把朋友排除掉)
        private boolean isValidTarget(LivingEntity e) {
            if (e == turret) return false; // 别打自己
            if (!e.isAlive()) return false; // 别鞭�?
            if (e instanceof Player) return false; // 别打�?
            if (e instanceof SkeletonTurret) return false; // 别打队友
            if (e instanceof net.minecraft.world.entity.decoration.ArmorStand) return false; // 别打架子

            // 别打我们认证过的友军僵尸/苦力�?
            if (e.getPersistentData().getBoolean("IsFriendlyZombie")) return false;
            if (e.getPersistentData().getBoolean("IsFriendlyCreeper")) return false;

            // 其他所有能动的东西 (僵尸、骷髅、猪、羊、村�?..)，全部视为猎物！
            return true;
        }

    }

    // ==========================================
    // �?新增：战术拉�?AI (拒绝贴脸，保�?3.5 格距�?
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
            this.setFlags(EnumSet.of(Flag.MOVE)); // 接管移动控制�?
        }

        @Override
        public boolean canUse() {
            // 🛑 1. 坚守模式 (Guard Mode) 检�?
            // 如果不是跟随状�?(即坚�?，绝对不动！死守原地�?
            if (!turret.isFollowing()) return false;

            // 🛑 2. 如果正在被玩家强制救援，也不要乱�?
            if (turret.isCommandRescue()) return false;

            // 3. 扫描周围 (range) 范围内的怪物
            List<Monster> enemies = turret.level().getEntitiesOfClass(Monster.class,
                    turret.getBoundingBox().inflate(range, 2.0, range),
                    e -> e != turret && e.isAlive() && !isFriendly(e)
            );

            if (enemies.isEmpty()) return false;

            // 4. 找到最近的一个，确立为躲避目�?
            // (简单的排序，找最近的)
            enemies.sort(Comparator.comparingDouble(turret::distanceToSqr));
            this.toAvoid = enemies.get(0);

            return true;
        }

        @Override
        public void start() {
            if (this.toAvoid == null) return;

            // 5. 计算撤退路径 (向反方向�?6 �?
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
    // 🛡�?冗余设计：排斥力场白名单
    // ==========================================
    private boolean isImmuneToPush(LivingEntity e) {
        // 1. 绝对排除：我自己
        if (e == this) return true;

        // 2. 绝对排除：玩�?(哪怕是敌人也不要乱推，体验不好，除非你想做PVP)
        if (e instanceof Player) return true;

        // 3. 绝对排除：同�?(所有炮�?
        if (e instanceof SkeletonTurret) return true;

        // 4. 【针对你的需求】：排除所有骷髅家族成�?
        // AbstractSkeleton 包含了：普通骷髅、流浪者、凋灵骷�?
        // �?只要你未来的“近战骷髅”继承自 Skeleton �?AbstractSkeleton，这里自动生效！
        if (e instanceof net.minecraft.world.entity.monster.AbstractSkeleton) return true;

        // 5. 排除铁傀儡和其他已知友军 (之前逻辑里的)
        if (e instanceof net.minecraft.world.entity.animal.IronGolem) return true;
        if (e.getPersistentData().getBoolean("IsFriendlyZombie")) return true;
        if (e.getPersistentData().getBoolean("IsFriendlyCreeper")) return true;

        // 6. 【冗余接�?- NBT标签】：终极扩展方案
        // 如果你以后做了一个“地狱火恶魔”，它不是骷髅类，但你也不想推它
        // 只需要在那只怪生成时写一句：entity.getPersistentData().putBoolean("TurretAlly", true);
        if (e.getPersistentData().getBoolean("TurretAlly")) return true;

        // 7. 【冗余接�?- 骑乘判断�?
        // 如果这个怪骑着我，或者我骑着它，别推
        if (this.hasPassenger(e) || e.hasPassenger(this)) return true;

        // 如果以上都不是，那就是可以推开的杂�?
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

    // 如果你需要设�?XP 的方法：
    public void setXp(int amount) {
        this.entityData.set(DATA_XP, amount);
    }



    // 3. 获取射击延迟 (Tick) - 核心算法
    public float getFireDelay() {
        int tier = getTier();

        // 基础冷却: 随着等级提升而降�?(20 -> 17 -> 14 -> 11 -> 8 -> 5)
        double cooldown = Math.max(5.0, 20.0 - (tier * 3.0));

        // 攻速叠加层�?(0.075 -> 7.5% per stack)
        // 满级 120�?-> +900% (10倍�?
        double stackMultiplier = 1.0 + (this.entityData.get(DATA_HEAT) * 0.075);
        cooldown /= stackMultiplier;

        // 狂暴模式 4倍�?
        if (this.entityData.get(IS_BRUTAL)) {
            cooldown /= 4.0;
        }

        // �?应用攻速属性加�?(Attribute Modifier)
        double attrSpeed = this.getAttributeValue(Attributes.ATTACK_SPEED);
        // 如果攻速属�?> 1.0 (比如有加速buff)，则冷却时间缩短
        if (attrSpeed > 0) {
            cooldown /= attrSpeed;
        }

        return (float) Math.max(1.0, cooldown);
    }

    // ==========================================
    // 🧠 自定义跟�?AI (适配 Skeleton)
    // ==========================================
    // (已移除重复且错误�?Goal 代码�?





    // (Method removed)





}





