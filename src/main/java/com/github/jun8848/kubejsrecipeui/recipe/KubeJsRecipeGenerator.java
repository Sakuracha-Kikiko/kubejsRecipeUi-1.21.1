package com.github.jun8848.kubejsrecipeui.recipe;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 将经过校验的配方模型转换为可直接执行的 KubeJS 服务端脚本。
 */
public final class KubeJsRecipeGenerator {
    private KubeJsRecipeGenerator() {
    }

    /** 使用默认的“仅新增”策略生成配方代码。 */
    public static String generate(RecipeDefinition recipe) {
        return generate(recipe, RecipeWriteMode.ADD_ONLY, RecipeDeleteMode.OUTPUT, recipe.output().itemId());
    }

    /**
     * 根据工作站、写入策略和删除条件生成完整的配方事件块。
     */
    public static String generate(
            RecipeDefinition recipe,
            RecipeWriteMode mode,
            RecipeDeleteMode deleteMode,
            String deleteValue
    ) {
        var body = switch (recipe.type()) {
            case CRAFTING -> recipe.shaped() ? generateShaped(recipe) : generateShapeless(recipe);
            case FURNACE -> generateCooking("smelting", recipe);
            case SMOKER -> generateCooking("smoking", recipe);
            case BLAST_FURNACE -> generateCooking("blasting", recipe);
            case STONECUTTER -> generateSingleInput("stonecutting", recipe);
            case SMITHING -> generateSmithing(recipe);
        };
        if (mode == RecipeWriteMode.REMOVE_AND_ADD) {
            body = generateRemoveBody(deleteMode, deleteValue) + "\n" + body;
        }
        return "ServerEvents.recipes(event => {\n" + body + "\n});\n";
    }

    /** 仅生成包含一条删除语句的配方事件块。 */
    public static String generateRemove(RecipeDeleteMode deleteMode, String deleteValue) {
        return "ServerEvents.recipes(event => {\n" + generateRemoveBody(deleteMode, deleteValue) + "\n});\n";
    }

    private static String generateRemoveBody(RecipeDeleteMode deleteMode, String deleteValue) {
        var normalized = deleteValue == null ? "" : deleteValue.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("kubejs_recipe_ui.recipe_delete_mode.error.empty");
        }
        var key = deleteMode.name().toLowerCase(java.util.Locale.ROOT);
        return "  event.remove({ " + key + ": " + quote(normalized) + " });";
    }

    private static String generateCooking(String method, RecipeDefinition recipe) {
        return "  event." + method + "(" + outputExpression(recipe.output()) + ", "
                + ingredientExpression(recipe.inputs().getFirst()) + ")"
                + ".xp(" + number(recipe.experience()) + ")"
                + ".cookingTime(" + recipe.cookingTime() + ");";
    }

    private static String generateSingleInput(String method, RecipeDefinition recipe) {
        return "  event." + method + "(" + outputExpression(recipe.output()) + ", "
                + ingredientExpression(recipe.inputs().getFirst()) + ");";
    }

    private static String generateSmithing(RecipeDefinition recipe) {
        return "  event.smithing(" + outputExpression(recipe.output()) + ", "
                + ingredientExpression(recipe.inputs().get(0)) + ", "
                + ingredientExpression(recipe.inputs().get(1)) + ", "
                + ingredientExpression(recipe.inputs().get(2)) + ");";
    }

    private static String generateShaped(RecipeDefinition recipe) {
        // 先裁剪配方四周的空白槽位，使生成图案与原配方实际尺寸一致。
        var bounds = findBounds(recipe.inputs());
        // LinkedHashMap 保证材料符号按照首次出现顺序稳定生成。
        var symbols = new LinkedHashMap<IngredientSpec, Character>();
        var rows = new ArrayList<String>();

        for (var row = bounds.minRow; row <= bounds.maxRow; row++) {
            var pattern = new StringBuilder();
            for (var column = bounds.minColumn; column <= bounds.maxColumn; column++) {
                var ingredient = recipe.inputs().get(row * 3 + column);
                if (ingredient == null) {
                    pattern.append(' ');
                } else {
                    var symbol = symbols.computeIfAbsent(ingredient, ignored -> (char) ('A' + symbols.size()));
                    pattern.append(symbol);
                }
            }
            rows.add(pattern.toString());
        }

        var builder = new StringBuilder();
        builder.append("  event.shaped(").append(outputExpression(recipe.output())).append(", [\n");
        for (var index = 0; index < rows.size(); index++) {
            builder.append("    ").append(quote(rows.get(index)));
            builder.append(index + 1 == rows.size() ? "\n" : ",\n");
        }
        builder.append("  ], {\n");

        var entries = new ArrayList<>(symbols.entrySet());
        for (var index = 0; index < entries.size(); index++) {
            Map.Entry<IngredientSpec, Character> entry = entries.get(index);
            builder.append("    ").append(entry.getValue()).append(": ")
                    .append(ingredientExpression(entry.getKey()));
            builder.append(index + 1 == entries.size() ? "\n" : ",\n");
        }
        builder.append("  });");
        return builder.toString();
    }

    private static String generateShapeless(RecipeDefinition recipe) {
        var ingredients = recipe.inputs().stream()
                .filter(spec -> spec != null)
                .map(KubeJsRecipeGenerator::ingredientExpression)
                .collect(Collectors.joining(",\n    "));
        return "  event.shapeless(" + outputExpression(recipe.output()) + ", [\n"
                + "    " + ingredients + "\n"
                + "  ]);";
    }

    private static Bounds findBounds(List<IngredientSpec> inputs) {
        var minRow = 3;
        var maxRow = -1;
        var minColumn = 3;
        var maxColumn = -1;
        for (var index = 0; index < inputs.size(); index++) {
            if (inputs.get(index) == null) {
                continue;
            }
            var row = index / 3;
            var column = index % 3;
            minRow = Math.min(minRow, row);
            maxRow = Math.max(maxRow, row);
            minColumn = Math.min(minColumn, column);
            maxColumn = Math.max(maxColumn, column);
        }
        return new Bounds(minRow, maxRow, minColumn, maxColumn);
    }

    private static String ingredientExpression(IngredientSpec ingredient) {
        return switch (ingredient.kind()) {
            case ITEM -> quote(ingredient.value() + ingredient.componentPatch());
            case TAG -> quote("#" + ingredient.value());
            case ALTERNATIVES -> ingredient.alternatives().stream()
                    .map(KubeJsRecipeGenerator::ingredientExpression)
                    .collect(Collectors.joining(", ", "[", "]"));
        };
    }

    private static String outputExpression(OutputSpec output) {
        var prefix = output.count() == 1 ? "" : output.count() + "x ";
        return quote(prefix + output.itemId() + output.componentPatch());
    }

    private static String quote(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static String number(float value) {
        return new BigDecimal(Float.toString(value)).stripTrailingZeros().toPlainString();
    }

    private record Bounds(int minRow, int maxRow, int minColumn, int maxColumn) {
    }
}
