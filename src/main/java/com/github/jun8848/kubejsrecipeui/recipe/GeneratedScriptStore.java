package com.github.jun8848.kubejsrecipeui.recipe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 负责以 UTF-8 保存自动生成的配方脚本，并将新配方合并到已有事件块中。
 */
public final class GeneratedScriptStore {
    /** 自动生成的服务端配方脚本文件名。 */
    public static final String FILE_NAME = "generated_recipes.js";
    private static final int MAX_CODE_LENGTH = 65_536;

    /**
     * 将一段完整的 {@code ServerEvents.recipes} 代码追加到生成文件。
     * 方法使用同步锁，避免同一服务端内并发点击造成文件内容互相覆盖。
     *
     * @param serverScriptsDirectory KubeJS 服务端脚本目录
     * @param code 待写入的完整配方事件代码
     * @return 实际写入的目标文件
     */
    public synchronized Path append(Path serverScriptsDirectory, String code) throws IOException {
        validate(code);
        Files.createDirectories(serverScriptsDirectory);
        var generated = code.strip();
        var target = serverScriptsDirectory.resolve(FILE_NAME);
        var content = Files.exists(target)
                ? mergeWithExisting(Files.readString(target, StandardCharsets.UTF_8), generated)
                : generated + System.lineSeparator();
        Files.writeString(
                target,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
        return target;
    }

    private static String mergeWithExisting(String existing, String generated) {
        if (existing.isBlank()) {
            return generated + System.lineSeparator();
        }

        var normalized = existing.strip();
        if (!normalized.startsWith("ServerEvents.recipes(event => {") || !normalized.endsWith("});")) {
            throw new IllegalArgumentException("现有生成脚本格式无效，无法追加配方");
        }

        // 只提取新事件块内部的语句，避免生成多个相互独立的 recipes 监听器。
        var closingIndex = normalized.lastIndexOf("});");
        var generatedBody = extractRecipeBody(generated);
        return normalized.substring(0, closingIndex).stripTrailing()
                + System.lineSeparator()
                + System.lineSeparator()
                + "  " + generatedBody
                + System.lineSeparator()
                + "});"
                + System.lineSeparator();
    }

    private static String extractRecipeBody(String generated) {
        var openingIndex = generated.indexOf('{');
        var closingIndex = generated.lastIndexOf("});");
        if (openingIndex < 0 || closingIndex <= openingIndex) {
            throw new IllegalArgumentException("生成的配方代码格式无效");
        }
        return generated.substring(openingIndex + 1, closingIndex).strip();
    }

    private static void validate(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("生成的配方代码不能为空");
        }
        if (code.length() > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException("生成的配方代码过长");
        }

        var normalized = code.strip();
        if (!isRecipeScript(normalized)) {
            throw new IllegalArgumentException("生成的配方代码格式无效");
        }
        if (normalized.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("生成的配方代码包含无效字符");
        }
    }

    private static boolean isRecipeScript(String code) {
        return code.startsWith("ServerEvents.recipes(event => {") && code.endsWith("});");
    }
}
