package com.github.jun8848.kubejsrecipeui.event;

import dev.latvian.mods.kubejs.recipe.viewer.RecipeViewerEntryType;
import dev.latvian.mods.kubejs.script.data.GeneratedDataStage;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * 根据事件目标类型读取 Minecraft 注册表，并生成带翻译名称的目标候选项。
 */
public final class EventTargetCatalog {
    private EventTargetCatalog() {
    }

    /**
     * 获取指定目标类型的可选 ID；无法枚举的自定义目标返回空列表供玩家手动输入。
     */
    public static List<EventTargetOption> candidates(EventTargetKind kind, RegistryAccess registryAccess) {
        return switch (kind) {
            case BLOCK -> BuiltInRegistries.BLOCK.keySet().stream()
                    .map(id -> option(id, BuiltInRegistries.BLOCK.getValue(id).getName()))
                    .sorted(EventTargetCatalog::compare)
                    .toList();
            case ITEM -> BuiltInRegistries.ITEM.keySet().stream()
                    .map(id -> {
                        var item = BuiltInRegistries.ITEM.getValue(id);
                        return option(id, item.getName(new ItemStack(item)));
                    })
                    .sorted(EventTargetCatalog::compare)
                    .toList();
            case ENTITY_TYPE -> BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                    .map(id -> option(id, BuiltInRegistries.ENTITY_TYPE.getValue(id).getDescription()))
                    .sorted(EventTargetCatalog::compare)
                    .toList();
            case DIMENSION -> registryAccess.lookup(Registries.DIMENSION)
                    .map(registry -> registry.keySet().stream()
                            .map(id -> translatedOption(id, "dimension." + id.getNamespace() + "." + translationPath(id)))
                            .sorted(EventTargetCatalog::compare)
                            .toList())
                    .orElseGet(List::of);
            case MENU -> translatedRegistryOptions(BuiltInRegistries.MENU, "container");
            case CREATIVE_TAB -> BuiltInRegistries.CREATIVE_MODE_TAB.keySet().stream()
                    .map(id -> option(id, BuiltInRegistries.CREATIVE_MODE_TAB.getValue(id).getDisplayName()))
                    .sorted(EventTargetCatalog::compare)
                    .toList();
            case REGISTRY -> registryAccess.listRegistryKeys()
                    .map(key -> translatedOption(
                            key.identifier(),
                            "registry." + key.identifier().getNamespace() + "." + translationPath(key.identifier())
                    ))
                    .sorted(EventTargetCatalog::compare)
                    .toList();
            case GENERATED_DATA_STAGE -> Arrays.stream(GeneratedDataStage.values())
                    .map(stage -> translatedOption(
                            stage.getSerializedName(),
                            "kubejs_recipe_ui.event.target.generated_data_stage." + stage.getSerializedName()
                    ))
                    .sorted(EventTargetCatalog::compare)
                    .toList();
            case RECIPE_VIEWER_ENTRY -> RecipeViewerEntryType.ALL_TYPES.get().stream()
                    .map(type -> translatedOption(
                            type.id,
                            "kubejs_recipe_ui.event.target.recipe_viewer_entry." + type.id
                    ))
                    .sorted(EventTargetCatalog::compare)
                    .toList();
            case NONE, CUSTOM -> List.of();
        };
    }

    private static List<EventTargetOption> translatedRegistryOptions(Registry<?> registry, String prefix) {
        return registry.keySet().stream()
                .map(id -> translatedOption(id, prefix + "." + translationPath(id)))
                .sorted(EventTargetCatalog::compare)
                .toList();
    }

    private static EventTargetOption option(Identifier id, Component name) {
        return new EventTargetOption(id.toString(), name.getString());
    }

    private static EventTargetOption translatedOption(Identifier id, String translationKey) {
        return translatedOption(id.toString(), translationKey);
    }

    private static EventTargetOption translatedOption(String id, String translationKey) {
        var translated = Component.translatable(translationKey).getString();
        // 缺少语言条目时用资源路径转换出的可读文本兜底。
        return new EventTargetOption(id, translated.equals(translationKey) ? humanize(id) : translated);
    }

    private static String translationPath(Identifier id) {
        return id.getPath().replace('/', '.');
    }

    private static String humanize(String id) {
        var separator = id.indexOf(':');
        var path = separator >= 0 ? id.substring(separator + 1) : id;
        var words = path.replace('/', ' ').replace('_', ' ').trim();
        if (words.isEmpty()) {
            return id;
        }
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private static int compare(EventTargetOption left, EventTargetOption right) {
        return left.id().compareTo(right.id());
    }
}
