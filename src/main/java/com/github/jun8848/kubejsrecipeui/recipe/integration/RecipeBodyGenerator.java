package com.github.jun8848.kubejsrecipeui.recipe.integration;

import com.github.jun8848.kubejsrecipeui.recipe.RecipeDefinition;

/** 生成单条配方语句，不包含 {@code ServerEvents.recipes} 外层包装。 */
@FunctionalInterface
public interface RecipeBodyGenerator {
    String generate(RecipeDefinition recipe);
}
