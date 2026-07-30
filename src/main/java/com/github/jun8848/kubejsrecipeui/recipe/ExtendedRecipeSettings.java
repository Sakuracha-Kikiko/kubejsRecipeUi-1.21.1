package com.github.jun8848.kubejsrecipeui.recipe;

/** Extended Crafting 各配方类型共享的数值参数快照。 */
public record ExtendedRecipeSettings(
        int tier,
        int powerCost,
        int powerRate,
        int ingredientCount,
        int craftingTime
) {
    public ExtendedRecipeSettings {
        if (tier < 0 || tier > 4) {
            throw new IllegalArgumentException("扩展工作台阶级必须在 0 到 4 之间");
        }
        if (powerCost < 0 || powerRate < 0) {
            throw new IllegalArgumentException("能量和功率不能小于 0");
        }
        if (ingredientCount < 1) {
            throw new IllegalArgumentException("压缩材料数量必须至少为 1");
        }
        if (craftingTime < 1) {
            throw new IllegalArgumentException("合成时间必须至少为 1");
        }
    }

    public static ExtendedRecipeSettings defaults(WorkstationType type) {
        return new ExtendedRecipeSettings(
                type.extendedTier(),
                type.defaultPowerCost(),
                type.defaultPowerRate(),
                type.defaultIngredientCount(),
                type.defaultCraftingTime()
        );
    }
}
