package com.github.jun8848.kubejsrecipeui.client;

import com.github.jun8848.kubejsrecipeui.recipe.IngredientSpec;
import com.github.jun8848.kubejsrecipeui.recipe.OutputSpec;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeDefinition;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeDeleteMode;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeSourceInfo;
import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;
import com.mojang.serialization.DynamicOps;
import dev.latvian.mods.kubejs.core.ItemStackKJS;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

public final class RecipeEditorSession {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Player player;
    private final EnumMap<WorkstationType, EditorState> states = new EnumMap<>(WorkstationType.class);
    private WorkstationType activeType = WorkstationType.CRAFTING;
    private WorkstationType selectedType = WorkstationType.CRAFTING;
    private int selectedSlot;
    private RecipeSourceInfo sourceInfo = RecipeSourceInfo.EMPTY;

    public RecipeEditorSession(Player player) {
        this.player = player;
        for (var type : WorkstationType.values()) {
            states.put(type, new EditorState(type.inputCount()));
        }
    }

    public WorkstationType getActiveType() {
        return activeType;
    }

    public void setActiveType(WorkstationType activeType) {
        this.activeType = activeType;
        LOGGER.info("[工作台编辑器] 切换工作方块: {}", activeType);
    }

    public ItemStack getInputStack(int index) {
        return getInputStack(activeType, index);
    }

    public ItemStack getInputStack(WorkstationType type, int index) {
        return state(type).inputStacks[index];
    }

    public void setInputStack(int index, ItemStack stack) {
        setInputStack(activeType, index, stack);
    }

    public void setInputStack(WorkstationType type, int index, ItemStack stack) {
        var state = state(type);
        var normalized = normalizeInput(stack);
        state.inputStacks[index] = normalized;
        state.inputSpecs[index] = normalized.isEmpty() ? null : ingredientFromStack(normalized);
        LOGGER.info(
                "[工作台编辑器] {} 输入槽位 {} 更新: {}, 侧: {}",
                type,
                index + 1,
                stackDescription(normalized),
                player.level().isClientSide() ? "客户端" : "服务端"
        );
    }

    public IngredientSpec getInputSpec(int index) {
        return getInputSpec(activeType, index);
    }

    public IngredientSpec getInputSpec(WorkstationType type, int index) {
        return state(type).inputSpecs[index];
    }

    public void setInputSpec(int index, IngredientSpec spec) {
        state(selectedType).inputSpecs[index] = spec;
    }

    public ItemStack getOutputStack() {
        return getOutputStack(activeType);
    }

    public ItemStack getOutputStack(WorkstationType type) {
        return state(type).outputStack;
    }

    public void setOutputStack(ItemStack stack) {
        setOutputStack(activeType, stack);
    }

    public void setOutputStack(WorkstationType type, ItemStack stack) {
        var state = state(type);
        state.outputStack = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        LOGGER.info(
                "[工作台编辑器] {} 输出槽位更新: {}, 侧: {}",
                type,
                stackDescription(state.outputStack),
                player.level().isClientSide() ? "客户端" : "服务端"
        );
    }

    public void setOutputCount(int count) {
        var state = state(activeType);
        var outputStack = state.outputStack;
        if (outputStack.isEmpty()) {
            throw new IllegalArgumentException("请先设置输出物品");
        }
        if (count < 1 || count > 99) {
            throw new IllegalArgumentException("输出数量必须在 1 到 99 之间");
        }
        outputStack.setCount(count);
        LOGGER.info("[工作台编辑器] 输出数量更新: {}", count);
    }

    public boolean isShaped() {
        return state(WorkstationType.CRAFTING).shaped;
    }

    public void toggleShaped() {
        var state = state(WorkstationType.CRAFTING);
        state.shaped = !state.shaped;
    }

    public void setShaped(boolean shaped) {
        state(WorkstationType.CRAFTING).shaped = shaped;
    }

    public RecipeSourceInfo getSourceInfo() {
        return sourceInfo;
    }

    public void setSourceInfo(RecipeSourceInfo sourceInfo) {
        this.sourceInfo = sourceInfo == null ? RecipeSourceInfo.EMPTY : sourceInfo;
    }

    public void setRecipeId(String recipeId) {
        var normalized = recipeId == null ? "" : recipeId.trim();
        var separator = normalized.indexOf(':');
        var mod = separator > 0 ? normalized.substring(0, separator) : sourceInfo.mod();
        sourceInfo = new RecipeSourceInfo(normalized, sourceInfo.type(), mod, sourceInfo.group());
    }

    public float getExperience(WorkstationType type) {
        return state(type).experience;
    }

    public void setExperience(WorkstationType type, float experience) {
        state(type).experience = experience;
    }

    public int getCookingTime(WorkstationType type) {
        return state(type).cookingTime;
    }

