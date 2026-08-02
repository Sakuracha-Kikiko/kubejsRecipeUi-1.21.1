package com.github.jun8848.kubejsrecipeui.client;

import com.github.jun8848.kubejsrecipeui.recipe.RecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeSourceInfo;
import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/** JEI 配方转移到编辑器时使用的完整不可变数据包。 */
public record RecipeTransferData(
        WorkstationType type,
        List<ItemStack> inputs,
        ItemStack output,
        RecipeSourceInfo sourceInfo,
        boolean shaped,
        int craftingWidth,
        int craftingHeight,
        float experience,
        int cookingTime,
        RecipeSettings settings
) {
    public RecipeTransferData {
        type = Objects.requireNonNull(type, "type");
        inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
        output = output == null ? ItemStack.EMPTY : output.copy();
        sourceInfo = sourceInfo == null ? RecipeSourceInfo.EMPTY : sourceInfo;
        settings = Objects.requireNonNull(settings, "settings");
    }
}
