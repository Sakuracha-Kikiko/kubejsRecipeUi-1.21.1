package com.github.jun8848.kubejsrecipeui.recipe;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Applied Energistics 2 四类编辑器共用的专属配方参数。 */
public record Ae2RecipeSettings(
        InscriberMode inscriberMode,
        TransformCondition transformCondition,
        String transformFluidTag,
        EntropyMode entropyMode,
        EntropyResourceType entropyInputType,
        String entropyInputId,
        EntropyOutputType entropyOutputType,
        String entropyOutputId
) {
    private static final Pattern RESOURCE_ID_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    public Ae2RecipeSettings {
        inscriberMode = Objects.requireNonNull(inscriberMode, "inscriberMode");
        transformCondition = Objects.requireNonNull(transformCondition, "transformCondition");
        transformFluidTag = normalizeResourceId(transformFluidTag);
        entropyMode = Objects.requireNonNull(entropyMode, "entropyMode");
        entropyInputType = Objects.requireNonNull(entropyInputType, "entropyInputType");
        entropyInputId = normalizeResourceId(entropyInputId);
        entropyOutputType = Objects.requireNonNull(entropyOutputType, "entropyOutputType");
        entropyOutputId = normalizeResourceId(entropyOutputId);
    }

    /** @return 新编辑会话使用的 AE2 参数默认值 */
    public static Ae2RecipeSettings defaults() {
        return new Ae2RecipeSettings(
                InscriberMode.PRESS,
                TransformCondition.EXPLOSION,
                "minecraft:water",
                EntropyMode.COOL,
                EntropyResourceType.FLUID,
                "minecraft:water",
                EntropyOutputType.BLOCK,
                "minecraft:ice"
        );
    }

    /** 只校验当前 AE2 配方类型实际会写入脚本的字段。 */
    public void validateFor(WorkstationType type) {
        if (type == WorkstationType.AE2_TRANSFORM && transformCondition == TransformCondition.FLUID) {
            requireResourceId(transformFluidTag, "AE2 物品转化的流体标签");
        }
        if (type == WorkstationType.AE2_ENTROPY) {
            requireResourceId(entropyInputId, "AE2 熵变输入");
            requireResourceId(entropyOutputId, "AE2 熵变输出");
        }
    }

    public Ae2RecipeSettings withInscriberMode(InscriberMode value) {
        return new Ae2RecipeSettings(value, transformCondition, transformFluidTag, entropyMode,
                entropyInputType, entropyInputId, entropyOutputType, entropyOutputId);
    }

    public Ae2RecipeSettings withTransformCondition(TransformCondition value) {
        return new Ae2RecipeSettings(inscriberMode, value, transformFluidTag, entropyMode,
                entropyInputType, entropyInputId, entropyOutputType, entropyOutputId);
    }

    public Ae2RecipeSettings withTransformFluidTag(String value) {
        return new Ae2RecipeSettings(inscriberMode, transformCondition, value, entropyMode,
                entropyInputType, entropyInputId, entropyOutputType, entropyOutputId);
    }

    public Ae2RecipeSettings withEntropyMode(EntropyMode value) {
        return new Ae2RecipeSettings(inscriberMode, transformCondition, transformFluidTag, value,
                entropyInputType, entropyInputId, entropyOutputType, entropyOutputId);
    }

    public Ae2RecipeSettings withEntropyInputType(EntropyResourceType value) {
        return new Ae2RecipeSettings(inscriberMode, transformCondition, transformFluidTag, entropyMode,
                value, entropyInputId, entropyOutputType, entropyOutputId);
    }

    public Ae2RecipeSettings withEntropyInputId(String value) {
        return new Ae2RecipeSettings(inscriberMode, transformCondition, transformFluidTag, entropyMode,
                entropyInputType, value, entropyOutputType, entropyOutputId);
    }

    public Ae2RecipeSettings withEntropyOutputType(EntropyOutputType value) {
        return new Ae2RecipeSettings(inscriberMode, transformCondition, transformFluidTag, entropyMode,
                entropyInputType, entropyInputId, value, entropyOutputId);
    }

    public Ae2RecipeSettings withEntropyOutputId(String value) {
        return new Ae2RecipeSettings(inscriberMode, transformCondition, transformFluidTag, entropyMode,
                entropyInputType, entropyInputId, entropyOutputType, value);
    }

    private static String normalizeResourceId(String value) {
        var normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("#") ? normalized.substring(1) : normalized;
    }

    private static void requireResourceId(String value, String fieldName) {
        if (!RESOURCE_ID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + "必须是 namespace:path 格式");
        }
    }

    public enum InscriberMode {
        PRESS("press", "kubejs_recipe_ui.ae2.inscriber_mode.press"),
        INSCRIBE("inscribe", "kubejs_recipe_ui.ae2.inscriber_mode.inscribe");

        private final String serializedName;
        private final String translationKey;

        InscriberMode(String serializedName, String translationKey) {
            this.serializedName = serializedName;
            this.translationKey = translationKey;
        }

        public String serializedName() {
            return serializedName;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    public enum TransformCondition {
        EXPLOSION("explosion", "kubejs_recipe_ui.ae2.transform_condition.explosion"),
        FLUID("fluid", "kubejs_recipe_ui.ae2.transform_condition.fluid");

        private final String serializedName;
        private final String translationKey;

        TransformCondition(String serializedName, String translationKey) {
            this.serializedName = serializedName;
            this.translationKey = translationKey;
        }

        public String serializedName() {
            return serializedName;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    public enum EntropyMode {
        HEAT("heat", "kubejs_recipe_ui.ae2.entropy_mode.heat"),
        COOL("cool", "kubejs_recipe_ui.ae2.entropy_mode.cool");

        private final String serializedName;
        private final String translationKey;

        EntropyMode(String serializedName, String translationKey) {
            this.serializedName = serializedName;
            this.translationKey = translationKey;
        }

        public String serializedName() {
            return serializedName;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    public enum EntropyResourceType {
        BLOCK("block", "kubejs_recipe_ui.ae2.resource_type.block"),
        FLUID("fluid", "kubejs_recipe_ui.ae2.resource_type.fluid");

        private final String serializedName;
        private final String translationKey;

        EntropyResourceType(String serializedName, String translationKey) {
            this.serializedName = serializedName;
            this.translationKey = translationKey;
        }

        public String serializedName() {
            return serializedName;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    public enum EntropyOutputType {
        BLOCK("block", "kubejs_recipe_ui.ae2.resource_type.block"),
        FLUID("fluid", "kubejs_recipe_ui.ae2.resource_type.fluid"),
        DROPS("drops", "kubejs_recipe_ui.ae2.resource_type.drops");

        private final String serializedName;
        private final String translationKey;

        EntropyOutputType(String serializedName, String translationKey) {
            this.serializedName = serializedName;
            this.translationKey = translationKey;
        }

        public String serializedName() {
            return serializedName;
        }

        public String translationKey() {
            return translationKey;
        }
    }
}
