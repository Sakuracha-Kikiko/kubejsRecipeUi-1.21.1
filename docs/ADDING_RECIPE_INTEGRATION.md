# 新增模组配方集成

本文说明如何在不修改核心代码生成流程的情况下增加一个模组配方编辑器。

## 1. 声明工作站

在 `WorkstationType` 中加入类型，只填写通用元数据：

- 翻译键；
- 图标物品 ID；
- 输入槽位数量；
- 网格宽高。

不要把模组专属能量、时间或模式参数放入 `WorkstationType`。

## 2. 注册配方行为

在 `recipe/integration` 下新增 `RecipeIntegration` 实现，并在
`RecipeIntegrationRegistry.createEntries()` 的集成列表中登记。

每个 `RecipeTypeSpec` 需要声明：

- 侧栏代表类型；
- 是否必须有输入和输出；
- 默认专属设置；
- 类型专属校验器；
- 自动 ID 的来源物品；
- 单条 KubeJS 配方代码生成器。

多个配方类型需要共用一个侧栏按钮时，将它们的 `navigationType` 指向同一个代表类型。

## 3. 增加专属参数

只有配方确实需要额外参数时，才新增一个实现 `RecipeIntegrationSettings` 的不可变记录：

```java
public record ExampleRecipeSettings(int energy) implements RecipeIntegrationSettings {
}
```

通过 `RecipeSettings.of(...)` 设置默认值，通过
`recipe.settings(ExampleRecipeSettings.class)` 在生成器中读取。JEI 回填时把同一设置对象放入
`RecipeTransferData.settings()`，无需修改配方模型、会话或桥接回调签名。

## 4. 注册 LDLib2 面板

在 `RecipeEditorPanelRegistry` 中为工作站登记面板工厂。面板应继续复用
`RecipeEditorUi` 的槽位、数值输入和通用属性控件，模组专属控件集中在自己的构建方法中。

## 5. 注册 JEI 转移

在 `integration/jei` 下新增独立集成类。只有依赖模组已加载时才调用该类，避免可选模组缺失时
JVM 解析其 API 类型。转移数据统一封装为 `RecipeTransferData`。

## 6. 完成检查

```powershell
.\gradlew.bat build
git diff --check
```

同时检查新增翻译键、可选依赖版本范围、未安装目标模组时的启动行为，以及 JEI 回填后的专属参数。
