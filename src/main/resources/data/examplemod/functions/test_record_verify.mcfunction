# Verify FollowMode
execute as @e[name=RecordTest] if data entity @s {FollowMode:0b} run tellraw @a {"text":"[Pass] FollowMode corrected","color":"green"}
execute as @e[name=RecordTest] if data entity @s {FollowMode:1b} run tellraw @a {"text":"[Fail] FollowMode NOT corrected","color":"red"}

# Verify SquadMember
execute as @e[name=RecordTest] if data entity @s {IsSquadMember:0b} run tellraw @a {"text":"[Pass] SquadMember corrected","color":"green"}
execute as @e[name=RecordTest] if data entity @s {IsSquadMember:1b} run tellraw @a {"text":"[Fail] SquadMember NOT corrected","color":"red"}

# Verify Ground
execute as @e[name=RecordTest] if data entity @s {OnGround:1b} run tellraw @a {"text":"[Pass] Entity OnGround","color":"green"}
execute as @e[name=RecordTest] unless data entity @s {OnGround:1b} run tellraw @a {"text":"[Fail] Entity NOT OnGround","color":"red"}

# Cleanup
kill @e[name=RecordTest]