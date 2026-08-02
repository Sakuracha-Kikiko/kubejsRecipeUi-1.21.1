package com.github.jun8848.kubejsrecipeui.recipe.integration;

import com.github.jun8848.kubejsrecipeui.recipe.MekanismRecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.MekanismRecipeSettings.ResourceStack;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeDefinition;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;

import java.util.ArrayList;
import java.util.List;

import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.customRecipe;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.ingredientJsonExpression;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.outputExpression;
import static com.github.jun8848.kubejsrecipeui.recipe.codegen.RecipeCodeBuilder.quote;

/** Mekanism 26.1 正式数据包配方类型的注册、校验和 KubeJS 代码生成。 */
public final class MekanismRecipeIntegration implements RecipeIntegration {
    public static final String ID = "mekanism";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String requiredModId() {
        return ID;
    }

    @Override
    public void register(Registrar registrar) {
        for (var type : WorkstationType.values()) {
            if (type.name().startsWith("MEKANISM_")) {
                registrar.register(spec(type));
            }
        }
    }

    private RecipeTypeSpec spec(WorkstationType type) {
        return new RecipeTypeSpec(
                type,
                WorkstationType.MEKANISM_CRUSHING,
                type.inputCount() > 0,
                requiresItemOutput(type),
                RecipeTypeCapabilities.SIMPLE,
                RecipeSettings.of(MekanismRecipeSettings.defaults(type)),
                this::validate,
                this::generatedIdSource,
                this::generate
        );
    }

    private void validate(RecipeValidationContext context) {
        var type = context.type();
        var settings = context.settings(MekanismRecipeSettings.class);
        if (type == WorkstationType.MEKANISM_COMBINING && context.inputs().stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException("Mekanism 合并配方需要主材料和额外材料");
        }

        switch (type) {
            case MEKANISM_CHEMICAL_INFUSING, MEKANISM_PIGMENT_MIXING -> {
                requireChemicalInput(settings.primaryChemicalInput(), "左侧化学品输入");
                requireChemicalInput(settings.secondaryChemicalInput(), "右侧化学品输入");
                requireChemicalOutput(settings.primaryChemicalOutput(), "化学品输出");
            }
            case MEKANISM_SEPARATING -> {
                requireFluidInput(settings.fluidInput(), "流体输入");
                requireChemicalOutput(settings.primaryChemicalOutput(), "左侧化学品输出");
                requireChemicalOutput(settings.secondaryChemicalOutput(), "右侧化学品输出");
            }
            case MEKANISM_WASHING -> {
                requireFluidInput(settings.fluidInput(), "流体输入");
                requireChemicalInput(settings.primaryChemicalInput(), "化学品输入");
                requireChemicalOutput(settings.primaryChemicalOutput(), "化学品输出");
            }
            case MEKANISM_EVAPORATING -> {
                requireFluidInput(settings.fluidInput(), "流体输入");
                requireFluidOutput(settings.fluidOutput(), "流体输出");
            }
            case MEKANISM_ACTIVATING, MEKANISM_CENTRIFUGING -> {
                requireChemicalInput(settings.primaryChemicalInput(), "化学品输入");
                requireChemicalOutput(settings.primaryChemicalOutput(), "化学品输出");
            }
            case MEKANISM_CRYSTALLIZING -> requireChemicalInput(settings.primaryChemicalInput(), "化学品输入");
            case MEKANISM_DISSOLUTION -> {
                requireChemicalInput(settings.primaryChemicalInput(), "化学品输入");
                requireChemicalOutput(settings.primaryChemicalOutput(), "化学品输出");
            }
            case MEKANISM_COMPRESSING, MEKANISM_PURIFYING, MEKANISM_INJECTING,
                    MEKANISM_NUCLEOSYNTHESIZING, MEKANISM_METALLURGIC_INFUSING, MEKANISM_PAINTING ->
                    requireChemicalInput(settings.primaryChemicalInput(), "化学品输入");
            case MEKANISM_CHEMICAL_CONVERSION, MEKANISM_OXIDIZING, MEKANISM_PIGMENT_EXTRACTING ->
                    requireChemicalOutput(settings.primaryChemicalOutput(), "化学品输出");
            case MEKANISM_REACTION -> {
                requireFluidInput(settings.fluidInput(), "流体输入");
                requireChemicalInput(settings.primaryChemicalInput(), "化学品输入");
                if (context.output() == null && settings.primaryChemicalOutput().id().isBlank()) {
                    throw new IllegalArgumentException("加压反应配方至少需要一个物品或化学品输出");
                }
                if (!settings.primaryChemicalOutput().id().isBlank()) {
                    requireChemicalOutput(settings.primaryChemicalOutput(), "化学品输出");
                }
            }
            case MEKANISM_ROTARY -> validateRotary(settings);
            case MEKANISM_SAWING -> {
                if (context.output() == null && settings.secondaryItemOutputId().isBlank()) {
                    throw new IllegalArgumentException("锯切配方至少需要一个主产物或副产物");
                }
                if (!settings.secondaryItemOutputId().isBlank()) {
                    MekanismRecipeSettings.requireItemId(settings.secondaryItemOutputId(), "锯切副产物");
                }
            }
            default -> {
                // 纯物品和能量配方无需额外资源校验。
            }
        }
    }

