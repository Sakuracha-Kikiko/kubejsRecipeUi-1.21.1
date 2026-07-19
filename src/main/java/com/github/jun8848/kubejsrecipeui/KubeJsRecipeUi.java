package com.github.jun8848.kubejsrecipeui;

import com.mojang.logging.LogUtils;
import com.github.jun8848.kubejsrecipeui.registry.ModContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 模组入口，负责将本模组的注册内容挂载到 NeoForge 模组事件总线。
 */
@Mod(KubeJsRecipeUi.MOD_ID)
public final class KubeJsRecipeUi {
    /** 模组命名空间，同时用于资源、注册表和 JEI 插件标识。 */
    public static final String MOD_ID = "kubejs_recipe_ui";
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * NeoForge 创建模组实例时调用。
     *
     * @param modEventBus 当前模组专用的事件总线
     */
    public KubeJsRecipeUi(IEventBus modEventBus) {
        ModContent.register(modEventBus);
        LOGGER.info("kubejsRecipeUi 已加载");
    }
}
