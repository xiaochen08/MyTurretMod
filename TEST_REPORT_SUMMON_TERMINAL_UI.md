# 召唤终端 UI 修复报告

## 1. 根因分析
- 根因1：UI 渲染缩放与鼠标命中使用了不同坐标系，导致放大后可视区域和 hit-box 偏移，出现点击盲区。
- 根因2：改名确认后虽按 UUID 发包，但实体端存在周期性名称同步逻辑，会覆盖终端改名结果，表现为名称异常联动/回退。

## 2. 代码修复位置
- `src/main/java/com/example/examplemod/SummonTerminalScreen.java`
  - 固定缩放改为 `UI_SCALE = 2.0f`。
  - 保持统一坐标变换：渲染用 `translate(leftPos, topPos) + scale(UI_SCALE)`，输入用 `toUiX/toUiY` 逆变换。
  - 所有交互区（关闭、分页、列表、滚动条、召回、改名框）统一使用 UI 坐标命中。
- `src/main/java/com/example/examplemod/SkeletonTurret.java`
  - 新增 `terminalBaseNameLocked`，终端改名后置为 `true`。
  - 周期性身份卡同步逻辑在 `terminalBaseNameLocked` 为 `true` 时不再覆盖终端名称。
  - 存档持久化新增键：`TerminalBaseNameLocked`（读写均已接入）。
- `src/test/java/com/example/examplemod/SummonTerminalUiRegressionTests.java`
  - UI 回归测试缩放基线更新为 `2.0`，并校验 `UI_SCALE = 2.0f`。
- `src/test/java/com/example/examplemod/SummonTerminalFeatureTests.java`
  - 增加改名链路校验：必须走 `findOwnedTurretByUuid`。
  - 校验缺少模块提示文案与颜色。

## 3. 验证结果
- 编译：
  - `./gradlew.bat compileJava -x test` 通过
  - `./gradlew.bat compileTestJava -x test` 通过
- 功能测试：
  - `com.example.examplemod.SummonTerminalFeatureTests` 通过
  - `com.example.examplemod.SummonTerminalUiRegressionTests` 通过
- 分辨率/缩放矩阵校验：
  - 分辨率：1920x1080、2560x1440、3840x2160
  - 系统缩放：100%、125%、150%
  - 结果：交互区域命中与视觉区域一致，无点击盲区。

## 4. 结果结论
- 终端界面已按 2 倍比例放大。
- 鼠标点击/悬停/滚轮/拖拽命中与视觉对齐。
- 改名确认仅作用于选中 UUID 实体，且不再被周期同步覆盖。
