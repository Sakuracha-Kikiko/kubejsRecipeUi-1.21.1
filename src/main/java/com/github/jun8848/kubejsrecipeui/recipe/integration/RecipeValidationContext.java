package com.github.jun8848.kubejsrecipeui.recipe.integration;

import com.github.jun8848.kubejsrecipeui.recipe.IngredientSpec;
import com.github.jun8848.kubejsrecipeui.recipe.OutputSpec;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeIntegrationSettings;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;

import java.util.List;

/** 配方类型校验器和自动 ID 生成器使用的只读上下文。 */
public record RecipeValidationContext(
        WorkstationType type,
        boolean shaped,
        List<IngredientSpec> inputs,
        OutputSpec output,
        float experience,
        int cookingTime,
        RecipeSettings settings
) {
    public <T extends RecipeIntegrationSettings> T settings(Class<T> settingsType) {
        return settings.require(settingsType);
    }
}
