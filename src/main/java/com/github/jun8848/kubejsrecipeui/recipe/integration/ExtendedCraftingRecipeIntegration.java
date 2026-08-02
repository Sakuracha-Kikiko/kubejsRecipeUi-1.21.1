package com.github.jun8848.kubejsrecipeui.recipe.integration;

import com.github.jun8848.kubejsrecipeui.recipe.ExtendedRecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeDefinition;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;

import java.util.ArrayList;

import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.appendPatternFields;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.createPattern;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.customRecipe;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.ingredientJsonExpression;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.ingredientListField;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.nonEmptyInputs;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.resultField;

/** Extended Crafting 工作站的注册、校验和脚本生成实现。 */
public final class ExtendedCraftingRecipeIntegration implements RecipeIntegration {
    public static final String ID = "extendedcrafting";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String requiredModId() {
        return ID;
    }

    @Override
    public String generatedIdSignature(RecipeValidationContext context) {
        return LegacyRecipeIdSignatures.extended(context);
    }

    @Override
    public void register(Registrar registrar) {
        registerTable(registrar, WorkstationType.EXTENDED_BASIC_TABLE);
        registerTable(registrar, WorkstationType.EXTENDED_ADVANCED_TABLE);
        registerTable(registrar, WorkstationType.EXTENDED_ELITE_TABLE);
        registerTable(registrar, WorkstationType.EXTENDED_ULTIMATE_TABLE);
        registrar.register(spec(
                WorkstationType.EXTENDED_COMBINATION,
                WorkstationType.EXTENDED_BASIC_TABLE,
                RecipeTypeCapabilities.SIMPLE,
                context -> {
                    if (context.inputs().getFirst() == null) {
                        throw new IllegalArgumentException("组合合成需要中心输入物品");
                    }
                    if (context.inputs().subList(1, context.inputs().size()).stream().allMatch(input -> input == null)) {
                        throw new IllegalArgumentException("组合合成至少需要一个基座材料");
                    }
                },
                this::generateCombination
        ));
        registrar.register(spec(
                WorkstationType.EXTENDED_COMPRESSOR,
                WorkstationType.EXTENDED_BASIC_TABLE,
                RecipeTypeCapabilities.SIMPLE,
                context -> {
                    if (context.inputs().stream().anyMatch(input -> input == null)) {
                        throw new IllegalArgumentException("压缩机配方需要压缩材料和催化剂");
                    }
                },
                this::generateCompressor
        ));
        registrar.register(spec(
                WorkstationType.EXTENDED_ENDER_CRAFTER,
                WorkstationType.EXTENDED_BASIC_TABLE,
                RecipeTypeCapabilities.SHAPED,
                RecipeValidator.NONE,
                this::generateEnderCrafter
        ));
        registrar.register(spec(
                WorkstationType.EXTENDED_FLUX_CRAFTER,
                WorkstationType.EXTENDED_BASIC_TABLE,
                RecipeTypeCapabilities.SHAPED,
                RecipeValidator.NONE,
                this::generateFluxCrafter
        ));
    }

    private void registerTable(Registrar registrar, WorkstationType type) {
        registrar.register(spec(
                type,
                WorkstationType.EXTENDED_BASIC_TABLE,
                RecipeTypeCapabilities.SHAPED,
                RecipeValidator.NONE,
                this::generateTable
        ));
    }

    private RecipeTypeSpec spec(
            WorkstationType type,
            WorkstationType navigationType,
            RecipeTypeCapabilities capabilities,
            RecipeValidator validator,
            RecipeBodyGenerator generator
    ) {
        return new RecipeTypeSpec(
                type,
                navigationType,
                true,
                true,
                capabilities,
                RecipeSettings.of(ExtendedRecipeSettings.defaults(type)),
                validator,
                context -> context.output().itemId(),
                generator
        );
    }

    private String generateTable(RecipeDefinition recipe) {
        var fields = new ArrayList<String>();
        if (recipe.shaped()) {
            appendPatternFields(fields, createPattern(recipe, recipe.type().gridWidth(), recipe.type().gridHeight()));
        } else {
            fields.add(ingredientListField("ingredients", nonEmptyInputs(recipe)));
        }
        fields.add(resultField(recipe.output()));
        fields.add("    tier: " + settings(recipe).tier());
        var serializer = recipe.shaped() ? "extendedcrafting:shaped_table" : "extendedcrafting:shapeless_table";
        return customRecipe(serializer, fields, recipe);
    }

    private String generateCombination(RecipeDefinition recipe) {
        var fields = new ArrayList<String>();
        fields.add("    input: " + ingredientJsonExpression(recipe.inputs().getFirst()));
        fields.add(ingredientListField("ingredients", recipe.inputs().subList(1, recipe.inputs().size()).stream()
                .filter(spec -> spec != null)
                .toList()));
        fields.add(resultField(recipe.output()));
        fields.add("    power_cost: " + settings(recipe).powerCost());
        fields.add("    power_rate: " + settings(recipe).powerRate());
        return customRecipe("extendedcrafting:combination", fields, recipe);
    }

    private String generateCompressor(RecipeDefinition recipe) {
        var settings = settings(recipe);
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

    private String generateEnderCrafter(RecipeDefinition recipe) {
        var fields = new ArrayList<String>();
        if (recipe.shaped()) {
            appendPatternFields(fields, createPattern(recipe, 3, 3));
        } else {
            fields.add(ingredientListField("ingredients", nonEmptyInputs(recipe)));
        }
        fields.add(resultField(recipe.output()));
        fields.add("    crafting_time: " + settings(recipe).craftingTime());
        var serializer = recipe.shaped()
                ? "extendedcrafting:shaped_ender_crafter"
                : "extendedcrafting:shapeless_ender_crafter";
        return customRecipe(serializer, fields, recipe);
    }

    private String generateFluxCrafter(RecipeDefinition recipe) {
        var fields = new ArrayList<String>();
        if (recipe.shaped()) {
            appendPatternFields(fields, createPattern(recipe, 3, 3));
        } else {
            fields.add(ingredientListField("ingredients", nonEmptyInputs(recipe)));
        }
        fields.add(resultField(recipe.output()));
        fields.add("    power_required: " + settings(recipe).powerCost());
        fields.add("    power_rate: " + settings(recipe).powerRate());
        var serializer = recipe.shaped()
                ? "extendedcrafting:shaped_flux_crafter"
                : "extendedcrafting:shapeless_flux_crafter";
        return customRecipe(serializer, fields, recipe);
    }

    private static ExtendedRecipeSettings settings(RecipeDefinition recipe) {
        return recipe.settings(ExtendedRecipeSettings.class);
    }
}