    private String generatedIdSource(RecipeValidationContext context) {
        if (context.output() != null) {
            return context.output().itemId();
        }
        var settings = context.settings(MekanismRecipeSettings.class);
        return switch (context.type()) {
            case MEKANISM_EVAPORATING -> settings.fluidOutput().id();
            case MEKANISM_ROTARY -> settings.rotaryMode() == MekanismRecipeSettings.RotaryMode.FLUID_TO_CHEMICAL
                    ? settings.primaryChemicalOutput().id()
                    : settings.fluidOutput().id();
            case MEKANISM_ENERGY_CONVERSION -> "mekanism:energy_conversion";
            case MEKANISM_SAWING -> settings.secondaryItemOutputId();
            default -> settings.primaryChemicalOutput().id();
        };
    }

    private String generate(RecipeDefinition recipe) {
        var settings = recipe.settings(MekanismRecipeSettings.class);
        var fields = new ArrayList<String>();
        switch (recipe.type()) {
            case MEKANISM_CRUSHING, MEKANISM_ENRICHING, MEKANISM_SMELTING -> {
                fields.add(itemInputField("input", recipe.inputs().getFirst(), settings.itemInputCount()));
                fields.add(itemOutputField("output", recipe.output()));
            }
            case MEKANISM_COMBINING -> {
                fields.add(itemInputField("main_input", recipe.inputs().get(0), settings.itemInputCount()));
                fields.add(itemInputField("extra_input", recipe.inputs().get(1), settings.extraItemInputCount()));
                fields.add(itemOutputField("output", recipe.output()));
            }
            case MEKANISM_CHEMICAL_INFUSING, MEKANISM_PIGMENT_MIXING -> {
                fields.add(chemicalInputField("left_input", settings.primaryChemicalInput()));
                fields.add(chemicalInputField("right_input", settings.secondaryChemicalInput()));
                fields.add(resourceOutputField("output", settings.primaryChemicalOutput()));
            }
            case MEKANISM_SEPARATING -> {
                fields.add(fluidInputField("input", settings.fluidInput()));
                if (settings.energyMultiplier() != 1) {
                    fields.add("    energy_multiplier: " + settings.energyMultiplier());
                }
                fields.add(resourceOutputField("left_chemical_output", settings.primaryChemicalOutput()));
                fields.add(resourceOutputField("right_chemical_output", settings.secondaryChemicalOutput()));
            }
            case MEKANISM_WASHING -> {
                fields.add(fluidInputField("fluid_input", settings.fluidInput()));
                fields.add(chemicalInputField("chemical_input", settings.primaryChemicalInput()));
                fields.add(resourceOutputField("output", settings.primaryChemicalOutput()));
            }
            case MEKANISM_EVAPORATING -> {
                fields.add(fluidInputField("input", settings.fluidInput()));
                fields.add(resourceOutputField("output", settings.fluidOutput()));
            }
            case MEKANISM_ACTIVATING, MEKANISM_CENTRIFUGING -> {
                fields.add(chemicalInputField("input", settings.primaryChemicalInput()));
                fields.add(resourceOutputField("output", settings.primaryChemicalOutput()));
            }
            case MEKANISM_CRYSTALLIZING -> {
                fields.add(chemicalInputField("input", settings.primaryChemicalInput()));
                fields.add(itemOutputField("output", recipe.output()));
            }
            case MEKANISM_DISSOLUTION -> {
                fields.add(itemInputField("item_input", recipe.inputs().getFirst(), settings.itemInputCount()));
                fields.add(chemicalInputField("chemical_input", settings.primaryChemicalInput()));
                fields.add(resourceOutputField("output", settings.primaryChemicalOutput()));
                fields.add("    per_tick_usage: " + settings.perTickUsage());
            }
            case MEKANISM_COMPRESSING, MEKANISM_PURIFYING, MEKANISM_INJECTING,
                    MEKANISM_METALLURGIC_INFUSING, MEKANISM_PAINTING -> {
                fields.add(itemInputField("item_input", recipe.inputs().getFirst(), settings.itemInputCount()));
                fields.add(chemicalInputField("chemical_input", settings.primaryChemicalInput()));
                fields.add(itemOutputField("output", recipe.output()));
                fields.add("    per_tick_usage: " + settings.perTickUsage());
            }
            case MEKANISM_NUCLEOSYNTHESIZING -> {
                fields.add(itemInputField("item_input", recipe.inputs().getFirst(), settings.itemInputCount()));
                fields.add(chemicalInputField("chemical_input", settings.primaryChemicalInput()));
                fields.add(itemOutputField("output", recipe.output()));
                fields.add("    duration: " + settings.duration());
                fields.add("    per_tick_usage: " + settings.perTickUsage());
            }
            case MEKANISM_ENERGY_CONVERSION -> {
                fields.add(itemInputField("input", recipe.inputs().getFirst(), settings.itemInputCount()));
                fields.add("    output: " + settings.energyOutput());
            }
            case MEKANISM_CHEMICAL_CONVERSION, MEKANISM_OXIDIZING, MEKANISM_PIGMENT_EXTRACTING -> {
                fields.add(itemInputField("input", recipe.inputs().getFirst(), settings.itemInputCount()));
                fields.add(resourceOutputField("output", settings.primaryChemicalOutput()));
            }
            case MEKANISM_REACTION -> generateReactionFields(fields, recipe, settings);
            case MEKANISM_ROTARY -> generateRotaryFields(fields, settings);
            case MEKANISM_SAWING -> generateSawingFields(fields, recipe, settings);
            default -> throw new IllegalArgumentException("不是 Mekanism 配方类型: " + recipe.type());
        }
        return customRecipe(serializer(recipe.type()), fields, recipe);
    }

