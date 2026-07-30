package com.github.jun8848.kubejsrecipeui.client;

import com.github.jun8848.kubejsrecipeui.recipe.RecipeSourceInfo;
import com.github.jun8848.kubejsrecipeui.recipe.ExtendedRecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 隐藏的 LDLib2 元素，供 JEI 转移处理器定位当前编辑器并回传配方数据。
 */
public final class RecipeEditorJeiBridge extends UIElement {
    /** JEI 插件在 ModularUI 元素树中查询此桥接元素所用的固定 ID。 */
    public static final String ELEMENT_ID = "kubejs_recipe_ui#jei_bridge";
    private final TransferConsumer transferConsumer;

    /** 创建一个不参与布局和绘制的 JEI 数据接收器。 */
    public RecipeEditorJeiBridge(TransferConsumer transferConsumer) {
        this.transferConsumer = transferConsumer;
        setId(ELEMENT_ID);
        setDisplay(false);
    }

    /**
     * 将 JEI 提供的配方快照转发给界面创建时注册的消费函数。
     */
    public void fill(
            WorkstationType type,
            List<ItemStack> inputs,
            ItemStack output,
            RecipeSourceInfo sourceInfo,
            boolean shaped,
            int craftingWidth,
            int craftingHeight,
            float experience,
            int cookingTime,
            ExtendedRecipeSettings extendedSettings
    ) {
        transferConsumer.accept(
                type,
                inputs,
                output,
                sourceInfo,
                shaped,
                craftingWidth,
                craftingHeight,
                experience,
                cookingTime,
                extendedSettings
        );
    }

    /** 描述 JEI 配方转移到编辑器所需的全部字段。 */
    @FunctionalInterface
    public interface TransferConsumer {
        void accept(
                WorkstationType type,
                List<ItemStack> inputs,
                ItemStack output,
                RecipeSourceInfo sourceInfo,
                boolean shaped,
                int craftingWidth,
                int craftingHeight,
                float experience,
                int cookingTime,
                ExtendedRecipeSettings extendedSettings
        );
    }
}
