package com.github.jun8848.kubejsrecipeui.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record RecipeDefinition(
        WorkstationType type,
        boolean shaped,
        List<IngredientSpec> inputs,
        OutputSpec output,
        float experience,
        int cookingTime
) {
    public RecipeDefinition {
        type = Objects.requireNonNull(type, "type");
        Objects.requireNonNull(inputs, "inputs");
        if (inputs.size() != type.inputCount()) {
            throw new IllegalArgumentException("配方输入槽位数量与工作方块不匹配");
        }
        inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
        output = Objects.requireNonNull(output, "output");
        if (inputs.stream().allMatch(Objects::isNull)) {
            throw new IllegalArgumentException("至少需要一个配方原料");
        }
        if (type == WorkstationType.SMITHING && inputs.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("锻造配方需要模板、基础物品和追加材料");
        }
        if (!Float.isFinite(experience) || experience < 0.0F) {
            throw new IllegalArgumentException("配方经验值不能小于 0");
        }
        if (type.isCooking() && cookingTime < 1) {
            throw new IllegalArgumentException("烹饪时间必须至少为 1 tick");
        }
    }
}
