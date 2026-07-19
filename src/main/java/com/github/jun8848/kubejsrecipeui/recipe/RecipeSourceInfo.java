package com.github.jun8848.kubejsrecipeui.recipe;

/**
 * 保存从 JEI 配方持有者中读取的来源信息，供不同删除模式复用。
 *
 * @param id 配方资源 ID
 * @param type 配方序列化器或类型 ID
 * @param mod 配方来源命名空间
 * @param group 配方分组
 */
public record RecipeSourceInfo(String id, String type, String mod, String group) {
    /** 尚未通过 JEI 填充来源信息时使用的空对象。 */
    public static final RecipeSourceInfo EMPTY = new RecipeSourceInfo("", "", "", "");

    /** 将所有外部字符串归一化为空字符串或去除首尾空白的值。 */
    public RecipeSourceInfo {
        id = normalize(id);
        type = normalize(type);
        mod = normalize(mod);
        group = normalize(group);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
