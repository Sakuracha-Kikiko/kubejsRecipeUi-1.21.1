package com.github.jun8848.kubejsrecipeui.integration.jei;

import com.blakebr0.extendedcrafting.api.crafting.ICombinationRecipe;
import com.blakebr0.extendedcrafting.api.crafting.ICompressorRecipe;
import com.blakebr0.extendedcrafting.api.crafting.IEnderCrafterRecipe;
import com.blakebr0.extendedcrafting.api.crafting.IFluxCrafterRecipe;
import com.blakebr0.extendedcrafting.api.crafting.ITableRecipe;
import com.blakebr0.extendedcrafting.compat.jei.category.CombinationCraftingCategory;
import com.blakebr0.extendedcrafting.compat.jei.category.CompressorCraftingCategory;
import com.blakebr0.extendedcrafting.compat.jei.category.EnderCrafterCategory;
import com.blakebr0.extendedcrafting.compat.jei.category.FluxCraftingCategory;
import com.blakebr0.extendedcrafting.compat.jei.category.table.AdvancedTableCategory;
import com.blakebr0.extendedcrafting.compat.jei.category.table.BasicTableCategory;
import com.blakebr0.extendedcrafting.compat.jei.category.table.EliteTableCategory;
import com.blakebr0.extendedcrafting.compat.jei.category.table.UltimateTableCategory;
import com.blakebr0.extendedcrafting.crafting.recipe.ShapedEnderCrafterRecipe;
import com.blakebr0.extendedcrafting.crafting.recipe.ShapedFluxCrafterRecipe;
import com.blakebr0.extendedcrafting.crafting.recipe.ShapedTableRecipe;
import com.github.jun8848.kubejsrecipeui.client.RecipeEditorJeiBridge;
import com.github.jun8848.kubejsrecipeui.client.RecipeTransferData;
import com.github.jun8848.kubejsrecipeui.recipe.ExtendedRecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeSourceInfo;
import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 仅在 Extended Crafting 已加载时调用的 JEI 配方转移注册器。 */
final class ExtendedCraftingJeiIntegration {
    private ExtendedCraftingJeiIntegration() {
    }

    static void register(IRecipeTransferRegistration registration) {
        register(registration, BasicTableCategory.RECIPE_TYPE, WorkstationType.EXTENDED_BASIC_TABLE,
                ExtendedCraftingJeiIntegration::tableSettings);
        register(registration, AdvancedTableCategory.RECIPE_TYPE, WorkstationType.EXTENDED_ADVANCED_TABLE,
                ExtendedCraftingJeiIntegration::tableSettings);
        register(registration, EliteTableCategory.RECIPE_TYPE, WorkstationType.EXTENDED_ELITE_TABLE,
                ExtendedCraftingJeiIntegration::tableSettings);
        register(registration, UltimateTableCategory.RECIPE_TYPE, WorkstationType.EXTENDED_ULTIMATE_TABLE,
                ExtendedCraftingJeiIntegration::tableSettings);
        register(registration, CombinationCraftingCategory.RECIPE_TYPE, WorkstationType.EXTENDED_COMBINATION,
                ExtendedCraftingJeiIntegration::combinationSettings);
        register(registration, CompressorCraftingCategory.RECIPE_TYPE, WorkstationType.EXTENDED_COMPRESSOR,
                ExtendedCraftingJeiIntegration::compressorSettings);
        register(registration, EnderCrafterCategory.RECIPE_TYPE, WorkstationType.EXTENDED_ENDER_CRAFTER,
                ExtendedCraftingJeiIntegration::enderSettings);
        register(registration, FluxCraftingCategory.RECIPE_TYPE, WorkstationType.EXTENDED_FLUX_CRAFTER,
                ExtendedCraftingJeiIntegration::fluxSettings);
    }

    private static ExtendedRecipeSettings tableSettings(ITableRecipe recipe, WorkstationType type) {
        var defaults = ExtendedRecipeSettings.defaults(type);
        return new ExtendedRecipeSettings(
                recipe.getTier(),
                defaults.powerCost(),
                defaults.powerRate(),
                defaults.ingredientCount(),
                defaults.craftingTime()
        );
    }

    private static <R extends Recipe<?>> void register(
            IRecipeTransferRegistration registration,
            IRecipeType<RecipeHolder<R>> recipeType,
            WorkstationType workstationType,
            SettingsFactory<R> settingsFactory
    ) {
        registration.addRecipeTransferHandler(
                new ExtendedTransferHandler<>(
                        recipeType,
                        workstationType,
                        settingsFactory,
                        registration.getTransferHelper()
                ),
                recipeType
        );
    }

    private static ExtendedRecipeSettings combinationSettings(ICombinationRecipe recipe, WorkstationType type) {
        return new ExtendedRecipeSettings(
                0,
                recipe.getPowerCost(),
                recipe.getPowerRate(),
                1,
                1
        );
    }

    private static ExtendedRecipeSettings compressorSettings(ICompressorRecipe recipe, WorkstationType type) {
        return new ExtendedRecipeSettings(
                0,
                recipe.getPowerCost(),
                recipe.getPowerRate(),
                recipe.getIngredient().count(),
                1
        );
    }

    private static ExtendedRecipeSettings enderSettings(IEnderCrafterRecipe recipe, WorkstationType type) {
        return new ExtendedRecipeSettings(0, 0, 0, 1, recipe.getCraftingTime());
    }

    private static ExtendedRecipeSettings fluxSettings(IFluxCrafterRecipe recipe, WorkstationType type) {
        return new ExtendedRecipeSettings(0, recipe.getPowerRequired(), recipe.getPowerRate(), 1, 1);
    }

    private record ExtendedTransferHandler<R extends Recipe<?>>(
            IRecipeType<RecipeHolder<R>> recipeType,
            WorkstationType workstationType,
            SettingsFactory<R> settingsFactory,
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
            var serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
            var sourceInfo = new RecipeSourceInfo(
                    recipeId.toString(),
                    serializerId == null ? "" : serializerId.toString(),
                    recipeId.getNamespace(),
                    recipe.group()
            );
            var shape = shape(recipe);
            bridge.fill(new RecipeTransferData(
                    workstationType,
                    List.copyOf(inputs),
                    output,
                    sourceInfo,
                    shape.shaped(),
                    shape.width(),
                    shape.height(),
                    0.0F,
                    200,
                    RecipeSettings.of(settingsFactory.create(recipe, workstationType))
            ));
            return null;
        }
    }

    private static Shape shape(Recipe<?> recipe) {
        return switch (recipe) {
            case ShapedTableRecipe shaped -> new Shape(true, shaped.getWidth(), shaped.getHeight());
            case ShapedEnderCrafterRecipe shaped -> new Shape(true, shaped.getWidth(), shaped.getHeight());
            case ShapedFluxCrafterRecipe shaped -> new Shape(true, shaped.getWidth(), shaped.getHeight());
            default -> new Shape(false, 0, 0);
        };
    }

    @FunctionalInterface
    private interface SettingsFactory<R extends Recipe<?>> {
        ExtendedRecipeSettings create(R recipe, WorkstationType type);
    }

    private record Shape(boolean shaped, int width, int height) {
    }
}
