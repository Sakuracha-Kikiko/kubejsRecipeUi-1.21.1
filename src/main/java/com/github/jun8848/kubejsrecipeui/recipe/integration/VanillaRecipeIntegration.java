package com.github.jun8848.kubejsrecipeui.recipe.integration;

import com.github.jun8848.kubejsrecipeui.recipe.RecipeDefinition;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.appendPatternRows;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.createPattern;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.idExpression;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.ingredientExpression;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.number;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.outputExpression;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.quote;

/** 原版工作站的校验和 KubeJS 代码生成实现。 */
public final class VanillaRecipeIntegration implements RecipeIntegration {
    public static final String ID = "minecraft";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String generatedIdSignature(RecipeValidationContext context) {
        return LegacyRecipeIdSignatures.vanilla();
    }

    @Override
    public void register(Registrar registrar) {
        registrar.register(RecipeTypeSpec.shaped(
                WorkstationType.CRAFTING,
                WorkstationType.CRAFTING,
                this::generateCrafting
        ));
        registrar.register(RecipeTypeSpec.cooking(
                WorkstationType.FURNACE,
                WorkstationType.CRAFTING,
                200,
                recipe -> generateCooking("smelting", recipe)
        ));
        registrar.register(RecipeTypeSpec.cooking(
                WorkstationType.SMOKER,
                WorkstationType.CRAFTING,
                200,
                recipe -> generateCooking("smoking", recipe)
        ));
        registrar.register(RecipeTypeSpec.cooking(
                WorkstationType.BLAST_FURNACE,
                WorkstationType.CRAFTING,
                200,
                recipe -> generateCooking("blasting", recipe)
        ));
        registrar.register(RecipeTypeSpec.cooking(
                WorkstationType.CAMPFIRE,
                WorkstationType.CRAFTING,
                600,
                recipe -> generateCooking("campfireCooking", recipe)
        ));
        registrar.register(RecipeTypeSpec.standard(
                WorkstationType.STONECUTTER,
                WorkstationType.CRAFTING,
                recipe -> generateSingleInput("stonecutting", recipe)
        ));
        registrar.register(new RecipeTypeSpec(
                WorkstationType.SMITHING,
                WorkstationType.CRAFTING,
                true,
                true,
                RecipeTypeCapabilities.SIMPLE,
                RecipeSettings.EMPTY,
                context -> {
                    if (context.inputs().stream().anyMatch(input -> input == null)) {
                        throw new IllegalArgumentException("锻造配方需要模板、基础物品和追加材料");
                    }
                },
                context -> context.output().itemId(),
                this::generateSmithing
        ));
    }

    private String generateCrafting(RecipeDefinition recipe) {
        return recipe.shaped() ? generateShaped(recipe) : generateShapeless(recipe);
    }

    private String generateCooking(String method, RecipeDefinition recipe) {
        return "  event." + method + "(" + outputExpression(recipe.output()) + ", "
                + ingredientExpression(recipe.inputs().getFirst()) + ")"
                + ".xp(" + number(recipe.experience()) + ")"
                + ".cookingTime(" + recipe.cookingTime() + ")"
                + idExpression(recipe) + ";";
    }

    private String generateSingleInput(String method, RecipeDefinition recipe) {
        return "  event." + method + "(" + outputExpression(recipe.output()) + ", "
                + ingredientExpression(recipe.inputs().getFirst()) + ")"
                + idExpression(recipe) + ";";
    }

    private String generateSmithing(RecipeDefinition recipe) {
        return "  event.smithing(" + outputExpression(recipe.output()) + ", "
                + ingredientExpression(recipe.inputs().get(0)) + ", "
                + ingredientExpression(recipe.inputs().get(1)) + ", "
                + ingredientExpression(recipe.inputs().get(2)) + ")"
                + idExpression(recipe) + ";";
    }

    private String generateShaped(RecipeDefinition recipe) {
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

    private String generateShapeless(RecipeDefinition recipe) {
        var ingredients = recipe.inputs().stream()
                .filter(spec -> spec != null)
                .map(com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder::ingredientExpression)
                .collect(Collectors.joining(",\n    "));
        return "  event.shapeless(" + outputExpression(recipe.output()) + ", [\n"
                + "    " + ingredients + "\n"
                + "  ])" + idExpression(recipe) + ";";
    }
}
