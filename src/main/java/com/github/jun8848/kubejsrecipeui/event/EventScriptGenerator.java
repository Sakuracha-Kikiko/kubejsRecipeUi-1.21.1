package com.github.jun8848.kubejsrecipeui.event;

import dev.latvian.mods.kubejs.script.ScriptType;

/** 将事件、脚本端、目标和可选方法组合为 KubeJS 事件代码。 */
public final class EventScriptGenerator {
    private EventScriptGenerator() {
    }

    /** 生成只有空回调体的事件模板。 */
    public static String generate(EventDescriptor event, ScriptType scriptType, String target) {
        return generate(event, scriptType, target, null);
    }

    /**
     * 生成带可选目标和事件方法调用的完整事件模板。
     */
    public static String generate(
            EventDescriptor event,
            ScriptType scriptType,
            String target,
            EventMethodOption method
    ) {
        if (!event.scriptTypes().contains(scriptType)) {
            throw new IllegalArgumentException("所选事件不支持该脚本端");
        }

        var normalizedTarget = target == null ? "" : target.trim();
        if (event.targetRequired() && normalizedTarget.isEmpty()) {
            throw new IllegalArgumentException("该事件必须填写目标 ID");
        }

        // KubeJS 有目标事件使用 (target, event)，普通事件只接收 event。
        var argument = event.supportsTarget() && !normalizedTarget.isEmpty()
                ? quote(normalizedTarget) + ", event"
                : "event";
        var body = method == null
                ? "  // 在这里编写事件逻辑"
                : method.code();
        return event.id() + "(" + argument + " => {\n"
                + body + "\n"
                + "});\n";
    }

    private static String quote(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }
}
