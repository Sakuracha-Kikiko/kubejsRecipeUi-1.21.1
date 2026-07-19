package com.github.jun8848.kubejsrecipeui.recipe;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 可生成 KubeJS 输出表达式的不可变数据模型。
 *
 * @param itemId 输出物品资源 ID
 * @param count 输出数量
 * @param componentPatch 可选的数据组件补丁
 */
public record OutputSpec(String itemId, int count, String componentPatch) {
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    /** 校验资源 ID 和数量，并把数据组件补丁统一为方括号格式。 */
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
