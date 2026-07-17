package com.github.jun8848.kubejsrecipeui.recipe;

import java.util.Objects;
import java.util.regex.Pattern;

public record OutputSpec(String itemId, int count, String componentPatch) {
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    public OutputSpec {
        itemId = Objects.requireNonNull(itemId, "itemId").trim();
        if (!IDENTIFIER.matcher(itemId).matches()) {
            throw new IllegalArgumentException("输出物品标识符无效: " + itemId);
        }
        if (count < 1 || count > 99) {
            throw new IllegalArgumentException("输出数量必须在 1 到 99 之间");
        }

        componentPatch = componentPatch == null ? "" : componentPatch.trim();
        if (!componentPatch.isEmpty()) {
            if (!componentPatch.startsWith("[")) {
                componentPatch = "[" + componentPatch;
            }
            if (!componentPatch.endsWith("]")) {
                componentPatch = componentPatch + "]";
            }
            if (componentPatch.length() < 3) {
                componentPatch = "";
            }
        }
    }
}
