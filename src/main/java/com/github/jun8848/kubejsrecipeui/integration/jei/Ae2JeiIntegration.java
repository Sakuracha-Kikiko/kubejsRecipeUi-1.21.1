package com.github.jun8848.kubejsrecipeui.integration.jei;

import appeng.recipes.AERecipeTypes;
import appeng.recipes.entropy.EntropyMode;
import appeng.recipes.entropy.EntropyRecipe;
import appeng.recipes.handlers.ChargerRecipe;
import appeng.recipes.handlers.InscriberProcessType;
import appeng.recipes.handlers.InscriberRecipe;
import appeng.recipes.transform.TransformRecipe;
import com.github.jun8848.kubejsrecipeui.client.RecipeEditorJeiBridge;
import com.github.jun8848.kubejsrecipeui.recipe.Ae2RecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.ExtendedRecipeSettings;
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
import java.util.function.Function;

/** 仅在 AE2 已加载时调用的四类 JEI 配方转移注册器。 */
final class Ae2JeiIntegration {
    private Ae2JeiIntegration() {
    }

    static void register(IRecipeTransferRegistration registration) {
        register(
                registration,
                IRecipeType.create(AERecipeTypes.CHARGER),
                WorkstationType.AE2_CHARGER,
                recipe -> recipe.result().create(),
                recipe -> Ae2RecipeSettings.defaults()
        );
        register(
                registration,
                IRecipeType.create(AERecipeTypes.INSCRIBER),
                WorkstationType.AE2_INSCRIBER,
                recipe -> recipe.result().create(),
                Ae2JeiIntegration::inscriberSettings
        );
        register(
                registration,
                IRecipeType.create(AERecipeTypes.TRANSFORM),
                WorkstationType.AE2_TRANSFORM,
                TransformRecipe::createResult,
                Ae2JeiIntegration::transformSettings
        );
        register(
                registration,
                IRecipeType.create(AERecipeTypes.ENTROPY),
                WorkstationType.AE2_ENTROPY,
                Ae2JeiIntegration::entropyDisplayOutput,
                Ae2JeiIntegration::entropySettings
        );
    }

    private static <R extends Recipe<?>> void register(
            IRecipeTransferRegistration registration,
            IRecipeType<RecipeHolder<R>> recipeType,
            WorkstationType workstationType,
            Function<R, ItemStack> outputFactory,
            Function<R, Ae2RecipeSettings> settingsFactory
    ) {
        registration.addRecipeTransferHandler(
                new Ae2TransferHandler<>(recipeType, workstationType, outputFactory, settingsFactory,
                        registration.getTransferHelper()),
                recipeType
        );
    }

    private static Ae2RecipeSettings inscriberSettings(InscriberRecipe recipe) {
        var mode = recipe.getProcessType() == InscriberProcessType.INSCRIBE
                ? Ae2RecipeSettings.InscriberMode.INSCRIBE
                : Ae2RecipeSettings.InscriberMode.PRESS;
        return Ae2RecipeSettings.defaults().withInscriberMode(mode);
    }

    private static Ae2RecipeSettings transformSettings(TransformRecipe recipe) {
        var circumstance = recipe.circumstance().toJson();
        var type = circumstance.has("type") ? circumstance.get("type").getAsString() : "fluid";
        var settings = Ae2RecipeSettings.defaults().withTransformCondition(
                "explosion".equals(type)
                        ? Ae2RecipeSettings.TransformCondition.EXPLOSION
                        : Ae2RecipeSettings.TransformCondition.FLUID
        );
        if (circumstance.has("tag")) {
            settings = settings.withTransformFluidTag(circumstance.get("tag").getAsString());
        }
        return settings;
    }

    private static Ae2RecipeSettings entropySettings(EntropyRecipe recipe) {
        var settings = Ae2RecipeSettings.defaults().withEntropyMode(
                recipe.getMode() == EntropyMode.HEAT
                        ? Ae2RecipeSettings.EntropyMode.HEAT
                        : Ae2RecipeSettings.EntropyMode.COOL
        );

        var input = recipe.getInput();
        if (input.block().isPresent()) {
            settings = settings
                    .withEntropyInputType(Ae2RecipeSettings.EntropyResourceType.BLOCK)
                    .withEntropyInputId(registryId(BuiltInRegistries.BLOCK.getKey(input.block().get().block())));
        } else if (input.fluid().isPresent()) {
            settings = settings
                    .withEntropyInputType(Ae2RecipeSettings.EntropyResourceType.FLUID)
                    .withEntropyInputId(registryId(BuiltInRegistries.FLUID.getKey(input.fluid().get().fluid())));
        }

        var output = recipe.getOutput();
        if (output.block().isPresent()) {
            settings = settings
                    .withEntropyOutputType(Ae2RecipeSettings.EntropyOutputType.BLOCK)
                    .withEntropyOutputId(registryId(BuiltInRegistries.BLOCK.getKey(output.block().get().block())));
        } else if (output.fluid().isPresent()) {
            settings = settings
                    .withEntropyOutputType(Ae2RecipeSettings.EntropyOutputType.FLUID)
                    .withEntropyOutputId(registryId(BuiltInRegistries.FLUID.getKey(output.fluid().get().fluid())));
        } else if (!output.drops().isEmpty()) {
            var drop = output.drops().getFirst().create();
            settings = settings
                    .withEntropyOutputType(Ae2RecipeSettings.EntropyOutputType.DROPS)
                    .withEntropyOutputId(registryId(BuiltInRegistries.ITEM.getKey(drop.getItem())));
        }
        return settings;
    }

    private static ItemStack entropyDisplayOutput(EntropyRecipe recipe) {
        var output = recipe.getOutput();
        if (!output.drops().isEmpty()) {
            return output.drops().getFirst().create();
        }
        if (output.block().isPresent()) {
            return output.block().get().block().asItem().getDefaultInstance();
        }
        return ItemStack.EMPTY;
    }

    private static String registryId(net.minecraft.resources.Identifier id) {
        return id == null ? "" : id.toString();
    }

    private record Ae2TransferHandler<R extends Recipe<?>>(
            IRecipeType<RecipeHolder<R>> recipeType,
            WorkstationType workstationType,
            Function<R, ItemStack> outputFactory,
            Function<R, Ae2RecipeSettings> settingsFactory,
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

            var recipe = holder.value();
            var recipeId = holder.id().identifier();
            var serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
            var sourceInfo = new RecipeSourceInfo(
                    recipeId.toString(),
                    serializerId == null ? "" : serializerId.toString(),
                    recipeId.getNamespace(),
                    recipe.group()
            );
            bridge.fill(
                    workstationType,
                    List.copyOf(inputs),
                    outputFactory.apply(recipe),
                    sourceInfo,
                    false,
                    0,
                    0,
                    0.0F,
                    200,
                    ExtendedRecipeSettings.defaults(workstationType),
                    settingsFactory.apply(recipe)
            );
            return null;
        }
    }
}
