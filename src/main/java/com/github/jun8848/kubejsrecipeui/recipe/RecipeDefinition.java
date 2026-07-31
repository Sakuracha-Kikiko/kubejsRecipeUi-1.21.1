package com.github.jun8848.kubejsrecipeui.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 从编辑器会话生成的完整配方快照，是代码生成器的唯一输入。
 */
public record RecipeDefinition(
        String id,
        WorkstationType type,
        boolean shaped,
        List<IngredientSpec> inputs,
        OutputSpec output,
        float experience,
        int cookingTime,
        ExtendedRecipeSettings extendedSettings,
        Ae2RecipeSettings ae2Settings
) {
    private static final Pattern RECIPE_ID_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    /** 校验槽位数量、必需原料以及烹饪参数，阻止无效状态进入脚本生成阶段。 */
    public RecipeDefinition {
        type = Objects.requireNonNull(type, "type");
        Objects.requireNonNull(inputs, "inputs");
        if (inputs.size() != type.inputCount()) {
            throw new IllegalArgumentException("配方输入槽位数量与工作方块不匹配");
        }
        inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
        if (type != WorkstationType.AE2_ENTROPY) {
            output = Objects.requireNonNull(output, "output");
        }
        extendedSettings = Objects.requireNonNull(extendedSettings, "extendedSettings");
        ae2Settings = Objects.requireNonNull(ae2Settings, "ae2Settings");
        if (type != WorkstationType.AE2_ENTROPY && inputs.stream().allMatch(Objects::isNull)) {
            throw new IllegalArgumentException("至少需要一个配方原料");
        }
        if (type == WorkstationType.SMITHING && inputs.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("锻造配方需要模板、基础物品和追加材料");
        }
        if (type == WorkstationType.EXTENDED_COMBINATION) {
            if (inputs.getFirst() == null) {
                throw new IllegalArgumentException("组合合成需要中心输入物品");
            }
            if (inputs.subList(1, inputs.size()).stream().allMatch(Objects::isNull)) {
                throw new IllegalArgumentException("组合合成至少需要一个基座材料");
            }
        }
        if (type == WorkstationType.EXTENDED_COMPRESSOR && inputs.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("压缩机配方需要压缩材料和催化剂");
        }
        if (type == WorkstationType.AE2_INSCRIBER && inputs.get(1) == null) {
            throw new IllegalArgumentException("压印器配方需要中间输入材料");
        }
        if (!Float.isFinite(experience) || experience < 0.0F) {
            throw new IllegalArgumentException("配方经验值不能小于 0");
        }
        if (type.isCooking() && cookingTime < 1) {
            throw new IllegalArgumentException("烹饪时间必须至少为 1 tick");
        }
        ae2Settings.validateFor(type);
        id = normalizeRecipeId(id, type, shaped, inputs, output, experience, cookingTime,
                extendedSettings, ae2Settings);
    }

    /**
     * 优先使用玩家输入或 JEI 读取到的 ID；空值则生成包含内容哈希的稳定 ID。
     */
    private static String normalizeRecipeId(
            String id,
            WorkstationType type,
            boolean shaped,
            List<IngredientSpec> inputs,
            OutputSpec output,
            float experience,
            int cookingTime,
            ExtendedRecipeSettings extendedSettings,
            Ae2RecipeSettings ae2Settings
    ) {
        var normalized = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        if (!normalized.isEmpty()) {
            if (!normalized.contains(":")) {
                normalized = "kubejs_recipe_ui:" + normalized;
            }
            if (!RECIPE_ID_PATTERN.matcher(normalized).matches()) {
                throw new IllegalArgumentException("配方 ID 格式无效，只能使用小写字母、数字、下划线、短横线、点和斜杠");
            }
            return normalized;
        }

        var itemId = (output == null ? ae2Settings.entropyOutputId() : output.itemId()).toLowerCase(Locale.ROOT);
        var namespaceSeparator = itemId.indexOf(':');
        var outputPath = namespaceSeparator >= 0 ? itemId.substring(namespaceSeparator + 1) : itemId;
        var signature = type.name() + '|' + shaped + '|' + inputs + '|' + output
                + '|' + experience + '|' + cookingTime + '|' + extendedSettings + '|' + ae2Settings;
        var hash = String.format(Locale.ROOT, "%08x", signature.hashCode());
        return "kubejs_recipe_ui:" + outputPath + "_" + hash;
    }
}
