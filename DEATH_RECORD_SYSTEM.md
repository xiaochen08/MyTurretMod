# 💀 死亡记录卡系统 (Death Record System) - 技术文档

## 1. 系统概述
本系统旨在解决骷髅炮台死亡后的数据持久化问题。当炮台死亡时，系统会自动生成一张包含其**完整状态数据**的记录卡（物品），玩家可以使用该记录卡复活（重构）具有相同属性、装备和等级的炮台。

## 2. 核心功能
*   **完整数据快照**：记录基础属性、等级、经验、热度、背包物品、穿戴装备等。
*   **数据冗余备份**：在 NBT 中同时存储 `Data` (主数据) 和 `Backup` (备份数据)，防止单点数据损坏。
*   **完整性校验**：使用 `Checksum` (校验和) 验证数据是否被篡改或损坏。
*   **版本兼容性**：内置 `Version` 字段，支持未来数据结构升级。
*   **复活无损还原**：复活后的实体将完全继承死亡时的状态（除了位置和当前生命值）。

## 3. 数据结构 (NBT)
记录卡物品 (`examplemod:death_record_card`) 的 `tag` 结构如下：

```json
{
  "Version": 1,             // [Int] 数据版本号
  "Checksum": "1a2b3c...",  // [String] Data 标签的哈希校验和
  "Data": {                 // [Compound] 主数据区
    "UnitID": 101,          // [Int] 唯一编号
    "RangeLevel": 3,        // [Int] 射程等级 (1-5)
    "Tier": 2,              // [Int] 炮台阶级
    "Level": 5,             // [Int] 当前等级
    "XP": 1234,             // [Int] 当前经验
    "Heat": 0,              // [Int] 热度/攻速层数
    "IsBrutal": true,       // [Boolean] 是否狂暴
    "UpgradeProgress": 50,  // [Int] 升级进度
    "KillCount": 99,        // [Int] 击杀数
    "Inventory": [          // [List] 背包物品列表
      {
        "Slot": 0,
        "id": "minecraft:arrow",
        "Count": 64
      },
      ...
    ],
    "Equipment": [          // [List] 装备槽位列表
      {
        "SlotName": "mainhand",
        "id": "minecraft:bow",
        "Count": 1
      },
      ...
    ]
  },
  "Backup": { ... }         // [Compound] Data 的完整副本 (用于灾难恢复)
}
```

## 4. 关键类与方法

### 4.1. `SkeletonTurret.java` (实体类)
*   **`die(DamageSource source)`**: 
    *   拦截死亡事件。
    *   收集所有状态数据。
    *   构建 `MasterTag` (包含 Data, Backup, Checksum)。
    *   生成并掉落记录卡物品。
    *   全服广播死亡/掉落信息。
*   **`restoreFromRecord(CompoundTag dataTag)`**:
    *   **核心恢复逻辑**。
    *   接收 `Data` 标签。
    *   解析并覆盖当前实体的基础属性。
    *   清空并重建背包 (`Inventory`)。
    *   清空并重建装备栏 (`Equipment`)。
    *   调用 `updateStatsAndEquip()` 刷新属性修饰符。

### 4.2. `DeathRecordItem.java` (物品类)
*   **`useOn(UseOnContext context)`**:
    *   **核心复活逻辑**。
    *   读取物品 NBT。
    *   **Step 1**: 检查 `Version`。
    *   **Step 2**: 计算 `Data` 的哈希值并与 `Checksum` 比对。
    *   **Step 3**: 若校验失败，尝试读取 `Backup` 并再次校验。
    *   **Step 4**: 若校验通过，召唤新 `SkeletonTurret` 实体。
    *   **Step 5**: 调用 `turret.restoreFromRecord(data)` 注入数据。
    *   **Step 6**: 初始化状态 (满血、非跟随模式、重置AI)。
*   **`appendHoverText(...)`**:
    *   在物品提示栏显示 ID、等级、射程等级、物品数量及“已启用冗余备份”标识。

## 5. 测试方法
已提供集成测试脚本：`data/examplemod/functions/test_record_system.mcfunction`

**使用步骤**:
1.  进入游戏，执行 `/function examplemod:test_record_system`。
2.  系统会生成一只携带大量装备和物品的 Lv.5 测试炮台。
3.  点击聊天栏中的红色指令 (或手动执行 `/kill ...`) 杀死炮台。
4.  捡起掉落的记录卡，观察物品提示信息（应显示 ID、等级等）。
5.  右键地面使用记录卡。
6.  **验证**: 复活的炮台是否拥有相同的装备（如钻石甲）、背包物品（如骨头、苹果）和属性（等级5）。

## 6. 异常处理
*   **数据损坏**: 如果 `Data` 校验失败且 `Backup` 也无效，系统会提示“数据损坏且无备份”，拒绝复活。
*   **版本不兼容**: 如果记录卡版本过旧（且未实现迁移逻辑），会提示“版本不兼容”。
*   **空卡**: 使用未初始化的记录卡会提示错误。
