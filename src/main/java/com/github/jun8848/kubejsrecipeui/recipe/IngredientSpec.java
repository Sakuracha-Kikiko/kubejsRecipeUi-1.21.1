package com.github.jun8848.kubejsrecipeui.recipe;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public record IngredientSpec(Kind kind, String value, List<IngredientSpec> alternatives, String componentPatch) {
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

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

    public static IngredientSpec item(String itemId, String componentPatch) {
        return new IngredientSpec(Kind.ITEM, itemId, List.of(), componentPatch);
    }

    public static IngredientSpec tag(String tagId) {
        var normalized = tagId == null ? "" : tagId.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        return new IngredientSpec(Kind.TAG, normalized, List.of(), "");
    }

    public static IngredientSpec alternatives(List<IngredientSpec> alternatives) {
        return new IngredientSpec(Kind.ALTERNATIVES, "", alternatives, "");
    }

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

    public enum Kind {
        ITEM,
        TAG,
        ALTERNATIVES
    }
}
