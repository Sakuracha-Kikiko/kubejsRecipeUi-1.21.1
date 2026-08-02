package com.github.jun8848.kubejsrecipeui.recipe.integration;

/** 执行某一工作站专属的配方约束检查。 */
@FunctionalInterface
public interface RecipeValidator {
    RecipeValidator NONE = context -> {
    };

    void validate(RecipeValidationContext context);
}
