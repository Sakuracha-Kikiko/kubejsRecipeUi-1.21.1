*Written by DeepSeek*

# kubejsRecipeUi

`kubejsRecipeUi` 是一个基于 Minecraft 26.1.2、NeoForge 和 KubeJS 的可视化配方编辑附属模组，作者为 mubai。

> 本仓库的 `port/1.21.1` 分支是上游 26.1.2 版本的 1.21.1 移植：`Identifier` 改回 `ResourceLocation`、`BuiltInRegistries#getValue` 改回 `#get`、JEI `IRecipeType` 改回 `RecipeType`、物品模型与战利品表改回 1.21.1 的目录结构，并把 LDLib2 内嵌的 `taffy`/`yoga` 放进 `libs/` 供编译使用。

它将常用的 KubeJS 配方修改流程放进游戏内界面中：玩家可以像使用原版工作台、熔炉、烟熏炉、高炉、营火、切石机和锻造台一样编辑配方，设置材料、标签、替代材料、NBT/数据组件以及输出数量，并生成可直接用于 KubeJS 的脚本代码。

## 主要功能

- 提供工作台编辑器方块，并加入独立的创造物品栏。
- 使用接近原版工作站的界面布局，移除不需要的配方书区域。
- 支持有序配方和无序配方切换。
- 支持工作台、熔炉、烟熏炉、高炉、营火、切石机和锻造台等配方类型。
- 安装 Extended Crafting 后，自动显示扩展工作台、组合合成、量子压缩机、末影合成器和通量合成器；扩展工作台通过一个下拉框切换 3×3、5×5、7×7、9×9 等级。
- Extended Crafting 集成支持能量、功率、材料数量、合成时间以及对应 JEI 配方自动填充。
- 安装 Applied Energistics 2 后，自动显示 AE2 编辑入口，可编写充能器、压印器、物品转化和熵变处理配方。
- AE2 集成支持压印模式、爆炸/流体转化条件、熵变加热/冷却及方块、流体、掉落物资源，并可从 JEI 自动读取配方 ID 和专属参数。
- 支持精确物品、物品标签和替代材料。
- 可从槽位读取物品的数据组件/NBT，并选择是否参与配方匹配。
- 支持通过 JEI 将配方材料和配方 ID 自动填入编辑器。
- 支持仅新增、删除后新增和仅删除等配方修改方式。
- 支持按配方 ID、类型、来源模组、分组、输入材料或输出物品删除配方。
- 提供代码预览，只有预览后的代码才会写入 `generated_recipes.js`。

## 依赖

- Minecraft `1.21.1`
- NeoForge `21.1.250` 或更高版本
- KubeJS `2101.7` 或更高版本（2101 系列）
- LDLib2 `2.2.44`（`mc1.21.1-2.2.44-neoforge`，必需）
- JEI `19.51.0.417` 或更高版本（可选，用于配方查看和自动填充）
- Extended Crafting `7.0.9` 或更高版本（可选，安装后自动启用扩展配方编辑）
- Applied Energistics 2 `19.2.18` 或更高版本（可选，安装后自动启用 AE2 配方编辑）

构建时 `libs/taffy-1.1.5.jar` 与 `libs/yoga-1.0.0.jar` 是必需的 `compileOnly` 依赖，它们直接取自 LDLib2 的 jarjar 内嵌库（这两个库没有公开的 Maven 坐标）。

## 使用方式

1. 安装 NeoForge、KubeJS 和必需的 LDLib2。
2. 将模组放入对应实例的 `mods` 文件夹。
3. 进入游戏后，从创造物品栏的“KubeJS 配方工具”标签中取得“工作台编辑器”。
4. 放置并打开编辑器，设置配方后点击“预览”，确认代码无误后再点击“写入”。

## 开发环境

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

项目使用 Java 25。开发环境默认加载 JEI、Extended Crafting 和 Applied Energistics 2，以便验证可选集成。
