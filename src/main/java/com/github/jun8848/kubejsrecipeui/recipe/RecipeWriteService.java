package com.github.jun8848.kubejsrecipeui.recipe;

import dev.latvian.mods.kubejs.KubeJSPaths;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;

/**
 * 服务端配方脚本写入入口，统一处理权限判断和用户可翻译的结果状态。
 */
public final class RecipeWriteService {
    public static final String SUCCESS = "kubejs_recipe_ui.message.write_success";
    public static final String DENIED = "kubejs_recipe_ui.message.write_denied";
    public static final String FAILED = "kubejs_recipe_ui.message.write_failed";
    private static final GeneratedScriptStore STORE = new GeneratedScriptStore();

    private RecipeWriteService() {
    }

    /**
     * 将预览框中的配方代码写入 KubeJS 服务端脚本目录。
     * 多人服务器仅允许管理员写入，单人游戏则允许本地玩家操作。
     *
     * @param player 发起写入操作的服务端玩家
     * @param code 已经由预览流程生成或编辑的完整脚本
     * @return 用于界面通知的翻译键
     */
    public static String write(ServerPlayer player, String code) {
        var server = player.level().getServer();
        if (server == null || (!server.isSingleplayer() && !server.getPlayerList().isOp(player.getGameProfile()))) {
            return DENIED;
        }

        try {
            STORE.append(KubeJSPaths.SERVER_SCRIPTS, code);
            return SUCCESS;
        } catch (IOException | IllegalArgumentException exception) {
            return FAILED;
        }
    }
}
