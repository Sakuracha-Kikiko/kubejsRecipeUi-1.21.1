package com.github.jun8848.kubejsrecipeui.recipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

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

    public String translationKey() {
        return translationKey;
    }

    public Item icon() {
        return icon;
    }

    public int inputCount() {
        return inputCount;
    }

    public boolean isCooking() {
        return this == FURNACE || this == SMOKER || this == BLAST_FURNACE;
    }
}
