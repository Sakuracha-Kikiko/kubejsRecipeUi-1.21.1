package com.github.jun8848.kubejsrecipeui.integration.jei;

import com.github.jun8848.kubejsrecipeui.client.RecipeEditorJeiBridge;
import com.github.jun8848.kubejsrecipeui.client.RecipeTransferData;
import com.github.jun8848.kubejsrecipeui.recipe.MekanismRecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.MekanismRecipeSettings.ResourceField;
import com.github.jun8848.kubejsrecipeui.recipe.MekanismRecipeSettings.ResourceStack;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeSourceInfo;
import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.mojang.logging.LogUtils;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mekanism JEI 配方转移器。
 * 化学品对象通过反射读取，避免未安装 Mekanism 时让 JVM 解析可选模组类。
 */
final class MekanismJeiIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CHEMICAL_STACK_CLASS = "mekanism.api.chemical.ChemicalStack";

    private MekanismJeiIntegration() {
    }

    static void register(IRecipeTransferRegistration registration) {
        register(registration, "crushing", WorkstationType.MEKANISM_CRUSHING);
        register(registration, "enriching", WorkstationType.MEKANISM_ENRICHING);
        register(registration, "smelting", WorkstationType.MEKANISM_SMELTING);
        register(registration, "chemical_infusing", WorkstationType.MEKANISM_CHEMICAL_INFUSING);
        register(registration, "combining", WorkstationType.MEKANISM_COMBINING);
        register(registration, "separating", WorkstationType.MEKANISM_SEPARATING);
        register(registration, "washing", WorkstationType.MEKANISM_WASHING);
        register(registration, "evaporating", WorkstationType.MEKANISM_EVAPORATING);
        register(registration, "activating", WorkstationType.MEKANISM_ACTIVATING);
        register(registration, "centrifuging", WorkstationType.MEKANISM_CENTRIFUGING);
        register(registration, "crystallizing", WorkstationType.MEKANISM_CRYSTALLIZING);
        register(registration, "dissolution", WorkstationType.MEKANISM_DISSOLUTION);
        register(registration, "compressing", WorkstationType.MEKANISM_COMPRESSING);
        register(registration, "purifying", WorkstationType.MEKANISM_PURIFYING);
        register(registration, "injecting", WorkstationType.MEKANISM_INJECTING);
        register(registration, "nucleosynthesizing", WorkstationType.MEKANISM_NUCLEOSYNTHESIZING);
        register(registration, "energy_conversion", WorkstationType.MEKANISM_ENERGY_CONVERSION);
        register(registration, "chemical_conversion", WorkstationType.MEKANISM_CHEMICAL_CONVERSION);
        register(registration, "oxidizing", WorkstationType.MEKANISM_OXIDIZING);
        register(registration, "pigment_extracting", WorkstationType.MEKANISM_PIGMENT_EXTRACTING);
        register(registration, "pigment_mixing", WorkstationType.MEKANISM_PIGMENT_MIXING);
        register(registration, "metallurgic_infusing", WorkstationType.MEKANISM_METALLURGIC_INFUSING);
        register(registration, "painting", WorkstationType.MEKANISM_PAINTING);
        register(registration, "reaction", WorkstationType.MEKANISM_REACTION);
        register(registration, "condensentrating", WorkstationType.MEKANISM_ROTARY);
        register(registration, "decondensentrating", WorkstationType.MEKANISM_ROTARY);
        register(registration, "sawing", WorkstationType.MEKANISM_SAWING);
    }

    private static void register(
            IRecipeTransferRegistration registration,
            String categoryPath,
            WorkstationType type
    ) {
        RecipeType<RecipeHolder<Recipe<?>>> recipeType = RecipeType.createRecipeHolderType(
                ResourceLocation.fromNamespaceAndPath("mekanism", categoryPath)
        );
        registration.addRecipeTransferHandler(
                new MekanismTransferHandler(
                        recipeType,
                        type,
                        categoryPath,
                        registration.getTransferHelper()
                ),
                recipeType
        );
    }

    /** 将 JEI ingredient 槽位按物品、流体和化学品三类归一化。 */
    private static SlotData collectSlots(IRecipeSlotsView recipeSlots, RecipeIngredientRole role) {
        var items = new ArrayList<ItemStack>();
        var fluids = new ArrayList<ResourceStack>();
        var chemicals = new ArrayList<ResourceStack>();
        for (var slot : recipeSlots.getSlotViews(role)) {
            displayedIngredient(slot).ifPresent(ingredient -> {
                if (ingredient instanceof ItemStack stack) {
                    items.add(stack.copy());
                } else if (ingredient instanceof FluidStack stack && !stack.isEmpty()) {
                    var id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
                    if (id != null) {
                        fluids.add(new ResourceStack(id.toString(), Math.max(1, stack.getAmount())));
                    }
                } else if (ingredient.getClass().getName().equals(CHEMICAL_STACK_CLASS)) {
                    chemicalStack(ingredient).ifPresent(chemicals::add);
                }
            });
        }
        return new SlotData(items, fluids, chemicals);
    }

    private static Optional<Object> displayedIngredient(IRecipeSlotView slot) {
        var displayed = slot.getDisplayedIngredient();
        if (displayed.isPresent()) {
            return Optional.of(displayed.get().getIngredient());
        }
        return slot.getAllIngredients().findFirst().map(value -> (Object) value.getIngredient());
    }

    /** 读取 ChemicalStack#typeHolder 和 amount，不在方法签名中引用 Mekanism API。 */
    private static Optional<ResourceStack> chemicalStack(Object stack) {
        try {
            var holder = stack.getClass().getMethod("typeHolder").invoke(stack);
            var id = holder.getClass().getMethod("getRegisteredName").invoke(holder).toString();
            var amount = ((Number) stack.getClass().getMethod("amount").invoke(stack)).intValue();
            return Optional.of(new ResourceStack(id, Math.max(1, amount)));
        } catch (ReflectiveOperationException | ClassCastException exception) {
            LOGGER.warn("[工作台编辑器] 无法读取 Mekanism 化学品 JEI 数据: {}", stack, exception);
            return Optional.empty();
        }
    }

    private static MekanismRecipeSettings createSettings(
            Recipe<?> recipe,
            WorkstationType type,
            String categoryPath,
            SlotData inputs,
            SlotData outputs
    ) {
        var settings = MekanismRecipeSettings.defaults(type);
        if (!inputs.chemicals().isEmpty()) {
            settings = settings.withResource(ResourceField.PRIMARY_CHEMICAL_INPUT, inputs.chemicals().get(0));
        }
        if (inputs.chemicals().size() > 1) {
            settings = settings.withResource(ResourceField.SECONDARY_CHEMICAL_INPUT, inputs.chemicals().get(1));
        }
        if (!outputs.chemicals().isEmpty()) {
            settings = settings.withResource(ResourceField.PRIMARY_CHEMICAL_OUTPUT, outputs.chemicals().get(0));
        }
        if (outputs.chemicals().size() > 1) {
            settings = settings.withResource(ResourceField.SECONDARY_CHEMICAL_OUTPUT, outputs.chemicals().get(1));
        }
        if (!inputs.fluids().isEmpty()) {
            settings = settings.withResource(ResourceField.FLUID_INPUT, inputs.fluids().getFirst());
        }
        if (!outputs.fluids().isEmpty()) {
            settings = settings.withResource(ResourceField.FLUID_OUTPUT, outputs.fluids().getFirst());
        }
        if (!inputs.items().isEmpty()) {
            settings = settings.withItemInputCount(Math.max(1, inputs.items().get(0).getCount()));
        }
        if (inputs.items().size() > 1) {
            settings = settings.withExtraItemInputCount(Math.max(1, inputs.items().get(1).getCount()));
        }

        var duration = invokeInt(recipe, "getDuration");
        if (duration > 0) {
            settings = settings.withDuration(duration);
        }
        var energyRequired = invokeInt(recipe, "getEnergyRequired");
        if (energyRequired >= 0) {
            settings = settings.withEnergyRequired(energyRequired);
        }
        var energyMultiplier = invokeInt(recipe, "getEnergyMultiplier");
        if (energyMultiplier > 0) {
            settings = settings.withEnergyMultiplier(energyMultiplier);
        }
        var energyOutput = invokeInt(recipe, "getOutputRaw");
        if (type == WorkstationType.MEKANISM_ENERGY_CONVERSION && energyOutput > 0) {
            settings = settings.withEnergyOutput(energyOutput);
        }
        var perTickUsage = invokeBoolean(recipe, "perTickUsage");
        if (perTickUsage != null) {
            settings = settings.withPerTickUsage(perTickUsage);
        }

        if (type == WorkstationType.MEKANISM_ROTARY) {
            settings = settings.withRotaryMode(categoryPath.equals("condensentrating")
                    ? MekanismRecipeSettings.RotaryMode.FLUID_TO_CHEMICAL
                    : MekanismRecipeSettings.RotaryMode.CHEMICAL_TO_FLUID);
        }
        if (type == WorkstationType.MEKANISM_SAWING) {
            var secondary = reflectedItemStack(recipe, "getSecondaryOutputRaw").orElse(ItemStack.EMPTY);
            if (secondary.isEmpty() && outputs.items().size() > 1) {
                secondary = outputs.items().get(1);
            }
            if (secondary.isEmpty()) {
                return settings;
            }
            var id = BuiltInRegistries.ITEM.getKey(secondary.getItem());
            var chance = invokeDouble(recipe, "getSecondaryChance");
            settings = settings.withSecondaryItemOutput(
                    id == null ? "" : id.toString(),
                    Math.max(1, secondary.getCount()),
                    chance > 0.0D && chance <= 1.0D ? chance : 1.0D
            );
        }
        return settings;
    }

    /** 将 BasicSawmillRecipe 保存的 Optional<ItemStackTemplate> 转换为实际物品。 */
    private static Optional<ItemStack> reflectedItemStack(Object recipe, String methodName) {
        var value = invoke(recipe, methodName);
        if (value instanceof Optional<?> optional) {
            value = optional.orElse(null);
        }
        if (value == null) {
            return Optional.empty();
        }
        var created = invoke(value, "create");
        return created instanceof ItemStack stack && !stack.isEmpty()
                ? Optional.of(stack.copy())
                : Optional.empty();
    }

    private static int invokeInt(Object target, String methodName) {
        var value = invoke(target, methodName);
        return value instanceof Number number ? number.intValue() : -1;
    }

    private static double invokeDouble(Object target, String methodName) {
        var value = invoke(target, methodName);
        return value instanceof Number number ? number.doubleValue() : -1.0D;
    }

    @Nullable
    private static Boolean invokeBoolean(Object target, String methodName) {
        var value = invoke(target, methodName);
        return value instanceof Boolean booleanValue ? booleanValue : null;
    }

    @Nullable
    private static Object invoke(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (NoSuchMethodException ignored) {
            return null;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            LOGGER.debug("[工作台编辑器] 读取 Mekanism 配方参数失败: {}#{}", target.getClass().getName(), methodName, exception);
            return null;
        }
    }

    private record SlotData(
            List<ItemStack> items,
            List<ResourceStack> fluids,
            List<ResourceStack> chemicals
    ) {
    }

    private record MekanismTransferHandler(
            RecipeType<RecipeHolder<Recipe<?>>> recipeType,
            WorkstationType workstationType,
            String categoryPath,
            IRecipeTransferHandlerHelper transferHelper
    ) implements IRecipeTransferHandler<ModularUIContainerMenu, RecipeHolder<Recipe<?>>> {
        @Override
        public Class<? extends ModularUIContainerMenu> getContainerClass() {
            return ModularUIContainerMenu.class;
        }

        @Override
        public Optional<MenuType<ModularUIContainerMenu>> getMenuType() {
            return Optional.empty();
        }

        @Override
        public RecipeType<RecipeHolder<Recipe<?>>> getRecipeType() {
            return recipeType;
        }

        @Override
        @Nullable
        public IRecipeTransferError transferRecipe(
                ModularUIContainerMenu container,
                RecipeHolder<Recipe<?>> holder,
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

            var inputs = collectSlots(recipeSlots, RecipeIngredientRole.INPUT);
            var outputs = collectSlots(recipeSlots, RecipeIngredientRole.OUTPUT);
            var recipe = holder.value();
            var recipeId = holder.id();
            var serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
            var sourceInfo = new RecipeSourceInfo(
                    recipeId.toString(),
                    serializerId == null ? "" : serializerId.toString(),
                    recipeId.getNamespace(),
                    recipe.getGroup()
            );
            var output = workstationType == WorkstationType.MEKANISM_SAWING
                    ? reflectedItemStack(recipe, "getMainOutputRaw")
                            .orElseGet(() -> outputs.items().isEmpty() ? ItemStack.EMPTY : outputs.items().getFirst())
                    : outputs.items().isEmpty() ? ItemStack.EMPTY : outputs.items().getFirst();
            var settings = createSettings(recipe, workstationType, categoryPath, inputs, outputs);
            bridge.fill(new RecipeTransferData(
                    workstationType,
                    List.copyOf(inputs.items()),
                    output,
                    sourceInfo,
                    false,
                    0,
                    0,
                    0.0F,
                    200,
                    RecipeSettings.of(settings)
            ));
            LOGGER.info(
                    "[工作台编辑器] Mekanism JEI 配方已读取: 类型={}, ID={}, 物品输入={}, 流体输入={}, 化学品输入={}",
                    workstationType,
                    recipeId,
                    inputs.items().size(),
                    inputs.fluids().size(),
                    inputs.chemicals().size()
            );
            return null;
        }
    }
}
