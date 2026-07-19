package com.github.jun8848.kubejsrecipeui.recipe;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 描述单个配方原料，支持精确物品、物品标签和候选材料列表。
 *
 * @param kind 原料表达方式
 * @param value 物品或标签资源 ID
 * @param alternatives 替代材料列表
 * @param componentPatch 精确物品携带的数据组件补丁
 */
public record IngredientSpec(Kind kind, String value, List<IngredientSpec> alternatives, String componentPatch) {
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    /** 根据原料类型执行互斥字段校验，保证生成器接收到的状态始终合法。 */
    public IngredientSpec {
        kind = Objects.requireNonNull(kind, "kind");
        value = value == null ? "" : value.trim();
        alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
        componentPatch = normalizeComponentPatch(componentPatch);

        switch (kind) {
            case ITEM -> validateIdentifier(value, "物品");
            case TAG -> {
                validateIdentifier(value, "标签");
                if (!componentPatch.isEmpty()) {
                    throw new IllegalArgumentException("标签配方原料不能携带数据组件");
                }
            }
            case ALTERNATIVES -> {
                if (alternatives.isEmpty()) {
                    throw new IllegalArgumentException("替代材料列表不能为空");
                }
                if (!value.isEmpty() || !componentPatch.isEmpty()) {
                    throw new IllegalArgumentException("替代材料不能设置单独的物品或数据组件");
                }
                if (alternatives.stream().anyMatch(spec -> spec.kind == Kind.ALTERNATIVES)) {
                    throw new IllegalArgumentException("替代材料不能继续嵌套替代材料");
                }
            }
        }
    }

    /** 创建一个精确物品原料。 */
    public static IngredientSpec item(String itemId, String componentPatch) {
        return new IngredientSpec(Kind.ITEM, itemId, List.of(), componentPatch);
    }

    /** 创建物品标签原料，允许调用方传入带或不带 {@code #} 的标签。 */
    public static IngredientSpec tag(String tagId) {
        var normalized = tagId == null ? "" : tagId.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        return new IngredientSpec(Kind.TAG, normalized, List.of(), "");
    }

    /** 创建由多个候选原料组成的替代材料。 */
    public static IngredientSpec alternatives(List<IngredientSpec> alternatives) {
        return new IngredientSpec(Kind.ALTERNATIVES, "", alternatives, "");
    }

    /**
     * 将界面中的单个候选表达式解析为物品或标签原料。
     *
     * @param text 物品、标签或带数据组件的物品表达式
     */
    public static IngredientSpec parseAlternative(String text) {
        var normalized = Objects.requireNonNull(text, "text").trim();
        if (normalized.startsWith("#")) {
            return tag(normalized);
        }

        var componentStart = normalized.indexOf('[');
        if (componentStart < 0) {
            return item(normalized, "");
        }
        return item(normalized.substring(0, componentStart), normalized.substring(componentStart));
    }

    private static void validateIdentifier(String identifier, String name) {
        if (!IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException(name + "标识符无效: " + identifier);
        }
    }

    private static String normalizeComponentPatch(String patch) {
        if (patch == null || patch.isBlank()) {
            return "";
        }

        var normalized = patch.trim();
        if (!normalized.startsWith("[")) {
            normalized = "[" + normalized;
        }
        if (!normalized.endsWith("]")) {
            normalized = normalized + "]";
        }
        if (normalized.length() < 3) {
            return "";
        }
        return normalized;
    }

    /** 原料在 KubeJS 代码中的三种表示方式。 */
    public enum Kind {
        ITEM,
        TAG,
        ALTERNATIVES
    }
}
