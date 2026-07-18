package com.github.jun8848.kubejsrecipeui.event;

import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;

public final class EventWriteService {
    public static final String SUCCESS = "kubejs_recipe_ui.event.message.write_success";
    public static final String DENIED = "kubejs_recipe_ui.message.write_denied";
    public static final String FAILED = "kubejs_recipe_ui.event.message.write_failed";
    private static final EventScriptStore STORE = new EventScriptStore();

    private EventWriteService() {
    }

    public static String write(ServerPlayer player, String payload) {
        var server = player.level().getServer();
        if (server == null || (!server.isSingleplayer() && !server.getPlayerList().isOp(player.nameAndId()))) {
            return DENIED;
        }

        try {
            var separator = payload.indexOf('\n');
            if (separator < 1) {
                return FAILED;
            }
            var type = ScriptType.valueOf(payload.substring(0, separator));
            STORE.append(type.path, payload.substring(separator + 1));
            return SUCCESS;
        } catch (IOException | IllegalArgumentException exception) {
            return FAILED;
        }
    }
}
