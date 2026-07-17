package com.github.jun8848.kubejsrecipeui.registry;

import com.github.jun8848.kubejsrecipeui.KubeJsRecipeUi;
import com.github.jun8848.kubejsrecipeui.block.RecipeEditorBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KubeJsRecipeUi.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KubeJsRecipeUi.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KubeJsRecipeUi.MOD_ID);

    public static final DeferredBlock<Block> RECIPE_EDITOR = BLOCKS.registerBlock(
            "recipe_editor",
            RecipeEditorBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)
    );

    public static final DeferredItem<BlockItem> RECIPE_EDITOR_ITEM =
            ITEMS.registerSimpleBlockItem("recipe_editor", RECIPE_EDITOR);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.kubejs_recipe_ui.main"))
                    .icon(() -> RECIPE_EDITOR_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(RECIPE_EDITOR_ITEM.get()))
                    .build()
    );

    private ModContent() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
