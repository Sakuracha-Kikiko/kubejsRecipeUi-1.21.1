package com.github.jun8848.kubejsrecipeui.event;

import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;

/** 服务端事件脚本写入入口，负责权限校验和脚本目录路由。 */
public final class EventWriteService {
    public static final String SUCCESS = "kubejs_recipe_ui.event.message.write_success";
    public static final String DENIED = "kubejs_recipe_ui.message.write_denied";
    public static final String FAILED = "kubejs_recipe_ui.event.message.write_failed";
    private static final EventScriptStore STORE = new EventScriptStore();

    private EventWriteService() {
    }

    /**
     * 解析“脚本端 + 换行 + 代码”载荷并写入对应 KubeJS 目录。
     */
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
            // ScriptType.path 由 KubeJS 提供，可准确路由 startup/server/client 脚本目录。
            var type = ScriptType.valueOf(payload.substring(0, separator));
            STORE.append(type.path, payload.substring(separator + 1));
            return SUCCESS;
        } catch (IOException | IllegalArgumentException exception) {
            return FAILED;
        }
    }
}
