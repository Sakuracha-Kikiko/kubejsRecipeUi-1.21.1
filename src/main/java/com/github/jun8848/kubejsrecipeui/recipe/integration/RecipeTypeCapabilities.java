package com.github.jun8848.kubejsrecipeui.recipe.integration;

/** 编辑器处理某类配方时需要了解的通用能力。 */
public record RecipeTypeCapabilities(
        boolean supportsShaped,
        boolean cooking,
        int defaultCookingTime
) {
    public static final RecipeTypeCapabilities SIMPLE = new RecipeTypeCapabilities(false, false, 200);
    public static final RecipeTypeCapabilities SHAPED = new RecipeTypeCapabilities(true, false, 200);

    public RecipeTypeCapabilities {
        if (defaultCookingTime < 1) {
            throw new IllegalArgumentException("默认烹饪时间必须至少为 1 tick");
        }
    }

    public static RecipeTypeCapabilities cooking(int defaultCookingTime) {
        return new RecipeTypeCapabilities(false, true, defaultCookingTime);
    }
}
