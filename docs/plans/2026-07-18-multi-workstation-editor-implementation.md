# Multi-Workstation Recipe Editor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在工作台编辑器左侧增加六种原版工作方块切换，并为每种类型生成正确的 KubeJS 配方。

**Architecture:** 使用 `WorkstationType` 描述类型、图标和输入数量；`RecipeEditorSession` 为每种类型维护独立状态；`RecipeDefinition` 和 `KubeJsRecipeGenerator` 根据类型验证并生成脚本。`RecipeEditorUi` 构建固定侧栏和六个可切换的原版风格编辑面板。

**Tech Stack:** Java 25、NeoForge 26.1.2、KubeJS 8.0.3、JEI、LDLib2。

---

### Task 1: 工作方块类型与通用配方模型

**Files:**
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/WorkstationType.java`
- Modify: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/RecipeDefinition.java`

1. 定义六种类型、原版图标物品和输入槽数量。
2. 扩展配方定义以保存类型、经验和烹饪时间。
3. 按类型验证输入数量、必填槽和炉子数值。

### Task 2: 多类型会话状态

**Files:**
- Modify: `src/main/java/com/github/jun8848/kubejsrecipeui/client/RecipeEditorSession.java`

1. 使用 `EnumMap` 为每种类型创建独立输入、输出和参数状态。
2. 将槽位读写 API 改为显式接收类型，避免隐藏面板绑定到错误状态。
3. 保存当前类型与当前选中原料槽，并据此读取标签和数据组件。

### Task 3: KubeJS 代码生成

**Files:**
- Modify: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/KubeJsRecipeGenerator.java`

1. 保留现有工作台有序和无序生成。
2. 增加三种炉子、切石和锻造调用。
3. 为炉子追加经验和时间链式设置。

### Task 4: 左侧类型切换栏

**Files:**
- Modify: `src/main/java/com/github/jun8848/kubejsrecipeui/client/RecipeEditorUi.java`

1. 将根布局改为左侧栏和主编辑区。
2. 使用 `ItemStackTexture` 创建六个方块图标按钮和工具提示。
3. 切换时更新标题、面板显示和工作台模式按钮状态。

### Task 5: 原版风格编辑面板

**Files:**
- Modify: `src/main/java/com/github/jun8848/kubejsrecipeui/client/RecipeEditorUi.java`

1. 抽取可复用幻影输入、输出槽和箭头组件。
2. 工作台构建 3x3 布局；炉子、切石机和锻造台按原版槽位关系构建。
3. 炉子面板加入经验与时间数值字段。
4. 所有输入槽继续支持原料设置弹窗。

### Task 6: 本地化

**Files:**
- Modify: `src/main/resources/assets/kubejs_recipe_ui/lang/zh_cn.json`
- Modify: `src/main/resources/assets/kubejs_recipe_ui/lang/en_us.json`

1. 添加六种编辑器标题、工具提示、经验和时间字段文本。
2. 保持 JSON 为 UTF-8 并验证语法。

### Task 7: 构建与客户端验证

1. 运行 `gradlew build -x test`，预期 `BUILD SUCCESSFUL`。
2. 确认没有现存开发客户端，再运行一次 `gradlew runClient`。
3. 实际检查六个切换按钮、各面板布局、状态保留和每类生成代码。
