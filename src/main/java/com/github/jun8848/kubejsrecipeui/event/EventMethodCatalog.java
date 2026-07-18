package com.github.jun8848.kubejsrecipeui.event;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.HideFromJS;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public final class EventMethodCatalog {
    private static final List<String> INTERNAL_METHODS = List.of(
            "afterPosted",
            "defaultExitValue",
            "mapExitValue",
            "getExitValueType"
    );

    private EventMethodCatalog() {
    }

    public static List<EventMethodOption> candidates(EventDescriptor event) {
        var methods = new LinkedHashMap<String, EventMethodOption>();
        Arrays.stream(event.eventType().getMethods())
                .filter(EventMethodCatalog::isScriptMethod)
                .sorted(Comparator
                        .comparing(Method::getName)
                        .thenComparingInt(EventMethodCatalog::visibleParameterCount))
                .map(EventMethodCatalog::createOption)
                .forEach(option -> methods.putIfAbsent(option.signature(), option));
        return List.copyOf(methods.values());
    }

    private static boolean isScriptMethod(Method method) {
        return Modifier.isPublic(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers())
                && !method.isSynthetic()
                && !method.isBridge()
                && method.getDeclaringClass() != Object.class
                && method.getAnnotation(HideFromJS.class) == null
                && method.getDeclaringClass().getAnnotation(HideFromJS.class) == null
                && !INTERNAL_METHODS.contains(method.getName());
    }

    private static EventMethodOption createOption(Method method) {
        var visibleParameters = Arrays.stream(method.getParameters())
                .filter(parameter -> parameter.getType() != Context.class)
                .toList();
        var parameters = new java.util.ArrayList<EventMethodParameter>();
        for (var index = 0; index < visibleParameters.size(); index++) {
            var parameter = visibleParameters.get(index);
            parameters.add(new EventMethodParameter(
                    parameterName(method, parameter, index, visibleParameters.size()),
                    simpleType(parameter.getType()),
                    placeholder(parameter.getType())
            ));
        }
        var signature = method.getName() + "(" + parameters.stream()
                .map(parameter -> parameter.type() + " " + parameter.name())
                .reduce((left, right) -> left + ", " + right)
                .orElse("") + ")";
        return new EventMethodOption(
                method.getName(),
                signature,
                parameters
        );
    }

    private static int visibleParameterCount(Method method) {
        return (int) Arrays.stream(method.getParameterTypes())
                .filter(type -> type != Context.class)
                .count();
    }

    private static String parameterName(Method method, Parameter parameter, int index, int total) {
        if (parameter.isNamePresent() && !parameter.getName().matches("arg\\d+")) {
            return parameter.getName();
        }
        var stem = methodStem(method.getName());
        if (total == 1 && !stem.isBlank()) {
            return stem;
        }
        return "参数" + (index + 1);
    }

    private static String methodStem(String methodName) {
        for (var prefix : List.of("set", "add", "remove", "contains", "modify", "replace")) {
            if (methodName.startsWith(prefix) && methodName.length() > prefix.length()) {
                var stem = methodName.substring(prefix.length());
                return Character.toLowerCase(stem.charAt(0)) + stem.substring(1);
            }
        }
        return "";
    }

    private static String simpleType(Class<?> type) {
        if (type.isArray()) {
            return simpleType(type.getComponentType()) + "[]";
        }
        return type.getSimpleName();
    }

    private static String placeholder(Class<?> type) {
        if (type == boolean.class || type == Boolean.class) {
            return "false";
        } else if (type == byte.class || type == short.class || type == int.class || type == long.class
                || Number.class.isAssignableFrom(type)) {
            return "0";
        } else if (type == float.class || type == double.class) {
            return "0.0";
        } else if (type == char.class || type == Character.class) {
            return "'?'";
        } else if (CharSequence.class.isAssignableFrom(type) || type.getSimpleName().contains("Identifier")) {
            return "'minecraft:target'";
        } else if (type.isEnum()) {
            return "'" + type.getEnumConstants()[0].toString().toLowerCase(Locale.ROOT) + "'";
        }
        return "null";
    }
}
