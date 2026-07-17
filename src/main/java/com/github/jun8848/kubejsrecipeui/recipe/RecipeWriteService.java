package com.github.jun8848.kubejsrecipeui.recipe;

import dev.latvian.mods.kubejs.KubeJSPaths;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;

public final class RecipeWriteService {
    public static final String SUCCESS = "kubejs_recipe_ui.message.write_success";
    public static final String DENIED = "kubejs_recipe_ui.message.write_denied";
    public static final String FAILED = "kubejs_recipe_ui.message.write_failed";
    private static final GeneratedScriptStore STORE = new GeneratedScriptStore();

    private RecipeWriteService() {
    }

    public static String write(ServerPlayer player, String code) {
        var server = player.level().getServer();
        if (server == null || (!server.isSingleplayer() && !server.getPlayerList().isOp(player.nameAndId()))) {
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
