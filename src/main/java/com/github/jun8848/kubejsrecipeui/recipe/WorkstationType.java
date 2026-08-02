package com.github.jun8848.kubejsrecipeui.recipe;

import com.github.jun8848.kubejsrecipeui.recipe.integration.Ae2RecipeIntegration;
import com.github.jun8848.kubejsrecipeui.recipe.integration.ExtendedCraftingRecipeIntegration;
import com.github.jun8848.kubejsrecipeui.recipe.integration.MekanismRecipeIntegration;
import com.github.jun8848.kubejsrecipeui.recipe.integration.RecipeIntegrationRegistry;
import com.github.jun8848.kubejsrecipeui.recipe.integration.VanillaRecipeIntegration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

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

    EXTENDED_BASIC_TABLE("kubejs_recipe_ui.workstation.extended_basic_table", "extendedcrafting:basic_table", 9, 3, 3),
    EXTENDED_ADVANCED_TABLE("kubejs_recipe_ui.workstation.extended_advanced_table", "extendedcrafting:advanced_table", 25, 5, 5),
    EXTENDED_ELITE_TABLE("kubejs_recipe_ui.workstation.extended_elite_table", "extendedcrafting:elite_table", 49, 7, 7),
    EXTENDED_ULTIMATE_TABLE("kubejs_recipe_ui.workstation.extended_ultimate_table", "extendedcrafting:ultimate_table", 81, 9, 9),
    EXTENDED_COMBINATION("kubejs_recipe_ui.workstation.extended_combination", "extendedcrafting:crafting_core", 49, 8, 6),
    EXTENDED_COMPRESSOR("kubejs_recipe_ui.workstation.extended_compressor", "extendedcrafting:compressor", 2, 2, 1),
    EXTENDED_ENDER_CRAFTER("kubejs_recipe_ui.workstation.extended_ender_crafter", "extendedcrafting:ender_crafter", 9, 3, 3),
    EXTENDED_FLUX_CRAFTER("kubejs_recipe_ui.workstation.extended_flux_crafter", "extendedcrafting:flux_crafter", 9, 3, 3),

    AE2_CHARGER("kubejs_recipe_ui.workstation.ae2_charger", "ae2:charger", 1, 1, 1),
    AE2_INSCRIBER("kubejs_recipe_ui.workstation.ae2_inscriber", "ae2:inscriber", 3, 1, 3),
    AE2_TRANSFORM("kubejs_recipe_ui.workstation.ae2_transform", "ae2:charged_certus_quartz_crystal", 3, 1, 3),
    AE2_ENTROPY("kubejs_recipe_ui.workstation.ae2_entropy", "ae2:entropy_manipulator", 0, 0, 0),

    MEKANISM_CRUSHING("kubejs_recipe_ui.workstation.mekanism_crushing", "mekanism:crusher", 1, 1, 1),
    MEKANISM_ENRICHING("kubejs_recipe_ui.workstation.mekanism_enriching", "mekanism:enrichment_chamber", 1, 1, 1),
    MEKANISM_SMELTING("kubejs_recipe_ui.workstation.mekanism_smelting", "mekanism:energized_smelter", 1, 1, 1),
    MEKANISM_CHEMICAL_INFUSING("kubejs_recipe_ui.workstation.mekanism_chemical_infusing", "mekanism:chemical_infuser", 0, 0, 0),
    MEKANISM_COMBINING("kubejs_recipe_ui.workstation.mekanism_combining", "mekanism:combiner", 2, 2, 1),
    MEKANISM_SEPARATING("kubejs_recipe_ui.workstation.mekanism_separating", "mekanism:electrolytic_separator", 0, 0, 0),
    MEKANISM_WASHING("kubejs_recipe_ui.workstation.mekanism_washing", "mekanism:chemical_washer", 0, 0, 0),
    MEKANISM_EVAPORATING("kubejs_recipe_ui.workstation.mekanism_evaporating", "mekanism:thermal_evaporation_controller", 0, 0, 0),
    MEKANISM_ACTIVATING("kubejs_recipe_ui.workstation.mekanism_activating", "mekanism:solar_neutron_activator", 0, 0, 0),
    MEKANISM_CENTRIFUGING("kubejs_recipe_ui.workstation.mekanism_centrifuging", "mekanism:isotopic_centrifuge", 0, 0, 0),
    MEKANISM_CRYSTALLIZING("kubejs_recipe_ui.workstation.mekanism_crystallizing", "mekanism:chemical_crystallizer", 0, 0, 0),
    MEKANISM_DISSOLUTION("kubejs_recipe_ui.workstation.mekanism_dissolution", "mekanism:chemical_dissolution_chamber", 1, 1, 1),
    MEKANISM_COMPRESSING("kubejs_recipe_ui.workstation.mekanism_compressing", "mekanism:osmium_compressor", 1, 1, 1),
    MEKANISM_PURIFYING("kubejs_recipe_ui.workstation.mekanism_purifying", "mekanism:purification_chamber", 1, 1, 1),
    MEKANISM_INJECTING("kubejs_recipe_ui.workstation.mekanism_injecting", "mekanism:chemical_injection_chamber", 1, 1, 1),
    MEKANISM_NUCLEOSYNTHESIZING("kubejs_recipe_ui.workstation.mekanism_nucleosynthesizing", "mekanism:antiprotonic_nucleosynthesizer", 1, 1, 1),
    MEKANISM_ENERGY_CONVERSION("kubejs_recipe_ui.workstation.mekanism_energy_conversion", "mekanism:basic_energy_cube", 1, 1, 1),
    MEKANISM_CHEMICAL_CONVERSION("kubejs_recipe_ui.workstation.mekanism_chemical_conversion", "mekanism:basic_chemical_tank", 1, 1, 1),
    MEKANISM_OXIDIZING("kubejs_recipe_ui.workstation.mekanism_oxidizing", "mekanism:chemical_oxidizer", 1, 1, 1),
    MEKANISM_PIGMENT_EXTRACTING("kubejs_recipe_ui.workstation.mekanism_pigment_extracting", "mekanism:pigment_extractor", 1, 1, 1),
    MEKANISM_PIGMENT_MIXING("kubejs_recipe_ui.workstation.mekanism_pigment_mixing", "mekanism:pigment_mixer", 0, 0, 0),
    MEKANISM_METALLURGIC_INFUSING("kubejs_recipe_ui.workstation.mekanism_metallurgic_infusing", "mekanism:metallurgic_infuser", 1, 1, 1),
    MEKANISM_PAINTING("kubejs_recipe_ui.workstation.mekanism_painting", "mekanism:painting_machine", 1, 1, 1),
    MEKANISM_REACTION("kubejs_recipe_ui.workstation.mekanism_reaction", "mekanism:pressurized_reaction_chamber", 1, 1, 1),
    MEKANISM_ROTARY("kubejs_recipe_ui.workstation.mekanism_rotary", "mekanism:rotary_condensentrator", 0, 0, 0),
    MEKANISM_SAWING("kubejs_recipe_ui.workstation.mekanism_sawing", "mekanism:precision_sawmill", 1, 1, 1);

    private final String translationKey;
    private final Identifier iconId;
    private final int inputCount;
    private final int gridWidth;
    private final int gridHeight;

    WorkstationType(String translationKey, String iconId, int inputCount, int gridWidth, int gridHeight) {
        this.translationKey = translationKey;
        this.iconId = identifier(iconId);
        this.inputCount = inputCount;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
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

    /** @return 当前环境是否具备显示并使用该工作站的依赖 */
    public boolean isAvailable() {
        return RecipeIntegrationRegistry.isAvailable(this);
    }

    /** @return 当前环境可加入侧栏的工作站列表 */
    public static List<WorkstationType> availableValues() {
        return RecipeIntegrationRegistry.availableWorkstations();
    }

    /** @return 该工作站所属配方集成的稳定 ID */
    public String integrationId() {
        return RecipeIntegrationRegistry.integrationId(this);
    }

    /** @return 侧栏中代表当前工作站分组的类型 */
    public WorkstationType navigationType() {
        return RecipeIntegrationRegistry.navigationType(this);
    }

    public boolean isExtendedCrafting() {
        return RecipeIntegrationRegistry.belongsTo(this, ExtendedCraftingRecipeIntegration.ID);
    }

    /** @return 是否属于原版配方集成 */
    public boolean isVanilla() {
        return RecipeIntegrationRegistry.belongsTo(this, VanillaRecipeIntegration.ID);
    }

    public boolean isAe2() {
        return RecipeIntegrationRegistry.belongsTo(this, Ae2RecipeIntegration.ID);
    }

    /** @return 是否属于 Mekanism 可选配方集成 */
    public boolean isMekanism() {
        return RecipeIntegrationRegistry.belongsTo(this, MekanismRecipeIntegration.ID);
    }

    public boolean isExtendedTable() {
        return switch (this) {
            case EXTENDED_BASIC_TABLE, EXTENDED_ADVANCED_TABLE, EXTENDED_ELITE_TABLE, EXTENDED_ULTIMATE_TABLE -> true;
            default -> false;
        };
    }

    public boolean supportsShaped() {
        return RecipeIntegrationRegistry.supportsShaped(this);
    }

    /** @return 当前类型是否需要经验和烹饪时间参数 */
    public boolean isCooking() {
        return RecipeIntegrationRegistry.isCooking(this);
    }

    /** @return 新建编辑器状态时使用的原版默认烹饪时间 */
    public int defaultCookingTime() {
        return RecipeIntegrationRegistry.defaultCookingTime(this);
    }

    private static Identifier identifier(String value) {
        var separator = value.indexOf(':');
        return Identifier.fromNamespaceAndPath(value.substring(0, separator), value.substring(separator + 1));
    }
}
