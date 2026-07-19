package com.github.jun8848.kubejsrecipeui.client;

import com.github.jun8848.kubejsrecipeui.recipe.RecipeSourceInfo;
import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class RecipeEditorJeiBridge extends UIElement {
    public static final String ELEMENT_ID = "kubejs_recipe_ui#jei_bridge";
    private final TransferConsumer transferConsumer;

    public RecipeEditorJeiBridge(TransferConsumer transferConsumer) {
        this.transferConsumer = transferConsumer;
        setId(ELEMENT_ID);
        setDisplay(false);
    }

    public void fill(
            WorkstationType type,
            List<ItemStack> inputs,
            ItemStack output,
            RecipeSourceInfo sourceInfo,
            boolean shaped,
            int craftingWidth,
            int craftingHeight,
            float experience,
            int cookingTime
    ) {
        transferConsumer.accept(type, inputs, output, sourceInfo, shaped, craftingWidth, craftingHeight, experience, cookingTime);
    }

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
                int cookingTime
        );
    }
}
