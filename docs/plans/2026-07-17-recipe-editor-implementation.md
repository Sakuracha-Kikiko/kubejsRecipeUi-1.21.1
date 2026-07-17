# Recipe Editor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add an LDLib2-powered crafting recipe editor block that generates and optionally writes KubeJS shaped or shapeless recipe code.

**Architecture:** A transient `RecipeEditorSession` owns the client-visible phantom stacks and pure recipe definition. A pure `KubeJsRecipeGenerator` formats validated definitions. `RecipeEditorBlock` provides an LDLib2 block UI, while `GeneratedScriptStore` performs the only filesystem mutation after server-side operator validation through an LDLib2 RPC event.

**Tech Stack:** Java 25, NeoForge 26.1.2.82, KubeJS 8.0.3, JEI 29.16.0.47, LDLib2 26.1.2.29 from the official Modrinth Maven.

---

### Task 1: Dependencies and content registration

**Files:**
- Modify: `build.gradle`
- Modify: `gradle.properties`
- Modify: `src/main/templates/META-INF/neoforge.mods.toml`
- Modify: `src/main/java/com/github/jun8848/kubejsrecipeui/KubeJsRecipeUi.java`
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/registry/ModContent.java`

1. Add the Modrinth Maven repository and LDLib2 `26.1.2.29` dependency.
2. Declare LDLib2 as a required mod dependency.
3. Register the custom block, block item, and creative tab on the mod event bus.
4. Run `./gradlew compileJava` and expect `BUILD SUCCESSFUL`.

### Task 2: Pure recipe model and generator

**Files:**
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/IngredientSpec.java`
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/OutputSpec.java`
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/RecipeDefinition.java`
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/KubeJsRecipeGenerator.java`

1. Implement immutable specs and deterministic formatting for shaped patterns, shapeless lists, tags, alternatives, component patches, output count, escaping, and invalid empty recipes.
2. Run `./gradlew compileJava` and expect `BUILD SUCCESSFUL`.

### Task 3: Script append service

**Files:**
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/GeneratedScriptStore.java`

1. Implement UTF-8 writes to a caller-provided server scripts directory, including directory creation and payload validation. Create the complete wrapper for the first recipe, then insert later event calls before the existing wrapper closes.
2. Run `./gradlew compileJava` and expect `BUILD SUCCESSFUL`.

### Task 4: LDLib2 block UI

**Files:**
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/block/RecipeEditorBlock.java`
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/client/RecipeEditorSession.java`
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/client/RecipeEditorUi.java`
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/client/ClientRecipeActions.java`

1. Implement `BlockUIMenuType.BlockUI` and open it from `useWithoutItem`.
2. Build a fixed-size vanilla-style panel with 3x3 phantom inputs, output slot, arrow, player inventory, and shaped/shapeless toggle. Copy carried inventory stacks into phantom slots without consuming them.
3. Open the settings dialog by clicking a non-empty input slot; ignore empty-slot clicks. Support exact item, tag, alternatives, automatic components, and manual component override.
4. Add a JavaScript `CodeEditor` preview dialog and clipboard action.
5. Use `ItemStackKJS.kjs$toItemString0` with registry-aware NBT ops to capture KubeJS component syntax.

### Task 5: Server write RPC and permissions

**Files:**
- Modify: `src/main/java/com/github/jun8848/kubejsrecipeui/client/RecipeEditorUi.java`
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/RecipeWriteService.java`

1. Add an LDLib2 `RPCEventBuilder.simple(String.class, String.class, ...)` event.
2. Allow integrated single-player or `PlayerList.isOp(player.nameAndId())`.
3. Write to `KubeJSPaths.SERVER_SCRIPTS/generated_recipes.js` through `GeneratedScriptStore`; create the wrapper once and insert subsequent event calls into it.
4. Return localized success or failure text to a client notification dialog.

### Task 6: Assets and localization

**Files:**
- Create: `src/main/resources/assets/kubejs_recipe_ui/blockstates/recipe_editor.json`
- Create: `src/main/resources/assets/kubejs_recipe_ui/models/block/recipe_editor.json`
- Create: `src/main/resources/assets/kubejs_recipe_ui/items/recipe_editor.json`
- Create: `src/main/resources/assets/kubejs_recipe_ui/lang/en_us.json`
- Create: `src/main/resources/assets/kubejs_recipe_ui/lang/zh_cn.json`
- Create: `src/main/resources/data/kubejs_recipe_ui/loot_table/blocks/recipe_editor.json`

1. Reuse the vanilla crafting table block model and textures.
2. Add English and Simplified Chinese names and UI messages.
3. Add a self-dropping block loot table.

### Task 7: Verification

1. Run `./gradlew build` and expect `BUILD SUCCESSFUL`.
2. Run `./gradlew runClient` directly; no unit-test task is used.
3. Verify LDLib2, JEI, KubeJS, and `kubejsRecipeUi` load without ERROR/FATAL entries.
4. Verify the creative tab contains the editor block, right-click opens the UI, ghost slots do not consume items, both recipe modes produce valid code, copy works, and the write action creates or appends the generated script.
