package com.github.jun8848.kubejsrecipeui;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(KubeJsRecipeUi.MOD_ID)
public final class KubeJsRecipeUi {
    public static final String MOD_ID = "kubejs_recipe_ui";
    private static final Logger LOGGER = LogUtils.getLogger();

    public KubeJsRecipeUi() {
        LOGGER.info("kubejsRecipeUi 已加载");
    }
}
