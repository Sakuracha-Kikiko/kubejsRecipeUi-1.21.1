package com.github.jun8848.kubejsrecipeui.recipe.integration;

import com.github.jun8848.kubejsrecipeui.recipe.RecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;

import java.util.Objects;
import java.util.function.Function;

/** 一个工作站在集成注册表中的完整行为声明。 */
public record RecipeTypeSpec(
        WorkstationType type,
        WorkstationType navigationType,
        boolean inputRequired,
        boolean outputRequired,
        RecipeTypeCapabilities capabilities,
        RecipeSettings defaultSettings,
        RecipeValidator validator,
        Function<RecipeValidationContext, String> generatedIdSource,
        RecipeBodyGenerator generator
) {
    public RecipeTypeSpec {
        type = Objects.requireNonNull(type, "type");
        navigationType = Objects.requireNonNull(navigationType, "navigationType");
        capabilities = Objects.requireNonNull(capabilities, "capabilities");
        defaultSettings = Objects.requireNonNull(defaultSettings, "defaultSettings");
        validator = Objects.requireNonNull(validator, "validator");
        generatedIdSource = Objects.requireNonNull(generatedIdSource, "generatedIdSource");
        generator = Objects.requireNonNull(generator, "generator");
    }

    /** 创建需要输入和输出、且无需专属参数与校验的常规类型声明。 */
    public static RecipeTypeSpec standard(WorkstationType type, RecipeBodyGenerator generator) {
        return standard(type, type, generator);
    }

    /** 创建可与其他类型共用侧栏入口的常规类型声明。 */
    public static RecipeTypeSpec standard(
            WorkstationType type,
            WorkstationType navigationType,
            RecipeBodyGenerator generator
    ) {
        return new RecipeTypeSpec(
                type,
                navigationType,
                true,
                true,
                RecipeTypeCapabilities.SIMPLE,
                RecipeSettings.EMPTY,
                RecipeValidator.NONE,
                context -> context.output().itemId(),
                generator
        );
    }

    /** 创建支持有序和无序切换的常规类型声明。 */
    public static RecipeTypeSpec shaped(
            WorkstationType type,
            WorkstationType navigationType,
            RecipeBodyGenerator generator
    ) {
        return new RecipeTypeSpec(
                type,
                navigationType,
                true,
                true,
                RecipeTypeCapabilities.SHAPED,
                RecipeSettings.EMPTY,
                RecipeValidator.NONE,
                context -> context.output().itemId(),
                generator
        );
    }

    /** 创建需要经验值和烹饪时间的原版烹饪类型声明。 */
    public static RecipeTypeSpec cooking(
            WorkstationType type,
            int defaultCookingTime,
            RecipeBodyGenerator generator
    ) {
        return cooking(type, type, defaultCookingTime, generator);
    }

    /** 创建可与其他类型共用侧栏入口的烹饪类型声明。 */
    public static RecipeTypeSpec cooking(
            WorkstationType type,
            WorkstationType navigationType,
            int defaultCookingTime,
            RecipeBodyGenerator generator
    ) {
        return new RecipeTypeSpec(
                type,
                navigationType,
                true,
                true,
                RecipeTypeCapabilities.cooking(defaultCookingTime),
                RecipeSettings.EMPTY,
                RecipeValidator.NONE,
                context -> context.output().itemId(),
                generator
        );
    }
}
