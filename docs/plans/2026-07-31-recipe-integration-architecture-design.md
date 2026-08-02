# 配方集成架构设计

## 目标

在不改变现有界面、JEI 回填和生成脚本行为的前提下，减少新增模组配方类型需要修改的核心文件数量。

## 核心结构

### 配方集成

`RecipeIntegration` 表示一组来自同一模组或原版的配方类型。集成负责登记：

- 集成 ID 和可选依赖模组 ID；
- 工作站类型；
- 侧栏代表工作站；
- 配方输入校验器；
- KubeJS 配方主体生成器。

`RecipeIntegrationRegistry` 在启动时装配原版、Extended Crafting 和 AE2 集成，并检查每个
`WorkstationType` 是否恰好注册一次。

模组专属参数实现 `RecipeIntegrationSettings`，由不可变的 `RecipeSettings` 容器按类型保存。
`RecipeDefinition`、编辑会话和 `RecipeTransferData` 只传递该容器，因此新集成不需要修改这些核心对象。

### 代码生成

生成逻辑分为三层：

1. `KubeJsRecipeGenerator` 处理删除条件和事件外层包装。
2. 集成类处理本模组各配方类型的脚本主体。
3. `RecipeCodeBuilder` 提供物品、原料、图案、JSON 和 ID 等公共格式化能力。

### 客户端面板

`RecipeEditorPanelRegistry` 保存工作站到 LDLib2 面板工厂的映射。第一阶段只迁移分派入口，
现有面板构建方法继续留在 `RecipeEditorUi` 中。后续可按原版、Extended Crafting、AE2 分批移动，
不再改变调用方。

## 扩展流程

后续接入新模组时：

1. 在 `WorkstationType` 增加稳定类型和基础槽位元数据。
2. 新增一个 `RecipeIntegration`，注册校验器和生成器。
3. 在客户端面板注册表登记 LDLib2 面板工厂。
4. 增加独立 JEI 集成贡献者。
5. 仅在确有专属参数时扩展设置模型。

## 兼容性

- 注册表只依赖 NeoForge 的 `ModList`，可选模组未安装时不显示对应工作站。
- 客户端面板注册表不进入服务端代码生成路径。
- 已生成的 KubeJS 代码格式保持不变。
