# Extended Crafting 可选集成设计

## 目标

当运行环境安装 `extendedcrafting` 时，工作台编辑器侧栏追加 Extended Crafting 工作站入口，并允许玩家通过现有 LDLib2 配方编辑流程生成、预览和写入相应的 KubeJS 配方。未安装该模组时不显示入口，也不应因为缺少其类或注册项而导致启动失败。

## 支持范围

- 基础工作台：3×3，有序或无序，可指定阶级 1。
- 进阶工作台：5×5，有序或无序，可指定阶级 2。
- 精英工作台：7×7，有序或无序，可指定阶级 3。
- 终极工作台：9×9，有序或无序，可指定阶级 4。
- 组合合成：中心输入、最多 48 个基座材料、输出、总耗电量和每 tick 输入功率。
- 压缩机：材料、材料数量、催化剂、输出、总耗电量和每 tick 输入功率。
- 末影合成器：3×3 有序或无序配方、输出和合成时间。
- 通量合成器：3×3 有序或无序配方、输出、所需能量和每台供能器输入功率。

`ultimate_singularity` 属于 Extended Crafting 的特殊动态配方，不提供普通材料布局，因此不加入可视化编辑器。

## 依赖和加载边界

项目通过 Blake's Mods Maven 以 `compileOnly` 方式引用 Extended Crafting 26.1.2-8.0.0，并在开发客户端通过 `localRuntime` 加载 Extended Crafting 与 Cucumber。模组元数据将 `extendedcrafting` 声明为可选依赖。

通用编辑器代码不直接要求 Extended Crafting 类存在。工作站枚举保存图标资源 ID 和所需模组 ID，通过 `ModList.isLoaded("extendedcrafting")` 判断是否可用；侧栏只遍历当前可用工作站。JEI 专用集成类允许编译时访问 Extended Crafting 的 API 和 JEI 配方类型，但注册入口会先检查模组是否已加载。

## 数据模型与代码生成

现有 `RecipeDefinition` 继续保存通用输入、输出、配方 ID 和有序状态，并增加不可变的扩展参数对象，用于保存阶级、总能量、功率、材料数量和合成时间。每个工作站只读取自己需要的字段。

Extended Crafting 配方统一生成 `event.custom({...}).id('namespace:path')`。这样既覆盖 KubeJS 已内置映射的扩展工作台，也能覆盖组合合成、压缩机、末影合成器和通量合成器。生成字段严格对应 26.1 分支的 Codec：

- `extendedcrafting:shaped_table` / `shapeless_table`
- `extendedcrafting:combination`
- `extendedcrafting:compressor`
- `extendedcrafting:shaped_ender_crafter` / `shapeless_ender_crafter`
- `extendedcrafting:shaped_flux_crafter` / `shapeless_flux_crafter`

删除、删除后新增、仅新增、显式或自动配方 ID、材料标签、替代材料和数据组件开关继续复用现有流程。

## 界面和 JEI 数据流

四个等级的扩展工作台共用一个侧栏入口和同一编辑区域，通过下拉框切换 3×3、5×5、7×7、9×9 紧凑网格。各等级继续保存独立槽位状态，避免切换后丢失材料；组合合成的中心材料独立显示，48 个基座材料使用 8×6 紧凑网格；压缩机、末影合成器和通量合成器参考原模组 JEI 的槽位语义排列。

JEI 转移处理器为 Extended Crafting 的八个 JEI 分类注册到同一个 LDLib2 编辑器菜单。转移时同步材料、输出、配方 ID、序列化器、来源模组、分组以及配方专属参数。扩展工作台的有序配方按原宽高映射进对应阶级网格，无序配方按顺序填入。

## 验证标准

- 未安装 Extended Crafting 时，项目仍可编译，原版七种工作站不受影响。
- 安装 Extended Crafting 时，只增加约定的八个工作站入口。
- 每种配方的预览代码字段可被上游 26.1 Codec 接受。
- JEI 能将每类配方的材料、输出、ID 和专属参数回填到对应面板。
- `gradlew build` 成功，中文和英文翻译 JSON 可解析，源码保持 UTF-8。
