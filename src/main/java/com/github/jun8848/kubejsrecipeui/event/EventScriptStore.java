package com.github.jun8848.kubejsrecipeui.event;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class EventScriptStore {
    public static final String FILE_NAME = "generated_events.js";
    private static final int MAX_CODE_LENGTH = 65_536;

    public synchronized Path append(Path scriptsDirectory, String code) throws IOException {
        validate(code);
        Files.createDirectories(scriptsDirectory);
        var target = scriptsDirectory.resolve(FILE_NAME);
        var prefix = Files.exists(target) && Files.size(target) > 0 ? System.lineSeparator() + System.lineSeparator() : "";
        Files.writeString(
                target,
                prefix + code.strip() + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
        return target;
    }

    private static void validate(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("事件代码不能为空");
        }
        if (code.length() > MAX_CODE_LENGTH || code.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("事件代码无效或过长");
        }
    }
}
