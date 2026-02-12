# ==========================================
# 🎯 射程更新系统 - 单元测试
# ==========================================

# 1. 清理环境
kill @e[type=examplemod:skeleton_turret,distance=..50]

# 2. 召唤初始炮台 (默认 Lv.1)
summon examplemod:skeleton_turret ~ ~ ~ {Tags:["range_test"],RangeLevel:1}
tellraw @p ["",{"text":"[测试] ","color":"green"},{"text":"炮台已生成 (Lv.1)，初始射程应为 20.0","color":"white"}]

# 3. 验证初始值
execute as @e[type=examplemod:skeleton_turret,tag=range_test,limit=1] run data get entity @s Attributes[{Name:"minecraft:generic.follow_range"}]

# 4. 模拟升级 (Lv.1 -> Lv.3)
tellraw @p ["",{"text":"[测试] ","color":"yellow"},{"text":"正在升级至 Lv.3 (目标射程 64.0)...","color":"white"}]
data merge entity @e[type=examplemod:skeleton_turret,tag=range_test,limit=1] {RangeLevel:3}

# 5. 验证升级后数值
execute as @e[type=examplemod:skeleton_turret,tag=range_test,limit=1] run data get entity @s Attributes[{Name:"minecraft:generic.follow_range"}]

# 6. 模拟降级 (Lv.3 -> Lv.2)
tellraw @p ["",{"text":"[测试] ","color":"yellow"},{"text":"正在降级至 Lv.2 (目标射程 32.0)...","color":"white"}]
data merge entity @e[type=examplemod:skeleton_turret,tag=range_test,limit=1] {RangeLevel:2}

# 7. 验证降级后数值
execute as @e[type=examplemod:skeleton_turret,tag=range_test,limit=1] run data get entity @s Attributes[{Name:"minecraft:generic.follow_range"}]

# 8. 异常值测试 (设置无效等级 99 -> 应回退或保持)
tellraw @p ["",{"text":"[测试] ","color":"red"},{"text":"测试无效等级 99 (应保持或默认)...","color":"white"}]
data merge entity @e[type=examplemod:skeleton_turret,tag=range_test,limit=1] {RangeLevel:99}

# 9. 验证异常处理
execute as @e[type=examplemod:skeleton_turret,tag=range_test,limit=1] run data get entity @s Attributes[{Name:"minecraft:generic.follow_range"}]

tellraw @p ["",{"text":"[测试] ","color":"green"},{"text":"测试完成！请检查上方输出是否符合预期。","color":"white"}]
