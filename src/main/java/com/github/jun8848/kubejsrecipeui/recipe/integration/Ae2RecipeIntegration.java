package com.github.jun8848.kubejsrecipeui.recipe.integration;

import com.github.jun8848.kubejsrecipeui.recipe.Ae2RecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeDefinition;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;

import java.util.ArrayList;
import java.util.List;

import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.customRecipe;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.ingredientJsonExpression;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.ingredientListField;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.nonEmptyInputs;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.quote;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.resultField;

/** Applied Energistics 2 配方类型的注册、校验和脚本生成实现。 */
public final class Ae2RecipeIntegration implements RecipeIntegration {
    public static final String ID = "ae2";

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
        return LegacyRecipeIdSignatures.ae2(context);
    }

    @Override
    public void register(Registrar registrar) {
        registrar.register(spec(WorkstationType.AE2_CHARGER, RecipeValidator.NONE, this::generateCharger));
        registrar.register(spec(
                WorkstationType.AE2_INSCRIBER,
                context -> {
                    if (context.inputs().get(1) == null) {
                        throw new IllegalArgumentException("压印器配方需要中间输入材料");
                    }
                },
                this::generateInscriber
        ));
        registrar.register(spec(WorkstationType.AE2_TRANSFORM, RecipeValidator.NONE, this::generateTransform));
        registrar.register(new RecipeTypeSpec(
                WorkstationType.AE2_ENTROPY,
                WorkstationType.AE2_CHARGER,
                false,
                false,
                RecipeTypeCapabilities.SIMPLE,
                defaultSettings(),
                context -> context.settings(Ae2RecipeSettings.class).validateFor(context.type()),
                context -> context.settings(Ae2RecipeSettings.class).entropyOutputId(),
                this::generateEntropy
        ));
    }

    private RecipeTypeSpec spec(
            WorkstationType type,
            RecipeValidator additionalValidator,
            RecipeBodyGenerator generator
    ) {
        return new RecipeTypeSpec(
                type,
                WorkstationType.AE2_CHARGER,
                true,
                true,
                RecipeTypeCapabilities.SIMPLE,
                defaultSettings(),
                context -> {
                    additionalValidator.validate(context);
                    context.settings(Ae2RecipeSettings.class).validateFor(context.type());
                },
                context -> context.output().itemId(),
                generator
        );
    }

    private RecipeSettings defaultSettings() {
        return RecipeSettings.of(Ae2RecipeSettings.defaults());
    }

    private String generateCharger(RecipeDefinition recipe) {
        var fields = List.of(
                "    ingredient: " + ingredientJsonExpression(recipe.inputs().getFirst()),
                resultField(recipe.output())
        );
        return customRecipe("ae2:charger", fields, recipe);
    }

    private String generateInscriber(RecipeDefinition recipe) {
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
                "    mode: " + quote(settings(recipe).inscriberMode().serializedName()),
                resultField(recipe.output())
        );
        return customRecipe("ae2:inscriber", fields, recipe);
    }

    private String generateTransform(RecipeDefinition recipe) {
        var settings = settings(recipe);
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

    private String generateEntropy(RecipeDefinition recipe) {
        var settings = settings(recipe);
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

    private static Ae2RecipeSettings settings(RecipeDefinition recipe) {
        return recipe.settings(Ae2RecipeSettings.class);
    }
}
