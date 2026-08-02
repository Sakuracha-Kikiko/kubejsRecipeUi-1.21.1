package com.github.jun8848.kubejsrecipeui.recipe.integration;

/** 一组由原版或同一可选模组提供的配方编辑类型。 */
public interface RecipeIntegration {
    /** 注册表和诊断信息使用的稳定标识。 */
    String id();

    /** @return 可选模组 ID；原版集成返回空字符串 */
    default String requiredModId() {
        return "";
    }

    /**
     * 返回自动配方 ID 使用的专属参数签名。
     * 集成可以覆盖此方法以兼容旧版本已经生成的 ID。
     */
    default String generatedIdSignature(RecipeValidationContext context) {
        return context.settings().toString();
    }

    /** 向注册表登记该集成拥有的全部工作站。 */
    void register(Registrar registrar);

    @FunctionalInterface
    interface Registrar {
        void register(RecipeTypeSpec spec);
    }
}
