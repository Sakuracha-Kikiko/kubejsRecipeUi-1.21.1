# KubeJS Event Editor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 动态列出所有 KubeJS 事件，并提供可翻译、可编辑、可写入的事件脚本界面。

**Architecture:** `KubeJsEventCatalog` 从 `EventGroups.ALL` 构造事件描述；`EventScriptGenerator` 生成监听器骨架；`EventScriptStore` 按脚本端追加文件。`RecipeEditorUi` 通过第七个侧栏按钮在同一主界面内切换到事件编辑页面。

**Tech Stack:** Java 25、KubeJS 8.0.3 EventGroups、LDLib2 Selector/CodeEditor/RPC。

---

### Task 1: 事件目录与脚本模型
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/event/EventDescriptor.java`
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/event/KubeJsEventCatalog.java`
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/event/EventScriptGenerator.java`

### Task 2: 事件文件写入
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/event/EventScriptStore.java`
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/event/EventWriteService.java`

### Task 3: LDLib 事件编辑器
- Modify: `src/main/java/com/github/jun8848/kubejsrecipeui/client/RecipeEditorUi.java`
- 添加入口按钮、两个下拉框、目标输入、代码区、重新生成、复制和写入。

### Task 4: 翻译与验证
- Modify: `src/main/resources/assets/kubejs_recipe_ui/lang/zh_cn.json`
- Modify: `src/main/resources/assets/kubejs_recipe_ui/lang/en_us.json`
- 运行 `gradlew build -x test` 和单实例 `runClient`。
