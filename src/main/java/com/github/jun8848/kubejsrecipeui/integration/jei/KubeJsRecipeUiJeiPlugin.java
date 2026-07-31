package com.github.jun8848.kubejsrecipeui.integration.jei;

import com.github.jun8848.kubejsrecipeui.KubeJsRecipeUi;
import com.github.jun8848.kubejsrecipeui.client.RecipeEditorJeiBridge;
import com.github.jun8848.kubejsrecipeui.recipe.Ae2RecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeSourceInfo;
import com.github.jun8848.kubejsrecipeui.recipe.ExtendedRecipeSettings;
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
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.fml.ModList;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JEI 集成入口，为编辑器菜单注册原版配方类型的“转移配方”处理器。
 */
@JeiPlugin
public final class KubeJsRecipeUiJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(KubeJsRecipeUi.MOD_ID, "recipe_transfer");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    /** 注册工作台、四种烹饪设备、切石机和锻造台的 JEI 转移处理器。 */
    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        register(registration, RecipeTypes.CRAFTING, WorkstationType.CRAFTING);
        register(registration, RecipeTypes.SMELTING, WorkstationType.FURNACE);
        register(registration, RecipeTypes.SMOKING, WorkstationType.SMOKER);
        register(registration, RecipeTypes.BLASTING, WorkstationType.BLAST_FURNACE);
        register(registration, RecipeTypes.CAMPFIRE_COOKING, WorkstationType.CAMPFIRE);
        register(registration, RecipeTypes.STONECUTTING, WorkstationType.STONECUTTER);
        register(registration, RecipeTypes.SMITHING, WorkstationType.SMITHING);
        if (ModList.get().isLoaded(WorkstationType.EXTENDED_CRAFTING_MOD_ID)) {
            ExtendedCraftingJeiIntegration.register(registration);
        }
        if (ModList.get().isLoaded(WorkstationType.AE2_MOD_ID)) {
            Ae2JeiIntegration.register(registration);
        }
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

    /** 将 JEI 当前展示的 RecipeHolder 转换为编辑器可消费的数据。 */
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

            // 优先读取 JEI 当前轮播展示的材料，否则使用槽位中的第一个候选项。
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

            // 保留来源信息，使“按 ID/类型/模组/分组删除”无需玩家重复输入。
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
            // 烹饪配方还需要同步 JEI 配方对象中的经验和时间，而不是沿用界面默认值。
            var experience = recipe instanceof AbstractCookingRecipe cooking ? cooking.experience() : 0.0F;
            var cookingTime = recipe instanceof AbstractCookingRecipe cooking ? cooking.cookingTime() : 200;
            bridge.fill(
                    workstationType,
                    List.copyOf(inputs),
                    output,
                    sourceInfo,
                    shaped,
                    width,
                    height,
                    experience,
                    cookingTime,
                    ExtendedRecipeSettings.defaults(workstationType),
                    Ae2RecipeSettings.defaults()
            );
            return null;
        }
    }
}
