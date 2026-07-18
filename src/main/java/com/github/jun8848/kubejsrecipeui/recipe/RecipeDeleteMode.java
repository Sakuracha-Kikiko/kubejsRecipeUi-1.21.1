package com.github.jun8848.kubejsrecipeui.recipe;

public enum RecipeDeleteMode {
    ID("kubejs_recipe_ui.recipe_delete_mode.id"),
    TYPE("kubejs_recipe_ui.recipe_delete_mode.type"),
    MOD("kubejs_recipe_ui.recipe_delete_mode.mod"),
    GROUP("kubejs_recipe_ui.recipe_delete_mode.group"),
    INPUT("kubejs_recipe_ui.recipe_delete_mode.input"),
    OUTPUT("kubejs_recipe_ui.recipe_delete_mode.output");

    private final String translationKey;

    RecipeDeleteMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
