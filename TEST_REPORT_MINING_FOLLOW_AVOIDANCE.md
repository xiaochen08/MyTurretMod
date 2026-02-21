# 骷髅跟随挖矿避让重构报告

## 1. 改动目标
- 解决玩家使用镐子在狭窄矿道挖掘时，跟随骷髅占位阻挡的问题。
- 将“避免阻挡玩家挖矿”提升为高优先级行为，覆盖默认跟随/攻击移动逻辑。

## 2. 核心实现

### 2.1 新增最高优先级挖矿避让 Goal
- 文件：`src/main/java/com/example/examplemod/SkeletonTurret.java`
- 变更：在 `registerGoals()` 中新增
  - `this.goalSelector.addGoal(1, new TurretMiningYieldGoal(this, 1.15));`
- 说明：优先级与紧急传送同级，优先于普通跟随(4)与攻击(2)移动逻辑。

### 2.2 挖矿状态识别与安全距离控制
- 文件：`src/main/java/com/example/examplemod/SkeletonTurret.java`
- 新增类：`TurretMiningYieldGoal`
- 行为点：
  - 玩家主手为镐子 + 视线命中可挖方块判定为 mining 状态。
  - 狭窄矿道（宽度<=2）检测：`isNarrowTunnel`。
  - 避让当前挖掘方块及相邻方块，至少 1 格安全距离。
  - 曼哈顿距离 <= 2 时触发强制侧移：`moveSidewaysAway`。

### 2.3 动态路径重计算
- 文件：`src/main/java/com/example/examplemod/SkeletonTurret.java`
- 逻辑：
  - 追踪玩家连续同方向挖掘进度（`continuousDirBlocks`）。
  - 超过 3 格后，自动重算至玩家后方/上方安全点（`chooseOwnerRearOrUpper`）。

### 2.4 挖掘动画窗口碰撞回退
- 文件：`src/main/java/com/example/examplemod/SkeletonTurret.java`
- 逻辑：
  - `miningPulseTicks = 16`（约 0.8 秒）窗口内暂停推进。
  - 使用 `computeRetreatPosition(..., 0.5)` 执行 0.5 格后退。

### 2.5 攻击行为让位
- 文件：`src/main/java/com/example/examplemod/SkeletonTurret.java`
- 变更：`RampUpBowAttackGoal.canUse()` 增加 mining 状态短路，玩家挖矿时暂停射击开始条件。

### 2.6 纯逻辑工具类
- 文件：`src/main/java/com/example/examplemod/MiningFollowAvoidanceLogic.java`
- 提供：方向推断、曼哈顿距离、后退向量、侧向候选、重算判定等工具函数。

## 3. 单元测试

### 3.1 场景覆盖
- 文件：`src/test/java/com/example/examplemod/MiningFollowAvoidanceLogicTests.java`
- 覆盖项：
  - 1×2 垂直矿道持续向下挖掘不卡位（源码行为覆盖检查）
  - 2×1 水平矿道横向挖掘时自动后退（源码行为覆盖检查）
  - 玩家突然转向时路径重算响应 <= 200ms（纯 Java 时延测试）

### 3.2 执行结果
- 命令：
  - `./gradlew.bat clean compileTestJava -x test`
  - `java -cp "build/classes/java/test;build/classes/java/main" com.example.examplemod.MiningFollowAvoidanceLogicTests`
- 结果：全部通过。

## 4. 性能与内存分析

### 4.1 基准脚本
- 文件：`src/test/java/com/example/examplemod/MiningAvoidancePerformanceBenchmark.java`
- 命令：
  - `java -cp "build/classes/java/test;build/classes/java/main" com.example.examplemod.MiningAvoidancePerformanceBenchmark`

### 4.2 实测数据
- `baseline_ns=17625133`
- `enhanced_ns=18393233`
- `increase_pct=4.36`
- `mem_delta_bytes=262160`（约 0.25 MB）

### 4.3 结论
- AI 计算耗时增量：`4.36%`（满足 <= 5%）
- 内存增量：`0.25 MB`（满足 < 1 MB）
