package com.github.jun8848.kubejsrecipeui.recipe;

/** Extended Crafting 各配方类型共享的数值参数快照。 */
public record ExtendedRecipeSettings(
        int tier,
        int powerCost,
        int powerRate,
        int ingredientCount,
        int craftingTime
) implements RecipeIntegrationSettings {
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
        return switch (type) {
            case EXTENDED_BASIC_TABLE -> new ExtendedRecipeSettings(1, 0, 0, 1, 1);
            case EXTENDED_ADVANCED_TABLE -> new ExtendedRecipeSettings(2, 0, 0, 1, 1);
            case EXTENDED_ELITE_TABLE -> new ExtendedRecipeSettings(3, 0, 0, 1, 1);
            case EXTENDED_ULTIMATE_TABLE -> new ExtendedRecipeSettings(4, 0, 0, 1, 1);
            case EXTENDED_COMBINATION -> new ExtendedRecipeSettings(0, 400_000, 500, 1, 1);
            case EXTENDED_COMPRESSOR -> new ExtendedRecipeSettings(0, 500_000, 5_000, 10_000, 1);
            case EXTENDED_ENDER_CRAFTER -> new ExtendedRecipeSettings(0, 0, 0, 1, 60);
            case EXTENDED_FLUX_CRAFTER -> new ExtendedRecipeSettings(0, 100_000, 400, 1, 1);
            default -> throw new IllegalArgumentException("不是 Extended Crafting 工作站: " + type);
        };
    }

    public ExtendedRecipeSettings withTier(int value) {
        return new ExtendedRecipeSettings(value, powerCost, powerRate, ingredientCount, craftingTime);
    }

    public ExtendedRecipeSettings withPowerCost(int value) {
        return new ExtendedRecipeSettings(tier, value, powerRate, ingredientCount, craftingTime);
    }

    public ExtendedRecipeSettings withPowerRate(int value) {
        return new ExtendedRecipeSettings(tier, powerCost, value, ingredientCount, craftingTime);
    }

    public ExtendedRecipeSettings withIngredientCount(int value) {
        return new ExtendedRecipeSettings(tier, powerCost, powerRate, value, craftingTime);
    }

    public ExtendedRecipeSettings withCraftingTime(int value) {
        return new ExtendedRecipeSettings(tier, powerCost, powerRate, ingredientCount, value);
    }
}
