# ==========================================
# 💀 死亡记录卡系统 - 集成测试脚本
# ==========================================

# 1. 清理旧测试实体
kill @e[type=examplemod:skeleton_turret,distance=..50]
kill @e[type=item,distance=..50,nbt={Item:{id:"examplemod:death_record_card"}}]

# 2. 召唤测试对象 (Lv.5, Tier 3, 携带物品)
summon examplemod:skeleton_turret ~ ~ ~ {CustomName:'{"text":"[测试对象] 007"}',UnitID:7,RangeLevel:3,TurretTier:3,Level:5,XP:1234,IsBrutal:1b,UpgradeProgress:50,KillCount:99,HandItems:[{id:"minecraft:bow",Count:1b},{id:"minecraft:shield",Count:1b}],ArmorItems:[{id:"minecraft:diamond_boots",Count:1b},{id:"minecraft:diamond_leggings",Count:1b},{id:"minecraft:diamond_chestplate",Count:1b},{id:"minecraft:diamond_helmet",Count:1b}],Inventory:[{Slot:0b,id:"minecraft:arrow",Count:64b},{Slot:1b,id:"minecraft:bone",Count:10b},{Slot:44b,id:"minecraft:apple",Count:1b}]}

# 3. 提示信息
tellraw @p ["",{"text":"[测试系统] ","color":"green"},{"text":"测试对象已生成！","color":"white"}]
tellraw @p ["",{"text":"请执行 ","color":"white"},{"text":"/kill @e[type=examplemod:skeleton_turret,limit=1,sort=nearest]","color":"red","clickEvent":{"action":"run_command","value":"/kill @e[type=examplemod:skeleton_turret,limit=1,sort=nearest]"}},{"text":" 模拟死亡","color":"white"}]

# 4. 后续验证步骤
tellraw @p ["",{"text":"[验证步骤]","color":"gold"}]
tellraw @p ["",{"text":"1. 捡起掉落的记录卡","color":"yellow"}]
tellraw @p ["",{"text":"2. 鼠标悬停查看信息 (应显示 ID:7, Lv.5, 物品等)","color":"yellow"}]
tellraw @p ["",{"text":"3. 右键地面使用记录卡","color":"yellow"}]
tellraw @p ["",{"text":"4. 检查复活的骷髅是否拥有相同的装备和属性","color":"yellow"}]
