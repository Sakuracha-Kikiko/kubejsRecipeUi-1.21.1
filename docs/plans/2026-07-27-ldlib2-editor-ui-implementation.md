# LDLib2 Editor UI Implementation Plan

**Goal:** Build a dense LDLib2 editor layout with the light vanilla/JEI visual language.

**Architecture:** Keep recipe state and code generation responsibilities separated from the UI. Use LDLib2's MC stylesheet for the light shell and advanced containers for the editor workspace, then add local classes only for visual hierarchy.

**Tech Stack:** Java 25, NeoForge, LDLib2 ModularUI, KubeJS, JEI

---

### Task 1: Add editor shell

**Files:**
- Modify: `src/main/java/com/github/jun8848/kubejsrecipeui/client/RecipeEditorUi.java`

1. Import `SplitView`, `TabView`, `Tab`, `ScrollerView`, and `Switch`.
2. Change the root to a wide column layout with a top toolbar and content row.
3. Use the `MC` stylesheet and add JEI-style classes for the root frame, title, sidebar, canvas, inspector and tabs.
4. Verify with `./gradlew.bat compileJava`.

### Task 2: Refactor recipe workspace

**Files:**
- Modify: `src/main/java/com/github/jun8848/kubejsrecipeui/client/RecipeEditorUi.java`

1. Move recipe controls into a right-side `ScrollerView` inspector.
2. Place workstation slots and inventory in the central canvas.
3. Place canvas and inspector in `SplitView.Horizontal`.
4. Use a full-height right-side `TabView` for property, code preview and JEI source modes.
5. Switch to the code tab after preview generation without changing existing data bindings.
6. Replace component `Toggle` controls with labeled `Switch` controls.
7. Verify with `./gradlew.bat compileJava`.

### Task 3: Add editor translations and verify

**Files:**
- Modify: `src/main/resources/assets/kubejs_recipe_ui/lang/zh_cn.json`
- Modify: `src/main/resources/assets/kubejs_recipe_ui/lang/en_us.json`

1. Add labels for inspector sections, code/source tabs, and editor status text.
2. Parse both files with an UTF-8 JSON parser.
3. Run `./gradlew.bat build` and confirm the JAR is generated.
4. Launch `./gradlew.bat runClient` for visual verification.
