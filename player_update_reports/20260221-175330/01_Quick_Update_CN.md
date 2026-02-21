# 更新公告

版本号：1.4.0.18

本次更新重点修复了多个联动兼容问题，并新增了模式增益系统：

1. Carry On（搬运）兼容修复
- 生存模式可正常搬运骷髅守卫。
- 搬运时不会再出现守卫模式“拉回原点/模型错位”的问题。
- 搬运期间异常行为与噪音已抑制。

2. 治疗类模组兼容修复（Healing Campfire 等）
- 骷髅守卫不再按亡灵判定，治疗效果将正常回血。
- 同时保留机械体免疫：中毒、饥饿无效。

3. Guard Villagers 兼容修复
- 警卫村民与骷髅守卫已设为友军关系。
- 双方不会再互相锁敌、互相伤害。

4. 头顶显示优化
- 修复队长标识乱码。
- 头顶“队伍”前缀取消加粗。
- 队长标记改为黄色王冠，且仅队长显示。

5. 新手礼包调整
- 首次新手礼包新增：死灵移动终端（1个）。

6. 新增模式互斥增益（含游戏内提示）
- 阵地/守卫模式：承伤 -60%。
- 跟随/机动模式：移速 +40%。
- 两者互斥，不可同时生效。
- 切换模式时会给玩家发送即时提示。

注意：
- 请重启客户端/服务端后生效。
- 旧单位若需刷新状态，切一次模式即可立即套用最新规则。

---

# Update Notes

Version: 1.4.0.18

This update focuses on cross-mod compatibility fixes and introduces an exclusive mode buff system:

1. Carry On compatibility fixes
- Skeleton Turrets can now be carried normally in Survival mode.
- Fixed the issue where Guard mode could snap the turret back to its old position after carrying.
- Suppressed abnormal behavior and noise during carrying.

2. Healing mod compatibility fixes (e.g. Healing Campfire)
- Skeleton Turrets are no longer treated as undead, so healing effects now heal correctly.
- Mechanical immunities are preserved: Poison and Hunger are ignored.

3. Guard Villagers compatibility fixes
- Guard Villagers and Skeleton Turrets are now treated as allies.
- They no longer target or damage each other.

4. Overhead display optimization
- Fixed corrupted captain marker text.
- Removed bold style from the overhead "[队伍]" prefix.
- Captain marker is now a yellow crown, shown only for captains.

5. Starter kit adjustment
- Added Necro Mobile Terminal x1 to first-time starter kit.

6. New exclusive mode buffs (with in-game prompt)
- Guard mode: -60% damage taken.
- Follow mode: +40% movement speed.
- These two buffs are mutually exclusive.
- Players receive immediate prompts when mode is switched.

Notes:
- Restart client/server for full effect.
- For existing units, toggle mode once to refresh and apply latest rules immediately.
