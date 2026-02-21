# 详细更新报告（技术版）

## 1. 报告范围
本报告覆盖本次会话内完成的核心玩法与兼容改动，目标是：
- 修复跨模组冲突（Carry On / Healing / Guard Villagers）。
- 修复队长显示乱码与样式不一致问题。
- 增加模式互斥增益并在游戏内告知玩家。
- 保持改动可回溯、可验证。

## 2. 时间序列改动清单

### [001] Carry On 守卫锚定冲突修复
文件：`src/main/java/com/example/examplemod/SkeletonTurret.java`
- 重写 `enforceGuardFreeze()`：
  - 若炮台处于 `isPassenger()`，立即暂停锚定并返回。
  - 移除守卫冻结中每 tick 清仇恨逻辑（不再 `setTarget(null)`）。
  - 保留原有导航停止、乘客处理、动能清零、坐标锁定逻辑。
- 新增“重定位重锚”判断，避免外部搬运后被强拉回旧坐标。

### [002] Carry On 交互放行（避免右键被实体吞掉）
文件：`src/main/java/com/example/examplemod/SkeletonTurret.java`
- `mobInteract(...)`：潜行右键返回 `InteractionResult.PASS`。
- 新增 `interactAt(...)`：潜行右键同样返回 `PASS`。
- 目的：让 Carry On 优先接管交互流程。

### [003] 搬运期静默与状态保护
文件：`src/main/java/com/example/examplemod/SkeletonTurret.java`
- 在主 `tick()` 前段增加 `isPassenger()` 分支：
  - 停止导航、清目标、清速度、重置锚定有效位并提前返回。
- 目的：搬运期间不触发战斗/位移链路，减少异常声音与行为干扰。

### [004] Carry On 生存模式关键兼容
文件：`src/main/java/com/example/examplemod/ExampleMod.java`
- 实体注册类别：`SkeletonTurret` 从 `MobCategory.MONSTER` 调整为 `MobCategory.CREATURE`。
- 背景：Carry On 生存默认禁止携带 hostile mob（MONSTER）。
- 效果：生存模式下可正常搬运炮台。

### [005] 治疗模组兼容（脱离亡灵、保留机械免疫）
文件：`src/main/java/com/example/examplemod/SkeletonTurret.java`
- 新增 `getMobType()` -> `MobType.UNDEFINED`。
- 新增 `canBeAffected(MobEffectInstance)`：免疫 `POISON` / `HUNGER`。
- 效果：治疗法术与治愈营火会正常治疗炮台，不再反伤。

### [006] 队长标识乱码与样式统一修复
文件：`src/main/java/com/example/examplemod/SkeletonTurret.java`
- `updateCustomName()` 改为组件化拼接：
  - `[队伍]` 前缀保留青色、去掉加粗。
  - 仅队长追加黄色王冠（`👑`）。
- 使用 Unicode 转义规避编码污染。

文件：`src/main/java/com/example/examplemod/ExampleMod.java`
- 战术面板名称清洗增加兜底：
  - 去除 `\uFFFD`。
  - 去除前导 `?`。

### [007] Guard Villagers 互不敌对兼容
文件：`src/main/java/com/example/examplemod/ExampleMod.java`
- 新增 `isGuardVillagerEntity(Entity)`：识别 `guardvillagers:*` 命名空间实体。
- 新增 `isAllianceFriendly(Entity)`：统一友军判定。
- `onLivingHurt` 与 `onLivingChangeTarget` 使用统一判定。
- 炮台箭矢命中逻辑忽略 Guard Villagers。
- `onLivingTick` 增加 hard-stop：若 Guard 与炮台已互设目标，每 tick 清目标。

### [008] 新手礼包加入死灵移动终端
文件：`src/main/java/com/example/examplemod/ExampleMod.java`
- 玩家首次礼包新增：`NECRO_TERMINAL x1`。
- 保持原礼包发放标记逻辑不变。

### [009] 新增模式互斥增益 + 游戏内提示
文件：`src/main/java/com/example/examplemod/SkeletonTurret.java`
- 新增常量：
  - 跟随模式移速加成 `+40%`（属性修饰器）。
  - 守卫模式承伤系数 `0.40`（即减伤 60%）。
- 新增 `refreshModeBuffs()`：切换模式时刷新移速增益。
- 在 `hurt(...)` 中注入守卫减伤逻辑。
- 在 `setFollowMode(...)` 中给玩家发送即时提示（互斥关系明确）。

## 3. 玩家可感知变化
- 搬运：可搬、不回弹、少噪音。
- 治疗：营火/治疗法术恢复正常。
- 联动：Guard Villagers 不再攻击炮台。
- UI：队长王冠正常，颜色与战术面板一致。
- 模式：跟随更快、守卫更硬，互斥且有提示。

## 4. 验证结果
- 多次执行：`./gradlew.bat compileJava -q`
- 结果：编译通过（存在过时 API 警告，无编译错误）。

## 5. 发布建议
1. 发布前重启服务端与客户端，确保实体注册与事件监听刷新。
2. 老存档在线单位建议切换一次模式，立即刷新增益状态。
3. 若有历史乱码名字，可执行一次统一重命名/刷新流程（可后续补命令）。
