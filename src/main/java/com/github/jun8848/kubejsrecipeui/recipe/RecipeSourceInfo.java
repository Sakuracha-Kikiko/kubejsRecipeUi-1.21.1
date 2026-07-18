package com.github.jun8848.kubejsrecipeui.recipe;

public record RecipeSourceInfo(String id, String type, String mod, String group) {
    public static final RecipeSourceInfo EMPTY = new RecipeSourceInfo("", "", "", "");

    public RecipeSourceInfo {
        id = normalize(id);
        type = normalize(type);
        mod = normalize(mod);
        group = normalize(group);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