    private static void generateReactionFields(
            List<String> fields,
            RecipeDefinition recipe,
            MekanismRecipeSettings settings
    ) {
        fields.add(itemInputField("item_input", recipe.inputs().getFirst(), settings.itemInputCount()));
        fields.add(fluidInputField("fluid_input", settings.fluidInput()));
        fields.add(chemicalInputField("chemical_input", settings.primaryChemicalInput()));
        if (settings.energyRequired() > 0) {
            fields.add("    energy_required: " + settings.energyRequired());
        }
        fields.add("    duration: " + settings.duration());
        if (recipe.output() != null) {
            fields.add(itemOutputField("item_output", recipe.output()));
        }
        if (!settings.primaryChemicalOutput().id().isBlank()) {
            fields.add(resourceOutputField("chemical_output", settings.primaryChemicalOutput()));
        }
    }

    private static void generateRotaryFields(List<String> fields, MekanismRecipeSettings settings) {
        if (settings.rotaryMode() != MekanismRecipeSettings.RotaryMode.CHEMICAL_TO_FLUID) {
            fields.add(fluidInputField("fluid_input", settings.fluidInput()));
            fields.add(resourceOutputField("chemical_output", settings.primaryChemicalOutput()));
        }
        if (settings.rotaryMode() != MekanismRecipeSettings.RotaryMode.FLUID_TO_CHEMICAL) {
            fields.add(chemicalInputField("chemical_input", settings.primaryChemicalInput()));
            fields.add(resourceOutputField("fluid_output", settings.fluidOutput()));
        }
    }

