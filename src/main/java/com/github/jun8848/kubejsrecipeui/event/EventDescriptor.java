package com.github.jun8848.kubejsrecipeui.event;

import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.event.KubeEvent;

import java.util.List;

/**
 * KubeJS 事件注册信息的界面友好快照。
 *
 * @param group 事件组名称，例如 {@code PlayerEvents}
 * @param name 事件方法名称
 * @param scriptTypes 允许使用该事件的脚本端
 * @param supportsTarget 事件是否支持目标参数
 * @param targetRequired 目标参数是否必填
 * @param targetKind 用于构建候选列表的目标类型
 * @param eventType 事件对象的 Java 类型
 */
public record EventDescriptor(
        String group,
        String name,
        List<ScriptType> scriptTypes,
        boolean supportsTarget,
        boolean targetRequired,
        EventTargetKind targetKind,
        Class<? extends KubeEvent> eventType
) {
    public EventDescriptor {
        scriptTypes = List.copyOf(scriptTypes);
    }

    /** @return 可直接写入 KubeJS 的完整事件名 */
    public String id() {
        return group + "." + name;
    }
}
