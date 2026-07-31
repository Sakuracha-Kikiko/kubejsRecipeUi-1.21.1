package com.github.jun8848.kubejsrecipeui.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

import java.util.Arrays;
import java.util.List;

/** 编辑器支持的原版与可选模组工作站，以及各自的槽位和参数能力。 */
public enum WorkstationType {
    CRAFTING("kubejs_recipe_ui.workstation.crafting", "minecraft:crafting_table", 9, 3, 3),
    FURNACE("kubejs_recipe_ui.workstation.furnace", "minecraft:furnace", 1, 1, 1),
    SMOKER("kubejs_recipe_ui.workstation.smoker", "minecraft:smoker", 1, 1, 1),
    BLAST_FURNACE("kubejs_recipe_ui.workstation.blast_furnace", "minecraft:blast_furnace", 1, 1, 1),
    CAMPFIRE("kubejs_recipe_ui.workstation.campfire", "minecraft:campfire", 1, 1, 1),
    STONECUTTER("kubejs_recipe_ui.workstation.stonecutter", "minecraft:stonecutter", 1, 1, 1),
    SMITHING("kubejs_recipe_ui.workstation.smithing", "minecraft:smithing_table", 3, 3, 1),

    EXTENDED_BASIC_TABLE("kubejs_recipe_ui.workstation.extended_basic_table", "extendedcrafting:basic_table", 9, 3, 3, 1, "extendedcrafting"),
    EXTENDED_ADVANCED_TABLE("kubejs_recipe_ui.workstation.extended_advanced_table", "extendedcrafting:advanced_table", 25, 5, 5, 2, "extendedcrafting"),
    EXTENDED_ELITE_TABLE("kubejs_recipe_ui.workstation.extended_elite_table", "extendedcrafting:elite_table", 49, 7, 7, 3, "extendedcrafting"),
    EXTENDED_ULTIMATE_TABLE("kubejs_recipe_ui.workstation.extended_ultimate_table", "extendedcrafting:ultimate_table", 81, 9, 9, 4, "extendedcrafting"),
    EXTENDED_COMBINATION("kubejs_recipe_ui.workstation.extended_combination", "extendedcrafting:crafting_core", 49, 8, 6, 0, "extendedcrafting"),
    EXTENDED_COMPRESSOR("kubejs_recipe_ui.workstation.extended_compressor", "extendedcrafting:compressor", 2, 2, 1, 0, "extendedcrafting"),
    EXTENDED_ENDER_CRAFTER("kubejs_recipe_ui.workstation.extended_ender_crafter", "extendedcrafting:ender_crafter", 9, 3, 3, 0, "extendedcrafting"),
    EXTENDED_FLUX_CRAFTER("kubejs_recipe_ui.workstation.extended_flux_crafter", "extendedcrafting:flux_crafter", 9, 3, 3, 0, "extendedcrafting"),

    AE2_CHARGER("kubejs_recipe_ui.workstation.ae2_charger", "ae2:charger", 1, 1, 1, 0, "ae2"),
    AE2_INSCRIBER("kubejs_recipe_ui.workstation.ae2_inscriber", "ae2:inscriber", 3, 1, 3, 0, "ae2"),
    AE2_TRANSFORM("kubejs_recipe_ui.workstation.ae2_transform", "ae2:charged_certus_quartz_crystal", 3, 1, 3, 0, "ae2"),
    AE2_ENTROPY("kubejs_recipe_ui.workstation.ae2_entropy", "ae2:entropy_manipulator", 0, 0, 0, 0, "ae2");

    public static final String EXTENDED_CRAFTING_MOD_ID = "extendedcrafting";
    public static final String AE2_MOD_ID = "ae2";

    private final String translationKey;
    private final Identifier iconId;
    private final int inputCount;
    private final int gridWidth;
    private final int gridHeight;
    private final int extendedTier;
    private final String requiredModId;

    WorkstationType(String translationKey, String iconId, int inputCount, int gridWidth, int gridHeight) {
        this(translationKey, iconId, inputCount, gridWidth, gridHeight, 0, null);
    }

    WorkstationType(
            String translationKey,
            String iconId,
            int inputCount,
            int gridWidth,
            int gridHeight,
            int extendedTier,
            String requiredModId
    ) {
        this.translationKey = translationKey;
        this.iconId = identifier(iconId);
        this.inputCount = inputCount;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.extendedTier = extendedTier;
        this.requiredModId = requiredModId;
    }

    /** @return 工作站名称的翻译键 */
    public String translationKey() {
        return translationKey;
    }

    /** @return 侧边栏按钮使用的物品图标；注册项缺失时使用屏障占位 */
    public Item icon() {
        var item = BuiltInRegistries.ITEM.getValue(iconId);
        return item == null || item == Items.AIR ? Items.BARRIER : item;
    }

    /** @return 该工作站配方需要的编辑器输入槽位数 */
    public int inputCount() {
        return inputCount;
    }

    public int gridWidth() {
        return gridWidth;
    }

    public int gridHeight() {
        return gridHeight;
    }

    public int extendedTier() {
        return extendedTier;
    }

    /** @return 当前环境是否具备显示并使用该工作站的依赖 */
    public boolean isAvailable() {
        return requiredModId == null || ModList.get().isLoaded(requiredModId);
    }

    /** @return 当前环境可加入侧栏的工作站列表 */
    public static List<WorkstationType> availableValues() {
        return Arrays.stream(values()).filter(WorkstationType::isAvailable).toList();
    }

    public boolean isExtendedCrafting() {
        return EXTENDED_CRAFTING_MOD_ID.equals(requiredModId);
    }

    public boolean isAe2() {
        return AE2_MOD_ID.equals(requiredModId);
    }

    public boolean isExtendedTable() {
        return this == EXTENDED_BASIC_TABLE
                || this == EXTENDED_ADVANCED_TABLE
                || this == EXTENDED_ELITE_TABLE
                || this == EXTENDED_ULTIMATE_TABLE;
    }

    public boolean supportsShaped() {
        return this == CRAFTING || isExtendedTable() || this == EXTENDED_ENDER_CRAFTER || this == EXTENDED_FLUX_CRAFTER;
    }

    /** @return 当前类型是否需要经验和烹饪时间参数 */
    public boolean isCooking() {
        return this == FURNACE || this == SMOKER || this == BLAST_FURNACE || this == CAMPFIRE;
    }

    /** @return 新建编辑器状态时使用的原版默认烹饪时间 */
    public int defaultCookingTime() {
        return this == CAMPFIRE ? 600 : 200;
    }

    public int defaultPowerCost() {
        return switch (this) {
            case EXTENDED_COMBINATION -> 400_000;
            case EXTENDED_COMPRESSOR -> 500_000;
            case EXTENDED_FLUX_CRAFTER -> 100_000;
            default -> 0;
        };
    }

    public int defaultPowerRate() {
        return switch (this) {
            case EXTENDED_COMBINATION -> 500;
            case EXTENDED_COMPRESSOR -> 5_000;
            case EXTENDED_FLUX_CRAFTER -> 400;
            default -> 0;
        };
    }

    public int defaultIngredientCount() {
        return this == EXTENDED_COMPRESSOR ? 10_000 : 1;
    }

    public int defaultCraftingTime() {
        return this == EXTENDED_ENDER_CRAFTER ? 60 : 1;
    }

    private static Identifier identifier(String value) {
        var separator = value.indexOf(':');
        return Identifier.fromNamespaceAndPath(value.substring(0, separator), value.substring(separator + 1));
    }
}
