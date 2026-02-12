# 骷髅炮台射程增强 Datapack

本 Datapack 包含用于测试和修改 `examplemod:skeleton_turret` 射程等级的功能函数。

## 功能介绍

该更新为骷髅炮台引入了 5 级射程系统，最大射程可达 256 格。

### 射程分级表

| 等级 | 射程 (格) | 弹道速度 | 适用场景 |
| :--- | :--- | :--- | :--- |
| **1** | 20 | 3.0 | 默认，近距离防御 |
| **2** | 32 | 3.5 | 中距离压制 |
| **3** | 64 | 4.5 | 远距离狙击 |
| **4** | 128 | 6.0 | 超视距打击 |
| **5** | 256 | 8.0 | 洲际导弹级覆盖 |

## 安装步骤

1. 将本文件夹内容放入你的世界存档的 `datapacks` 文件夹中。
2. 进入游戏，输入 `/reload` 重新加载数据包。

## 命令用法

### 修改射程等级

选中骷髅炮台（或让其执行命令），运行以下函数：

- `/function examplemod:set_range_1` - 设置为等级 1 (20格)
- `/function examplemod:set_range_2` - 设置为等级 2 (32格)
- `/function examplemod:set_range_3` - 设置为等级 3 (64格)
- `/function examplemod:set_range_4` - 设置为等级 4 (128格)
- `/function examplemod:set_range_5` - 设置为等级 5 (256格)

也可以直接使用 NBT 修改命令：
`/data merge entity @e[type=examplemod:skeleton_turret,limit=1,sort=nearest] {RangeLevel:5}`

### 测试环境搭建

运行以下命令，将在当前位置生成一个 5 级射程的炮台，并在 X 轴正方向每 32 格生成一个盔甲架靶子，最远至 256 格。

`/function examplemod:test_range_setup`

## 已知兼容性问题

- **OptiFine**: 若开启了“快速渲染”或“动态更新”，极远距离（>128格）的实体可能不渲染，建议调高视距。
- **其他 AI 模组**: 若安装了修改骷髅 AI 的模组，可能会覆盖本模组的射程逻辑，请调整加载顺序。

## 开发者说明

- 新增 NBT 标签: `RangeLevel` (Int, 1-5)
- 默认值: 1 (兼容旧存档)
