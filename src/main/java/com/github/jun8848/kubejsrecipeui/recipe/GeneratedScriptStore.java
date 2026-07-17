package com.github.jun8848.kubejsrecipeui.recipe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class GeneratedScriptStore {
    public static final String FILE_NAME = "generated_recipes.js";
    private static final int MAX_CODE_LENGTH = 65_536;

    public synchronized Path append(Path serverScriptsDirectory, String code) throws IOException {
        validate(code);
        Files.createDirectories(serverScriptsDirectory);
        var target = serverScriptsDirectory.resolve(FILE_NAME);
        var generated = code.strip();
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
        if (!normalized.startsWith("ServerEvents.recipes(event => {") || !normalized.endsWith("});")) {
            throw new IllegalArgumentException("生成的配方代码格式无效");
        }
        if (normalized.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("生成的配方代码包含无效字符");
        }
    }
}
