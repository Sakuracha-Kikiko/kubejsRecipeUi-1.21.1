package com.github.jun8848.kubejsrecipeui.recipe;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Mekanism 机器配方共用的非物品资源和数值参数。
 * 未被当前配方类型使用的字段保留默认值，不会写入生成代码。
 */
public record MekanismRecipeSettings(
        ResourceStack primaryChemicalInput,
        ResourceStack secondaryChemicalInput,
        ResourceStack primaryChemicalOutput,
        ResourceStack secondaryChemicalOutput,
        ResourceStack fluidInput,
        ResourceStack fluidOutput,
        int itemInputCount,
        int extraItemInputCount,
        int energyOutput,
        int duration,
        int energyRequired,
        int energyMultiplier,
        boolean perTickUsage,
        RotaryMode rotaryMode,
        String secondaryItemOutputId,
        int secondaryItemOutputCount,
        double secondaryChance
) implements RecipeIntegrationSettings {
    private static final Pattern RESOURCE_ID_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    public MekanismRecipeSettings {
        primaryChemicalInput = Objects.requireNonNull(primaryChemicalInput, "primaryChemicalInput");
        secondaryChemicalInput = Objects.requireNonNull(secondaryChemicalInput, "secondaryChemicalInput");
        primaryChemicalOutput = Objects.requireNonNull(primaryChemicalOutput, "primaryChemicalOutput");
        secondaryChemicalOutput = Objects.requireNonNull(secondaryChemicalOutput, "secondaryChemicalOutput");
        fluidInput = Objects.requireNonNull(fluidInput, "fluidInput");
        fluidOutput = Objects.requireNonNull(fluidOutput, "fluidOutput");
        rotaryMode = Objects.requireNonNull(rotaryMode, "rotaryMode");
        secondaryItemOutputId = normalizeResourceId(secondaryItemOutputId, true);
        if (itemInputCount < 1 || extraItemInputCount < 1 || energyOutput < 1 || duration < 1
                || energyRequired < 0 || energyMultiplier < 1 || secondaryItemOutputCount < 1) {
            throw new IllegalArgumentException("Mekanism 配方数量和时间参数超出有效范围");
        }
        if (!Double.isFinite(secondaryChance) || secondaryChance <= 0.0D || secondaryChance > 1.0D) {
            throw new IllegalArgumentException("Mekanism 锯切副产物概率必须大于 0 且不超过 1");
        }
    }

    /** 创建适合新配方编辑会话的稳定默认值。 */
    public static MekanismRecipeSettings defaults() {
        return new MekanismRecipeSettings(
                new ResourceStack("mekanism:oxygen", 1),
                new ResourceStack("mekanism:hydrogen", 1),
                new ResourceStack("mekanism:hydrogen", 1),
                new ResourceStack("mekanism:oxygen", 1),
                new ResourceStack("minecraft:water", 1),
                new ResourceStack("minecraft:water", 1),
                1,
                1,
                1000,
                100,
                0,
                1,
                false,
                RotaryMode.BOTH,
                "",
                1,
                1.0D
        );
    }

    /** 按配方类型清空合法的可选输出，避免新建配方时生成玩家未选择的副产物。 */
    public static MekanismRecipeSettings defaults(WorkstationType type) {
        var settings = defaults();
        if (type == WorkstationType.MEKANISM_REACTION) {
            settings = settings.withResource(ResourceField.PRIMARY_CHEMICAL_OUTPUT, new ResourceStack("", 1));
        }
        return settings;
    }

    /** 返回替换指定资源字段后的新设置。 */
    public MekanismRecipeSettings withResource(ResourceField field, ResourceStack value) {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(value, "value");
        return new MekanismRecipeSettings(
                field == ResourceField.PRIMARY_CHEMICAL_INPUT ? value : primaryChemicalInput,
                field == ResourceField.SECONDARY_CHEMICAL_INPUT ? value : secondaryChemicalInput,
                field == ResourceField.PRIMARY_CHEMICAL_OUTPUT ? value : primaryChemicalOutput,
                field == ResourceField.SECONDARY_CHEMICAL_OUTPUT ? value : secondaryChemicalOutput,
                field == ResourceField.FLUID_INPUT ? value : fluidInput,
                field == ResourceField.FLUID_OUTPUT ? value : fluidOutput,
                itemInputCount, extraItemInputCount, energyOutput, duration, energyRequired, energyMultiplier,
                perTickUsage, rotaryMode, secondaryItemOutputId, secondaryItemOutputCount, secondaryChance
        );
    }

    public ResourceStack resource(ResourceField field) {
        return switch (field) {
            case PRIMARY_CHEMICAL_INPUT -> primaryChemicalInput;
            case SECONDARY_CHEMICAL_INPUT -> secondaryChemicalInput;
            case PRIMARY_CHEMICAL_OUTPUT -> primaryChemicalOutput;
            case SECONDARY_CHEMICAL_OUTPUT -> secondaryChemicalOutput;
            case FLUID_INPUT -> fluidInput;
            case FLUID_OUTPUT -> fluidOutput;
        };
    }

    public MekanismRecipeSettings withItemInputCount(int value) {
        return copy(value, extraItemInputCount, energyOutput, duration, energyRequired, energyMultiplier,
                perTickUsage, rotaryMode, secondaryItemOutputId, secondaryItemOutputCount, secondaryChance);
    }

    public MekanismRecipeSettings withExtraItemInputCount(int value) {
        return copy(itemInputCount, value, energyOutput, duration, energyRequired, energyMultiplier,
                perTickUsage, rotaryMode, secondaryItemOutputId, secondaryItemOutputCount, secondaryChance);
    }

    public MekanismRecipeSettings withEnergyOutput(int value) {
        return copy(itemInputCount, extraItemInputCount, value, duration, energyRequired, energyMultiplier,
                perTickUsage, rotaryMode, secondaryItemOutputId, secondaryItemOutputCount, secondaryChance);
    }

    public MekanismRecipeSettings withDuration(int value) {
        return copy(itemInputCount, extraItemInputCount, energyOutput, value, energyRequired, energyMultiplier,
                perTickUsage, rotaryMode, secondaryItemOutputId, secondaryItemOutputCount, secondaryChance);
    }

    public MekanismRecipeSettings withEnergyRequired(int value) {
        return copy(itemInputCount, extraItemInputCount, energyOutput, duration, value, energyMultiplier,
                perTickUsage, rotaryMode, secondaryItemOutputId, secondaryItemOutputCount, secondaryChance);
    }

    public MekanismRecipeSettings withEnergyMultiplier(int value) {
        return copy(itemInputCount, extraItemInputCount, energyOutput, duration, energyRequired, value,
                perTickUsage, rotaryMode, secondaryItemOutputId, secondaryItemOutputCount, secondaryChance);
    }

    public MekanismRecipeSettings withPerTickUsage(boolean value) {
        return copy(itemInputCount, extraItemInputCount, energyOutput, duration, energyRequired, energyMultiplier,
                value, rotaryMode, secondaryItemOutputId, secondaryItemOutputCount, secondaryChance);
    }

    public MekanismRecipeSettings withRotaryMode(RotaryMode value) {
        return copy(itemInputCount, extraItemInputCount, energyOutput, duration, energyRequired, energyMultiplier,
                perTickUsage, value, secondaryItemOutputId, secondaryItemOutputCount, secondaryChance);
    }

    public MekanismRecipeSettings withSecondaryItemOutput(String id, int count, double chance) {
        return copy(itemInputCount, extraItemInputCount, energyOutput, duration, energyRequired, energyMultiplier,
                perTickUsage, rotaryMode, id, count, chance);
    }

    /** 校验当前配方实际使用的资源 ID；输入资源允许使用以 # 开头的标签。 */
    public static void requireResource(ResourceStack value, String fieldName, boolean allowTag) {
        var id = value.id();
        var tag = id.startsWith("#");
        var plainId = tag ? id.substring(1) : id;
        if ((!allowTag && tag) || !RESOURCE_ID_PATTERN.matcher(plainId).matches()) {
            throw new IllegalArgumentException(fieldName + "必须是 namespace:path" + (allowTag ? " 或 #namespace:tag" : "") + " 格式");
        }
    }

    public static void requireItemId(String value, String fieldName) {
        var normalized = normalizeResourceId(value, false);
        if (normalized.startsWith("#") || !RESOURCE_ID_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(fieldName + "必须是 namespace:path 格式");
        }
    }

    private MekanismRecipeSettings copy(
            int newItemInputCount,
            int newExtraItemInputCount,
            int newEnergyOutput,
            int newDuration,
            int newEnergyRequired,
            int newEnergyMultiplier,
            boolean newPerTickUsage,
            RotaryMode newRotaryMode,
            String newSecondaryItemOutputId,
            int newSecondaryItemOutputCount,
            double newSecondaryChance
    ) {
        return new MekanismRecipeSettings(
                primaryChemicalInput, secondaryChemicalInput, primaryChemicalOutput, secondaryChemicalOutput,
                fluidInput, fluidOutput, newItemInputCount, newExtraItemInputCount, newEnergyOutput, newDuration,
                newEnergyRequired, newEnergyMultiplier, newPerTickUsage, newRotaryMode,
                newSecondaryItemOutputId, newSecondaryItemOutputCount, newSecondaryChance
        );
    }

    private static String normalizeResourceId(String value, boolean stripTag) {
        var normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return stripTag && normalized.startsWith("#") ? normalized.substring(1) : normalized;
    }

    /** 带数量的流体或化学品资源；输入字段可使用 #namespace:tag。 */
    public record ResourceStack(String id, int amount) {
        public ResourceStack {
            id = normalizeResourceId(id, false);
            if (amount < 1) {
                throw new IllegalArgumentException("Mekanism 资源数量必须至少为 1");
            }
        }
    }

    public enum ResourceField {
        PRIMARY_CHEMICAL_INPUT,
        SECONDARY_CHEMICAL_INPUT,
        PRIMARY_CHEMICAL_OUTPUT,
        SECONDARY_CHEMICAL_OUTPUT,
        FLUID_INPUT,
        FLUID_OUTPUT
    }

    public enum RotaryMode {
        BOTH("kubejs_recipe_ui.mekanism.rotary_mode.both"),
        FLUID_TO_CHEMICAL("kubejs_recipe_ui.mekanism.rotary_mode.fluid_to_chemical"),
        CHEMICAL_TO_FLUID("kubejs_recipe_ui.mekanism.rotary_mode.chemical_to_fluid");

        private final String translationKey;

        RotaryMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }
    }
}
