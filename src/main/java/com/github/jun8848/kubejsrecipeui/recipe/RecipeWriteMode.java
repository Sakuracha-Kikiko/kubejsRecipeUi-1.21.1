package com.github.jun8848.kubejsrecipeui.recipe;

public enum RecipeWriteMode {
    ADD_ONLY("kubejs_recipe_ui.recipe_write_mode.add_only"),
    REMOVE_AND_ADD("kubejs_recipe_ui.recipe_write_mode.remove_and_add"),
    REMOVE_ONLY("kubejs_recipe_ui.recipe_write_mode.remove_only");

    private final String translationKey;

    RecipeWriteMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public RecipeWriteMode next() {
        var values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
