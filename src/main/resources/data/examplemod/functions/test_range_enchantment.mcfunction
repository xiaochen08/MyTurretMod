# ==========================================
# 🎯 射程一致性测试 (附魔与等级)
# ==========================================

# 1. 清理
kill @e[type=examplemod:skeleton_turret,tag=range_test_enc]

# 2. 召唤 Lv.1 炮台
summon examplemod:skeleton_turret ~ ~ ~ {Tags:["range_test_enc"],RangeLevel:1}
tellraw @p ["",{"text":"[测试] ","color":"green"},{"text":"步骤1: 召唤 Lv.1 炮台","color":"white"}]

# 3. 验证 Lv.1 射程 (应为 20.0)
execute as @e[type=examplemod:skeleton_turret,tag=range_test_enc,limit=1] run data get entity @s Attributes[{Name:"minecraft:generic.follow_range"}]

# 4. 给予附魔弓 (冲击 II, 力量 V)
tellraw @p ["",{"text":"[测试] ","color":"yellow"},{"text":"步骤2: 给予强力附魔弓...","color":"white"}]
item replace entity @e[type=examplemod:skeleton_turret,tag=range_test_enc,limit=1] weapon.mainhand with minecraft:bow{Enchantments:[{id:"minecraft:punch",lvl:2},{id:"minecraft:power",lvl:5}]}

# 5. 再次验证射程 (应仍为 20.0，附魔不应影响射程)
execute as @e[type=examplemod:skeleton_turret,tag=range_test_enc,limit=1] run data get entity @s Attributes[{Name:"minecraft:generic.follow_range"}]

# 6. 升级至 Lv.5
tellraw @p ["",{"text":"[测试] ","color":"yellow"},{"text":"步骤3: 升级至 Lv.5 (目标 256.0)...","color":"white"}]
data merge entity @e[type=examplemod:skeleton_turret,tag=range_test_enc,limit=1] {RangeLevel:5}

# 7. 验证 Lv.5 射程 (应为 256.0)
execute as @e[type=examplemod:skeleton_turret,tag=range_test_enc,limit=1] run data get entity @s Attributes[{Name:"minecraft:generic.follow_range"}]

tellraw @p ["",{"text":"[测试] ","color":"green"},{"text":"一致性测试完成！射程数值应只随等级变化，不受装备影响。","color":"white"}]
