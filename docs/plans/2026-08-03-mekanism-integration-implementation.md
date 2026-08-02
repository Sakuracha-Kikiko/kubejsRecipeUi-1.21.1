# Mekanism 26.1 Recipe Integration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add optional Mekanism 26.1 recipe editing and JEI transfer support for all formal Mekanism datapack recipe serializers.

**Architecture:** Register all Mekanism workstations through the existing integration registry, store non-item resources in one immutable settings record, and reuse LDLib2 panels by recipe shape. Register JEI transfers by identifier so the base mod does not require Mekanism classes when Mekanism is absent.

**Tech Stack:** Java 25, NeoForge 26.1, KubeJS 8, LDLib2, JEI 29, Gradle ModDev.

---

### Task 1: Register Mekanism recipe types

**Files:**
- Modify: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/WorkstationType.java`
- Modify: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/integration/RecipeIntegrationRegistry.java`
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/MekanismRecipeSettings.java`
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/recipe/integration/MekanismRecipeIntegration.java`

1. Add all 26 serializer-backed workstation constants and a common navigation type.
2. Add immutable resource stacks and type-specific defaults to the settings model.
3. Register validation and code generation for each Codec shape.
4. Ensure optional availability uses `ModList.isLoaded("mekanism")`.

### Task 2: Add LDLib2 editor panels

**Files:**
- Modify: `src/main/java/com/github/jun8848/kubejsrecipeui/client/RecipeEditorSession.java`
- Modify: `src/main/java/com/github/jun8848/kubejsrecipeui/client/RecipeEditorPanelRegistry.java`
- Modify: `src/main/java/com/github/jun8848/kubejsrecipeui/client/RecipeEditorUi.java`

1. Expose typed Mekanism settings from each editor state.
2. Add the Mekanism type selector beside the existing optional integration selectors.
3. Build reusable item, chemical, fluid, energy, reaction, rotary, and sawing panels.
4. Synchronize JEI transfer settings into mounted fields.

### Task 3: Add JEI transfer support

**Files:**
- Create: `src/main/java/com/github/jun8848/kubejsrecipeui/integration/jei/MekanismJeiIntegration.java`
- Modify: `src/main/java/com/github/jun8848/kubejsrecipeui/integration/jei/KubeJsRecipeUiJeiPlugin.java`

1. Register holder recipe types using Mekanism JEI category identifiers.
2. Extract item and fluid stacks through public JEI/NeoForge APIs.
3. Extract Mekanism chemical stacks reflectively and normalize registry IDs.
4. Fill source ID, serializer ID, item slots, outputs, and specialized settings.

### Task 4: Metadata and translations

**Files:**
- Modify: `gradle.properties`
- Modify: `build.gradle`
- Modify: `src/main/templates/META-INF/neoforge.mods.toml`
- Modify: `src/main/resources/assets/kubejs_recipe_ui/lang/zh_cn.json`
- Modify: `src/main/resources/assets/kubejs_recipe_ui/lang/en_us.json`

1. Add an optional Mekanism 10.8 dependency range without a hard runtime artifact.
2. Add workstation, selector, resource, parameter, and validation labels in Chinese and English.
3. Validate both language files as JSON.

### Task 5: Build verification

1. Run `gradlew.bat build`.
2. Fix compilation, resource expansion, and integration registration errors.
3. Review `git diff --check` and confirm no unrelated user changes were reverted.
