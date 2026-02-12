# Test Setup for Resurrection Gravity Fix
# 1. Clear inventory and give Death Record
clear @p
give @p examplemod:death_record_card{Version:1,Data:{UnitID:101,RangeLevel:5,Tier:2,Level:10,XP:500,IsBrutal:1b,Inventory:[{Slot:0,id:"minecraft:arrow",Count:64b}],Equipment:[{SlotName:"mainhand",id:"minecraft:bow",Count:1b},{SlotName:"chest",id:"minecraft:diamond_chestplate",Count:1b}]},Checksum:"test"} 1

# 2. Build a platform to test spawning
fill ~2 ~ ~ ~4 ~ ~ minecraft:stone
fill ~2 ~1 ~ ~4 ~1 ~ minecraft:air

tellraw @p ["",{"text":"[Resurrection Gravity Test]","color":"green"},{"text":"\n1. Right click the stone platform with the Record Card","color":"yellow"},{"text":"\n2. Observe if the Turret falls to the ground or floats","color":"aqua"},{"text":"\n3. Turret should spawn with Diamond Chestplate and Bow","color":"yellow"}]
