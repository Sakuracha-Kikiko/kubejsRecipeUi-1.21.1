package com.github.jun8848.kubejsrecipeui.event;

public record EventTargetOption(String id, String name) {
    public EventTargetOption {
        id = id == null ? "" : id;
        name = name == null ? "" : name;
    }

    public String displayText() {
        return name.isBlank() || name.equals(id)
                ? id
                : name + "（" + id + "）";
    }
}
