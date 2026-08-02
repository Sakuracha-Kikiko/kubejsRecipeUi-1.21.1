# 配方集成架构实施计划

1. 新增配方主体生成器、校验器、集成接口和注册表。
2. 将原版、Extended Crafting、AE2 的生成与专属校验迁移到各自集成类。
3. 将公共代码格式化逻辑移动到 `RecipeCodeBuilder`。
4. 简化 `KubeJsRecipeGenerator`，由注册表选择生成器。
5. 让 `WorkstationType` 的可用性、集成归属和侧栏分组委托给注册表。
6. 新增客户端面板注册表，替换 `RecipeEditorUi` 中覆盖全部类型的 `switch`。
7. 修复构建脚本中的乱码注释。
8. 执行 Gradle 构建、`git diff --check`、UTF-8 和翻译键检查。
