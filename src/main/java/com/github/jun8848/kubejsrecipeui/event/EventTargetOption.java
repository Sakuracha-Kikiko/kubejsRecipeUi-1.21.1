package com.github.jun8848.kubejsrecipeui.event;

/**
 * 事件目标下拉框中的单个选项。
 *
 * @param id 写入脚本的目标 ID
 * @param name 面向玩家显示的翻译名称
 */
public record EventTargetOption(String id, String name) {
    public EventTargetOption {
        id = id == null ? "" : id;
        name = name == null ? "" : name;
    }

    /** @return 同时包含名称和 ID 的显示文本 */
    public String displayText() {
        return name.isBlank() || name.equals(id)
                ? id
                : name + "（" + id + "）";
    }
}
