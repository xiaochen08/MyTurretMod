# Kill it
kill @e[type=examplemod:skeleton_turret,name=DeathTest,limit=1]

tellraw @a {"text":"[Test] Turret killed. It should play death animation for 2.5s.","color":"yellow"}

# Check if it exists immediately (should exist)
execute if entity @e[type=examplemod:skeleton_turret,name=DeathTest] run tellraw @a {"text":"[Pass 1/2] Entity remains for animation.","color":"green"}
execute unless entity @e[type=examplemod:skeleton_turret,name=DeathTest] run tellraw @a {"text":"[Fail 1/2] Entity vanished instantly!","color":"red"}

# Check again in 3 seconds (60 ticks)
schedule function examplemod:test_death_step3 60t