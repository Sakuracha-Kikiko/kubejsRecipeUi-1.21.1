package com.github.jun8848.kubejsrecipeui.recipe.integration;

import com.github.jun8848.kubejsrecipeui.recipe.Ae2RecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.ExtendedRecipeSettings;

/** 保留 1.x 自动配方 ID 哈希输入格式，避免架构迁移改变已有配方 ID。 */
final class LegacyRecipeIdSignatures {
    private static final ExtendedRecipeSettings EMPTY_EXTENDED =
            new ExtendedRecipeSettings(0, 0, 0, 1, 1);
    private static final Ae2RecipeSettings DEFAULT_AE2 = Ae2RecipeSettings.defaults();

    private LegacyRecipeIdSignatures() {
    }

    static String vanilla() {
        return EMPTY_EXTENDED + "|" + DEFAULT_AE2;
    }

    static String extended(RecipeValidationContext context) {
        return context.settings(ExtendedRecipeSettings.class) + "|" + DEFAULT_AE2;
    }

    static String ae2(RecipeValidationContext context) {
        return EMPTY_EXTENDED + "|" + context.settings(Ae2RecipeSettings.class);
    }
}
