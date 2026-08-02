package com.github.jun8848.kubejsrecipeui.client;

import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import java.util.EnumMap;
import java.util.Map;

/** 客户端工作站类型到 LDLib2 面板工厂的统一注册表。 */
final class RecipeEditorPanelRegistry {
    private static final Map<WorkstationType, PanelFactory> FACTORIES = createFactories();

    private RecipeEditorPanelRegistry() {
    }

    static UIElement create(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session,
            WorkstationType type
    ) {
        var factory = FACTORIES.get(type);
        if (factory == null) {
            throw new IllegalStateException("工作站缺少编辑面板: " + type);
        }
        return factory.create(root, holder, session, type);
    }

    private static Map<WorkstationType, PanelFactory> createFactories() {
        var factories = new EnumMap<WorkstationType, PanelFactory>(WorkstationType.class);
        register(factories, WorkstationType.CRAFTING,
                (root, holder, session, type) -> RecipeEditorUi.createCraftingPanel(root, holder, session));
        register(factories, WorkstationType.FURNACE, RecipeEditorUi::createCookingPanel);
        register(factories, WorkstationType.SMOKER, RecipeEditorUi::createCookingPanel);
        register(factories, WorkstationType.BLAST_FURNACE, RecipeEditorUi::createCookingPanel);
        register(factories, WorkstationType.CAMPFIRE,
                (root, holder, session, type) -> RecipeEditorUi.createCampfirePanel(root, holder, session));
        register(factories, WorkstationType.STONECUTTER, RecipeEditorUi::createSingleInputPanel);
        register(factories, WorkstationType.SMITHING,
                (root, holder, session, type) -> RecipeEditorUi.createSmithingPanel(root, holder, session));

        register(factories, WorkstationType.EXTENDED_BASIC_TABLE, RecipeEditorUi::createExtendedTablePanel);
        register(factories, WorkstationType.EXTENDED_ADVANCED_TABLE, RecipeEditorUi::createExtendedTablePanel);
        register(factories, WorkstationType.EXTENDED_ELITE_TABLE, RecipeEditorUi::createExtendedTablePanel);
        register(factories, WorkstationType.EXTENDED_ULTIMATE_TABLE, RecipeEditorUi::createExtendedTablePanel);
        register(factories, WorkstationType.EXTENDED_COMBINATION,
                (root, holder, session, type) -> RecipeEditorUi.createCombinationPanel(root, holder, session));
        register(factories, WorkstationType.EXTENDED_COMPRESSOR,
                (root, holder, session, type) -> RecipeEditorUi.createCompressorPanel(root, holder, session));
        register(factories, WorkstationType.EXTENDED_ENDER_CRAFTER,
                (root, holder, session, type) -> RecipeEditorUi.createEnderCrafterPanel(root, holder, session));
        register(factories, WorkstationType.EXTENDED_FLUX_CRAFTER,
                (root, holder, session, type) -> RecipeEditorUi.createFluxCrafterPanel(root, holder, session));

        register(factories, WorkstationType.AE2_CHARGER,
                (root, holder, session, type) -> RecipeEditorUi.createAe2ChargerPanel(root, holder, session));
        register(factories, WorkstationType.AE2_INSCRIBER,
                (root, holder, session, type) -> RecipeEditorUi.createAe2InscriberPanel(root, holder, session));
        register(factories, WorkstationType.AE2_TRANSFORM,
                (root, holder, session, type) -> RecipeEditorUi.createAe2TransformPanel(root, holder, session));
        register(factories, WorkstationType.AE2_ENTROPY,
                (root, holder, session, type) -> RecipeEditorUi.createAe2EntropyPanel(session));

        for (var type : WorkstationType.values()) {
            if (type.isMekanism()) {
                register(factories, type, RecipeEditorUi::createMekanismPanel);
            }
        }

        for (var type : WorkstationType.values()) {
            if (!factories.containsKey(type)) {
                throw new IllegalStateException("工作站未注册编辑面板: " + type);
            }
        }
        return Map.copyOf(factories);
    }

    private static void register(
            EnumMap<WorkstationType, PanelFactory> factories,
            WorkstationType type,
            PanelFactory factory
    ) {
        if (factories.put(type, factory) != null) {
            throw new IllegalStateException("工作站重复注册编辑面板: " + type);
        }
    }

    @FunctionalInterface
    private interface PanelFactory {
        UIElement create(
                UIElement root,
                BlockUIMenuType.BlockUIHolder holder,
                RecipeEditorSession session,
                WorkstationType type
        );
    }
}
