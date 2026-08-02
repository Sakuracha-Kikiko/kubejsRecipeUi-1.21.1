package com.github.jun8848.kubejsrecipeui.recipe.codegen;

import com.github.jun8848.kubejsrecipeui.recipe.IngredientSpec;
import com.github.jun8848.kubejsrecipeui.recipe.OutputSpec;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeDefinition;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 各模组配方生成器共用的 KubeJS 表达式和 JSON 构建工具。 */
public final class RecipeCodeBuilder {
    private static final String PATTERN_SYMBOLS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!$%&()*+,-./:;<=>?@[]^_`{|}~";

    private RecipeCodeBuilder() {
    }

    public static String customRecipe(String serializer, List<String> fields, RecipeDefinition recipe) {
        var allFields = new ArrayList<String>();
        allFields.add("    type: " + quote(serializer));
        allFields.addAll(fields);
        return "  event.custom({\n"
                + String.join(",\n", allFields)
                + "\n  })\n"
                + "    .id(" + quote(recipe.id()) + ");";
    }

    public static void appendPatternFields(List<String> fields, PatternData pattern) {
        var patternBuilder = new StringBuilder("    pattern: [\n");
        appendPatternRows(patternBuilder, pattern.rows(), "      ");
        patternBuilder.append("    ]");
        fields.add(patternBuilder.toString());

        var keyBuilder = new StringBuilder("    key: {\n");
        var entries = new ArrayList<>(pattern.symbols().entrySet());
        for (var index = 0; index < entries.size(); index++) {
            var entry = entries.get(index);
            keyBuilder.append("      ").append(quote(entry.getValue().toString())).append(": ")
                    .append(ingredientJsonExpression(entry.getKey()));
            keyBuilder.append(index + 1 == entries.size() ? "\n" : ",\n");
        }
        keyBuilder.append("    }");
        fields.add(keyBuilder.toString());
    }

    public static void appendPatternRows(StringBuilder builder, List<String> rows, String indent) {
        for (var index = 0; index < rows.size(); index++) {
            builder.append(indent).append(quote(rows.get(index)));
            builder.append(index + 1 == rows.size() ? "\n" : ",\n");
        }
    }

    public static String ingredientListField(String name, List<IngredientSpec> ingredients) {
        return "    " + name + ": [\n"
                + ingredients.stream()
                .map(RecipeCodeBuilder::ingredientJsonExpression)
                .map(value -> "      " + value)
                .collect(Collectors.joining(",\n"))
                + "\n    ]";
    }

    public static String resultField(OutputSpec output) {
        return "    result: Item.of(" + outputExpression(output) + ")";
    }

    public static List<IngredientSpec> nonEmptyInputs(RecipeDefinition recipe) {
        return recipe.inputs().stream().filter(spec -> spec != null).toList();
    }

    /** 裁剪网格四周空白并为每种原料分配稳定图案字符。 */
    public static PatternData createPattern(RecipeDefinition recipe, int gridWidth, int gridHeight) {
        var bounds = findBounds(recipe.inputs(), gridWidth, gridHeight);
        var symbols = new LinkedHashMap<IngredientSpec, Character>();
        var rows = new ArrayList<String>();

        for (var row = bounds.minRow; row <= bounds.maxRow; row++) {
            var pattern = new StringBuilder();
            for (var column = bounds.minColumn; column <= bounds.maxColumn; column++) {
                var ingredient = recipe.inputs().get(row * gridWidth + column);
                if (ingredient == null) {
                    pattern.append(' ');
                } else {
                    var symbol = symbols.computeIfAbsent(ingredient, ignored -> nextPatternSymbol(symbols.size()));
                    pattern.append(symbol);
                }
            }
            rows.add(pattern.toString());
        }
        return new PatternData(rows, symbols);
    }

    private static Bounds findBounds(List<IngredientSpec> inputs, int gridWidth, int gridHeight) {
        var minRow = gridHeight;
        var maxRow = -1;
        var minColumn = gridWidth;
        var maxColumn = -1;
        for (var index = 0; index < inputs.size(); index++) {
            if (inputs.get(index) == null) {
                continue;
            }
            var row = index / gridWidth;
            var column = index % gridWidth;
            minRow = Math.min(minRow, row);
            maxRow = Math.max(maxRow, row);
            minColumn = Math.min(minColumn, column);
            maxColumn = Math.max(maxColumn, column);
        }
        return new Bounds(minRow, maxRow, minColumn, maxColumn);
    }

    private static char nextPatternSymbol(int index) {
        if (index >= PATTERN_SYMBOLS.length()) {
            throw new IllegalArgumentException("有序配方使用了过多不同材料");
        }
        return PATTERN_SYMBOLS.charAt(index);
    }

    public static String ingredientExpression(IngredientSpec ingredient) {
        return switch (ingredient.kind()) {
            case ITEM -> quote(ingredient.value() + ingredient.componentPatch());
            case TAG -> quote("#" + ingredient.value());
            case ALTERNATIVES -> ingredient.alternatives().stream()
                    .map(RecipeCodeBuilder::ingredientExpression)
                    .collect(Collectors.joining(", ", "[", "]"));
        };
    }

    /** 让 event.custom 通过 KubeJS Codec 将标签、替代材料和数据组件编码为合法 JSON。 */
    public static String ingredientJsonExpression(IngredientSpec ingredient) {
        return "Ingredient.of(" + ingredientExpression(ingredient) + ")";
    }

    public static String outputExpression(OutputSpec output) {
        var prefix = output.count() == 1 ? "" : output.count() + "x ";
        return quote(prefix + output.itemId() + output.componentPatch());
    }

    public static String idExpression(RecipeDefinition recipe) {
        return ".id(" + quote(recipe.id()) + ")";
    }

    public static String quote(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    public static String number(float value) {
        return new BigDecimal(Float.toString(value)).stripTrailingZeros().toPlainString();
    }

    private record Bounds(int minRow, int maxRow, int minColumn, int maxColumn) {
    }

    public record PatternData(List<String> rows, Map<IngredientSpec, Character> symbols) {
    }
}
