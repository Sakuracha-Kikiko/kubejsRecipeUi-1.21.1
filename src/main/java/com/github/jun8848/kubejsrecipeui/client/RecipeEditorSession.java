package com.github.jun8848.kubejsrecipeui.client;

import com.github.jun8848.kubejsrecipeui.recipe.IngredientSpec;
import com.github.jun8848.kubejsrecipeui.recipe.ExtendedRecipeSettings;
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

/**
 * 单次打开编辑器期间的状态容器，隔离 UI 控件、JEI 数据和代码生成模型。
 * 每种工作站拥有独立状态，切换侧边栏不会丢失已经填写的内容。
 */
public final class RecipeEditorSession {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Player player;
    /** 按工作站类型保存槽位、配方形状和烹饪参数。 */
    private final EnumMap<WorkstationType, EditorState> states = new EnumMap<>(WorkstationType.class);
    private WorkstationType activeType = WorkstationType.CRAFTING;
    private WorkstationType selectedType = WorkstationType.CRAFTING;
    private int selectedSlot;
    private RecipeSourceInfo sourceInfo = RecipeSourceInfo.EMPTY;

    /** 为全部工作站预先创建固定尺寸的状态数组。 */
    public RecipeEditorSession(Player player) {
        this.player = player;
        for (var type : WorkstationType.values()) {
            states.put(type, new EditorState(type));
        }
    }

    /** @return 当前在主界面中显示的工作站类型 */
    public WorkstationType getActiveType() {
        return activeType;
    }

    /** 切换当前工作站，但保留其他工作站已经编辑的状态。 */
    public void setActiveType(WorkstationType activeType) {
        this.activeType = activeType;
        LOGGER.info("[工作台编辑器] 切换工作方块: {}", activeType);
    }

    /** @return 指定工作站和槽位的物品快照 */
    public ItemStack getInputStack(WorkstationType type, int index) {
        return state(type).inputStacks[index];
    }

    /** 更新当前工作站的输入槽位。 */
    public void setInputStack(int index, ItemStack stack) {
        setInputStack(activeType, index, stack);
    }

    /**
     * 更新指定输入槽位，并同时重建默认的精确物品原料描述。
     */
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

    /** @return 当前工作站指定槽位的原料表达式 */
    public IngredientSpec getInputSpec(int index) {
        return state(activeType).inputSpecs[index];
    }

    /** 保存原料设置弹窗生成的物品、标签或替代材料描述。 */
    public void setInputSpec(int index, IngredientSpec spec) {
        state(selectedType).inputSpecs[index] = spec;
    }

    /** @return 当前工作站输出槽的物品快照 */
    public ItemStack getOutputStack() {
        return getOutputStack(activeType);
    }

    /** @return 指定工作站输出槽的物品快照 */
    public ItemStack getOutputStack(WorkstationType type) {
        return state(type).outputStack;
    }

    /** 更新当前工作站输出槽。 */
    public void setOutputStack(ItemStack stack) {
        setOutputStack(activeType, stack);
    }

    /** 复制外部 ItemStack，避免 JEI 或玩家容器后续修改同一个对象。 */
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

    /** 修改当前输出物品数量，并执行界面允许范围校验。 */
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

    /** @return 当前工作站配方是否为有序配方 */
    public boolean isShaped() {
        return state(activeType).shaped;
    }

    /** 在当前支持形状的工作站上切换有序和无序。 */
    public void toggleShaped() {
        var state = state(activeType);
        state.shaped = !state.shaped;
    }

    /** 由 JEI 转移结果直接设置当前工作站配方形状。 */
    public void setShaped(boolean shaped) {
        state(activeType).shaped = shaped;
    }

    public int getExtendedTier(WorkstationType type) {
        return state(type).extendedTier;
    }

    public void setExtendedTier(WorkstationType type, int tier) {
        state(type).extendedTier = tier;
    }

    public int getPowerCost(WorkstationType type) {
        return state(type).powerCost;
    }

    public void setPowerCost(WorkstationType type, int powerCost) {
        state(type).powerCost = powerCost;
    }

    public int getPowerRate(WorkstationType type) {
        return state(type).powerRate;
    }

    public void setPowerRate(WorkstationType type, int powerRate) {
        state(type).powerRate = powerRate;
    }

    public int getIngredientCount(WorkstationType type) {
        return state(type).ingredientCount;
    }

    public void setIngredientCount(WorkstationType type, int ingredientCount) {
        state(type).ingredientCount = ingredientCount;
    }

    public int getExtendedCraftingTime(WorkstationType type) {
        return state(type).extendedCraftingTime;
    }

    public void setExtendedCraftingTime(WorkstationType type, int craftingTime) {
        state(type).extendedCraftingTime = craftingTime;
    }

