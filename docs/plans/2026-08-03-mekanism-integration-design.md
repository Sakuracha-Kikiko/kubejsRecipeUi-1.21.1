# Mekanism 26.1 配方编辑集成设计

## 目标

当游戏安装 Mekanism 时，在工作台编辑器侧栏显示 Mekanism 入口，并支持 Mekanism 26.1 分支定义的 26 种正式数据包配方。未安装 Mekanism 时不显示入口，也不解析任何 Mekanism 运行时类。

## 配方范围

支持以下序列化器：`crushing`、`enriching`、`smelting`、`chemical_infusing`、`combining`、`separating`、`washing`、`evaporating`、`activating`、`centrifuging`、`crystallizing`、`dissolution`、`compressing`、`purifying`、`injecting`、`nucleosynthesizing`、`energy_conversion`、`chemical_conversion`、`oxidizing`、`pigment_extracting`、`pigment_mixing`、`metallurgic_infusing`、`painting`、`reaction`、`rotary` 和 `sawing`。

锅炉、SPS 和营养液化器在 Mekanism JEI 中是展示用的伪配方分类，没有对应的可写数据包序列化器，因此不作为编辑类型注册。

## 架构

- `WorkstationType` 只保存每种类型的翻译键、图标和物品槽元数据。全部 Mekanism 类型共用一个侧栏入口，通过下拉框切换。
- `MekanismRecipeIntegration` 负责声明可选模组 ID、输入输出要求、参数校验、自动 ID 来源和 `event.custom` 代码生成。
- `MekanismRecipeSettings` 保存物品数量、流体、化学品、能量、持续时间、每 tick 消耗、旋转方向和锯切副产物等专属参数。
- `RecipeEditorUi` 按配方的数据形态复用 LDLib2 面板构造器，不为 26 种类型复制完整布局。
- `MekanismJeiIntegration` 只依赖 JEI 和原版/NeoForge 公共类型。化学品通过 JEI 暴露的 ingredient 对象反射读取，避免未安装 Mekanism 时发生类加载失败。

## 数据与生成

物品原料继续使用现有 `IngredientSpec`，Mekanism 所需数量由设置对象保存并生成 `{ count, ingredient }`。流体输入生成 `{ amount, ingredient }`；化学品输入支持具体化学品和 `#tag`，分别生成 `chemical` 或 `tag` 字段。物品、流体和化学品输出严格使用 26.1 Codec 的字段名。

预览和写入仍走现有流程，自动配方 ID 会将 Mekanism 专属设置加入签名，避免参数不同的配方发生 ID 冲突。

## 失败处理

- 未安装 `mekanism`：注册表过滤全部 Mekanism 工作站，JEI 转移器不注册。
- 缺少资源 ID、数量无效或必需输出为空：预览阶段显示现有游戏内错误消息，不生成脚本。
- JEI 中无法识别某个第三方 ingredient：保留默认字段，其余能够识别的物品、流体和化学品仍正常回填。

## 验证

不新增单元测试。完成后运行 `gradlew.bat build`，验证不安装 Mekanism 编译路径、资源 JSON、可选依赖元数据和全部 Java 源码。