    private static void generateSawingFields(
            List<String> fields,
            RecipeDefinition recipe,
            MekanismRecipeSettings settings
    ) {
        fields.add(itemInputField("input", recipe.inputs().getFirst(), settings.itemInputCount()));
        if (recipe.output() != null) {
            fields.add(itemOutputField("main_output", recipe.output()));
        }
        if (!settings.secondaryItemOutputId().isBlank()) {
            var countPrefix = settings.secondaryItemOutputCount() == 1
                    ? ""
                    : settings.secondaryItemOutputCount() + "x ";
            fields.add("    secondary_output: Item.of("
                    + quote(countPrefix + settings.secondaryItemOutputId()) + ")");
            fields.add("    secondary_chance: " + settings.secondaryChance());
        }
    }

    private static void validateRotary(MekanismRecipeSettings settings) {
        if (settings.rotaryMode() != MekanismRecipeSettings.RotaryMode.CHEMICAL_TO_FLUID) {
            requireFluidInput(settings.fluidInput(), "流体输入");
            requireChemicalOutput(settings.primaryChemicalOutput(), "化学品输出");
        }
        if (settings.rotaryMode() != MekanismRecipeSettings.RotaryMode.FLUID_TO_CHEMICAL) {
            requireChemicalInput(settings.primaryChemicalInput(), "化学品输入");
            requireFluidOutput(settings.fluidOutput(), "流体输出");
        }
    }

    private static boolean requiresItemOutput(WorkstationType type) {
        return switch (type) {
            case MEKANISM_CRUSHING, MEKANISM_ENRICHING, MEKANISM_SMELTING, MEKANISM_COMBINING,
                    MEKANISM_CRYSTALLIZING, MEKANISM_COMPRESSING, MEKANISM_PURIFYING,
                    MEKANISM_INJECTING, MEKANISM_NUCLEOSYNTHESIZING,
                    MEKANISM_METALLURGIC_INFUSING, MEKANISM_PAINTING -> true;
            default -> false;
        };
    }

    private static String serializer(WorkstationType type) {
        return "mekanism:" + type.name().substring("MEKANISM_".length()).toLowerCase(java.util.Locale.ROOT);
    }

    private static String itemInputField(String name, com.github.jun8848.kubejsrecipeui.recipe.IngredientSpec input, int count) {
        return "    " + name + ": {\n"
                + "      count: " + count + ",\n"
                + "      ingredient: " + ingredientJsonExpression(input) + "\n"
                + "    }";
    }

    private static String itemOutputField(String name, com.github.jun8848.kubejsrecipeui.recipe.OutputSpec output) {
        return "    " + name + ": Item.of(" + outputExpression(output) + ")";
    }

    private static String chemicalInputField(String name, ResourceStack value) {
        var tag = value.id().startsWith("#");
        var id = tag ? value.id().substring(1) : value.id();
        return "    " + name + ": {\n"
                + "      amount: " + value.amount() + ",\n"
                + "      " + (tag ? "tag" : "chemical") + ": " + quote(id) + "\n"
                + "    }";
    }

    private static String fluidInputField(String name, ResourceStack value) {
        return "    " + name + ": {\n"
                + "      amount: " + value.amount() + ",\n"
                + "      ingredient: " + quote(value.id()) + "\n"
                + "    }";
    }

    private static String resourceOutputField(String name, ResourceStack value) {
        return "    " + name + ": {\n"
                + "      id: " + quote(value.id()) + ",\n"
                + "      amount: " + value.amount() + "\n"
                + "    }";
    }

    private static void requireChemicalInput(ResourceStack value, String fieldName) {
        MekanismRecipeSettings.requireResource(value, fieldName, true);
    }

    private static void requireChemicalOutput(ResourceStack value, String fieldName) {
        MekanismRecipeSettings.requireResource(value, fieldName, false);
    }

    private static void requireFluidInput(ResourceStack value, String fieldName) {
        MekanismRecipeSettings.requireResource(value, fieldName, true);
    }

    private static void requireFluidOutput(ResourceStack value, String fieldName) {
        MekanismRecipeSettings.requireResource(value, fieldName, false);
    }
}