    /** 批量应用 JEI 读取到的 Extended Crafting 专属参数。 */
    public void setExtendedSettings(WorkstationType type, ExtendedRecipeSettings settings) {
        var state = state(type);
        state.extendedTier = settings.tier();
        state.powerCost = settings.powerCost();
        state.powerRate = settings.powerRate();
        state.ingredientCount = settings.ingredientCount();
        state.extendedCraftingTime = settings.craftingTime();
    }

    /** 更新配方 ID、类型、来源模组和分组。 */
    public void setSourceInfo(RecipeSourceInfo sourceInfo) {
        this.sourceInfo = sourceInfo == null ? RecipeSourceInfo.EMPTY : sourceInfo;
    }

    /** 修改玩家可编辑的配方 ID，并同步推导来源模组命名空间。 */
    public void setRecipeId(String recipeId) {
        var normalized = recipeId == null ? "" : recipeId.trim();
        var separator = normalized.indexOf(':');
        var mod = separator > 0 ? normalized.substring(0, separator) : sourceInfo.mod();
        sourceInfo = new RecipeSourceInfo(normalized, sourceInfo.type(), mod, sourceInfo.group());
    }

    /** @return 指定烹饪工作站的经验值 */
    public float getExperience(WorkstationType type) {
        return state(type).experience;
    }

    /** 设置指定烹饪工作站的经验值。 */
    public void setExperience(WorkstationType type, float experience) {
        state(type).experience = experience;
    }

    /** @return 指定烹饪工作站的烹饪时间，单位为 tick */
    public int getCookingTime(WorkstationType type) {
        return state(type).cookingTime;
    }

    /** 设置指定烹饪工作站的烹饪时间。 */
    public void setCookingTime(WorkstationType type, int cookingTime) {
        state(type).cookingTime = cookingTime;
    }

    /** @return 原料设置弹窗当前操作的槽位索引 */
    public int getSelectedSlot() {
        return selectedSlot;
    }

    /** 选择当前工作站中的原料槽位。 */
    public void setSelectedSlot(int selectedSlot) {
        setSelectedSlot(activeType, selectedSlot);
    }

    /** 选择指定工作站中的原料槽位，并检查索引是否有效。 */
    public void setSelectedSlot(WorkstationType type, int selectedSlot) {
        if (selectedSlot < 0 || selectedSlot >= state(type).inputStacks.length) {
            throw new IllegalArgumentException("选择的输入槽位无效");
        }
        this.selectedType = type;
        this.selectedSlot = selectedSlot;
    }

    /** @return 当前选中物品由 KubeJS 序列化得到的数据组件补丁 */
    public String captureSelectedComponents() {
        return splitItemExpression(state(selectedType).inputStacks[selectedSlot]).componentPatch();
    }

    /** @return 当前选中物品所属的全部物品标签，按 ID 排序 */
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

    /**
     * 创建供代码生成器使用的不可变配方快照，并按开关移除输入或输出组件。
     */
    public RecipeDefinition createDefinition(boolean includeInputComponents, boolean includeOutputComponents) {
        var state = state(activeType);
        var output = createOutput(includeOutputComponents);
        var inputs = Arrays.stream(state.inputSpecs)
                .map(spec -> includeInputComponents ? spec : withoutComponents(spec))
                .toList();
        return new RecipeDefinition(
                sourceInfo.id(),
                activeType,
                state.shaped,
                new ArrayList<>(inputs),
                output,
                state.experience,
                state.cookingTime,
                new ExtendedRecipeSettings(
                        state.extendedTier,
                        state.powerCost,
                        state.powerRate,
                        state.ingredientCount,
                        state.extendedCraftingTime
                )
        );
    }

    /** 将当前输出槽转换为经过校验的输出模型。 */
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

    /**
     * 根据删除方式从 JEI 来源信息或当前槽位中提取 {@code event.remove} 的条件值。
     */
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
        // 复用 KubeJS 自身的物品字符串序列化，确保数据组件语法与 kubejs hand 一致。
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

    /** 单个工作站的可变编辑状态，仅由外层会话控制。 */
    private static final class EditorState {
        private final ItemStack[] inputStacks;
        private final IngredientSpec[] inputSpecs;
        private ItemStack outputStack = ItemStack.EMPTY;
        private boolean shaped = true;
        private float experience;
        private int cookingTime;
        private int extendedTier;
        private int powerCost;
        private int powerRate;
        private int ingredientCount;
        private int extendedCraftingTime;

        private EditorState(WorkstationType type) {
            inputStacks = new ItemStack[type.inputCount()];
            inputSpecs = new IngredientSpec[type.inputCount()];
            cookingTime = type.defaultCookingTime();
            extendedTier = type.extendedTier();
            powerCost = type.defaultPowerCost();
            powerRate = type.defaultPowerRate();
            ingredientCount = type.defaultIngredientCount();
            extendedCraftingTime = type.defaultCraftingTime();
            Arrays.fill(inputStacks, ItemStack.EMPTY);
        }
    }
}
