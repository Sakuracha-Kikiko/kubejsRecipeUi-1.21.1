package com.github.jun8848.kubejsrecipeui.event;

/** 决定事件目标下拉框应从哪个注册表或固定集合读取数据。 */
public enum EventTargetKind {
    NONE,
    BLOCK,
    ITEM,
    ENTITY_TYPE,
    DIMENSION,
    MENU,
    CREATIVE_TAB,
    REGISTRY,
    GENERATED_DATA_STAGE,
    RECIPE_VIEWER_ENTRY,
    CUSTOM
}
