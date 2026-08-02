package com.github.jun8848.kubejsrecipeui.recipe.integration;

import com.github.jun8848.kubejsrecipeui.recipe.RecipeDefinition;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;
import net.neoforged.fml.ModList;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 配方类型的统一扩展入口，集中管理可选依赖、侧栏分组、校验和代码生成。
 */
public final class RecipeIntegrationRegistry {
    private static final Map<WorkstationType, Entry> ENTRIES = createEntries();

    private RecipeIntegrationRegistry() {
    }

    /** @return 当前环境中依赖完整、可以显示的工作站，顺序与枚举声明一致 */
    public static List<WorkstationType> availableWorkstations() {
        return Arrays.stream(WorkstationType.values())
                .filter(RecipeIntegrationRegistry::isAvailable)
                .toList();
    }

    public static boolean isAvailable(WorkstationType type) {
        var requiredModId = entry(type).integration().requiredModId();
        return requiredModId.isEmpty() || ModList.get().isLoaded(requiredModId);
    }

    public static String integrationId(WorkstationType type) {
        return entry(type).integration().id();
    }

    public static boolean belongsTo(WorkstationType type, String integrationId) {
        return entry(type).integration().id().equals(integrationId);
    }

    public static WorkstationType navigationType(WorkstationType type) {
        return entry(type).spec().navigationType();
    }

    public static RecipeSettings defaultSettings(WorkstationType type) {
        return entry(type).spec().defaultSettings();
    }

    public static void validate(RecipeValidationContext context) {
        entry(context.type()).spec().validator().validate(context);
    }

    public static String generatedIdSource(RecipeValidationContext context) {
        return entry(context.type()).spec().generatedIdSource().apply(context);
    }

    public static String generatedIdSignature(RecipeValidationContext context) {
        return entry(context.type()).integration().generatedIdSignature(context);
    }

    public static String generateBody(RecipeDefinition recipe) {
        return entry(recipe.type()).spec().generator().generate(recipe);
    }

    public static boolean inputRequired(WorkstationType type) {
        return entry(type).spec().inputRequired();
    }

    public static boolean outputRequired(WorkstationType type) {
        return entry(type).spec().outputRequired();
    }

    public static boolean supportsShaped(WorkstationType type) {
        return entry(type).spec().capabilities().supportsShaped();
    }

    public static boolean isCooking(WorkstationType type) {
        return entry(type).spec().capabilities().cooking();
    }

    public static int defaultCookingTime(WorkstationType type) {
        return entry(type).spec().capabilities().defaultCookingTime();
    }

    private static Entry entry(WorkstationType type) {
        var entry = ENTRIES.get(type);
        if (entry == null) {
            throw new IllegalStateException("工作站未注册配方集成: " + type);
        }
        return entry;
    }

    private static Map<WorkstationType, Entry> createEntries() {
        var entries = new EnumMap<WorkstationType, Entry>(WorkstationType.class);
        var integrations = List.<RecipeIntegration>of(
                new VanillaRecipeIntegration(),
                new ExtendedCraftingRecipeIntegration(),
                new Ae2RecipeIntegration(),
                new MekanismRecipeIntegration()
        );
        for (var integration : integrations) {
            integration.register(spec -> {
                var previous = entries.put(
                        spec.type(),
                        new Entry(integration, spec)
                );
                if (previous != null) {
                    throw new IllegalStateException("工作站被重复注册: " + spec.type());
                }
            });
        }
        for (var type : WorkstationType.values()) {
            if (!entries.containsKey(type)) {
                throw new IllegalStateException("工作站缺少配方集成: " + type);
            }
        }
        return Map.copyOf(entries);
    }

    private record Entry(RecipeIntegration integration, RecipeTypeSpec spec) {
    }
}
