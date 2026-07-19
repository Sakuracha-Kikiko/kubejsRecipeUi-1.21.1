package com.github.jun8848.kubejsrecipeui.recipe;

/** 配方写入时可选择的新增与删除组合策略。 */
public enum RecipeWriteMode {
    ADD_ONLY("kubejs_recipe_ui.recipe_write_mode.add_only"),
    REMOVE_AND_ADD("kubejs_recipe_ui.recipe_write_mode.remove_and_add"),
    REMOVE_ONLY("kubejs_recipe_ui.recipe_write_mode.remove_only");

    private final String translationKey;

    RecipeWriteMode(String translationKey) {
        this.translationKey = translationKey;
    }

    /** @return 当前写入策略对应的界面翻译键 */
    public String translationKey() {
        return translationKey;
    }

    /** @return 按枚举声明顺序循环得到的下一个写入策略 */
    public RecipeWriteMode next() {
        var values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
