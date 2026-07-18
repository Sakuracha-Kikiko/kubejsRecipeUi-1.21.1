package com.github.jun8848.kubejsrecipeui.event;

import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.event.KubeEvent;

import java.util.List;

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

    public String id() {
        return group + "." + name;
    }
}
