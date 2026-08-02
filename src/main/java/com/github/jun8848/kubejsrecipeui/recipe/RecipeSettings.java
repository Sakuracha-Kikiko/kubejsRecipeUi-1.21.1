package com.github.jun8848.kubejsrecipeui.recipe;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 按设置类型保存模组专属配方参数的不可变容器。
 * 核心配方模型无需了解每个可选模组的具体字段。
 */
public final class RecipeSettings {
    public static final RecipeSettings EMPTY = new RecipeSettings(Map.of());

    private final Map<Class<? extends RecipeIntegrationSettings>, RecipeIntegrationSettings> values;

    private RecipeSettings(Map<Class<? extends RecipeIntegrationSettings>, RecipeIntegrationSettings> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /** 创建包含给定设置对象的容器，后出现的同类型设置会覆盖前一个。 */
    public static RecipeSettings of(RecipeIntegrationSettings... settings) {
        if (settings == null || settings.length == 0) {
            return EMPTY;
        }
        var values = new LinkedHashMap<Class<? extends RecipeIntegrationSettings>, RecipeIntegrationSettings>();
        for (var setting : settings) {
            var value = Objects.requireNonNull(setting, "setting");
            values.put(value.getClass(), value);
        }
        return new RecipeSettings(values);
    }

    /** @return 是否保存了指定类型的设置 */
    public boolean contains(Class<? extends RecipeIntegrationSettings> type) {
        return values.containsKey(type);
    }

    /** 获取指定类型的设置；缺失时抛出异常，以便尽早发现集成注册错误。 */
    public <T extends RecipeIntegrationSettings> T require(Class<T> type) {
        var value = values.get(Objects.requireNonNull(type, "type"));
        if (value == null) {
            throw new IllegalStateException("缺少配方集成设置: " + type.getName());
        }
        return type.cast(value);
    }

    /** 返回加入或替换指定设置后的新容器。 */
    public RecipeSettings with(RecipeIntegrationSettings setting) {
        var value = Objects.requireNonNull(setting, "setting");
        var updated = new LinkedHashMap<>(values);
        updated.put(value.getClass(), value);
        return new RecipeSettings(updated);
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof RecipeSettings other && values.equals(other.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return values.values().toString();
    }
}
