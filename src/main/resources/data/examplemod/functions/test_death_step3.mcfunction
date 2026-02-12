# Check if it's gone
execute unless entity @e[type=examplemod:skeleton_turret,name=DeathTest] run tellraw @a {"text":"[Pass 2/2] Entity removed successfully.","color":"green"}
execute if entity @e[type=examplemod:skeleton_turret,name=DeathTest] run tellraw @a {"text":"[Fail 2/2] Entity still stuck!","color":"red"}
# Cleanup if failed
kill @e[type=examplemod:skeleton_turret,name=DeathTest]