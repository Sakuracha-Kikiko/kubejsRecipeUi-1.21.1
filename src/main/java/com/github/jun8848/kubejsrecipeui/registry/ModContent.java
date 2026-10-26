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

/**
 * 集中声明方块、物品和创造模式标签页，避免注册逻辑散落在客户端代码中。
 */
public final class ModContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KubeJsRecipeUi.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KubeJsRecipeUi.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KubeJsRecipeUi.MOD_ID);

    /** 工作台编辑器方块，沿用原版工作台的基础方块属性。 */
    public static final DeferredBlock<Block> RECIPE_EDITOR = BLOCKS.registerBlock(
            "recipe_editor",
            RecipeEditorBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)
    );

    /** 工作台编辑器对应的方块物品。 */
    public static final DeferredItem<BlockItem> RECIPE_EDITOR_ITEM =
            ITEMS.registerSimpleBlockItem("recipe_editor", RECIPE_EDITOR);

    /** 展示本模组工具的独立创造模式标签页。 */
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

    /**
     * 将全部延迟注册器绑定到 NeoForge 模组事件总线。
     *
     * @param modEventBus 当前模组专用的事件总线
     */
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
