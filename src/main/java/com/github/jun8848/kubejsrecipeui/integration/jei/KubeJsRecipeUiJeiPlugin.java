package com.github.jun8848.kubejsrecipeui.integration.jei;

import com.github.jun8848.kubejsrecipeui.KubeJsRecipeUi;
import com.github.jun8848.kubejsrecipeui.client.RecipeEditorJeiBridge;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeSourceInfo;
import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@JeiPlugin
public final class KubeJsRecipeUiJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(KubeJsRecipeUi.MOD_ID, "recipe_transfer");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        register(registration, RecipeTypes.CRAFTING, WorkstationType.CRAFTING);
        register(registration, RecipeTypes.SMELTING, WorkstationType.FURNACE);
        register(registration, RecipeTypes.SMOKING, WorkstationType.SMOKER);
        register(registration, RecipeTypes.BLASTING, WorkstationType.BLAST_FURNACE);
        register(registration, RecipeTypes.STONECUTTING, WorkstationType.STONECUTTER);
        register(registration, RecipeTypes.SMITHING, WorkstationType.SMITHING);
    }

    private static <R extends Recipe<?>> void register(
            IRecipeTransferRegistration registration,
            IRecipeType<RecipeHolder<R>> recipeType,
            WorkstationType workstationType
    ) {
        registration.addRecipeTransferHandler(
                new EditorTransferHandler<>(recipeType, workstationType, registration.getTransferHelper()),
                recipeType
        );
    }

    private record EditorTransferHandler<R extends Recipe<?>>(
            IRecipeType<RecipeHolder<R>> recipeType,
            WorkstationType workstationType,
            IRecipeTransferHandlerHelper transferHelper
    ) implements IRecipeTransferHandler<ModularUIContainerMenu, RecipeHolder<R>> {
        @Override
        public Class<? extends ModularUIContainerMenu> getContainerClass() {
            return ModularUIContainerMenu.class;
        }

        @Override
        public Optional<MenuType<ModularUIContainerMenu>> getMenuType() {
            return Optional.empty();
        }

        @Override
        public IRecipeType<RecipeHolder<R>> getRecipeType() {
            return recipeType;
        }

        @Override
        @Nullable
        public IRecipeTransferError transferRecipe(
                ModularUIContainerMenu container,
                RecipeHolder<R> holder,
                IRecipeSlotsView recipeSlots,
                Player player,
                boolean maxTransfer,
                boolean doTransfer
        ) {
            var element = container.getModularUI().getElementById(RecipeEditorJeiBridge.ELEMENT_ID);
            if (!(element instanceof RecipeEditorJeiBridge bridge)) {
                return transferHelper.createInternalError();
            }
            if (!doTransfer) {
                return null;
            }

            var inputs = new ArrayList<ItemStack>();
            for (var slot : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
                inputs.add(slot.getDisplayedItemStack()
                        .or(() -> slot.getItemStacks().findFirst())
                        .map(ItemStack::copy)
                        .orElse(ItemStack.EMPTY));
            }
            var output = recipeSlots.getSlotViews(RecipeIngredientRole.OUTPUT).stream()
                    .flatMap(slot -> slot.getDisplayedItemStack().stream())
                    .findFirst()
                    .map(ItemStack::copy)
                    .orElse(ItemStack.EMPTY);

            var recipe = holder.value();
            var recipeId = holder.id().identifier();
            var recipeTypeId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
            var sourceInfo = new RecipeSourceInfo(
                    recipeId.toString(),
                    recipeTypeId == null ? "" : recipeTypeId.toString(),
                    recipeId.getNamespace(),
                    recipe.group()
            );
            var shaped = recipe instanceof ShapedRecipe;
            var width = shaped ? ((ShapedRecipe) recipe).getWidth() : 0;
            var height = shaped ? ((ShapedRecipe) recipe).getHeight() : 0;
            var experience = recipe instanceof AbstractCookingRecipe cooking ? cooking.experience() : 0.0F;
            var cookingTime = recipe instanceof AbstractCookingRecipe cooking ? cooking.cookingTime() : 200;
            bridge.fill(workstationType, List.copyOf(inputs), output, sourceInfo, shaped, width, height, experience, cookingTime);
            return null;
        }
    }
}
