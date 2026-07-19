package com.github.jun8848.kubejsrecipeui.recipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** 编辑器支持的原版工作站及其界面输入槽位配置。 */
public enum WorkstationType {
    CRAFTING("kubejs_recipe_ui.workstation.crafting", Items.CRAFTING_TABLE, 9),
    FURNACE("kubejs_recipe_ui.workstation.furnace", Items.FURNACE, 1),
    SMOKER("kubejs_recipe_ui.workstation.smoker", Items.SMOKER, 1),
    BLAST_FURNACE("kubejs_recipe_ui.workstation.blast_furnace", Items.BLAST_FURNACE, 1),
    STONECUTTER("kubejs_recipe_ui.workstation.stonecutter", Items.STONECUTTER, 1),
    SMITHING("kubejs_recipe_ui.workstation.smithing", Items.SMITHING_TABLE, 3);

    private final String translationKey;
    private final Item icon;
    private final int inputCount;

    WorkstationType(String translationKey, Item icon, int inputCount) {
        this.translationKey = translationKey;
        this.icon = icon;
        this.inputCount = inputCount;
    }

    /** @return 工作站名称的翻译键 */
    public String translationKey() {
        return translationKey;
    }

    /** @return 用于侧边栏按钮的原版物品图标 */
    public Item icon() {
        return icon;
    }

    /** @return 该工作站配方所需的编辑器输入槽位数 */
    public int inputCount() {
        return inputCount;
    }

    /** @return 当前类型是否需要经验和烹饪时间参数 */
    public boolean isCooking() {
        return this == FURNACE || this == SMOKER || this == BLAST_FURNACE;
    }
}
