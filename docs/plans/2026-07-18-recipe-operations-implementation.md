# Recipe Operations Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为工作台编辑器增加删除配方、替换输入、替换输出和自定义 JSON，并与现有新增配方共同组成五种操作模式。

**Architecture:** 新增不可变的配方筛选与操作数据模型，由独立生成器输出完整的 `ServerEvents.recipes` 代码。LDLib 界面通过一个侧栏入口打开模态编辑器，按操作模式切换表单；生成结果复用现有预览、剪贴板和服务端写入流程。

**Tech Stack:** Java 25、NeoForge 26.1.2、KubeJS 8.0.3、LDLib2、Minecraft 客户端运行配置。

---

### Task 1: 配方操作模型

**Files:**
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/RecipeOperation.java`
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/RecipeFilterSpec.java`
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/RecipeOperationRequest.java`

**Step 1:** 定义新增、删除、替换输入、替换输出、自定义 JSON 五种操作。

**Step 2:** 定义 ID、类型、模组、输入、输出筛选字段，并提供是否为空的判断。

**Step 3:** 定义操作请求，保存筛选条件、匹配材料、替换材料和自定义 JSON。

### Task 2: 操作代码生成器

**Files:**
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/RecipeOperationGenerator.java`

**Step 1:** 将非空筛选字段生成 KubeJS 筛选对象。

**Step 2:** 生成 `event.remove`、`event.replaceInput` 和 `event.replaceOutput`。

**Step 3:** 校验自定义 JSON，并生成 `event.custom`。

**Step 4:** 对空筛选删除和空替换参数给出明确错误，避免误删全部配方。

### Task 3: LDLib 操作编辑器

**Files:**
- Modify: `src/main/java/com/github/jun8848/kubejsrecipeui/client/RecipeEditorUi.java`

**Step 1:** 在侧栏增加配方修改器按钮。

**Step 2:** 增加操作模式下拉框和通用筛选字段。

**Step 3:** 根据模式显示替换字段或 JavaScript/JSON 编辑器。

**Step 4:** 接入重新生成、复制、写入按钮和现有 RPC 写入服务。

### Task 4: 界面语言

**Files:**
- Modify: `src/main/resources/assets/kubejs_recipe_ui/lang/zh_cn.json`
- Modify: `src/main/resources/assets/kubejs_recipe_ui/lang/en_us.json`

**Step 1:** 增加编辑器标题、模式名、筛选字段和错误提示中文。

**Step 2:** 增加对应英文翻译，避免非中文客户端显示语言键。

### Task 5: 验证

**Files:**
- Verify: `src/main/java/com/github/jun8848/kubejsrecipeui/client/RecipeEditorUi.java`
- Verify: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/RecipeOperationGenerator.java`

**Step 1:** 运行 `gradlew.bat build -x test --console=plain`，预期 `BUILD SUCCESSFUL`。

**Step 2:** 确认没有旧客户端后运行 `gradlew.bat runClient --console=plain`。

**Step 3:** 打开配方修改器，逐一切换删除、替换输入、替换输出、自定义 JSON，确认界面字段和生成代码正确。
