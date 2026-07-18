package com.github.jun8848.kubejsrecipeui.event;

import dev.latvian.mods.kubejs.event.EventGroups;
import dev.latvian.mods.kubejs.event.EventTargetType;
import dev.latvian.mods.kubejs.plugin.builtin.event.BlockEvents;
import dev.latvian.mods.kubejs.plugin.builtin.event.EntityEvents;
import dev.latvian.mods.kubejs.plugin.builtin.event.ItemEvents;
import dev.latvian.mods.kubejs.plugin.builtin.event.LevelEvents;
import dev.latvian.mods.kubejs.plugin.builtin.event.PlayerEvents;
import dev.latvian.mods.kubejs.plugin.builtin.event.RecipeViewerEvents;
import dev.latvian.mods.kubejs.recipe.viewer.RecipeViewerEntryType;
import dev.latvian.mods.kubejs.script.data.GeneratedDataStage;

import java.util.Comparator;
import java.util.List;

public final class KubeJsEventCatalog {
    private KubeJsEventCatalog() {
    }

    public static List<EventDescriptor> all() {
        return EventGroups.ALL.get().map().values().stream()
                .flatMap(group -> group.getHandlers().values().stream())
                .map(handler -> new EventDescriptor(
                        handler.group.name,
                        handler.name,
                        handler.scriptTypePredicate.getValidTypes(),
                        handler.target != null,
                        handler.targetRequired,
                        targetKind(handler.group.name, handler.name, handler.target),
                        handler.eventType.get()
                ))
                .sorted(Comparator.comparing(EventDescriptor::id))
                .toList();
    }

    private static EventTargetKind targetKind(String group, String name, EventTargetType<?> target) {
        if (target == null) {
            return EventTargetKind.NONE;
        } else if (target == BlockEvents.TARGET) {
            return EventTargetKind.BLOCK;
        } else if (target == ItemEvents.TARGET) {
            return EventTargetKind.ITEM;
        } else if (target == EntityEvents.TARGET) {
            return EventTargetKind.ENTITY_TYPE;
        } else if (target == LevelEvents.TARGET) {
            return EventTargetKind.DIMENSION;
        } else if (target == PlayerEvents.MENU_TARGET) {
            return EventTargetKind.MENU;
        } else if (target == EventTargetType.REGISTRY) {
            return EventTargetKind.REGISTRY;
        } else if (target == GeneratedDataStage.TARGET) {
            return EventTargetKind.GENERATED_DATA_STAGE;
        } else if (target == RecipeViewerEvents.TARGET || target.type == RecipeViewerEntryType.class) {
            return EventTargetKind.RECIPE_VIEWER_ENTRY;
        } else if (group.equals("StartupEvents") && name.equals("modifyCreativeTab")) {
            return EventTargetKind.CREATIVE_TAB;
        }
        return EventTargetKind.CUSTOM;
    }
}