    public void setCookingTime(WorkstationType type, int cookingTime) {
        state(type).cookingTime = cookingTime;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public void setSelectedSlot(int selectedSlot) {
        setSelectedSlot(activeType, selectedSlot);
    }

    public void setSelectedSlot(WorkstationType type, int selectedSlot) {
        if (selectedSlot < 0 || selectedSlot >= state(type).inputStacks.length) {
            throw new IllegalArgumentException("选择的输入槽位无效");
        }
        this.selectedType = type;
        this.selectedSlot = selectedSlot;
    }

    public String captureSelectedComponents() {
        return splitItemExpression(state(selectedType).inputStacks[selectedSlot]).componentPatch();
    }

    public List<String> getSelectedItemTags() {
        var stack = state(selectedType).inputStacks[selectedSlot];
        if (stack.isEmpty()) {
            return List.of();
        }

        var tags = stack.typeHolder()
                .tags()
                .map(tag -> "#" + tag.location())
                .sorted()
                .toList();
        LOGGER.info(
                "[工作台编辑器] 读取物品标签: 物品={}, 数量={}, 标签={}",
                BuiltInRegistries.ITEM.getKey(stack.getItem()),
                tags.size(),
                tags
        );
        return tags;
    }

    public RecipeDefinition createDefinition() {
        return createDefinition(true, true);
    }

    public RecipeDefinition createDefinition(boolean includeInputComponents, boolean includeOutputComponents) {
        var state = state(activeType);
        var output = createOutput(includeOutputComponents);
        var inputs = Arrays.stream(state.inputSpecs)
                .map(spec -> includeInputComponents ? spec : withoutComponents(spec))
                .toList();
        return new RecipeDefinition(
                activeType,
                state.shaped,
                new ArrayList<>(inputs),
                output,
                state.experience,
                state.cookingTime
        );
    }

    public OutputSpec createOutput(boolean includeComponents) {
        var outputStack = state(activeType).outputStack;
        if (outputStack.isEmpty()) {
            throw new IllegalArgumentException("请先设置输出物品");
        }

        var outputParts = splitItemExpression(outputStack);
        return new OutputSpec(
                outputParts.itemId(),
                outputStack.getCount(),
                includeComponents ? outputParts.componentPatch() : ""
        );
    }

    public String createDeleteValue(
            RecipeDeleteMode deleteMode,
            boolean includeInputComponents,
            boolean includeOutputComponents
    ) {
        return switch (deleteMode) {
            case ID -> sourceInfo.id();
            case TYPE -> sourceInfo.type();
            case MOD -> sourceInfo.mod();
            case GROUP -> sourceInfo.group();
            case INPUT -> {
                var specification = Arrays.stream(state(activeType).inputSpecs)
                        .filter(java.util.Objects::nonNull)
                        .findFirst()
                        .orElse(null);
                yield ingredientExpression(includeInputComponents
                        ? specification
                        : withoutComponents(specification));
            }
            case OUTPUT -> {
                var output = createOutput(includeOutputComponents);
                yield output.itemId() + output.componentPatch();
            }
        };
    }

    private EditorState state(WorkstationType type) {
        return states.get(type);
    }

    private IngredientSpec ingredientFromStack(ItemStack stack) {
        var parts = splitItemExpression(stack);
        return IngredientSpec.item(parts.itemId(), parts.componentPatch());
    }

    private static IngredientSpec withoutComponents(IngredientSpec specification) {
        if (specification == null) {
            return null;
        }
        return switch (specification.kind()) {
            case ITEM -> IngredientSpec.item(specification.value(), "");
            case TAG -> specification;
            case ALTERNATIVES -> IngredientSpec.alternatives(
                    specification.alternatives().stream()
                            .map(RecipeEditorSession::withoutComponents)
                            .toList()
            );
        };
    }

    private static String ingredientExpression(IngredientSpec specification) {
        if (specification == null) {
            return "";
        }
        return switch (specification.kind()) {
            case ITEM -> specification.value() + specification.componentPatch();
            case TAG -> "#" + specification.value();
            case ALTERNATIVES -> specification.alternatives().stream()
                    .map(RecipeEditorSession::ingredientExpression)
                    .findFirst()
                    .orElse("");
        };
    }

    private ItemExpression splitItemExpression(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new ItemExpression("", "");
        }

        var single = stack.copy();
        single.setCount(1);
        @SuppressWarnings("unchecked")
        DynamicOps<Tag> ops = (DynamicOps<Tag>) player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var expression = ((ItemStackKJS) (Object) single).kjs$toItemString0(ops);
        if (expression.length() >= 2 && expression.startsWith("'") && expression.endsWith("'")) {
            expression = expression.substring(1, expression.length() - 1);
        }
        var componentStart = expression.indexOf('[');
        if (componentStart < 0) {
            return new ItemExpression(expression, "");
        }
        return new ItemExpression(expression.substring(0, componentStart), expression.substring(componentStart));
    }

    private static ItemStack normalizeInput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private static String stackDescription(ItemStack stack) {
        if (stack.isEmpty()) {
            return "空";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount();
    }

    private record ItemExpression(String itemId, String componentPatch) {
    }

    private static final class EditorState {
        private final ItemStack[] inputStacks;
        private final IngredientSpec[] inputSpecs;
        private ItemStack outputStack = ItemStack.EMPTY;
        private boolean shaped = true;
        private float experience;
        private int cookingTime = 200;

        private EditorState(int inputCount) {
            inputStacks = new ItemStack[inputCount];
            inputSpecs = new IngredientSpec[inputCount];
            Arrays.fill(inputStacks, ItemStack.EMPTY);
        }
    }
}
