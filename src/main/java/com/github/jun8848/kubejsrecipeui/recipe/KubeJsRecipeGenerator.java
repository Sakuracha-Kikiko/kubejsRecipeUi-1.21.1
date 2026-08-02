package com.github.jun8848.kubejsrecipeui.recipe;

import com.github.jun8848.kubejsrecipeui.recipe.integration.RecipeIntegrationRegistry;

import java.util.Locale;

import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.quote;

/** 将经过校验的配方模型转换为可直接执行的 KubeJS 服务端脚本。 */
public final class KubeJsRecipeGenerator {
    private KubeJsRecipeGenerator() {
    }

    /** 根据工作站、写入策略和删除条件生成完整的配方事件块。 */
    public static String generate(
            RecipeDefinition recipe,
            RecipeWriteMode mode,
            RecipeDeleteMode deleteMode,
            String deleteValue
    ) {
        var body = RecipeIntegrationRegistry.generateBody(recipe);
        if (mode == RecipeWriteMode.REMOVE_AND_ADD) {
            body = generateRemoveBody(deleteMode, deleteValue) + "\n" + body;
        }
        return wrapRecipeEvent(body);
    }

    /** 仅生成包含一条删除语句的配方事件块。 */
    public static String generateRemove(RecipeDeleteMode deleteMode, String deleteValue) {
        return wrapRecipeEvent(generateRemoveBody(deleteMode, deleteValue));
    }

    private static String wrapRecipeEvent(String body) {
        return "ServerEvents.recipes(event => {\n" + body + "\n});\n";
    }

    private static String generateRemoveBody(RecipeDeleteMode deleteMode, String deleteValue) {
        var normalized = deleteValue == null ? "" : deleteValue.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("kubejs_recipe_ui.recipe_delete_mode.error.empty");
        }
        var key = deleteMode.name().toLowerCase(Locale.ROOT);
        return "  event.remove({ " + key + ": " + quote(normalized) + " });";
    }
}
