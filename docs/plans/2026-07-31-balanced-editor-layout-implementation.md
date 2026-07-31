# 平衡三栏编辑器布局实施计划

**目标：** 将现有 LDLib2 配方编辑器调整为稳定、紧凑的三栏布局，并保证最大 9×9 配方网格不会与玩家背包重叠。

**布局：** 根窗口使用紧凑的 `600×360` 尺寸。正文左侧为 34 像素工作站图标栏；主区域按 `64/36` 分隔固定配方/背包画布与右侧模式区；右侧使用标签页在配方属性、代码预览和 JEI 来源三种全尺寸模式间切换。

**技术栈：** Java 25、NeoForge、LDLib2 ModularUI、KubeJS、JEI

---

### 任务 1：稳定中央编辑区

**文件：**
- 修改：`src/main/java/com/github/jun8848/kubejsrecipeui/client/RecipeEditorUi.java`

1. 将根窗口调整为 `600×360`，侧边栏调整为 34 像素。
2. 将水平分割比例调整为 `64/36`。
3. 将配方画布固定为 190 像素高并在两个方向居中小型工作站面板。
4. 将背包标签和 `162×77` 槽位移入独立的 97 像素容器。

### 任务 2：将代码区域集成到属性栏

**文件：**
- 修改：`src/main/java/com/github/jun8848/kubejsrecipeui/client/RecipeEditorUi.java`
- 修改：`src/main/resources/assets/kubejs_recipe_ui/lang/zh_cn.json`
- 修改：`src/main/resources/assets/kubejs_recipe_ui/lang/en_us.json`

1. 标题保持居中，不增加额外工具按钮。
2. 使用一个占满右栏的 `TabView` 承载属性、代码和来源模式。
3. 属性模式独立滚动，点击预览成功后自动切换到代码模式。

### 任务 3：验证

1. 运行 `gradlew.bat compileJava`。
2. 运行 `gradlew.bat build`。
3. 校验双语翻译键一致、JSON 可解析且文本文件为 UTF-8。
4. 运行 `git diff --check`。
