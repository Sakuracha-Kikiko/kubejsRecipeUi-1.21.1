package com.github.jun8848.kubejsrecipeui.recipe;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 将经过校验的配方模型转换为可直接执行的 KubeJS 服务端脚本。 */
public final class KubeJsRecipeGenerator {
    private static final String PATTERN_SYMBOLS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!$%&()*+,-./:;<=>?@[]^_`{|}~";

    private KubeJsRecipeGenerator() {
    }

    /** 根据工作站、写入策略和删除条件生成完整的配方事件块。 */
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
            case CAMPFIRE -> generateCooking("campfireCooking", recipe);
            case STONECUTTER -> generateSingleInput("stonecutting", recipe);
            case SMITHING -> generateSmithing(recipe);
            case EXTENDED_BASIC_TABLE, EXTENDED_ADVANCED_TABLE, EXTENDED_ELITE_TABLE, EXTENDED_ULTIMATE_TABLE ->
                    generateExtendedTable(recipe);
            case EXTENDED_COMBINATION -> generateCombination(recipe);
            case EXTENDED_COMPRESSOR -> generateCompressor(recipe);
            case EXTENDED_ENDER_CRAFTER -> generateEnderCrafter(recipe);
            case EXTENDED_FLUX_CRAFTER -> generateFluxCrafter(recipe);
            case AE2_CHARGER -> generateAe2Charger(recipe);
            case AE2_INSCRIBER -> generateAe2Inscriber(recipe);
            case AE2_TRANSFORM -> generateAe2Transform(recipe);
            case AE2_ENTROPY -> generateAe2Entropy(recipe);
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
                + ".cookingTime(" + recipe.cookingTime() + ")"
                + idExpression(recipe) + ";";
    }

    private static String generateSingleInput(String method, RecipeDefinition recipe) {
        return "  event." + method + "(" + outputExpression(recipe.output()) + ", "
                + ingredientExpression(recipe.inputs().getFirst()) + ")"
                + idExpression(recipe) + ";";
    }

    private static String generateSmithing(RecipeDefinition recipe) {
        return "  event.smithing(" + outputExpression(recipe.output()) + ", "
                + ingredientExpression(recipe.inputs().get(0)) + ", "
                + ingredientExpression(recipe.inputs().get(1)) + ", "
                + ingredientExpression(recipe.inputs().get(2)) + ")"
                + idExpression(recipe) + ";";
    }

    private static String generateShaped(RecipeDefinition recipe) {
        var pattern = createPattern(recipe, 3, 3);
        var builder = new StringBuilder();
        builder.append("  event.shaped(").append(outputExpression(recipe.output())).append(", [\n");
        appendPatternRows(builder, pattern.rows(), "    ");
        builder.append("  ], {\n");

        var entries = new ArrayList<>(pattern.symbols().entrySet());
        for (var index = 0; index < entries.size(); index++) {
            var entry = entries.get(index);
            builder.append("    ").append(quote(entry.getValue().toString())).append(": ")
                    .append(ingredientExpression(entry.getKey()));
            builder.append(index + 1 == entries.size() ? "\n" : ",\n");
        }
        builder.append("  })").append(idExpression(recipe)).append(";");
        return builder.toString();
    }

    private static String generateShapeless(RecipeDefinition recipe) {
        var ingredients = recipe.inputs().stream()
                .filter(spec -> spec != null)
                .map(KubeJsRecipeGenerator::ingredientExpression)
                .collect(Collectors.joining(",\n    "));
        return "  event.shapeless(" + outputExpression(recipe.output()) + ", [\n"
                + "    " + ingredients + "\n"
                + "  ])" + idExpression(recipe) + ";";
    }

    private static String generateExtendedTable(RecipeDefinition recipe) {
        var fields = new ArrayList<String>();
        if (recipe.shaped()) {
            appendPatternFields(fields, createPattern(recipe, recipe.type().gridWidth(), recipe.type().gridHeight()));
        } else {
            fields.add(ingredientListField("ingredients", nonEmptyInputs(recipe)));
        }
        fields.add(resultField(recipe.output()));
        fields.add("    tier: " + recipe.extendedSettings().tier());
        var serializer = recipe.shaped() ? "extendedcrafting:shaped_table" : "extendedcrafting:shapeless_table";
        return customRecipe(serializer, fields, recipe);
    }

    private static String generateCombination(RecipeDefinition recipe) {
        var fields = new ArrayList<String>();
        fields.add("    input: " + ingredientJsonExpression(recipe.inputs().getFirst()));
        fields.add(ingredientListField("ingredients", recipe.inputs().subList(1, recipe.inputs().size()).stream()
                .filter(spec -> spec != null)
                .toList()));
        fields.add(resultField(recipe.output()));
        fields.add("    power_cost: " + recipe.extendedSettings().powerCost());
        fields.add("    power_rate: " + recipe.extendedSettings().powerRate());
        return customRecipe("extendedcrafting:combination", fields, recipe);
    }

    private static String generateCompressor(RecipeDefinition recipe) {
        var settings = recipe.extendedSettings();
        var fields = new ArrayList<String>();
        fields.add("    ingredient: {\n"
                + "      ingredient: " + ingredientJsonExpression(recipe.inputs().get(0)) + ",\n"
                + "      count: " + settings.ingredientCount() + "\n"
                + "    }");
        fields.add("    catalyst: " + ingredientJsonExpression(recipe.inputs().get(1)));
        fields.add(resultField(recipe.output()));
        fields.add("    power_cost: " + settings.powerCost());
        fields.add("    power_rate: " + settings.powerRate());
        return customRecipe("extendedcrafting:compressor", fields, recipe);
    }

    private static String generateEnderCrafter(RecipeDefinition recipe) {
        var fields = new ArrayList<String>();
        if (recipe.shaped()) {
            appendPatternFields(fields, createPattern(recipe, 3, 3));
        } else {
            fields.add(ingredientListField("ingredients", nonEmptyInputs(recipe)));
        }
        fields.add(resultField(recipe.output()));
        fields.add("    crafting_time: " + recipe.extendedSettings().craftingTime());
        var serializer = recipe.shaped()
                ? "extendedcrafting:shaped_ender_crafter"
                : "extendedcrafting:shapeless_ender_crafter";
        return customRecipe(serializer, fields, recipe);
    }

    private static String generateFluxCrafter(RecipeDefinition recipe) {
        var fields = new ArrayList<String>();
        if (recipe.shaped()) {
            appendPatternFields(fields, createPattern(recipe, 3, 3));
        } else {
            fields.add(ingredientListField("ingredients", nonEmptyInputs(recipe)));
        }
        fields.add(resultField(recipe.output()));
        fields.add("    power_required: " + recipe.extendedSettings().powerCost());
        fields.add("    power_rate: " + recipe.extendedSettings().powerRate());
        var serializer = recipe.shaped()
                ? "extendedcrafting:shaped_flux_crafter"
                : "extendedcrafting:shapeless_flux_crafter";
        return customRecipe(serializer, fields, recipe);
    }

    /** 生成 AE2 充能器的单输入配方。 */
    private static String generateAe2Charger(RecipeDefinition recipe) {
        var fields = List.of(
                "    ingredient: " + ingredientJsonExpression(recipe.inputs().getFirst()),
                resultField(recipe.output())
        );
        return customRecipe("ae2:charger", fields, recipe);
    }

    /** 生成 AE2 压印器配方，并省略未使用的上、下压印槽。 */
    private static String generateAe2Inscriber(RecipeDefinition recipe) {
        var ingredientFields = new ArrayList<String>();
        if (recipe.inputs().get(0) != null) {
            ingredientFields.add("      top: " + ingredientJsonExpression(recipe.inputs().get(0)));
        }
        ingredientFields.add("      middle: " + ingredientJsonExpression(recipe.inputs().get(1)));
        if (recipe.inputs().get(2) != null) {
            ingredientFields.add("      bottom: " + ingredientJsonExpression(recipe.inputs().get(2)));
        }

        var fields = List.of(
                "    ingredients: {\n" + String.join(",\n", ingredientFields) + "\n    }",
                "    mode: " + quote(recipe.ae2Settings().inscriberMode().serializedName()),
                resultField(recipe.output())
        );
        return customRecipe("ae2:inscriber", fields, recipe);
    }

    /** 生成 AE2 地面物品转化配方及爆炸或流体条件。 */
    private static String generateAe2Transform(RecipeDefinition recipe) {
        var settings = recipe.ae2Settings();
        var circumstance = settings.transformCondition() == Ae2RecipeSettings.TransformCondition.EXPLOSION
                ? "    circumstance: { type: " + quote("explosion") + " }"
                : "    circumstance: { type: " + quote("fluid") + ", tag: "
                + quote(settings.transformFluidTag()) + " }";
        var fields = List.of(
                ingredientListField("ingredients", nonEmptyInputs(recipe)),
                circumstance,
                resultField(recipe.output())
        );
        return customRecipe("ae2:transform", fields, recipe);
    }

    /** 生成 AE2 熵变处理配方，输入和输出可分别选择方块、流体或掉落物。 */
    private static String generateAe2Entropy(RecipeDefinition recipe) {
        var settings = recipe.ae2Settings();
        var input = "    input: {\n"
                + "      " + settings.entropyInputType().serializedName() + ": { id: "
                + quote(settings.entropyInputId()) + " }\n"
                + "    }";
        var outputValue = settings.entropyOutputType() == Ae2RecipeSettings.EntropyOutputType.DROPS
                ? "[{ id: " + quote(settings.entropyOutputId()) + " }]"
                : "{ id: " + quote(settings.entropyOutputId()) + " }";
        var output = "    output: {\n"
                + "      " + settings.entropyOutputType().serializedName() + ": " + outputValue + "\n"
                + "    }";
        var fields = List.of(
                input,
                "    mode: " + quote(settings.entropyMode().serializedName()),
                output
        );
        return customRecipe("ae2:entropy", fields, recipe);
    }

    private static String customRecipe(String serializer, List<String> fields, RecipeDefinition recipe) {
        var allFields = new ArrayList<String>();
        allFields.add("    type: " + quote(serializer));
        allFields.addAll(fields);
        return "  event.custom({\n"
                + String.join(",\n", allFields)
                + "\n  }).id(" + quote(recipe.id()) + ");";
    }

    private static void appendPatternFields(List<String> fields, PatternData pattern) {
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

    private static void appendPatternRows(StringBuilder builder, List<String> rows, String indent) {
        for (var index = 0; index < rows.size(); index++) {
            builder.append(indent).append(quote(rows.get(index)));
            builder.append(index + 1 == rows.size() ? "\n" : ",\n");
        }
    }

    private static String ingredientListField(String name, List<IngredientSpec> ingredients) {
        return "    " + name + ": [\n"
                + ingredients.stream()
                .map(KubeJsRecipeGenerator::ingredientJsonExpression)
                .map(value -> "      " + value)
                .collect(Collectors.joining(",\n"))
                + "\n    ]";
    }

    private static String resultField(OutputSpec output) {
        return "    result: Item.of(" + outputExpression(output) + ")";
    }

    private static List<IngredientSpec> nonEmptyInputs(RecipeDefinition recipe) {
        return recipe.inputs().stream().filter(spec -> spec != null).toList();
    }

    /** 裁剪网格四周空白并为每种原料分配稳定图案字符。 */
    private static PatternData createPattern(RecipeDefinition recipe, int gridWidth, int gridHeight) {
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

    private static String ingredientExpression(IngredientSpec ingredient) {
        return switch (ingredient.kind()) {
            case ITEM -> quote(ingredient.value() + ingredient.componentPatch());
            case TAG -> quote("#" + ingredient.value());
            case ALTERNATIVES -> ingredient.alternatives().stream()
                    .map(KubeJsRecipeGenerator::ingredientExpression)
                    .collect(Collectors.joining(", ", "[", "]"));
        };
    }

    /** 让 event.custom 通过 KubeJS Codec 将标签、替代材料和数据组件编码为合法 JSON。 */
    private static String ingredientJsonExpression(IngredientSpec ingredient) {
        return "Ingredient.of(" + ingredientExpression(ingredient) + ")";
    }

    private static String outputExpression(OutputSpec output) {
        var prefix = output.count() == 1 ? "" : output.count() + "x ";
        return quote(prefix + output.itemId() + output.componentPatch());
    }

    private static String idExpression(RecipeDefinition recipe) {
        return ".id(" + quote(recipe.id()) + ")";
    }

    private static String quote(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static String number(float value) {
        return new BigDecimal(Float.toString(value)).stripTrailingZeros().toPlainString();
    }

    private record Bounds(int minRow, int maxRow, int minColumn, int maxColumn) {
    }

    private record PatternData(List<String> rows, Map<IngredientSpec, Character> symbols) {
    }
}
