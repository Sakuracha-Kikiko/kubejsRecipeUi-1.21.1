package com.github.jun8848.kubejsrecipeui.event;

import java.util.List;

public record EventMethodOption(
        String name,
        String signature,
        List<EventMethodParameter> parameters
) {
    public static final EventMethodOption NONE = new EventMethodOption("", "", List.of());

    public EventMethodOption {
        parameters = List.copyOf(parameters);
    }

    public String displayText(String translatedName) {
        return signature.isEmpty() ? translatedName : translatedName + " — " + signature;
    }

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
