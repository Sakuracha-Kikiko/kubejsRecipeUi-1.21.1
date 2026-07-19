package com.github.jun8848.kubejsrecipeui.event;

import java.util.List;

/** 可供用户插入事件回调体的方法及其参数元数据。 */
public record EventMethodOption(
        String name,
        String signature,
        List<EventMethodParameter> parameters
) {
    /** 表示尚未选择事件方法的空选项。 */
    public static final EventMethodOption NONE = new EventMethodOption("", "", List.of());

    public EventMethodOption {
        parameters = List.copyOf(parameters);
    }

    /** 组合翻译名称与 Java 方法签名，便于区分重载方法。 */
    public String displayText(String translatedName) {
        return signature.isEmpty() ? translatedName : translatedName + " — " + signature;
    }

    /**
     * 生成方法调用，并在调用前用注释列出可用参数名称和类型。
     */
    public String code() {
        var builder = new StringBuilder();
        if (!parameters.isEmpty()) {
            builder.append("  // 可用参数：");
            for (var index = 0; index < parameters.size(); index++) {
                if (index > 0) {
                    builder.append("，");
                }
                var parameter = parameters.get(index);
                builder.append(parameter.name()).append(": ").append(parameter.type());
            }
            builder.append('\n');
        }

        var arguments = parameters.stream()
                .map(EventMethodParameter::placeholder)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        builder.append("  ");
        return builder.append("event.").append(name).append('(').append(arguments).append(");").toString();
    }
}
