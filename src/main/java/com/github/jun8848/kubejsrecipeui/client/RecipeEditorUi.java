package com.github.jun8848.kubejsrecipeui.client;

import com.github.jun8848.kubejsrecipeui.recipe.Ae2RecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.IngredientSpec;
import com.github.jun8848.kubejsrecipeui.recipe.ExtendedRecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.KubeJsRecipeGenerator;
import com.github.jun8848.kubejsrecipeui.recipe.MekanismRecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.MekanismRecipeSettings.ResourceField;
import com.github.jun8848.kubejsrecipeui.recipe.MekanismRecipeSettings.ResourceStack;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeDeleteMode;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeDefinition;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeWriteService;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeWriteMode;
import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.LDLibFonts;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEventBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SplitView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TabView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.Languages;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.StyleManager;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.TokenTypes;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 使用 LDLib2 构建配方编辑器的完整客户端界面。
 * 该类只负责控件布局和交互，配方状态存放在 {@link RecipeEditorSession} 中。
 */
public final class RecipeEditorUi {
    private static final float SLOT_SIZE = 18.0F;
    private static final int EDITOR_WIDTH = 600;
    private static final int EDITOR_HEIGHT = 360;
    private static final int SCREEN_MARGIN = 6;
    private static final float RECIPE_PANEL_PERCENT = 64.0F;
    private static final float PREVIEW_PANEL_PERCENT = 48.0F;
    private static final ResourceLocation AE2_JEI_TEXTURE = ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/jei.png");
    private static final ResourceLocation JEI_RECIPE_ARROW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "jei",
            "textures/jei/atlas/gui/recipe_arrow.png"
    );
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile Map<String, String> mekanismChemicalTranslationKeys;

    private RecipeEditorUi() {
    }

    /**
     * 创建一个包含工作站侧边栏、配方编辑区和代码预览的 ModularUI。
     */
    public static ModularUI create(BlockUIMenuType.BlockUIHolder holder) {
        var session = new RecipeEditorSession(holder.player);
        var availableTypes = WorkstationType.availableValues();
        var sidebarTypes = availableTypes.stream()
                .filter(type -> type.navigationType() == type)
                .toList();
        var selectedTypesByNavigation = new EnumMap<WorkstationType, WorkstationType>(WorkstationType.class);
        sidebarTypes.forEach(type -> selectedTypesByNavigation.put(type, type));
        var root = new UIElement();
        root.layout(style -> style
                .width(EDITOR_WIDTH)
                .height(EDITOR_HEIGHT)
                .flexShrink(0)
                .gapAll(4)
        );
        root.lss("flex-direction", "column");
        root.addClass("kru_root");

        var header = new UIElement();
        header.layout(style -> style
                .widthPercent(100)
                .height(26)
                .paddingHorizontal(8)
        );
        header.lss("flex-direction", "row");
        header.lss("align-items", "center");
        header.addClass("kru_header");

        var title = new Label();
        title.setText(Component.translatable(WorkstationType.CRAFTING.translationKey()));
        title.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
        title.layout(style -> style.widthPercent(100).height(16));
        title.addClass("kru_header_title");
        header.addChild(title);

        var contentBody = new UIElement();
        contentBody.layout(style -> style
                .widthPercent(100)
                .flex(1)
                .gapAll(4)
        );
        contentBody.lss("flex-direction", "row");
        contentBody.addClass("kru_content_body");

        var sidebar = new UIElement();
        sidebar.layout(style -> style
                .width(34)
                .heightPercent(100)
                .paddingAll(3)
                .gapAll(2)
        );
        sidebar.lss("flex-direction", "column");
        sidebar.lss("align-items", "center");
        sidebar.addClass("kru_sidebar");

        var main = new UIElement();
        main.layout(style -> style
                .flex(1)
                .heightPercent(100)
        );
        main.lss("flex-direction", "column");
        main.addClass("kru_main");

        var recipeCanvas = new UIElement();
        recipeCanvas.layout(style -> style
                .widthPercent(100)
                .heightPercent(100)
                .paddingAll(4)
                .gapAll(5)
        );
        recipeCanvas.lss("flex-direction", "column");
        recipeCanvas.lss("justify-content", "center");
        recipeCanvas.addClass("kru_recipe_canvas");

        var inspectorContent = new UIElement();
        inspectorContent.layout(style -> style
                .widthPercent(100)
                .paddingAll(7)
                .gapAll(5)
        );
        inspectorContent.lss("flex-direction", "column");
        inspectorContent.addClass("kru_inspector_content");

        var inspector = new ScrollerView();
        inspector.layout(style -> style.widthPercent(100).heightPercent(100));
        inspector.addClass("kru_inspector");
        inspector.viewContainer(element -> {
            element.layout(style -> style.widthPercent(100));
            element.lss("flex-direction", "column");
        });
        inspector.addScrollViewChild(inspectorContent);

        // LDLib2 回调要求捕获变量保持有效 final 引用，因此用单元素数组保存可变选项。
        var writeMode = new RecipeWriteMode[]{RecipeWriteMode.ADD_ONLY};
        var writeModeButton = actionButton(Component.translatable(writeMode[0].translationKey()), 88);
        writeModeButton.layout(style -> style.widthPercent(100).height(20));
        captureClick(writeModeButton, "切换配方写入方式", event -> {
            writeMode[0] = writeMode[0].next();
            writeModeButton.setText(Component.translatable(writeMode[0].translationKey()));
        });

        var inputComponents = componentSwitch("kubejs_recipe_ui.recipe_components.input", true);
        var outputComponents = componentSwitch("kubejs_recipe_ui.recipe_components.output", true);
        inspectorContent.addChildren(
                writeModeButton,
                switchOptionRow("kubejs_recipe_ui.recipe_components.input", inputComponents),
                switchOptionRow("kubejs_recipe_ui.recipe_components.output", outputComponents)
        );

        // 每个模组只占用一个侧栏入口，具体配方类型统一在属性栏内选择。
        var vanillaTypeByDisplay = new HashMap<String, WorkstationType>();
        var vanillaTypeDisplay = new EnumMap<WorkstationType, String>(WorkstationType.class);
        var vanillaTypeCandidates = availableTypes.stream()
                .filter(WorkstationType::isVanilla)
                .map(type -> {
                    var display = translate(type.translationKey());
                    vanillaTypeByDisplay.put(display, type);
                    vanillaTypeDisplay.put(type, display);
                    return display;
                })
                .toList();
        var vanillaTypeSelector = new Selector<String>();
        vanillaTypeSelector.dialog.style(style -> style.zIndex(200));
        vanillaTypeSelector.setCandidates(vanillaTypeCandidates);
        if (!vanillaTypeCandidates.isEmpty()) {
            vanillaTypeSelector.setSelected(vanillaTypeCandidates.getFirst());
        }
        vanillaTypeSelector.setCandidateUIProvider(RecipeEditorUi::slowRollingLabel);
        vanillaTypeSelector.layout(style -> style.width(115).height(20));
        var vanillaTypeSelectorRow = labeledField("kubejs_recipe_ui.editor.recipe_type", vanillaTypeSelector);
        vanillaTypeSelectorRow.layout(style -> style.widthPercent(100).height(20));
        inspectorContent.addChild(vanillaTypeSelectorRow);

        var extendedTypeByDisplay = new HashMap<String, WorkstationType>();
        var extendedTypeDisplay = new EnumMap<WorkstationType, String>(WorkstationType.class);
        var extendedTypeCandidates = availableTypes.stream()
                .filter(WorkstationType::isExtendedCrafting)
                .map(type -> {
                    var display = translate(type.translationKey());
                    extendedTypeByDisplay.put(display, type);
                    extendedTypeDisplay.put(type, display);
                    return display;
                })
                .toList();
        var extendedTypeSelector = new Selector<String>();
        extendedTypeSelector.dialog.style(style -> style.zIndex(200));
        extendedTypeSelector.setCandidates(extendedTypeCandidates);
        if (!extendedTypeCandidates.isEmpty()) {
            extendedTypeSelector.setSelected(extendedTypeCandidates.getFirst());
        }
        extendedTypeSelector.setCandidateUIProvider(RecipeEditorUi::slowRollingLabel);
        extendedTypeSelector.layout(style -> style.width(115).height(20));
        var extendedTypeSelectorRow = labeledField("kubejs_recipe_ui.editor.recipe_type", extendedTypeSelector);
        extendedTypeSelectorRow.layout(style -> style.widthPercent(100).height(20));
        extendedTypeSelectorRow.setDisplay(false);
        inspectorContent.addChild(extendedTypeSelectorRow);

        // 四类 AE2 配方共用一个侧栏入口，类型下拉框负责切换各自独立的编辑状态。
        var ae2TypeByDisplay = new HashMap<String, WorkstationType>();
        var ae2TypeDisplay = new EnumMap<WorkstationType, String>(WorkstationType.class);
        var ae2TypeCandidates = availableTypes.stream()
                .filter(WorkstationType::isAe2)
                .map(type -> {
                    var display = translate(type.translationKey());
                    ae2TypeByDisplay.put(display, type);
                    ae2TypeDisplay.put(type, display);
                    return display;
                })
                .toList();
        var ae2TypeSelector = new Selector<String>();
        ae2TypeSelector.dialog.style(style -> style.zIndex(200));
        ae2TypeSelector.setCandidates(ae2TypeCandidates);
        if (!ae2TypeCandidates.isEmpty()) {
            ae2TypeSelector.setSelected(ae2TypeCandidates.getFirst());
        }
        ae2TypeSelector.setCandidateUIProvider(RecipeEditorUi::slowRollingLabel);
        ae2TypeSelector.layout(style -> style.width(115).height(20));
        var ae2TypeSelectorRow = labeledField("kubejs_recipe_ui.editor.recipe_type", ae2TypeSelector);
        ae2TypeSelectorRow.layout(style -> style.widthPercent(100).height(20));
        ae2TypeSelectorRow.setDisplay(false);
        inspectorContent.addChild(ae2TypeSelectorRow);

        // 全部 Mekanism 数据包配方共用一个入口，通过下拉框切换独立编辑状态。
        var mekanismTypeByDisplay = new HashMap<String, WorkstationType>();
        var mekanismTypeDisplay = new EnumMap<WorkstationType, String>(WorkstationType.class);
        var mekanismTypeCandidates = availableTypes.stream()
                .filter(WorkstationType::isMekanism)
                .map(type -> {
                    var display = translate(type.translationKey());
                    mekanismTypeByDisplay.put(display, type);
                    mekanismTypeDisplay.put(type, display);
                    return display;
                })
                .toList();
        var mekanismTypeSelector = new Selector<String>();
        mekanismTypeSelector.dialog.style(style -> style.zIndex(200));
        mekanismTypeSelector.setCandidates(mekanismTypeCandidates);
        if (!mekanismTypeCandidates.isEmpty()) {
            mekanismTypeSelector.setSelected(mekanismTypeCandidates.getFirst());
        }
        mekanismTypeSelector.setCandidateUIProvider(RecipeEditorUi::slowRollingLabel);
        mekanismTypeSelector.layout(style -> style.width(115).height(20));
        var mekanismTypeSelectorRow = labeledField("kubejs_recipe_ui.editor.recipe_type", mekanismTypeSelector);
        mekanismTypeSelectorRow.layout(style -> style.widthPercent(100).height(20));
        mekanismTypeSelectorRow.setDisplay(false);
        inspectorContent.addChild(mekanismTypeSelectorRow);

        // Selector 返回显示文本，通过映射恢复实际删除枚举。
        var deleteModeByDisplay = new HashMap<String, RecipeDeleteMode>();
        var deleteModeCandidates = Arrays.stream(RecipeDeleteMode.values()).map(mode -> {
            var display = translate(mode.translationKey());
            deleteModeByDisplay.put(display, mode);
            return display;
        }).toList();
        var deleteMode = new RecipeDeleteMode[]{RecipeDeleteMode.ID};
        var deleteModeSelector = new Selector<String>();
        deleteModeSelector.dialog.style(style -> style.zIndex(200));
        deleteModeSelector.setCandidates(deleteModeCandidates)
                .setSelected(translate(RecipeDeleteMode.ID.translationKey()));
        deleteModeSelector.setCandidateUIProvider(RecipeEditorUi::slowRollingLabel);
        deleteModeSelector.layout(style -> style.widthPercent(100).height(20));
        deleteModeSelector.setOnValueChanged(value -> {
            var selected = deleteModeByDisplay.get(value);
            if (selected != null) {
                deleteMode[0] = selected;
            }
        });

        // 来源标签页会在 JEI 填充时同步刷新，空值用短横线保持版面稳定。
        var sourceIdValue = sourceValueLabel();
        var sourceTypeValue = sourceValueLabel();
        var sourceModValue = sourceValueLabel();
        var sourceGroupValue = sourceValueLabel();

        var recipeIdField = new TextField();
        recipeIdField.setAnyString();
        recipeIdField.layout(style -> style.widthPercent(100).height(20));
        recipeIdField.textFieldStyle(style -> style.placeholder(
                Component.translatable("kubejs_recipe_ui.jei.recipe_id.placeholder")
        ));
        recipeIdField.registerValueListener(value -> {
            session.setRecipeId(value);
            sourceIdValue.setText(Component.literal(sourceDisplayValue(value)));
        });
        inspectorContent.addChildren(
                editorSectionLabel("kubejs_recipe_ui.editor.inspector.delete"),
                fieldLabel("kubejs_recipe_ui.recipe.id"),
                recipeIdField,
                deleteModeSelector
        );

        var actions = new UIElement();
        actions.layout(style -> style
                .height(46)
                .widthPercent(100)
                .gapAll(4)
        );
        actions.lss("flex-direction", "column");

        var modeButton = actionButton(Component.translatable("kubejs_recipe_ui.action.shaped"), 74);
        modeButton.layout(style -> style.widthPercent(100).height(20));
        modeButton.style(style -> style.tooltips(Component.translatable("kubejs_recipe_ui.tooltip.mode")));
        captureClick(modeButton, "切换有序/无序", event -> {
            session.toggleShaped();
            modeButton.setText(Component.translatable(session.isShaped()
                    ? "kubejs_recipe_ui.action.shaped"
                    : "kubejs_recipe_ui.action.shapeless"));
        });

        var previewButton = actionButton(Component.translatable("kubejs_recipe_ui.action.preview"), 74);
        previewButton.addPreIcon(Icons.COPY);
        previewButton.style(style -> style.tooltips(Component.translatable("kubejs_recipe_ui.tooltip.preview")));

        // 预览框既展示生成结果，也允许玩家在写入前手动调整代码。
        var codePreview = new CodeEditor();
        codePreview.setLanguage(Languages.JAVASCRIPT);
        codePreview.setStyleManager(createLightCodeStyle());
        codePreview.setActive(true);
        codePreview.setFocusable(true);
        codePreview.contentView.setAllowHitTest(true);
        // 标签页和滚动条都可能参与命中测试，捕获点击后显式把键盘焦点交还给代码编辑器。
        codePreview.contentView.addEventListener("mouseDown", event -> codePreview.focus(), true);
        // CodeEditor 不继承 text-area 的文本样式，必须显式关闭默认阴影，否则高 GUI 缩放下会出现重影。
        codePreview.textAreaStyle(style -> style
                .font(LDLibFonts.JETBRAINS_MONO_BOLD)
                .fontSize(9.0F)
                .lineSpacing(2.0F)
                .textShadow(false)
        );
        codePreview.setValue(new String[0], false);
        codePreview.layout(style -> style.widthPercent(100).heightPercent(100));

        var writeButton = actionButton(Component.translatable("kubejs_recipe_ui.action.write"), 74);
        writeButton.addPreIcon(Icons.SAVE);
        writeButton.style(style -> style.tooltips(Component.translatable("kubejs_recipe_ui.tooltip.write")));
        // 文件系统只允许服务端访问；客户端按钮通过 RPC 请求服务端执行权限校验和写入。
        var writeRpc = writeButton.addRPCEvent(RPCEventBuilder.simple(
                String.class,
                String.class,
                code -> holder.player instanceof ServerPlayer serverPlayer
                        ? RecipeWriteService.write(serverPlayer, code)
                        : RecipeWriteService.FAILED
        ));
        captureClick(writeButton, "写入预览代码", event -> {
            var code = editorCode(codePreview).trim();
            if (code.isEmpty()) {
                showInGameMessage(holder.player, "kubejs_recipe_ui.message.preview_required");
                return;
            }
            writeRpc.send(
                    result -> showWriteResult(holder.player, result, RecipeWriteService.FAILED),
                    code
            );
        });

        var generateActions = new UIElement();
        generateActions.layout(style -> style.widthPercent(100).height(22).gapAll(4));
        generateActions.lss("flex-direction", "row");
        previewButton.layout(style -> style.flex(1).height(20));
        writeButton.layout(style -> style.flex(1).height(20));
        generateActions.addChildren(previewButton, writeButton);
        actions.addChildren(modeButton, generateActions);
        inspectorContent.addChildren(
                editorSectionLabel("kubejs_recipe_ui.editor.inspector.actions"),
                actions
        );

        var editorArea = new UIElement();
        editorArea.layout(style -> style.widthPercent(100).height(190));
        editorArea.lss("flex-direction", "column");
        editorArea.lss("justify-content", "center");
        editorArea.addClass("kru_recipe_frame");
        // 所有工作站面板只创建一次，侧边栏切换时仅改变可见性以保留控件状态。
        var panels = new EnumMap<WorkstationType, UIElement>(WorkstationType.class);
        for (var type : availableTypes) {
            var panel = RecipeEditorPanelRegistry.create(root, holder, session, type);
            panel.setDisplay(type == WorkstationType.CRAFTING);
            panels.put(type, panel);
            editorArea.addChild(panel);
        }
        var inventoryLabel = new Label();
        inventoryLabel.setText(Component.translatable("container.inventory"));
        inventoryLabel.layout(style -> style.height(11).marginTop(1));
        inventoryLabel.addClass("kru_inventory_label");

        var inventorySlots = new InventorySlots();
        inventorySlots.layout(style -> style.width(162).height(77).marginHorizontalAuto());

        var inventoryArea = new UIElement();
        inventoryArea.layout(style -> style.widthPercent(100).height(97).paddingHorizontal(4).paddingVertical(3).gapAll(2));
        inventoryArea.lss("flex-direction", "column");
        inventoryArea.addClass("kru_inventory_frame");
        inventoryArea.addChildren(inventoryLabel, inventorySlots);
        recipeCanvas.addChildren(editorArea, inventoryArea);

        var sourceContent = new UIElement();
        sourceContent.layout(style -> style
                .widthPercent(100)
                .heightPercent(100)
                .paddingAll(6)
                .gapAll(3)
        );
        sourceContent.lss("flex-direction", "column");
        sourceContent.addClass("kru_source_content");
        sourceContent.addChildren(
                sourceInfoRow("kubejs_recipe_ui.editor.source.id", sourceIdValue),
                sourceInfoRow("kubejs_recipe_ui.editor.source.type", sourceTypeValue),
                sourceInfoRow("kubejs_recipe_ui.editor.source.mod", sourceModValue),
                sourceInfoRow("kubejs_recipe_ui.editor.source.group", sourceGroupValue)
        );

        var codeContent = new UIElement();
        codeContent.layout(style -> style.widthPercent(100).heightPercent(100).paddingAll(4));
        codeContent.addClass("kru_code_content");
        codeContent.addChild(codePreview);

        var propertiesTab = new Tab().setText(Component.translatable("kubejs_recipe_ui.editor.inspector.recipe"));
        var codeTab = new Tab().setText(Component.translatable("kubejs_recipe_ui.editor.tab.code"));
        var sourceTab = new Tab().setText(Component.translatable("kubejs_recipe_ui.editor.tab.source"));
        var inspectorTabs = new TabView();
        inspectorTabs.layout(style -> style.widthPercent(100).heightPercent(100));
        inspectorTabs.addClass("kru_inspector_tabs");
        inspectorTabs.addTab(propertiesTab, inspector);
        inspectorTabs.addTab(
                codeTab,
                codeContent
        );
        inspectorTabs.addTab(
                sourceTab,
                sourceContent
        );
        inspectorTabs.selectTab(propertiesTab);

        var workspaceSplit = new SplitView.Horizontal();
        workspaceSplit.setPercentage(RECIPE_PANEL_PERCENT).left(recipeCanvas).right(inspectorTabs);
        workspaceSplit.layout(style -> style.widthPercent(100).heightPercent(100));
        workspaceSplit.addClass("kru_workspace_split");
        // 配方属性需要较宽的工作区；代码和来源模式自动扩大右栏，长 ID 无需挤在窄列中。
        inspectorTabs.setOnTabSelected(tab -> workspaceSplit.setPercentage(
                tab == propertiesTab ? RECIPE_PANEL_PERCENT : PREVIEW_PANEL_PERCENT
        ));

        // 代码生成成功后自动切换到预览模式，属性、预览和来源共用完整右栏空间。
        captureClick(previewButton, "生成代码预览", event -> generateRecipe(
                session,
                writeMode[0],
                deleteMode[0],
                inputComponents.isOn(),
                outputComponents.isOn(),
                code -> {
                    codePreview.setValue(code.split("\\R", -1), false);
                    inspectorTabs.selectTab(codeTab);
                }
        ));
        main.addChild(workspaceSplit);

        var workstationButtons = new EnumMap<WorkstationType, Button>(WorkstationType.class);
        for (var type : sidebarTypes) {
            var button = new Button();
            button.noText();
            button.layout(style -> style.width(20).height(20));
            button.lss("align-items", "center");
            button.lss("justify-content", "center");
            button.addClass("kru_sidebar_button");
            var icon = new UIElement();
            icon.layout(style -> style.width(16).height(16));
            icon.style(style -> style.background(new ItemStackTexture(type.icon())));
            icon.addClass("kru_sidebar_icon");
            button.addChild(icon);
            button.style(style -> style.tooltips(Component.translatable(
                    type == WorkstationType.CRAFTING
                            ? "kubejs_recipe_ui.integration.minecraft"
                            : type == WorkstationType.EXTENDED_BASIC_TABLE
                            ? "kubejs_recipe_ui.integration.extendedcrafting"
                            : type == WorkstationType.AE2_CHARGER
                            ? "kubejs_recipe_ui.integration.ae2"
                            : type == WorkstationType.MEKANISM_CRUSHING
                            ? "kubejs_recipe_ui.integration.mekanism"
                            : type.translationKey()
            )));
            button.setActive(type != WorkstationType.CRAFTING);
            captureClick(button, "切换工作方块: " + type, event -> {
                var targetType = selectedTypesByNavigation.getOrDefault(type, type);
                main.setDisplay(true);
                session.setActiveType(targetType);
                title.setText(Component.translatable(targetType.translationKey()));
                showWorkstationPanel(panels, targetType);
                updateWorkstationButtons(workstationButtons, targetType);
                vanillaTypeSelectorRow.setDisplay(targetType.isVanilla());
                extendedTypeSelectorRow.setDisplay(targetType.isExtendedCrafting());
                ae2TypeSelectorRow.setDisplay(targetType.isAe2());
                mekanismTypeSelectorRow.setDisplay(targetType.isMekanism());
                if (targetType.isVanilla()) {
                    vanillaTypeSelector.setSelected(vanillaTypeDisplay.get(targetType));
                }
                if (targetType.isExtendedCrafting()) {
                    extendedTypeSelector.setSelected(extendedTypeDisplay.get(targetType));
                }
                if (targetType.isAe2()) {
                    ae2TypeSelector.setSelected(ae2TypeDisplay.get(targetType));
                }
                if (targetType.isMekanism()) {
                    mekanismTypeSelector.setSelected(mekanismTypeDisplay.get(targetType));
                }
                modeButton.setDisplay(targetType.supportsShaped());
                modeButton.setText(Component.translatable(session.isShaped()
                        ? "kubejs_recipe_ui.action.shaped"
                        : "kubejs_recipe_ui.action.shapeless"));
            });
            workstationButtons.put(type, button);
            sidebar.addChild(button);
        }

        vanillaTypeSelector.setOnValueChanged(value -> {
            var type = vanillaTypeByDisplay.get(value);
            if (type == null) {
                return;
            }
            selectedTypesByNavigation.put(type.navigationType(), type);
            session.setActiveType(type);
            title.setText(Component.translatable(type.translationKey()));
            showWorkstationPanel(panels, type);
            updateWorkstationButtons(workstationButtons, type);
            modeButton.setDisplay(type.supportsShaped());
            modeButton.setText(Component.translatable(session.isShaped()
                    ? "kubejs_recipe_ui.action.shaped"
                    : "kubejs_recipe_ui.action.shapeless"));
        });

        extendedTypeSelector.setOnValueChanged(value -> {
            var type = extendedTypeByDisplay.get(value);
            if (type == null) {
                return;
            }
            selectedTypesByNavigation.put(type.navigationType(), type);
            session.setActiveType(type);
            title.setText(Component.translatable(type.translationKey()));
            showWorkstationPanel(panels, type);
            updateWorkstationButtons(workstationButtons, type);
            modeButton.setDisplay(type.supportsShaped());
            modeButton.setText(Component.translatable(session.isShaped()
                    ? "kubejs_recipe_ui.action.shaped"
                    : "kubejs_recipe_ui.action.shapeless"));
        });

        ae2TypeSelector.setOnValueChanged(value -> {
            var type = ae2TypeByDisplay.get(value);
            if (type == null) {
                return;
            }
            selectedTypesByNavigation.put(type.navigationType(), type);
            session.setActiveType(type);
            title.setText(Component.translatable(type.translationKey()));
            showWorkstationPanel(panels, type);
            updateWorkstationButtons(workstationButtons, type);
            modeButton.setDisplay(false);
        });

        mekanismTypeSelector.setOnValueChanged(value -> {
            var type = mekanismTypeByDisplay.get(value);
            if (type == null) {
                return;
            }
            selectedTypesByNavigation.put(type.navigationType(), type);
            session.setActiveType(type);
            title.setText(Component.translatable(type.translationKey()));
            showWorkstationPanel(panels, type);
            updateWorkstationButtons(workstationButtons, type);
            modeButton.setDisplay(false);
        });

        // JEI 转移回调同时更新会话和已经挂载的槽位控件。
        var jeiBridge = new RecipeEditorJeiBridge(data -> {
            var type = data.type();
            var inputs = data.inputs();
            var output = data.output();
            var sourceInfo = data.sourceInfo();
            var shaped = data.shaped();
            var craftingWidth = data.craftingWidth();
            var craftingHeight = data.craftingHeight();
            var experience = data.experience();
            var cookingTime = data.cookingTime();
            session.setActiveType(type);
            session.setSourceInfo(sourceInfo);
            session.setShaped(shaped);
            session.setSettings(type, data.settings());
            if (type.isCooking()) {
                session.setExperience(type, experience);
                session.setCookingTime(type, cookingTime);
                var experienceElement = root.getModularUI() == null
                        ? null
                        : root.getModularUI().getElementById(cookingExperienceId(type));
                if (experienceElement instanceof TextField field) {
                    field.setText(Float.toString(experience));
                }
                var cookingTimeElement = root.getModularUI() == null
                        ? null
                        : root.getModularUI().getElementById(cookingTimeId(type));
                if (cookingTimeElement instanceof TextField field) {
                    field.setText(Integer.toString(cookingTime));
                }
            }
            if (type.isVanilla()) {
                selectedTypesByNavigation.put(type.navigationType(), type);
                vanillaTypeSelector.setSelected(vanillaTypeDisplay.get(type));
            }
            vanillaTypeSelectorRow.setDisplay(type.isVanilla());
            if (type.isExtendedCrafting()) {
                selectedTypesByNavigation.put(type.navigationType(), type);
                extendedTypeSelector.setSelected(extendedTypeDisplay.get(type));
            }
            extendedTypeSelectorRow.setDisplay(type.isExtendedCrafting());
            if (type.isAe2()) {
                var ae2Settings = data.settings().require(Ae2RecipeSettings.class);
                selectedTypesByNavigation.put(type.navigationType(), type);
                ae2TypeSelector.setSelected(ae2TypeDisplay.get(type));
                updateAe2Fields(root, type, ae2Settings);
            }
            ae2TypeSelectorRow.setDisplay(type.isAe2());
            if (type.isMekanism()) {
                var mekanismSettings = data.settings().require(MekanismRecipeSettings.class);
                selectedTypesByNavigation.put(type.navigationType(), type);
                mekanismTypeSelector.setSelected(mekanismTypeDisplay.get(type));
                updateMekanismFields(root, type, mekanismSettings);
            }
            mekanismTypeSelectorRow.setDisplay(type.isMekanism());
            if (type.isExtendedCrafting()) {
                var extendedSettings = data.settings().require(ExtendedRecipeSettings.class);
                updateNumericField(root, extendedFieldId(type, "power_cost"), extendedSettings.powerCost());
                updateNumericField(root, extendedFieldId(type, "power_rate"), extendedSettings.powerRate());
                updateNumericField(root, extendedFieldId(type, "ingredient_count"), extendedSettings.ingredientCount());
                updateNumericField(root, extendedFieldId(type, "crafting_time"), extendedSettings.craftingTime());
            }
            for (var index = 0; index < type.inputCount(); index++) {
                session.setInputStack(type, index, ItemStack.EMPTY);
            }

            // 小尺寸有序配方需要按原始宽高映射回当前工作站的固定编辑网格。
            if (type.supportsShaped() && shaped && craftingWidth > 0 && craftingHeight > 0
                    && inputs.size() != type.inputCount()) {
                for (var index = 0; index < inputs.size() && index < craftingWidth * craftingHeight; index++) {
                    var row = index / Math.max(1, craftingWidth);
                    var column = index % Math.max(1, craftingWidth);
                    var target = row * type.gridWidth() + column;
                    if (target < type.inputCount()) {
                        session.setInputStack(type, target, inputs.get(index));
                    }
                }
            } else {
                for (var index = 0; index < inputs.size() && index < type.inputCount(); index++) {
                    session.setInputStack(type, index, inputs.get(index));
                }
            }
            session.setOutputStack(type, output);
            for (var index = 0; index < type.inputCount(); index++) {
                var element = root.getModularUI() == null
                        ? null
                        : root.getModularUI().getElementById(inputSlotId(type, index));
                if (element instanceof ItemSlot slot) {
                    slot.setItem(session.getInputStack(type, index));
                }
            }
            var outputElement = root.getModularUI() == null
                    ? null
                    : root.getModularUI().getElementById(outputSlotId(type));
            if (outputElement instanceof ItemSlot outputSlot) {
                outputSlot.setItem(output);
            }
            var outputCountElement = root.getModularUI() == null
                    ? null
                    : root.getModularUI().getElementById(outputCountId(type));
            if (outputCountElement instanceof TextField field && !output.isEmpty()) {
                field.setText(Integer.toString(output.getCount()));
            }
            recipeIdField.setText(sourceInfo.id());
            sourceIdValue.setText(Component.literal(sourceDisplayValue(sourceInfo.id())));
            sourceTypeValue.setText(Component.literal(sourceDisplayValue(sourceInfo.type())));
            sourceModValue.setText(Component.literal(sourceDisplayValue(sourceInfo.mod())));
            sourceGroupValue.setText(Component.literal(sourceDisplayValue(sourceInfo.group())));
            main.setDisplay(true);
            title.setText(Component.translatable(type.translationKey()));
            showWorkstationPanel(panels, type);
            updateWorkstationButtons(workstationButtons, type);
            modeButton.setDisplay(type.supportsShaped());
            modeButton.setText(Component.translatable(shaped
                    ? "kubejs_recipe_ui.action.shaped"
                    : "kubejs_recipe_ui.action.shapeless"));
            Dialog.showNotification(translate("kubejs_recipe_ui.jei.transfer.success"), 2.5F);
            LOGGER.info("[工作台编辑器] JEI 配方已填充: 类型={}, ID={}", type, sourceInfo.id());
        });
        root.addChild(jeiBridge);

        contentBody.addChildren(sidebar, main);
        root.addChildren(header, contentBody);

        // 使用原版 JEI 风格的浅灰面板、斜边框和高对比控件，同时统一使用原版字体显示中文。
        root.addLocalStylesheet("""
                text, label,
                text-field:host,
                text-area,
                .__tag-field_text-field__,
                .__search-component_text-field__ {
                  font: minecraft:default;
                  text-color: #3f3f3f;
                  text-shadow: false;
                }

                .kru_root {
                  background: built-in(ui-mc:RECT);
                  padding-all: 6;
                  gap-all: 4;
                }

                .kru_header {
                  background: sdf(#ffaaaaaa, 0, 1, #ff555555);
                  padding-vertical: 3;
                  padding-horizontal: 8;
                }

                .kru_header_title {
                  text-color: #ffffff;
                  text-shadow: true;
                }

                .kru_sidebar {
                  background: built-in(ui-mc:RECT_THIN);
                  padding-all: 5;
                  gap-all: 4;
                }

                .kru_main {
                  background: built-in(ui-mc:RECT_THIN);
                  padding-all: 3;
                }

                .kru_recipe_canvas {
                  background: built-in(ui-mc:RECT);
                  padding-all: 4;
                  gap-all: 5;
                }

                .kru_recipe_frame {
                  background: built-in(ui-mc:RECT);
                  padding-all: 5;
                }

                .kru_inventory_frame {
                  background: built-in(ui-mc:RECT_THIN);
                  padding-horizontal: 4;
                  padding-vertical: 3;
                  gap-all: 2;
                }

                .kru_inspector .__scroller_view_view-port__ {
                  background: built-in(ui-mc:RECT);
                  padding-all: 2;
                }

                .kru_inspector_content {
                  padding-all: 6;
                  gap-all: 5;
                }

                .kru_section_label {
                  background: sdf(#ffd0d0d0, 0, 1, #ff8b8b8b);
                  padding-horizontal: 4;
                  text-color: #3f3f3f;
                }

                .kru_sidebar_button {
                  padding-all: 2;
                }

                .kru_sidebar_icon {
                  width: 16;
                  height: 16;
                }

                .kru_inspector_tabs .__tab-view_tab_header_container__ {
                  background: built-in(ui-mc:RECT_THIN);
                  padding-horizontal: 3;
                }

                .kru_inspector_tabs .__tab-view_tab_content_container__,
                .kru_source_content,
                .kru_code_content {
                  background: built-in(ui-mc:RECT);
                }

                .kru_source_row {
                  background: built-in(ui-mc:RECT_THIN);
                  padding-horizontal: 4;
                }

                .kru_jei_canvas {
                  background: built-in(ui-mc:RECT);
                }

                button:host {
                  padding-all: 3;
                }

                text-field:host,
                selector:host,
                .__tag-field_text-field__,
                .__search-component_text-field__ {
                  background: sdf(#fff0f0f0, 1, 1, #ff6b6b6b);
                  focus-overlay: sdf(#00000000, 1, 1, #ffffffff);
                }

                .__selector_dialog__,
                .__search-component_dialog__ {
                  background: built-in(ui-mc:RECT);
                }

                .__text-area_content-view__ {
                  background: sdf(#fff3f3f3, 1, 1, #ff777777);
                }

                .kru_slot_count {
                  font-size: 8;
                  text-color: #202020;
                }

                item-slot, .item-slot_bg {
                  background: built-in(ui-mc:RECT_1);
                }
                """);

        // 外层视口使用实际屏幕尺寸，内部编辑器保持设计尺寸并在小窗口中等比缩放。
        var viewport = new UIElement();
        viewport.layout(style -> style.width(EDITOR_WIDTH).height(EDITOR_HEIGHT));
        viewport.lss("flex-direction", "row");
        viewport.lss("align-items", "center");
        viewport.lss("justify-content", "center");
        viewport.addChild(root);

        var ui = UI.of(
                viewport,
                java.util.List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC)),
                screenSize -> {
                    var viewportWidth = Math.min(EDITOR_WIDTH, Math.max(1, screenSize.getWidth() - SCREEN_MARGIN * 2));
                    var viewportHeight = Math.min(EDITOR_HEIGHT, Math.max(1, screenSize.getHeight() - SCREEN_MARGIN * 2));
                    var scale = Math.min(
                            viewportWidth / (float) EDITOR_WIDTH,
                            viewportHeight / (float) EDITOR_HEIGHT
                    );
                    root.transform(transform -> transform.pivot(0.5F, 0.5F).scale(scale));
                    return Size.of(viewportWidth, viewportHeight);
                }
        );
        return new ModularUI(ui, holder.player);
    }

    /** 只显示当前工作站对应的编辑面板。 */
    private static void showWorkstationPanel(
            EnumMap<WorkstationType, UIElement> panels,
            WorkstationType activeType
    ) {
        panels.forEach((type, panel) -> panel.setDisplay(type == activeType));
    }

    /** 可选模组的同族工作站共用一个侧栏按钮，因此选中状态按工作站族判断。 */
    private static void updateWorkstationButtons(
            EnumMap<WorkstationType, Button> buttons,
            WorkstationType activeType
    ) {
        buttons.forEach((type, button) -> {
            var selected = type == activeType.navigationType();
            button.setActive(!selected);
        });
    }

    /** 创建 3x3 工作台输入网格和输出槽。 */
    static UIElement createCraftingPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session
    ) {
        var panel = workstationRow(14);
        var grid = createInputGrid(root, holder, session, WorkstationType.CRAFTING, 0, 9, 3, null);
        panel.addChildren(grid, recipeArrow(), createOutputSlot(holder, session, WorkstationType.CRAFTING));
        return panel;
    }

    /** 创建 Extended Crafting 的 3x3、5x5、7x7 或 9x9 工作台网格。 */
    static UIElement createExtendedTablePanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session,
            WorkstationType type
    ) {
        var panel = workstationRow(10);
        panel.layout(style -> style.widthPercent(100).height(180).gapAll(10));
        panel.addChildren(
                createInputGrid(root, holder, session, type, 0, type.inputCount(), type.gridWidth(), null),
                recipeArrow(),
                createOutputSlot(holder, session, type)
        );
        return panel;
    }

    /** 创建组合合成的中心材料、48 个基座材料以及能量参数。 */
    static UIElement createCombinationPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session
    ) {
        var type = WorkstationType.EXTENDED_COMBINATION;
        var panel = new UIElement();
        panel.layout(style -> style.widthPercent(100).height(180).gapAll(4));
        panel.lss("flex-direction", "column");

        var recipe = workstationRow(8);
        recipe.layout(style -> style.widthPercent(100).height(142).gapAll(8));
        recipe.addChildren(
                createInputGrid(root, holder, session, type, 1, 48, 8, "kubejs_recipe_ui.slot.pedestal"),
                createInputSlot(root, holder, session, type, 0, "kubejs_recipe_ui.slot.center"),
                recipeArrow(),
                createOutputSlot(holder, session, type)
        );
        panel.addChildren(recipe, createPowerFields(session, type, "kubejs_recipe_ui.extended.power_cost"));
        return panel;
    }

    /** 创建压缩机材料、催化剂、数量和能量参数。 */
    static UIElement createCompressorPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session
    ) {
        var type = WorkstationType.EXTENDED_COMPRESSOR;
        var panel = new UIElement();
        panel.layout(style -> style.widthPercent(100).height(150).gapAll(5));
        panel.lss("flex-direction", "column");

        var recipe = workstationRow(10);
        recipe.addChildren(
                createInputSlot(root, holder, session, type, 0, "kubejs_recipe_ui.slot.compressor_ingredient"),
                symbol("+"),
                createInputSlot(root, holder, session, type, 1, "kubejs_recipe_ui.slot.catalyst"),
                recipeArrow(),
                createOutputSlot(holder, session, type)
        );

        var ingredientCount = numericField(
                Integer.toString(session.getIngredientCount(type)),
                1,
                Integer.MAX_VALUE,
                64
        );
        ingredientCount.setId(extendedFieldId(type, "ingredient_count"));
        ingredientCount.registerValueListener(value -> session.setIngredientCount(type, parseInt(value, 1)));
        var countRow = labeledField("kubejs_recipe_ui.extended.ingredient_count", ingredientCount);
        countRow.layout(style -> style.width(125).height(20).marginHorizontalAuto());
        panel.addChildren(
                recipe,
                countRow,
                createPowerFields(session, type, "kubejs_recipe_ui.extended.power_cost")
        );
        return panel;
    }

    /** 创建末影合成器的 3x3 网格和合成时间。 */
    static UIElement createEnderCrafterPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session
    ) {
        var type = WorkstationType.EXTENDED_ENDER_CRAFTER;
        var panel = new UIElement();
        panel.layout(style -> style.widthPercent(100).height(115).gapAll(5));
        panel.lss("flex-direction", "column");

        var recipe = workstationRow(14);
        recipe.addChildren(
                createInputGrid(root, holder, session, type, 0, 9, 3, null),
                recipeArrow(),
                createOutputSlot(holder, session, type)
        );

        var craftingTime = numericField(
                Integer.toString(session.getExtendedCraftingTime(type)),
                1,
                Integer.MAX_VALUE,
                64
        );
        craftingTime.setId(extendedFieldId(type, "crafting_time"));
        craftingTime.registerValueListener(value -> session.setExtendedCraftingTime(type, parseInt(value, 60)));
        var timeRow = labeledField("kubejs_recipe_ui.extended.crafting_time", craftingTime);
        timeRow.layout(style -> style.width(130).height(20).marginHorizontalAuto());
        panel.addChildren(recipe, timeRow);
        return panel;
    }

    /** 创建通量合成器的 3x3 网格和能量参数。 */
    static UIElement createFluxCrafterPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session
    ) {
        var type = WorkstationType.EXTENDED_FLUX_CRAFTER;
        var panel = new UIElement();
        panel.layout(style -> style.widthPercent(100).height(115).gapAll(5));
        panel.lss("flex-direction", "column");

        var recipe = workstationRow(14);
        recipe.addChildren(
                createInputGrid(root, holder, session, type, 0, 9, 3, null),
                recipeArrow(),
                createOutputSlot(holder, session, type)
        );
        panel.addChildren(recipe, createPowerFields(session, type, "kubejs_recipe_ui.extended.power_required"));
        return panel;
    }

    /** 完整复刻 AE2 充能器 JEI 分类的 130x50 布局。 */
    static UIElement createAe2ChargerPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session
    ) {
        var type = WorkstationType.AE2_CHARGER;
        var panel = new UIElement();
        panel.layout(style -> style.widthPercent(100).height(86).paddingTop(16));
        panel.lss("flex-direction", "column");
        panel.lss("align-items", "center");

        var jei = ae2JeiCanvas(130, 50);
        var input = createInputSlot(root, holder, session, type, 0, "kubejs_recipe_ui.slot.ingredient");
        input.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(31).top(8));
        var arrow = ae2JeiSprite(0, 17, 24, 17);
        arrow.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(52).top(8));
        var output = createOutputSlot(holder, session, type);
        output.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(81).top(8));
        var crank = ae2JeiItemSlot(null, optionalItemStack("ae2:crank"));
        crank.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(3).top(30));
        crank.style(style -> style.tooltips(Component.translatable("block.ae2.crank")));
        var power = new Label();
        power.setText("10 AE/t");
        power.layout(style -> style
                .positionType(TaffyPosition.ABSOLUTE)
                .left(20)
                .top(35)
                .width(48)
                .height(10));
        jei.addChildren(input, arrow, output, crank, power);
        panel.addChild(jei);
        return panel;
    }

    /** 复刻 AE2 压印器 JEI 分类，并将编辑器专用模式控件放在分类下方。 */
    static UIElement createAe2InscriberPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session
    ) {
        var type = WorkstationType.AE2_INSCRIBER;
        var panel = new UIElement();
        panel.layout(style -> style.widthPercent(100).height(104).paddingTop(5).gapAll(5));
        panel.lss("flex-direction", "column");
        panel.lss("align-items", "center");

        var jei = ae2JeiCanvas(97, 64);
        var machineBackground = ae2JeiSprite(24, 0, 48, 64);
        machineBackground.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(16).top(0));
        var top = createInputSlot(root, holder, session, type, 0, "kubejs_recipe_ui.ae2.slot.top");
        top.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(1).top(1));
        var middle = createInputSlot(root, holder, session, type, 1, "kubejs_recipe_ui.ae2.slot.middle");
        middle.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(22).top(24));
        var bottom = createInputSlot(root, holder, session, type, 2, "kubejs_recipe_ui.ae2.slot.bottom");
        bottom.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(1).top(47));
        var output = createOutputSlot(holder, session, type);
        output.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(69).top(24));
        var progress = ae2JeiSprite(72, 0, 6, 18);
        progress.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(91).top(23));
        jei.addChildren(machineBackground, top, middle, bottom, output, progress);

        var settings = session.getAe2Settings(type);
        var mode = translatedSelector(
                Ae2RecipeSettings.InscriberMode.values(),
                settings.inscriberMode(),
                Ae2RecipeSettings.InscriberMode::translationKey,
                value -> session.setAe2Settings(type, session.getAe2Settings(type).withInscriberMode(value))
        );
        mode.setId(ae2FieldId(type, "inscriber_mode"));
        var modeRow = labeledField("kubejs_recipe_ui.ae2.inscriber_mode", mode);
        modeRow.layout(style -> style.width(170).height(20).marginHorizontalAuto());
        panel.addChildren(jei, modeRow);
        return panel;
    }

    /** 复刻 AE2 物品转化 JEI 分类，并在分类下方提供条件参数。 */
    static UIElement createAe2TransformPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session
    ) {
        var type = WorkstationType.AE2_TRANSFORM;
        var settings = session.getAe2Settings(type);
        var panel = new UIElement();
        panel.layout(style -> style.widthPercent(100).height(136).paddingTop(2).gapAll(5));
        panel.lss("flex-direction", "column");
        panel.lss("align-items", "center");

        var jei = ae2JeiCanvas(130, 62);
        for (var row = 0; row < 3; row++) {
            var index = row;
            var slot = createInputSlot(
                    root,
                    holder,
                    session,
                    type,
                    index,
                    "kubejs_recipe_ui.slot.ingredient"
            );
            var top = 6 + row * 18;
            slot.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(6).top(top));
            jei.addChild(slot);
        }
        var firstArrow = ae2JeiSprite(0, 17, 24, 17);
        firstArrow.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(25).top(23));
        var conditionPreview = ae2JeiItemSlot(
                ae2FieldId(type, "transform_condition_preview"),
                ae2TransformPreviewStacks(settings)
        );
        conditionPreview.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(56).top(24));
        var secondArrow = ae2JeiSprite(0, 17, 24, 17);
        secondArrow.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(76).top(23));
        var output = createOutputSlot(holder, session, type);
        output.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(106).top(24));
        var conditionLabel = new Label();
        conditionLabel.setId(ae2FieldId(type, "transform_condition_label"));
        conditionLabel.setText(Component.translatable(transformPreviewTranslationKey(settings.transformCondition())));
        conditionLabel.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
        conditionLabel.layout(style -> style
                .positionType(TaffyPosition.ABSOLUTE)
                .left(35)
                .top(9)
                .width(60)
                .height(10));
        jei.addChildren(firstArrow, conditionPreview, secondArrow, output, conditionLabel);

        var fluidTag = resourceIdField(settings.transformFluidTag(), "kubejs_recipe_ui.ae2.fluid_tag.placeholder");
        fluidTag.setId(ae2FieldId(type, "transform_fluid_tag"));
        fluidTag.layout(style -> style.width(115).height(20));
        fluidTag.style(style -> style.tooltips(Component.translatable("kubejs_recipe_ui.ae2.fluid_tag")));
        fluidTag.registerValueListener(value -> {
            var updated = session.getAe2Settings(type).withTransformFluidTag(value);
            session.setAe2Settings(type, updated);
            if (updated.transformCondition() == Ae2RecipeSettings.TransformCondition.FLUID) {
                setAe2ItemPreview(conditionPreview, ae2TransformPreviewStacks(updated));
            }
        });
        var fluidTagRow = labeledField("kubejs_recipe_ui.ae2.fluid_tag", fluidTag);
        fluidTagRow.setId(ae2FieldId(type, "transform_fluid_row"));
        fluidTagRow.layout(style -> style.width(170).height(20));
        fluidTagRow.setDisplay(settings.transformCondition() == Ae2RecipeSettings.TransformCondition.FLUID);

        var condition = translatedSelector(
                Ae2RecipeSettings.TransformCondition.values(),
                settings.transformCondition(),
                Ae2RecipeSettings.TransformCondition::translationKey,
                value -> {
                    var updated = session.getAe2Settings(type).withTransformCondition(value);
                    session.setAe2Settings(type, updated);
                    fluidTagRow.setDisplay(value == Ae2RecipeSettings.TransformCondition.FLUID);
                    setAe2ItemPreview(conditionPreview, ae2TransformPreviewStacks(updated));
                    conditionLabel.setText(Component.translatable(transformPreviewTranslationKey(value)));
                }
        );
        condition.setId(ae2FieldId(type, "transform_condition"));
        condition.layout(style -> style.width(115).height(20));
        var conditionRow = labeledField("kubejs_recipe_ui.ae2.transform_condition", condition);
        conditionRow.layout(style -> style.width(170).height(20));
        panel.addChildren(jei, conditionRow, fluidTagRow);
        return panel;
    }

    /** 复刻 AE2 熵变处理 JEI 分类，并在下方编辑模式与资源。 */
    static UIElement createAe2EntropyPanel(RecipeEditorSession session) {
        var type = WorkstationType.AE2_ENTROPY;
        var settings = session.getAe2Settings(type);
        var panel = new UIElement();
        panel.layout(style -> style.widthPercent(100).height(150).paddingTop(2).gapAll(5));
        panel.lss("flex-direction", "column");
        panel.lss("align-items", "center");

        var jei = ae2JeiCanvas(130, 50);
        var inputPreview = ae2JeiItemSlot(
                ae2FieldId(type, "entropy_input_preview"),
                ae2EntropyInputStack(settings)
        );
        inputPreview.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(29).top(15));
        var arrow = ae2JeiSprite(0, 17, 24, 17);
        arrow.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(53).top(14));
        var outputPreview = ae2JeiItemSlot(
                ae2FieldId(type, "entropy_output_preview"),
                ae2EntropyOutputStack(settings)
        );
        outputPreview.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(85).top(15));
        var modeIcon = ae2EntropyModeIcon(settings.entropyMode());
        modeIcon.setId(ae2FieldId(type, "entropy_mode_icon"));
        modeIcon.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(36).top(3));
        var modeLabel = new Label();
        modeLabel.setId(ae2FieldId(type, "entropy_mode_label"));
        modeLabel.setText(Component.translatable(entropyPreviewTranslationKey(settings.entropyMode())));
        modeLabel.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
        modeLabel.layout(style -> style
                .positionType(TaffyPosition.ABSOLUTE)
                .left(43)
                .top(2)
                .width(84)
                .height(10));
        var actionLabel = new Label();
        actionLabel.setId(ae2FieldId(type, "entropy_action_label"));
        actionLabel.setText(Component.translatable(entropyActionTranslationKey(settings.entropyMode())));
        actionLabel.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
        actionLabel.layout(style -> style
                .positionType(TaffyPosition.ABSOLUTE)
                .left(15)
                .top(38)
                .width(100)
                .height(10));
        jei.addChildren(inputPreview, arrow, outputPreview, modeIcon, modeLabel, actionLabel);

        var mode = translatedSelector(
                Ae2RecipeSettings.EntropyMode.values(),
                settings.entropyMode(),
                Ae2RecipeSettings.EntropyMode::translationKey,
                value -> {
                    session.setAe2Settings(type, session.getAe2Settings(type).withEntropyMode(value));
                    setEntropyModePreview(modeIcon, modeLabel, actionLabel, value);
                }
        );
        mode.setId(ae2FieldId(type, "entropy_mode"));
        mode.layout(style -> style.width(115).height(20));
        var modeRow = labeledField("kubejs_recipe_ui.ae2.entropy_mode", mode);
        modeRow.layout(style -> style.width(170).height(20));

        var inputType = translatedSelector(
                Ae2RecipeSettings.EntropyResourceType.values(),
                settings.entropyInputType(),
                Ae2RecipeSettings.EntropyResourceType::translationKey,
                value -> {
                    var updated = session.getAe2Settings(type).withEntropyInputType(value);
                    session.setAe2Settings(type, updated);
                    setAe2ItemPreview(inputPreview, ae2EntropyInputStack(updated));
                }
        );
        inputType.setId(ae2FieldId(type, "entropy_input_type"));
        inputType.layout(style -> style.width(54).height(20));
        var inputId = resourceIdField(settings.entropyInputId(), "kubejs_recipe_ui.ae2.resource_id.placeholder");
        inputId.setId(ae2FieldId(type, "entropy_input_id"));
        inputId.layout(style -> style.flex(1).height(20));
        inputId.registerValueListener(value -> {
            var updated = session.getAe2Settings(type).withEntropyInputId(value);
            session.setAe2Settings(type, updated);
            setAe2ItemPreview(inputPreview, ae2EntropyInputStack(updated));
        });
        var inputEditor = ae2ResourceEditor("kubejs_recipe_ui.ae2.entropy_input", inputType, inputId);

        var outputType = translatedSelector(
                Ae2RecipeSettings.EntropyOutputType.values(),
                settings.entropyOutputType(),
                Ae2RecipeSettings.EntropyOutputType::translationKey,
                value -> {
                    var updated = session.getAe2Settings(type).withEntropyOutputType(value);
                    session.setAe2Settings(type, updated);
                    setAe2ItemPreview(outputPreview, ae2EntropyOutputStack(updated));
                }
        );
        outputType.setId(ae2FieldId(type, "entropy_output_type"));
        outputType.layout(style -> style.width(54).height(20));
        var outputId = resourceIdField(settings.entropyOutputId(), "kubejs_recipe_ui.ae2.resource_id.placeholder");
        outputId.setId(ae2FieldId(type, "entropy_output_id"));
        outputId.layout(style -> style.flex(1).height(20));
        outputId.registerValueListener(value -> {
            var updated = session.getAe2Settings(type).withEntropyOutputId(value);
            session.setAe2Settings(type, updated);
            setAe2ItemPreview(outputPreview, ae2EntropyOutputStack(updated));
        });
        var outputEditor = ae2ResourceEditor("kubejs_recipe_ui.ae2.entropy_output", outputType, outputId);

        var resources = new UIElement();
        resources.layout(style -> style.width(304).height(34).gapAll(4));
        resources.lss("flex-direction", "row");
        resources.addChildren(inputEditor, outputEditor);
        panel.addChildren(jei, modeRow, resources);
        return panel;
    }

    /** 创建熵变分类下方的紧凑资源类型与 ID 编辑器。 */
    private static UIElement ae2ResourceEditor(String translationKey, Selector<String> type, TextField id) {
        var editor = new UIElement();
        editor.layout(style -> style.width(150).height(34).gapAll(3));
        editor.lss("flex-direction", "column");
        var label = fieldLabel(translationKey);
        label.layout(style -> style.widthPercent(100).height(10));
        var fields = new UIElement();
        fields.layout(style -> style.widthPercent(100).height(20).gapAll(3));
        fields.lss("flex-direction", "row");
        fields.addChildren(type, id);
        editor.addChildren(label, fields);
        return editor;
    }

    /** 创建与 AE2 JEI 分类尺寸完全一致的透明画布。 */
    private static UIElement ae2JeiCanvas(float width, float height) {
        var canvas = new UIElement();
        canvas.layout(style -> style.width(width).height(height));
        canvas.addClass("kru_jei_canvas");
        return canvas;
    }

    /** 从 AE2 自带 JEI 纹理中截取一个原尺寸图块。 */
    private static UIElement ae2JeiSprite(int x, int y, int width, int height) {
        var element = new UIElement();
        element.layout(style -> style.width(width).height(height));
        element.style(style -> style.background(SpriteTexture.of(AE2_JEI_TEXTURE).setSprite(x, y, width, height)));
        return element;
    }

    /** 创建 AE2 JEI 风格的纯展示槽，不参与物品编辑。 */
    private static UIElement ae2JeiItemSlot(String previewId, ItemStack... stacks) {
        var slot = ae2JeiSprite(0, 34, 18, 18);
        if (previewId != null) {
            slot.setId(previewId);
        }
        var item = new UIElement();
        item.layout(style -> style
                .positionType(TaffyPosition.ABSOLUTE)
                .left(1)
                .top(1)
                .width(16)
                .height(16));
        setAe2ItemPreview(item, stacks);
        slot.addChild(item);
        return slot;
    }

    /** 更新纯展示槽中的物品图标。 */
    private static void setAe2ItemPreview(UIElement preview, ItemStack... stacks) {
        var item = preview.getChildren().isEmpty() ? preview : preview.getChildren().getFirst();
        item.style(style -> style.background(new ItemStackTexture(stacks)));
    }

    /** @return 物品注册 ID 对应的物品堆，缺失或无物品形态时返回空堆 */
    private static ItemStack optionalItemStack(String id) {
        var identifier = ResourceLocation.tryParse(id);
        if (identifier == null) {
            return ItemStack.EMPTY;
        }
        var item = BuiltInRegistries.ITEM.get(identifier);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
    }

    /** @return 方块注册 ID 对应的展示物品 */
    private static ItemStack blockPreviewStack(String id) {
        var identifier = ResourceLocation.tryParse(id);
        if (identifier == null) {
            return ItemStack.EMPTY;
        }
        var block = BuiltInRegistries.BLOCK.get(identifier);
        return block == null || block.asItem() == Items.AIR ? ItemStack.EMPTY : block.asItem().getDefaultInstance();
    }

    /** @return 流体注册 ID 对应的桶物品 */
    private static ItemStack fluidPreviewStack(String id) {
        var identifier = ResourceLocation.tryParse(id);
        if (identifier == null) {
            return ItemStack.EMPTY;
        }
        var fluid = BuiltInRegistries.FLUID.get(identifier);
        return fluid == null || fluid.getBucket() == Items.AIR
                ? ItemStack.EMPTY
                : fluid.getBucket().getDefaultInstance();
    }

    /** @return 物品转化条件在 JEI 中使用的 TNT 或流体桶图标 */
    private static ItemStack[] ae2TransformPreviewStacks(Ae2RecipeSettings settings) {
        if (settings.transformCondition() == Ae2RecipeSettings.TransformCondition.FLUID) {
            return new ItemStack[]{fluidPreviewStack(settings.transformFluidTag())};
        }
        var tinyTnt = optionalItemStack("ae2:tiny_tnt");
        return tinyTnt.isEmpty()
                ? new ItemStack[]{Items.TNT.getDefaultInstance()}
                : new ItemStack[]{Items.TNT.getDefaultInstance(), tinyTnt};
    }

    /** @return 物品转化 JEI 顶部条件说明的翻译键 */
    private static String transformPreviewTranslationKey(Ae2RecipeSettings.TransformCondition condition) {
        return condition == Ae2RecipeSettings.TransformCondition.EXPLOSION
                ? "kubejs_recipe_ui.ae2.transform_preview.explosion"
                : "kubejs_recipe_ui.ae2.transform_preview.fluid";
    }

    /** @return 熵变输入在 JEI 展示槽中使用的图标 */
    private static ItemStack ae2EntropyInputStack(Ae2RecipeSettings settings) {
        return settings.entropyInputType() == Ae2RecipeSettings.EntropyResourceType.BLOCK
                ? blockPreviewStack(settings.entropyInputId())
                : fluidPreviewStack(settings.entropyInputId());
    }

    /** @return 熵变输出在 JEI 展示槽中使用的图标 */
    private static ItemStack ae2EntropyOutputStack(Ae2RecipeSettings settings) {
        return switch (settings.entropyOutputType()) {
            case BLOCK -> blockPreviewStack(settings.entropyOutputId());
            case FLUID -> fluidPreviewStack(settings.entropyOutputId());
            case DROPS -> optionalItemStack(settings.entropyOutputId());
        };
    }

    /** 创建 AE2 JEI 纹理中的加热或冷却小图标。 */
    private static UIElement ae2EntropyModeIcon(Ae2RecipeSettings.EntropyMode mode) {
        return ae2JeiSprite(mode == Ae2RecipeSettings.EntropyMode.HEAT ? 0 : 6, 68, 6, 6);
    }

    /** 同步熵变 JEI 预览中的模式图标、能耗说明和操作说明。 */
    private static void setEntropyModePreview(
            UIElement icon,
            Label modeLabel,
            Label actionLabel,
            Ae2RecipeSettings.EntropyMode mode
    ) {
        icon.style(style -> style.background(SpriteTexture.of(AE2_JEI_TEXTURE)
                .setSprite(mode == Ae2RecipeSettings.EntropyMode.HEAT ? 0 : 6, 68, 6, 6)));
        modeLabel.setText(Component.translatable(entropyPreviewTranslationKey(mode)));
        actionLabel.setText(Component.translatable(entropyActionTranslationKey(mode)));
    }

    /** @return 熵变 JEI 能耗说明的翻译键 */
    private static String entropyPreviewTranslationKey(Ae2RecipeSettings.EntropyMode mode) {
        return mode == Ae2RecipeSettings.EntropyMode.HEAT
                ? "kubejs_recipe_ui.ae2.entropy_preview.heat"
                : "kubejs_recipe_ui.ae2.entropy_preview.cool";
    }

    /** @return 熵变 JEI 操作说明的翻译键 */
    private static String entropyActionTranslationKey(Ae2RecipeSettings.EntropyMode mode) {
        return mode == Ae2RecipeSettings.EntropyMode.HEAT
                ? "kubejs_recipe_ui.ae2.entropy_action.heat"
                : "kubejs_recipe_ui.ae2.entropy_action.cool";
    }

    /** 创建显示已翻译枚举值的 LDLib2 下拉框。 */
    private static <T> Selector<String> translatedSelector(
            T[] values,
            T selected,
            java.util.function.Function<T, String> translationKey,
            java.util.function.Consumer<T> listener
    ) {
        var byDisplay = new HashMap<String, T>();
        var candidates = Arrays.stream(values).map(value -> {
            var display = translate(translationKey.apply(value));
            byDisplay.put(display, value);
            return display;
        }).toList();
        var selector = new Selector<String>();
        selector.dialog.style(style -> style.zIndex(200));
        selector.setCandidates(candidates);
        selector.setCandidateUIProvider(RecipeEditorUi::slowRollingLabel);
        selector.layout(style -> style.width(120).height(20));
        selector.setSelected(translate(translationKey.apply(selected)));
        selector.setOnValueChanged(value -> {
            var mapped = byDisplay.get(value);
            if (mapped != null) {
                listener.accept(mapped);
            }
        });
        return selector;
    }

    /** 创建允许输入 namespace:path 的资源 ID 文本框。 */
    private static TextField resourceIdField(String value, String placeholderKey) {
        var field = new TextField();
        field.setAnyString();
        field.setText(value);
        field.textFieldStyle(style -> style.placeholder(Component.translatable(placeholderKey)));
        field.layout(style -> style.width(150).height(20));
        return field;
    }

    /** 创建可复用的紧凑物品槽网格。 */
    private static UIElement createInputGrid(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session,
            WorkstationType type,
            int startIndex,
            int count,
            int columns,
            String tooltipKey
    ) {
        var rows = (count + columns - 1) / columns;
        var grid = new UIElement();
        grid.layout(style -> style.width(columns * 20 - 2).height(rows * 20 - 2).gapAll(2));
        grid.lss("flex-direction", "row");
        grid.lss("flex-wrap", "wrap");
        for (var index = 0; index < count; index++) {
            grid.addChild(createInputSlot(root, holder, session, type, startIndex + index, tooltipKey));
        }
        return grid;
    }

    /**
     * 按 Mekanism 配方的数据形态复用一组紧凑 LDLib2 面板。
     * 物品继续使用可拖放槽位，流体与化学品均使用本地化下拉框。
     */
    static UIElement createMekanismPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session,
            WorkstationType type
    ) {
        var panel = new UIElement();
        panel.layout(style -> style.widthPercent(100).height(180).paddingVertical(4).gapAll(4));
        panel.lss("flex-direction", "column");
        panel.lss("align-items", "center");

        switch (type) {
            case MEKANISM_CRUSHING, MEKANISM_ENRICHING, MEKANISM_SMELTING -> {
                panel.addChild(mekanismItemFlow(root, holder, session, type, true));
                panel.addChild(mekanismParameterPair(
                        mekanismIntegerRow(session, type, "item_input_count", "kubejs_recipe_ui.mekanism.item_input_count",
                                session.getMekanismSettings(type).itemInputCount(), 1,
                                (settings, value) -> settings.withItemInputCount(value))
                ));
            }
            case MEKANISM_COMBINING -> {
                panel.addChild(mekanismItemFlow(root, holder, session, type, true));
                panel.addChild(mekanismParameterPair(
                        mekanismIntegerRow(session, type, "item_input_count", "kubejs_recipe_ui.mekanism.main_input_count",
                                session.getMekanismSettings(type).itemInputCount(), 1,
                                (settings, value) -> settings.withItemInputCount(value)),
                        mekanismIntegerRow(session, type, "extra_item_input_count", "kubejs_recipe_ui.mekanism.extra_input_count",
                                session.getMekanismSettings(type).extraItemInputCount(), 1,
                                (settings, value) -> settings.withExtraItemInputCount(value))
                ));
            }
            case MEKANISM_CHEMICAL_INFUSING, MEKANISM_PIGMENT_MIXING -> {
                panel.addChild(mekanismParameterPair(
                        mekanismResourceRow(session, type, ResourceField.PRIMARY_CHEMICAL_INPUT,
                                "kubejs_recipe_ui.mekanism.left_chemical_input"),
                        mekanismResourceRow(session, type, ResourceField.SECONDARY_CHEMICAL_INPUT,
                                "kubejs_recipe_ui.mekanism.right_chemical_input")
                ));
                panel.addChild(mekanismParameterPair(
                        mekanismResourceRow(session, type, ResourceField.PRIMARY_CHEMICAL_OUTPUT,
                                "kubejs_recipe_ui.mekanism.chemical_output")
                ));
            }
            case MEKANISM_SEPARATING -> {
                panel.addChild(mekanismParameterPair(
                        mekanismResourceRow(session, type, ResourceField.FLUID_INPUT,
                                "kubejs_recipe_ui.mekanism.fluid_input"),
                        mekanismIntegerRow(session, type, "energy_multiplier", "kubejs_recipe_ui.mekanism.energy_multiplier",
                                session.getMekanismSettings(type).energyMultiplier(), 1,
                                (settings, value) -> settings.withEnergyMultiplier(value))
                ));
                panel.addChild(mekanismParameterPair(
                        mekanismResourceRow(session, type, ResourceField.PRIMARY_CHEMICAL_OUTPUT,
                                "kubejs_recipe_ui.mekanism.left_chemical_output"),
                        mekanismResourceRow(session, type, ResourceField.SECONDARY_CHEMICAL_OUTPUT,
                                "kubejs_recipe_ui.mekanism.right_chemical_output")
                ));
            }
            case MEKANISM_WASHING -> {
                panel.addChild(mekanismParameterPair(
                        mekanismResourceRow(session, type, ResourceField.FLUID_INPUT,
                                "kubejs_recipe_ui.mekanism.fluid_input"),
                        mekanismResourceRow(session, type, ResourceField.PRIMARY_CHEMICAL_INPUT,
                                "kubejs_recipe_ui.mekanism.chemical_input")
                ));
                panel.addChild(mekanismParameterPair(
                        mekanismResourceRow(session, type, ResourceField.PRIMARY_CHEMICAL_OUTPUT,
                                "kubejs_recipe_ui.mekanism.chemical_output")
                ));
            }
            case MEKANISM_EVAPORATING -> panel.addChild(mekanismParameterPair(
                    mekanismResourceRow(session, type, ResourceField.FLUID_INPUT,
                            "kubejs_recipe_ui.mekanism.fluid_input"),
                    mekanismResourceRow(session, type, ResourceField.FLUID_OUTPUT,
                            "kubejs_recipe_ui.mekanism.fluid_output")
            ));
            case MEKANISM_ACTIVATING, MEKANISM_CENTRIFUGING -> panel.addChild(mekanismParameterPair(
                    mekanismResourceRow(session, type, ResourceField.PRIMARY_CHEMICAL_INPUT,
                            "kubejs_recipe_ui.mekanism.chemical_input"),
                    mekanismResourceRow(session, type, ResourceField.PRIMARY_CHEMICAL_OUTPUT,
                            "kubejs_recipe_ui.mekanism.chemical_output")
            ));
            case MEKANISM_CRYSTALLIZING -> {
                panel.addChild(mekanismItemFlow(root, holder, session, type, true));
                panel.addChild(mekanismParameterPair(
                        mekanismResourceRow(session, type, ResourceField.PRIMARY_CHEMICAL_INPUT,
                                "kubejs_recipe_ui.mekanism.chemical_input")
                ));
            }
            case MEKANISM_DISSOLUTION -> {
                panel.addChild(mekanismItemFlow(root, holder, session, type, false));
                panel.addChild(mekanismParameterPair(
                        mekanismResourceRow(session, type, ResourceField.PRIMARY_CHEMICAL_INPUT,
                                "kubejs_recipe_ui.mekanism.chemical_input"),
                        mekanismResourceRow(session, type, ResourceField.PRIMARY_CHEMICAL_OUTPUT,
                                "kubejs_recipe_ui.mekanism.chemical_output")
                ));
                panel.addChild(mekanismParameterPair(
                        mekanismIntegerRow(session, type, "item_input_count", "kubejs_recipe_ui.mekanism.item_input_count",
                                session.getMekanismSettings(type).itemInputCount(), 1,
                                (settings, value) -> settings.withItemInputCount(value)),
                        mekanismPerTickRow(session, type)
                ));
            }
            case MEKANISM_COMPRESSING, MEKANISM_PURIFYING, MEKANISM_INJECTING,
                    MEKANISM_METALLURGIC_INFUSING, MEKANISM_PAINTING -> {
                panel.addChild(mekanismItemFlow(root, holder, session, type, true));
                panel.addChild(mekanismParameterPair(
                        mekanismResourceRow(session, type, ResourceField.PRIMARY_CHEMICAL_INPUT,
                                "kubejs_recipe_ui.mekanism.chemical_input"),
                        mekanismIntegerRow(session, type, "item_input_count", "kubejs_recipe_ui.mekanism.item_input_count",
                                session.getMekanismSettings(type).itemInputCount(), 1,
                                (settings, value) -> settings.withItemInputCount(value))
                ));
                panel.addChild(mekanismParameterPair(mekanismPerTickRow(session, type)));
            }
            case MEKANISM_NUCLEOSYNTHESIZING -> {
                panel.addChild(mekanismItemFlow(root, holder, session, type, true));
                panel.addChild(mekanismParameterPair(
                        mekanismResourceRow(session, type, ResourceField.PRIMARY_CHEMICAL_INPUT,
                                "kubejs_recipe_ui.mekanism.chemical_input"),
                        mekanismIntegerRow(session, type, "duration", "kubejs_recipe_ui.mekanism.duration",
                                session.getMekanismSettings(type).duration(), 1,
                                (settings, value) -> settings.withDuration(value))
                ));
                panel.addChild(mekanismParameterPair(
                        mekanismIntegerRow(session, type, "item_input_count", "kubejs_recipe_ui.mekanism.item_input_count",
                                session.getMekanismSettings(type).itemInputCount(), 1,
                                (settings, value) -> settings.withItemInputCount(value)),
                        mekanismPerTickRow(session, type)
                ));
            }
            case MEKANISM_ENERGY_CONVERSION -> {
                panel.addChild(mekanismItemFlow(root, holder, session, type, false));
                panel.addChild(mekanismParameterPair(
                        mekanismIntegerRow(session, type, "item_input_count", "kubejs_recipe_ui.mekanism.item_input_count",
                                session.getMekanismSettings(type).itemInputCount(), 1,
                                (settings, value) -> settings.withItemInputCount(value)),
                        mekanismIntegerRow(session, type, "energy_output", "kubejs_recipe_ui.mekanism.energy_output",
                                session.getMekanismSettings(type).energyOutput(), 1,
                                (settings, value) -> settings.withEnergyOutput(value))
                ));
            }
            case MEKANISM_CHEMICAL_CONVERSION, MEKANISM_OXIDIZING, MEKANISM_PIGMENT_EXTRACTING -> {
                panel.addChild(mekanismItemFlow(root, holder, session, type, false));
                panel.addChild(mekanismParameterPair(
                        mekanismIntegerRow(session, type, "item_input_count", "kubejs_recipe_ui.mekanism.item_input_count",
                                session.getMekanismSettings(type).itemInputCount(), 1,
                                (settings, value) -> settings.withItemInputCount(value)),
                        mekanismResourceRow(session, type, ResourceField.PRIMARY_CHEMICAL_OUTPUT,
                                "kubejs_recipe_ui.mekanism.chemical_output")
                ));
            }
            case MEKANISM_REACTION -> createMekanismReactionPanel(root, holder, session, type, panel);
            case MEKANISM_ROTARY -> createMekanismRotaryPanel(session, type, panel);
            case MEKANISM_SAWING -> createMekanismSawingPanel(root, holder, session, type, panel);
            default -> throw new IllegalArgumentException("不是 Mekanism 工作站: " + type);
        }
        return panel;
    }

    /** 创建加压反应室的三输入、双输出和处理参数。 */
    private static void createMekanismReactionPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session,
            WorkstationType type,
            UIElement panel
    ) {
        panel.addChild(mekanismItemFlow(root, holder, session, type, true));
        panel.addChild(mekanismParameterPair(
                mekanismResourceRow(session, type, ResourceField.FLUID_INPUT,
                        "kubejs_recipe_ui.mekanism.fluid_input"),
                mekanismResourceRow(session, type, ResourceField.PRIMARY_CHEMICAL_INPUT,
                        "kubejs_recipe_ui.mekanism.chemical_input")
        ));
        panel.addChild(mekanismParameterPair(
                mekanismResourceRow(session, type, ResourceField.PRIMARY_CHEMICAL_OUTPUT,
                        "kubejs_recipe_ui.mekanism.chemical_output"),
                mekanismIntegerRow(session, type, "item_input_count", "kubejs_recipe_ui.mekanism.item_input_count",
                        session.getMekanismSettings(type).itemInputCount(), 1,
                        (settings, value) -> settings.withItemInputCount(value))
        ));
        panel.addChild(mekanismParameterPair(
                mekanismIntegerRow(session, type, "duration", "kubejs_recipe_ui.mekanism.duration",
                        session.getMekanismSettings(type).duration(), 1,
                        (settings, value) -> settings.withDuration(value)),
                mekanismIntegerRow(session, type, "energy_required", "kubejs_recipe_ui.mekanism.energy_required",
                        session.getMekanismSettings(type).energyRequired(), 0,
                        (settings, value) -> settings.withEnergyRequired(value))
        ));
    }

    /** 创建旋转式冷凝机的方向选择和双向资源字段。 */
    private static void createMekanismRotaryPanel(
            RecipeEditorSession session,
            WorkstationType type,
            UIElement panel
    ) {
        var settings = session.getMekanismSettings(type);
        var mode = translatedSelector(
                MekanismRecipeSettings.RotaryMode.values(),
                settings.rotaryMode(),
                MekanismRecipeSettings.RotaryMode::translationKey,
                value -> session.setMekanismSettings(type, session.getMekanismSettings(type).withRotaryMode(value))
        );
        mode.setId(mekanismFieldId(type, "rotary_mode"));
        var modeRow = labeledField("kubejs_recipe_ui.mekanism.rotary_mode", mode);
        modeRow.layout(style -> style.width(220).height(20));
        panel.addChild(modeRow);
        panel.addChild(mekanismParameterPair(
                mekanismResourceRow(session, type, ResourceField.FLUID_INPUT,
                        "kubejs_recipe_ui.mekanism.fluid_input"),
                mekanismResourceRow(session, type, ResourceField.PRIMARY_CHEMICAL_OUTPUT,
                        "kubejs_recipe_ui.mekanism.chemical_output")
        ));
        panel.addChild(mekanismParameterPair(
                mekanismResourceRow(session, type, ResourceField.PRIMARY_CHEMICAL_INPUT,
                        "kubejs_recipe_ui.mekanism.chemical_input"),
                mekanismResourceRow(session, type, ResourceField.FLUID_OUTPUT,
                        "kubejs_recipe_ui.mekanism.fluid_output")
        ));
    }

    /** 创建精密锯木机主产物、副产物和概率参数。 */
    private static void createMekanismSawingPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session,
            WorkstationType type,
            UIElement panel
    ) {
        var recipe = workstationRow(8);
        recipe.layout(style -> style.widthPercent(100).height(46).gapAll(8));
        recipe.addChildren(
                createInputSlot(root, holder, session, type, 0, "kubejs_recipe_ui.slot.ingredient"),
                recipeArrow(),
                createOutputSlot(holder, session, type),
                symbol("+"),
                createMekanismSecondaryOutputSlot(holder, session, type)
        );
        panel.addChild(recipe);
        panel.addChild(mekanismParameterPair(
                mekanismIntegerRow(session, type, "item_input_count", "kubejs_recipe_ui.mekanism.item_input_count",
                        session.getMekanismSettings(type).itemInputCount(), 1,
                        (settings, value) -> settings.withItemInputCount(value)),
                mekanismSecondaryChanceRow(session, type)
        ));
    }

    /** 创建 Mekanism 面板顶部的物品输入、JEI 箭头和可选物品输出。 */
    private static UIElement mekanismItemFlow(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session,
            WorkstationType type,
            boolean showOutput
    ) {
        var row = workstationRow(8);
        row.layout(style -> style.widthPercent(100).height(46).gapAll(8));
        for (var index = 0; index < type.inputCount(); index++) {
            if (index > 0) {
                row.addChild(symbol("+"));
            }
            row.addChild(createInputSlot(root, holder, session, type, index, "kubejs_recipe_ui.slot.ingredient"));
        }
        row.addChild(recipeArrow());
        if (showOutput) {
            row.addChild(createOutputSlot(holder, session, type));
        }
        return row;
    }

    /** 将一到两个 Mekanism 参数控件并排放置。 */
    private static UIElement mekanismParameterPair(UIElement... children) {
        var row = new UIElement();
        row.layout(style -> style.widthPercent(100).height(22).gapAll(4));
        row.lss("flex-direction", "row");
        row.lss("justify-content", "center");
        row.addChildren(children);
        return row;
    }

    /** 创建资源选择器或 ID/标签输入框，并附带数量字段。 */
    private static UIElement mekanismResourceRow(
            RecipeEditorSession session,
            WorkstationType type,
            ResourceField resourceField,
            String labelKey
    ) {
        var current = session.getMekanismSettings(type).resource(resourceField);
        java.util.function.Consumer<String> resourceListener = value -> {
            var settings = session.getMekanismSettings(type);
            var old = settings.resource(resourceField);
            session.setMekanismSettings(type, settings.withResource(resourceField, new ResourceStack(value, old.amount())));
        };

        UIElement resourceControl;
        if (isFluidField(resourceField)) {
            resourceControl = mekanismFluidSelector(current.id(), resourceListener);
        } else {
            resourceControl = mekanismChemicalSelector(current.id(), resourceListener);
        }
        resourceControl.setId(mekanismFieldId(
                type,
                resourceField.name().toLowerCase(java.util.Locale.ROOT) + "_id"
        ));
        resourceControl.layout(style -> style.flex(1).height(20));

        var amount = numericField(Integer.toString(current.amount()), 1, Integer.MAX_VALUE, 34);
        amount.setId(mekanismFieldId(type, resourceField.name().toLowerCase(java.util.Locale.ROOT) + "_amount"));
        amount.registerValueListener(value -> {
            var settings = session.getMekanismSettings(type);
            var old = settings.resource(resourceField);
            session.setMekanismSettings(type, settings.withResource(
                    resourceField,
                    new ResourceStack(old.id(), parseInt(value, old.amount()))
            ));
        });

        var row = new UIElement();
        row.layout(style -> style.width(166).height(20).gapAll(2));
        row.lss("flex-direction", "row");
        row.lss("align-items", "center");
        var label = fieldLabel(labelKey);
        label.layout(style -> style.width(46).height(10));
        row.addChildren(label, resourceControl, amount);
        row.style(style -> style.tooltips(Component.translatable(labelKey)));
        return row;
    }

    /** 创建显示本地化名称、内部保存注册 ID 的流体下拉框。 */
    private static Selector<String> mekanismFluidSelector(
            String selectedId,
            java.util.function.Consumer<String> listener
    ) {
        var candidates = new ArrayList<>(BuiltInRegistries.FLUID.keySet().stream()
                .filter(identifier -> {
                    var fluid = BuiltInRegistries.FLUID.get(identifier);
                    return fluid != null && fluid != Fluids.EMPTY && fluid.defaultFluidState().isSource();
                })
                .map(ResourceLocation::toString)
                .sorted(Comparator.comparing(RecipeEditorUi::fluidDisplayName).thenComparing(value -> value))
                .toList());
        if (!selectedId.isBlank() && !candidates.contains(selectedId)) {
            candidates.addFirst(selectedId);
        }

        var selector = new Selector<String>();
        selector.dialog.style(style -> style.zIndex(200));
        selector.setCandidateUIProvider(RecipeEditorUi::fluidOptionLabel);
        selector.setCandidates(candidates);
        selector.layout(style -> style.width(82).height(20));
        if (!selectedId.isBlank()) {
            selector.setSelected(selectedId, false);
        } else if (!candidates.isEmpty()) {
            selector.setSelected(candidates.getFirst(), false);
        }
        selector.setOnValueChanged(listener);
        return selector;
    }

    /** 创建显示本地化名称、内部保存注册 ID 的 Mekanism 化学品下拉框。 */
    private static Selector<String> mekanismChemicalSelector(
            String selectedId,
            java.util.function.Consumer<String> listener
    ) {
        var translationKeys = mekanismChemicalTranslationKeys();
        var candidates = new ArrayList<>(translationKeys.keySet());
        if (!selectedId.isBlank() && !candidates.contains(selectedId)) {
            candidates.addFirst(selectedId);
        }

        var selector = new Selector<String>();
        selector.dialog.style(style -> style.zIndex(200));
        selector.setCandidateUIProvider(id -> chemicalOptionLabel(id, translationKeys));
        selector.setCandidates(candidates);
        selector.layout(style -> style.width(82).height(20));
        if (!selectedId.isBlank()) {
            selector.setSelected(selectedId, false);
        } else if (!candidates.isEmpty()) {
            selector.setSelected(candidates.getFirst(), false);
        }
        selector.setOnValueChanged(listener);
        return selector;
    }

    /**
     * 通过反射读取 Mekanism 统一化学品注册表，避免可选模组未安装时产生类加载错误。
     * 注册表涵盖气体、灌注物、颜料和浆液，也包含其他模组扩展的化学品。
     */
    private static Map<String, String> mekanismChemicalTranslationKeys() {
        var cached = mekanismChemicalTranslationKeys;
        if (cached != null) {
            return cached;
        }
        synchronized (RecipeEditorUi.class) {
            cached = mekanismChemicalTranslationKeys;
            if (cached != null) {
                return cached;
            }
            mekanismChemicalTranslationKeys = cached = loadMekanismChemicalTranslationKeys();
            return cached;
        }
    }

    private static Map<String, String> loadMekanismChemicalTranslationKeys() {
        try {
            var apiClass = Class.forName("mekanism.api.MekanismAPI");
            var chemicalClass = Class.forName("mekanism.api.chemical.Chemical");
            var registryObject = apiClass.getField("CHEMICAL_REGISTRY").get(null);
            if (!(registryObject instanceof Registry<?> registry)) {
                return Map.of();
            }
            var getTranslationKey = chemicalClass.getMethod("getTranslationKey");
            var entries = new ArrayList<Map.Entry<String, String>>();
            for (var entry : registry.entrySet()) {
                var id = entry.getKey().location().toString();
                if (id.equals("mekanism:empty")) {
                    continue;
                }
                var translationKey = getTranslationKey.invoke(entry.getValue());
                if (translationKey instanceof String key && !key.isBlank()) {
                    entries.add(Map.entry(id, key));
                }
            }
            entries.sort(Comparator
                    .comparing((Map.Entry<String, String> entry) -> translate(entry.getValue()))
                    .thenComparing(Map.Entry::getKey));
            var result = new LinkedHashMap<String, String>();
            entries.forEach(entry -> result.put(entry.getKey(), entry.getValue()));
            return Collections.unmodifiableMap(result);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("[工作台编辑器] 无法读取 Mekanism 化学品注册表", exception);
            return Map.of();
        }
    }

    /** @return 流体注册 ID 的本地化显示名，无法解析时回退原 ID */
    private static String fluidDisplayName(String id) {
        if (id == null || id.isBlank()) {
            return "-";
        }
        var identifier = ResourceLocation.tryParse(id);
        if (identifier == null) {
            return id;
        }
        var fluid = BuiltInRegistries.FLUID.get(identifier);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return id;
        }
        return new FluidStack(fluid, 1).getHoverName().getString();
    }

    /** 创建流体下拉选项；名称用于显示，注册 ID 仅在悬停时提示。 */
    private static Label fluidOptionLabel(String id) {
        var label = slowRollingLabel(fluidDisplayName(id));
        if (id != null && !id.isBlank()) {
            label.style(style -> style.tooltips(Component.literal(id)));
        }
        return label;
    }

    /** 创建化学品下拉选项；实际名称来自 Mekanism 提供的翻译键。 */
    private static Label chemicalOptionLabel(String id, Map<String, String> translationKeys) {
        var translationKey = id == null ? null : translationKeys.get(id);
        var displayName = translationKey == null ? (id == null || id.isBlank() ? "-" : id) : translate(translationKey);
        var label = slowRollingLabel(displayName);
        if (id != null && !id.isBlank()) {
            label.style(style -> style.tooltips(Component.literal(id)));
        }
        return label;
    }

    private static boolean isFluidField(ResourceField resourceField) {
        return resourceField == ResourceField.FLUID_INPUT || resourceField == ResourceField.FLUID_OUTPUT;
    }

    /** 创建一个绑定到 Mekanism 不可变设置对象的整数输入字段。 */
    private static UIElement mekanismIntegerRow(
            RecipeEditorSession session,
            WorkstationType type,
            String fieldName,
            String labelKey,
            int current,
            int minimum,
            java.util.function.BiFunction<MekanismRecipeSettings, Integer, MekanismRecipeSettings> updater
    ) {
        var field = numericField(Integer.toString(current), minimum, Integer.MAX_VALUE, 72);
        field.setId(mekanismFieldId(type, fieldName));
        field.registerValueListener(value -> {
            var settings = session.getMekanismSettings(type);
            session.setMekanismSettings(type, updater.apply(settings, parseInt(value, Math.max(minimum, current))));
        });
        var row = labeledField(labelKey, field);
        row.layout(style -> style.width(166).height(20));
        row.style(style -> style.tooltips(Component.translatable(labelKey)));
        return row;
    }

    /** 创建每 tick 消耗化学品开关。 */
    private static UIElement mekanismPerTickRow(RecipeEditorSession session, WorkstationType type) {
        var control = new Switch();
        control.setId(mekanismFieldId(type, "per_tick_usage"));
        control.setOn(session.getMekanismSettings(type).perTickUsage(), false);
        control.layout(style -> style.width(26).height(14));
        control.setOnSwitchChanged(value -> session.setMekanismSettings(
                type,
                session.getMekanismSettings(type).withPerTickUsage(value)
        ));
        var row = new UIElement();
        row.layout(style -> style.width(166).height(20).gapAll(4));
        row.lss("flex-direction", "row");
        row.lss("align-items", "center");
        var label = fieldLabel("kubejs_recipe_ui.mekanism.per_tick_usage");
        label.layout(style -> style.flex(1).height(10));
        row.addChildren(label, control);
        return row;
    }

    private static UIElement mekanismSecondaryChanceRow(RecipeEditorSession session, WorkstationType type) {
        var current = session.getMekanismSettings(type);
        var field = numericField(Double.toString(current.secondaryChance()), 0.0001F, 1.0F, 72);
        field.setId(mekanismFieldId(type, "secondary_chance"));
        field.registerValueListener(value -> {
            var settings = session.getMekanismSettings(type);
            session.setMekanismSettings(type, settings.withSecondaryItemOutput(
                    settings.secondaryItemOutputId(),
                    settings.secondaryItemOutputCount(),
                    parseFloat(value, (float) settings.secondaryChance())
            ));
        });
        var row = labeledField("kubejs_recipe_ui.mekanism.secondary_chance", field);
        row.layout(style -> style.width(166).height(20));
        return row;
    }

    /** 创建总能量和每 tick 功率两个字段。 */
    private static UIElement createPowerFields(
            RecipeEditorSession session,
            WorkstationType type,
            String powerCostTranslationKey
    ) {
        var powerCost = numericField(Integer.toString(session.getPowerCost(type)), 0, Integer.MAX_VALUE, 64);
        powerCost.setId(extendedFieldId(type, "power_cost"));
        powerCost.registerValueListener(value -> session.setPowerCost(type, parseInt(value, 0)));
        var powerRate = numericField(Integer.toString(session.getPowerRate(type)), 0, Integer.MAX_VALUE, 64);
        powerRate.setId(extendedFieldId(type, "power_rate"));
        powerRate.registerValueListener(value -> session.setPowerRate(type, parseInt(value, 0)));

        var values = new UIElement();
        values.layout(style -> style.width(250).height(22).gapAll(5).marginHorizontalAuto());
        values.lss("flex-direction", "row");
        values.lss("justify-content", "center");
        var costRow = labeledField(powerCostTranslationKey, powerCost);
        costRow.layout(style -> style.width(122).height(20));
        var rateRow = labeledField("kubejs_recipe_ui.extended.power_rate", powerRate);
        rateRow.layout(style -> style.width(122).height(20));
        values.addChildren(costRow, rateRow);
        return values;
    }

    /** 创建熔炉类面板，包括输入、燃料预览、输出、经验和烹饪时间。 */
    static UIElement createCookingPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session,
            WorkstationType type
    ) {
        var experience = numericField(Float.toString(session.getExperience(type)), 0.0F, 10000.0F, 48);
        experience.setId(cookingExperienceId(type));
        experience.registerValueListener(value -> session.setExperience(type, parseFloat(value, 0.0F)));
        var cookingTime = numericField(Integer.toString(session.getCookingTime(type)), 1, 72000, 48);
        cookingTime.setId(cookingTimeId(type));
        cookingTime.registerValueListener(value -> session.setCookingTime(type, parseInt(value, 200)));

        var panel = new UIElement();
        panel.layout(style -> style.widthPercent(100).height(102).gapAll(4));
        panel.lss("flex-direction", "column");
        panel.lss("align-items", "center");

        var furnaceArea = new UIElement();
        furnaceArea.layout(style -> style.width(176).height(76));
        furnaceArea.addClass("panel_bg");

        // 使用原版熔炉容器的槽位坐标，保留输入、燃料、进度和输出的空间关系。
        var input = createInputSlot(root, holder, session, type, 0, "kubejs_recipe_ui.slot.ingredient");
        input.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(56).top(17));
        var fuel = createFuelSlot(root, holder, session, type);
        fuel.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(56).top(53));
        var arrow = recipeArrow();
        arrow.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(79).top(34));
        var output = createOutputSlot(holder, session, type);
        output.layout(style -> style.positionType(TaffyPosition.ABSOLUTE).left(116).top(35));
        furnaceArea.addChildren(input, fuel, arrow, output);

        var values = new UIElement();
        values.layout(style -> style.width(184).height(22).gapAll(4));
        values.lss("flex-direction", "row");
        var experienceRow = labeledField("kubejs_recipe_ui.cooking.experience", experience);
        experienceRow.layout(style -> style.width(90).height(20));
        var timeRow = labeledField("kubejs_recipe_ui.cooking.time", cookingTime);
        timeRow.layout(style -> style.width(90).height(20));
        values.addChildren(experienceRow, timeRow);

        panel.addChildren(furnaceArea, values);
        return panel;
    }

    /** 创建仅用于复刻原版布局的燃料幻影槽，燃料不参与配方代码。 */
    private static ItemSlot createFuelSlot(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session,
            WorkstationType type
    ) {
        var fuel = new ItemSlot();
        fuel.xeiPhantom();
        fuel.style(style -> style.tooltips(Component.translatable("kubejs_recipe_ui.slot.fuel")));
        var fuelStack = new ItemStack[]{ItemStack.EMPTY};
        fuel.bind(DataBindingBuilder.itemStack(
                () -> fuelStack[0],
                stack -> fuelStack[0] = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy()
        ).build());
        fuel.addEventListener("mouseDown", event -> {
            var carried = holder.player.containerMenu.getCarried();
            if (event.button == 0 && !carried.isEmpty()) {
                event.stopImmediatePropagation();
                var copied = carried.copy();
                copied.setCount(1);
                fuel.setItem(copied);
                return;
            }
            if (event.button == 0) {
                event.stopImmediatePropagation();
                fuel.setItem(ItemStack.EMPTY);
            }
        }, true);
        return fuel;
    }

    /** 创建营火烹饪面板：营火不需要燃料槽，但保留经验与烹饪时间设置。 */
    static UIElement createCampfirePanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session
    ) {
        var type = WorkstationType.CAMPFIRE;
        var experience = numericField(Float.toString(session.getExperience(type)), 0.0F, 10000.0F, 48);
        experience.setId(cookingExperienceId(type));
        experience.registerValueListener(value -> session.setExperience(type, parseFloat(value, 0.0F)));
        var cookingTime = numericField(Integer.toString(session.getCookingTime(type)), 1, 72000, 48);
        cookingTime.setId(cookingTimeId(type));
        cookingTime.registerValueListener(value -> session.setCookingTime(type, parseInt(value, type.defaultCookingTime())));

        var panel = new UIElement();
        panel.layout(style -> style.widthPercent(100).height(88).gapAll(4));
        panel.lss("flex-direction", "column");
        panel.lss("align-items", "center");

        var recipe = workstationRow(14);
        recipe.addChildren(
                createInputSlot(root, holder, session, type, 0, "kubejs_recipe_ui.slot.ingredient"),
                recipeArrow(),
                createOutputSlot(holder, session, type)
        );

        var values = new UIElement();
        values.layout(style -> style.width(184).height(22).gapAll(4));
        values.lss("flex-direction", "row");
        var experienceRow = labeledField("kubejs_recipe_ui.cooking.experience", experience);
        experienceRow.layout(style -> style.width(90).height(20));
        var timeRow = labeledField("kubejs_recipe_ui.cooking.time", cookingTime);
        timeRow.layout(style -> style.width(90).height(20));
        values.addChildren(experienceRow, timeRow);

        panel.addChildren(recipe, values);
        return panel;
    }

    /** 创建切石机使用的单输入配方面板。 */
    static UIElement createSingleInputPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session,
            WorkstationType type
    ) {
        var panel = workstationRow(14);
        panel.addChildren(
                createInputSlot(root, holder, session, type, 0, "kubejs_recipe_ui.slot.ingredient"),
                recipeArrow(),
                createOutputSlot(holder, session, type)
        );
        return panel;
    }

    /** 创建锻造台的模板、基础物品、追加材料和输出槽布局。 */
    static UIElement createSmithingPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session
    ) {
        var type = WorkstationType.SMITHING;
        var panel = workstationRow(5);
        panel.addChildren(
                createInputSlot(root, holder, session, type, 0, "kubejs_recipe_ui.slot.template"),
                symbol("+"),
                createInputSlot(root, holder, session, type, 1, "kubejs_recipe_ui.slot.base"),
                symbol("+"),
                createInputSlot(root, holder, session, type, 2, "kubejs_recipe_ui.slot.addition"),
                recipeArrow(),
                createOutputSlot(holder, session, type)
        );
        return panel;
    }

    /** 创建水平排列且垂直居中的工作站槽位容器。 */
    private static UIElement workstationRow(float gap) {
        var panel = new UIElement();
        panel.layout(style -> style.widthPercent(100).height(62).gapAll(gap));
        panel.lss("flex-direction", "row");
        panel.lss("align-items", "center");
        panel.lss("justify-content", "center");
        return panel;
    }

    /**
     * 创建可接受玩家光标物品和 JEI 幻影填充的输入槽。
     * 左键清空，右键打开原料类型与数据组件设置。
     */
    private static ItemSlot createInputSlot(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session,
            WorkstationType type,
            int slotIndex,
            String tooltipKey
    ) {
        var slot = new ItemSlot();
        slot.xeiPhantom();
        slot.setId(inputSlotId(type, slotIndex));
        slot.layout(style -> style.width(SLOT_SIZE).height(SLOT_SIZE));
        if (tooltipKey != null) {
            slot.style(style -> style.tooltips(Component.translatable(tooltipKey)));
        }
        slot.registerValueListener(stack -> session.setInputStack(type, slotIndex, stack));
        slot.bind(DataBindingBuilder.itemStack(
                () -> session.getInputStack(type, slotIndex),
                stack -> session.setInputStack(type, slotIndex, stack)
        ).build());
        slot.addEventListener("mouseDown", event -> {
            var carried = holder.player.containerMenu.getCarried();
            if (event.button == 0 && !carried.isEmpty()) {
                var copied = carried.copy();
                copied.setCount(1);
                event.stopImmediatePropagation();
                slot.setItem(copied);
                LOGGER.info("[工作台编辑器] {} 从玩家光标复制物品到输入槽位 {}: {}", type, slotIndex + 1, copied.getItem());
                return;
            }
            if (event.button == 0) {
                event.stopImmediatePropagation();
                slot.setItem(ItemStack.EMPTY);
                LOGGER.info("[工作台编辑器] {} 左键清空输入槽位 {}", type, slotIndex + 1);
                return;
            }
            if (event.button != 1) {
                return;
            }
            if (session.getInputStack(type, slotIndex).isEmpty()) {
                LOGGER.info("[工作台编辑器] {} 右键输入槽位 {}: 空槽位，不打开设置", type, slotIndex + 1);
                return;
            }
            session.setSelectedSlot(type, slotIndex);
            LOGGER.info("[工作台编辑器] {} 右键输入槽位 {}: 打开原料设置", type, slotIndex + 1);
            event.stopImmediatePropagation();
            showIngredientSettings(root, session);
        }, true);
        return slot;
    }

        /** 创建带可见数量输入框的输出幻影槽，并与玩家光标和 JEI 回填同步。 */
    private static ItemSlot createOutputSlot(
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session,
            WorkstationType type
    ) {
        var output = new ItemSlot();
        output.xeiPhantom();
        output.setId(outputSlotId(type));
        output.layout(style -> style.width(SLOT_SIZE).height(SLOT_SIZE));
        output.setOverflowVisible(true);
        output.style(style -> style.tooltips(Component.translatable("kubejs_recipe_ui.slot.output")));

        // 数量输入框是槽位的子元素：程序化回写文本时要屏蔽监听，否则会反过来覆盖玩家输入。
        var syncingField = new boolean[]{false};
        // 玩家在数量框里输入过的数量；-1 表示没有手动输入，此时沿用光标物品自身的数量。
        var typedCount = new int[]{-1};

        var countField = numericField(
                Integer.toString(Math.max(1, session.getOutputStack(type).getCount())),
                1,
                99,
                32
        );
        countField.setId(outputCountId(type));
        countField.addClass("kru_slot_count");
        countField.layout(style -> style
                .positionType(TaffyPosition.ABSOLUTE)
                .left(-7)
                .top(20)
                .width(32)
                .height(14));
        countField.style(style -> style
                .zIndex(20)
                .tooltips(Component.translatable("kubejs_recipe_ui.output.count")));

        java.util.function.IntConsumer writeCount = count -> {
            syncingField[0] = true;
            countField.setText(Integer.toString(count));
            syncingField[0] = false;
        };

        countField.registerValueListener(value -> {
            if (syncingField[0]) {
                return;
            }
            var count = Math.min(99, Math.max(1, parseInt(value, 1)));
            typedCount[0] = count;
            if (session.getOutputStack(type).isEmpty()) {
                return;
            }
            session.setOutputCount(type, count);
            // 会话里保存的是物品堆副本，原地改数量不会通知槽位，这里同步显示用的那一份。
            var displayed = output.getValue();
            if (!displayed.isEmpty()) {
                displayed.setCount(count);
            }
        });

        output.registerValueListener(stack -> {
            session.setOutputStack(type, stack);
            if (stack != null && !stack.isEmpty()) {
                writeCount.accept(stack.getCount());
            }
        });
        output.bind(DataBindingBuilder.itemStack(
                () -> session.getOutputStack(type),
                stack -> {
                    session.setOutputStack(type, stack);
                    if (stack != null && !stack.isEmpty()) {
                        writeCount.accept(stack.getCount());
                    }
                }
        ).build());
        output.addChild(countField);
        output.addEventListener("mouseDown", event -> {
            // 数量输入框上的点击会冒泡到槽位；不区分来源就会把刚放好的输出清空或覆盖。
            if (event.target != output) {
                return;
            }
            var carried = holder.player.containerMenu.getCarried();
            if (event.button == 0 && !carried.isEmpty()) {
                event.stopImmediatePropagation();
                var count = Math.min(99, Math.max(1, typedCount[0] > 0 ? typedCount[0] : carried.getCount()));
                fillOutputSlot(output, session, type, carried, count);
                writeCount.accept(count);
                LOGGER.info("[工作台编辑器] {} 从玩家光标复制输出物品: {} x{}", type, carried.getItem(), count);
                return;
            }
            if (event.button == 0) {
                event.stopImmediatePropagation();
                typedCount[0] = -1;
                output.setItem(ItemStack.EMPTY);
                LOGGER.info("[工作台编辑器] {} 左键清空输出槽位", type);
            }
        });
        return output;
    }

    /**
     * 把光标物品按指定数量写入输出槽。
     *
     * <p>{@code ItemSlot#setValue} 在物品相同、只有数量不同时会提前返回，
     * 所以这里同时回写会话与槽位显示，保证数量框里的数字总能真正落到输出上。</p>
     */
    private static void fillOutputSlot(ItemSlot output, RecipeEditorSession session, WorkstationType type,
                                       ItemStack carried, int count) {
        var clamped = Math.min(99, Math.max(1, count));
        var stack = carried.copyWithCount(clamped);
        output.setItem(stack);
        session.setOutputStack(type, stack);
        var displayed = output.getValue();
        if (!displayed.isEmpty() && displayed.getCount() != clamped) {
            displayed.setCount(clamped);
        }
    }

        /** 创建精密锯木机副产物幻影槽，并同步副产物 ID、数量和 JEI 回填。 */
    private static ItemSlot createMekanismSecondaryOutputSlot(
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session,
            WorkstationType type
    ) {
        var output = new ItemSlot();
        output.xeiPhantom();
        output.setId(mekanismFieldId(type, "secondary_item_output_slot"));
        output.layout(style -> style.width(SLOT_SIZE).height(SLOT_SIZE));
        output.setOverflowVisible(true);
        output.style(style -> style.tooltips(Component.translatable("kubejs_recipe_ui.mekanism.secondary_output")));

        var settings = session.getMekanismSettings(type);
        // 与主输出槽一致：屏蔽程序化回写产生的监听，并记住玩家手动输入过的数量。
        var syncingField = new boolean[]{false};
        var typedCount = new int[]{-1};
        var countField = numericField(
                Integer.toString(settings.secondaryItemOutputCount()),
                1,
                99,
                32
        );
        countField.setId(mekanismFieldId(type, "secondary_item_output_count"));
        countField.addClass("kru_slot_count");
        countField.layout(style -> style
                .positionType(TaffyPosition.ABSOLUTE)
                .left(-7)
                .top(20)
                .width(32)
                .height(14));
        countField.style(style -> style
                .zIndex(20)
                .tooltips(Component.translatable("kubejs_recipe_ui.mekanism.secondary_count")));

        java.util.function.IntConsumer writeCount = count -> {
            syncingField[0] = true;
            countField.setText(Integer.toString(count));
            syncingField[0] = false;
        };

        countField.registerValueListener(value -> {
            if (syncingField[0]) {
                return;
            }
            var count = Math.min(99, Math.max(1, parseInt(value, 1)));
            typedCount[0] = count;
            var current = session.getMekanismSettings(type);
            session.setMekanismSettings(type, current.withSecondaryItemOutput(
                    current.secondaryItemOutputId(),
                    count,
                    current.secondaryChance()
            ));
            var displayed = output.getValue();
            if (!displayed.isEmpty()) {
                displayed.setCount(count);
            }
        });

        java.util.function.Consumer<ItemStack> updateSecondaryOutput = stack -> {
            var current = session.getMekanismSettings(type);
            if (stack == null || stack.isEmpty()) {
                session.setMekanismSettings(type, current.withSecondaryItemOutput(
                        "",
                        current.secondaryItemOutputCount(),
                        current.secondaryChance()
                ));
                return;
            }
            var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            var count = Math.max(1, stack.getCount());
            session.setMekanismSettings(type, current.withSecondaryItemOutput(
                    itemId.toString(),
                    count,
                    current.secondaryChance()
            ));
            writeCount.accept(count);
        };

        output.registerValueListener(updateSecondaryOutput);
        output.bind(DataBindingBuilder.itemStack(
                () -> mekanismSecondaryOutputStack(session.getMekanismSettings(type)),
                updateSecondaryOutput
        ).build());
        output.addChild(countField);
        output.addEventListener("mouseDown", event -> {
            // 数量输入框上的点击会冒泡到槽位，必须只处理槽位自身的点击。
            if (event.target != output) {
                return;
            }
            var carried = holder.player.containerMenu.getCarried();
            if (event.button == 0 && !carried.isEmpty()) {
                event.stopImmediatePropagation();
                var count = Math.min(99, Math.max(1, typedCount[0] > 0 ? typedCount[0] : carried.getCount()));
                var stack = carried.copyWithCount(count);
                output.setItem(stack);
                // setItem 在物品相同、数量不同时会提前返回，这里直接回写会话与显示。
                updateSecondaryOutput.accept(stack);
                var displayed = output.getValue();
                if (!displayed.isEmpty() && displayed.getCount() != count) {
                    displayed.setCount(count);
                }
                writeCount.accept(count);
                LOGGER.info("[工作台编辑器] {} 从玩家光标复制副产物: {} x{}", type, carried.getItem(), count);
                return;
            }
            if (event.button == 0) {
                event.stopImmediatePropagation();
                typedCount[0] = -1;
                output.setItem(ItemStack.EMPTY);
                LOGGER.info("[工作台编辑器] {} 左键清空副产物槽位", type);
            }
        });
        return output;
    }

    /** 将锯木机副产物设置转换为幻影槽展示的物品堆。 */
    private static ItemStack mekanismSecondaryOutputStack(MekanismRecipeSettings settings) {
        var stack = optionalItemStack(settings.secondaryItemOutputId());
        if (!stack.isEmpty()) {
            stack.setCount(settings.secondaryItemOutputCount());
        }
        return stack;
    }

    /** 直接使用 JEI 的配方箭头资源；未安装 JEI 时保留通用箭头作为回退。 */
    private static UIElement recipeArrow() {
        var arrow = new UIElement();
        arrow.layout(style -> style.width(22).height(16));
        arrow.style(style -> style.background(ModList.get().isLoaded("jei")
                ? SpriteTexture.of(JEI_RECIPE_ARROW_TEXTURE).setSprite(0, 0, 22, 16)
                : Icons.RIGHT_ARROW_NO_BAR));
        return arrow;
    }

    /** 生成 JEI 回填时定位输入槽所需的稳定元素 ID。 */
    private static String inputSlotId(WorkstationType type, int slotIndex) {
        return "kubejs_recipe_ui#input_" + type.name().toLowerCase(java.util.Locale.ROOT) + "_" + slotIndex;
    }

    /** 生成 JEI 回填时定位输出槽所需的稳定元素 ID。 */
    private static String outputSlotId(WorkstationType type) {
        return "kubejs_recipe_ui#output_" + type.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** 生成 JEI 回填时定位输出数量框所需的稳定元素 ID。 */
    private static String outputCountId(WorkstationType type) {
        return "kubejs_recipe_ui#output_count_" + type.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** 生成 JEI 回填经验输入框所需的稳定元素 ID。 */
    private static String cookingExperienceId(WorkstationType type) {
        return "kubejs_recipe_ui#experience_" + type.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** 生成 JEI 回填烹饪时间输入框所需的稳定元素 ID。 */
    private static String cookingTimeId(WorkstationType type) {
        return "kubejs_recipe_ui#cooking_time_" + type.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** 生成 Extended Crafting 数值字段在 JEI 回填时使用的稳定元素 ID。 */
    private static String extendedFieldId(WorkstationType type, String fieldName) {
        return "kubejs_recipe_ui#extended_"
                + type.name().toLowerCase(java.util.Locale.ROOT)
                + "_"
                + fieldName;
    }

    /** 生成 AE2 专属控件在 JEI 回填时使用的稳定元素 ID。 */
    private static String ae2FieldId(WorkstationType type, String fieldName) {
        return "kubejs_recipe_ui#ae2_"
                + type.name().toLowerCase(java.util.Locale.ROOT)
                + "_"
                + fieldName;
    }

    /** 生成 Mekanism 专属控件在 JEI 回填时使用的稳定元素 ID。 */
    private static String mekanismFieldId(WorkstationType type, String fieldName) {
        return "kubejs_recipe_ui#mekanism_"
                + type.name().toLowerCase(java.util.Locale.ROOT)
                + "_"
                + fieldName;
    }

    /** 将 JEI 读取到的 Mekanism 资源和机器参数同步到已经挂载的 LDLib2 控件。 */
    private static void updateMekanismFields(
            UIElement root,
            WorkstationType type,
            MekanismRecipeSettings settings
    ) {
        for (var resourceField : ResourceField.values()) {
            var resource = settings.resource(resourceField);
            var fieldName = resourceField.name().toLowerCase(java.util.Locale.ROOT);
            updateSelector(root, mekanismFieldId(type, fieldName + "_id"), resource.id());
            updateNumericField(root, mekanismFieldId(type, fieldName + "_amount"), resource.amount());
        }
        updateNumericField(root, mekanismFieldId(type, "item_input_count"), settings.itemInputCount());
        updateNumericField(root, mekanismFieldId(type, "extra_item_input_count"), settings.extraItemInputCount());
        updateNumericField(root, mekanismFieldId(type, "energy_output"), settings.energyOutput());
        updateNumericField(root, mekanismFieldId(type, "duration"), settings.duration());
        updateNumericField(root, mekanismFieldId(type, "energy_required"), settings.energyRequired());
        updateNumericField(root, mekanismFieldId(type, "energy_multiplier"), settings.energyMultiplier());
        var secondaryOutput = getElement(root, mekanismFieldId(type, "secondary_item_output_slot"));
        if (secondaryOutput instanceof ItemSlot slot) {
            slot.setItem(mekanismSecondaryOutputStack(settings));
        }
        updateNumericField(root, mekanismFieldId(type, "secondary_item_output_count"), settings.secondaryItemOutputCount());
        updateTextField(root, mekanismFieldId(type, "secondary_chance"), Double.toString(settings.secondaryChance()));
        updateSelector(
                root,
                mekanismFieldId(type, "rotary_mode"),
                translate(settings.rotaryMode().translationKey())
        );
        updateSwitch(root, mekanismFieldId(type, "per_tick_usage"), settings.perTickUsage());
    }

    /** 将 AE2 配方对象读取到的专属参数同步到已经挂载的 LDLib2 控件。 */
    private static void updateAe2Fields(
            UIElement root,
            WorkstationType type,
            Ae2RecipeSettings settings
    ) {
        switch (type) {
            case AE2_INSCRIBER -> updateSelector(
                    root,
                    ae2FieldId(type, "inscriber_mode"),
                    translate(settings.inscriberMode().translationKey())
            );
            case AE2_TRANSFORM -> {
                updateSelector(
                        root,
                        ae2FieldId(type, "transform_condition"),
                        translate(settings.transformCondition().translationKey())
                );
                updateTextField(root, ae2FieldId(type, "transform_fluid_tag"), settings.transformFluidTag());
                var row = root.getModularUI() == null
                        ? null
                        : root.getModularUI().getElementById(ae2FieldId(type, "transform_fluid_row"));
                if (row != null) {
                    row.setDisplay(settings.transformCondition() == Ae2RecipeSettings.TransformCondition.FLUID);
                }
                updateAe2ItemPreview(
                        root,
                        ae2FieldId(type, "transform_condition_preview"),
                        ae2TransformPreviewStacks(settings)
                );
                updateLabel(
                        root,
                        ae2FieldId(type, "transform_condition_label"),
                        Component.translatable(transformPreviewTranslationKey(settings.transformCondition()))
                );
            }
            case AE2_ENTROPY -> {
                updateSelector(
                        root,
                        ae2FieldId(type, "entropy_mode"),
                        translate(settings.entropyMode().translationKey())
                );
                updateSelector(
                        root,
                        ae2FieldId(type, "entropy_input_type"),
                        translate(settings.entropyInputType().translationKey())
                );
                updateTextField(root, ae2FieldId(type, "entropy_input_id"), settings.entropyInputId());
                updateSelector(
                        root,
                        ae2FieldId(type, "entropy_output_type"),
                        translate(settings.entropyOutputType().translationKey())
                );
                updateTextField(root, ae2FieldId(type, "entropy_output_id"), settings.entropyOutputId());
                updateAe2ItemPreview(
                        root,
                        ae2FieldId(type, "entropy_input_preview"),
                        ae2EntropyInputStack(settings)
                );
                updateAe2ItemPreview(
                        root,
                        ae2FieldId(type, "entropy_output_preview"),
                        ae2EntropyOutputStack(settings)
                );
                var modeIcon = getElement(root, ae2FieldId(type, "entropy_mode_icon"));
                var modeLabel = getElement(root, ae2FieldId(type, "entropy_mode_label"));
                var actionLabel = getElement(root, ae2FieldId(type, "entropy_action_label"));
                if (modeIcon != null && modeLabel instanceof Label label && actionLabel instanceof Label action) {
                    setEntropyModePreview(modeIcon, label, action, settings.entropyMode());
                }
            }
            default -> {
            }
        }
    }

    /** 按稳定 ID 获取已经挂载到 ModularUI 的控件。 */
    private static UIElement getElement(UIElement root, String id) {
        return root.getModularUI() == null ? null : root.getModularUI().getElementById(id);
    }

    /** JEI 回填时同步纯展示槽中的图标。 */
    private static void updateAe2ItemPreview(UIElement root, String id, ItemStack... stacks) {
        var element = getElement(root, id);
        if (element != null) {
            setAe2ItemPreview(element, stacks);
        }
    }

    /** JEI 回填时同步预览说明文字。 */
    private static void updateLabel(UIElement root, String id, Component text) {
        var element = getElement(root, id);
        if (element instanceof Label label) {
            label.setText(text);
        }
    }

    /** JEI 回填时同步修改已经挂载到 ModularUI 的下拉框。 */
    @SuppressWarnings("unchecked")
    private static void updateSelector(UIElement root, String id, String value) {
        var element = root.getModularUI() == null ? null : root.getModularUI().getElementById(id);
        if (element instanceof Selector<?> selector) {
            ((Selector<String>) selector).setSelected(value);
        }
    }

    /** JEI 回填时同步修改已经挂载到 ModularUI 的文本输入框。 */
    private static void updateTextField(UIElement root, String id, String value) {
        var element = root.getModularUI() == null ? null : root.getModularUI().getElementById(id);
        if (element instanceof TextField field) {
            field.setText(value);
        }
    }

    /** JEI 回填时同步修改已经挂载到 ModularUI 的开关。 */
    private static void updateSwitch(UIElement root, String id, boolean value) {
        var element = root.getModularUI() == null ? null : root.getModularUI().getElementById(id);
        if (element instanceof Switch control) {
            control.setOn(value, false);
        }
    }

    /** JEI 回填时同步修改已经挂载到 ModularUI 的整数输入框。 */
    private static void updateNumericField(UIElement root, String id, int value) {
        var element = root.getModularUI() == null ? null : root.getModularUI().getElementById(id);
        if (element instanceof TextField field) {
            field.setText(Integer.toString(value));
        }
    }

    /** 创建紧凑的配方符号标签。 */
    private static Label symbol(String text) {
        var label = new Label();
        label.setText(text);
        label.layout(style -> style.width(8).height(10));
        return label;
    }

    /** 将翻译标签和输入控件组合成固定高度的一行。 */
    private static UIElement labeledField(String key, UIElement field) {
        var row = new UIElement();
        row.layout(style -> style.widthPercent(100).height(20).gapAll(3));
        row.lss("flex-direction", "row");
        row.lss("align-items", "center");
        var label = fieldLabel(key);
        label.layout(style -> style.width(38).height(10));
        row.addChildren(label, field);
        return row;
    }

    /** 创建限制浮点范围的数字输入框。 */
    private static TextField numericField(String value, float min, float max, float width) {
        var field = new TextField();
        field.setNumbersOnlyFloat(min, max);
        field.setText(value);
        field.layout(style -> style.width(width).height(20));
        return field;
    }

    /** 创建限制整数范围的数字输入框。 */
    private static TextField numericField(String value, int min, int max, float width) {
        var field = new TextField();
        field.setNumbersOnlyInt(min, max);
        field.setText(value);
        field.layout(style -> style.width(width).height(20));
        return field;
    }

    /** 解析输入框浮点值，编辑中的临时非法文本使用回退值。 */
    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    /** 解析输入框整数值，编辑中的临时非法文本使用回退值。 */
    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    /** 创建尺寸统一的文字操作按钮。 */
    private static Button actionButton(Component text, float width) {
        var button = new Button();
        button.setText(text);
        button.layout(style -> style.width(width).height(20));
        button.addClass("kru_action_button");
        return button;
    }

    /** 创建控制输入或输出数据组件是否参与生成的 LDLib2 原版风格开关。 */
    private static Switch componentSwitch(String translationKey, boolean enabled) {
        var control = new Switch();
        control.setOn(enabled, false);
        control.layout(style -> style.width(26).height(14));
        control.style(style -> style.tooltips(Component.translatable(translationKey + ".tooltip")));
        return control;
    }

    /** 将原版风格开关与可翻译标签组合成属性检查器中的一行。 */
    private static UIElement switchOptionRow(String translationKey, Switch control) {
        var row = new UIElement();
        row.layout(style -> style.widthPercent(100).height(20).gapAll(4));
        row.lss("flex-direction", "row");
        row.lss("align-items", "center");

        var label = new Label();
        label.setText(Component.translatable(translationKey));
        label.layout(style -> style.flex(1).height(16));
        label.textStyle(style -> style.textAlignVertical(Vertical.CENTER));
        row.addChildren(label, control);
        row.style(style -> style.tooltips(Component.translatable(translationKey + ".tooltip")));
        return row;
    }

    /** 创建浅色编辑器属性检查器的分组标题。 */
    private static Label editorSectionLabel(String translationKey) {
        var label = new Label();
        label.setText(Component.translatable(translationKey));
        label.layout(style -> style.widthPercent(100).height(14).marginTop(2));
        label.textStyle(style -> style.textAlignVertical(Vertical.CENTER));
        label.addClass("kru_section_label");
        return label;
    }

    /** 创建 JEI 来源页使用的长文本值标签。 */
    private static Label sourceValueLabel() {
        var label = slowRollingLabel("-");
        label.layout(style -> style.flex(1).height(16));
        return label;
    }

    /** 创建 JEI 来源信息的一行键值显示。 */
    private static UIElement sourceInfoRow(String translationKey, Label value) {
        var row = new UIElement();
        row.layout(style -> style.widthPercent(100).height(18).gapAll(6));
        row.lss("flex-direction", "row");
        row.lss("align-items", "center");
        row.addClass("kru_source_row");

        var key = new Label();
        key.setText(Component.translatable(translationKey));
        key.layout(style -> style.width(82).height(16));
        key.textStyle(style -> style.textAlignVertical(Vertical.CENTER));
        row.addChildren(key, value);
        return row;
    }

    /** 将 JEI 缺失的来源字段显示为短横线。 */
    private static String sourceDisplayValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /**
     * 在捕获阶段注册按钮点击，统一拦截事件并输出调试日志。
     */
    private static void captureClick(
            Button button,
            String actionName,
            java.util.function.Consumer<UIEvent> listener
    ) {
        button.addEventListener("mouseDown", event -> {
            if (event.button == 0 && button.isActive()) {
                LOGGER.info("[工作台编辑器] 按钮点击: {}, 侧: {}", actionName, LDLib2.isRemote() ? "客户端" : "服务端");
                listener.accept(event);
            }
        }, true);
    }

    /** 打开当前输入槽的物品、标签、替代材料和数据组件设置弹窗。 */
    private static void showIngredientSettings(UIElement root, RecipeEditorSession session) {
        var selected = session.getInputSpec(session.getSelectedSlot());
        var modes = IngredientSpec.Kind.values();
        var mode = new IngredientSpec.Kind[]{selected == null ? IngredientSpec.Kind.ITEM : selected.kind()};
        var itemTags = session.getSelectedItemTags();

        var valueField = new TextField();
        valueField.setAnyString();
        valueField.setText(valueText(selected));
        valueField.layout(style -> style.widthPercent(100).height(20));
        valueField.textFieldStyle(style -> style.placeholder(
                Component.translatable("kubejs_recipe_ui.settings.value_placeholder")
        ));

        var tagSelector = new Selector<String>();
        // Selector 的候选浮层会挂到 UI 根节点，层级必须高于当前模态框。
        tagSelector.dialog.style(style -> style.zIndex(200));
        tagSelector.setCandidates(itemTags);
        if (!itemTags.isEmpty()) {
            var currentTag = selected != null && selected.kind() == IngredientSpec.Kind.TAG
                    ? "#" + selected.value()
                    : itemTags.getFirst();
            tagSelector.setSelected(itemTags.contains(currentTag) ? currentTag : itemTags.getFirst());
        }
        tagSelector.layout(style -> style.widthPercent(100).height(20));

        var componentField = new TextField();
        componentField.setAnyString();
        componentField.setText(selected == null ? "" : selected.componentPatch());
        componentField.layout(style -> style.widthPercent(100).height(20));
        componentField.textFieldStyle(style -> style.placeholder(
                Component.translatable("kubejs_recipe_ui.settings.components_placeholder")
        ));

        var content = new UIElement();
        content.layout(style -> style
                .widthPercent(100)
                .paddingAll(6)
                .gapAll(5)
        );
        content.lss("flex-direction", "column");

        var valueLabel = fieldLabel("kubejs_recipe_ui.settings.value");
        var tagLabel = fieldLabel("kubejs_recipe_ui.settings.available_tags", itemTags.size());

        var modeButton = actionButton(modeText(mode[0]), 128);
        captureClick(modeButton, "切换原料类型", event -> {
            mode[0] = modes[(mode[0].ordinal() + 1) % modes.length];
            modeButton.setText(modeText(mode[0]));
            updateIngredientValueControls(mode[0], valueLabel, valueField, tagLabel, tagSelector);
        });

        var captureButton = actionButton(Component.translatable("kubejs_recipe_ui.settings.capture_components"), 128);
        captureClick(captureButton, "读取槽位数据组件", event -> componentField.setText(session.captureSelectedComponents()));

        content.addChildren(
                fieldLabel("kubejs_recipe_ui.settings.type"),
                modeButton,
                valueLabel,
                valueField,
                tagLabel,
                tagSelector,
                fieldLabel("kubejs_recipe_ui.settings.components"),
                componentField,
                captureButton
        );
        updateIngredientValueControls(mode[0], valueLabel, valueField, tagLabel, tagSelector);

        var dialog = createModal("kubejs_recipe_ui.settings.title", 300, 225);
        dialog.addContent(content);

        var save = actionButton(Component.translatable("gui.done"), 70);
        captureClick(save, "保存原料设置", event -> {
            try {
                var value = mode[0] == IngredientSpec.Kind.TAG
                        ? selectedTag(tagSelector)
                        : valueField.getText();
                var specification = createSpecification(mode[0], value, componentField.getText());
                session.setInputSpec(session.getSelectedSlot(), specification);
                dialog.close();
            } catch (IllegalArgumentException exception) {
                Dialog.showNotification(exception.getMessage(), 3.0F);
            }
        });
        var cancel = actionButton(Component.translatable("gui.cancel"), 70);
        captureClick(cancel, "取消原料设置", event -> dialog.close());
        dialog.addButton(save).addButton(cancel).show(root);
    }

    /** 根据原料模式切换普通文本框、标签选择器和数据组件控件的可见性。 */
    private static void updateIngredientValueControls(
            IngredientSpec.Kind mode,
            Label valueLabel,
            TextField valueField,
            Label tagLabel,
            Selector<String> tagSelector
    ) {
        var tagMode = mode == IngredientSpec.Kind.TAG;
        valueLabel.setDisplay(!tagMode);
        valueField.setDisplay(!tagMode);
        tagLabel.setDisplay(tagMode);
        tagSelector.setDisplay(tagMode);
    }

    /** 从标签下拉框读取当前选项，未选择时返回空字符串。 */
    private static String selectedTag(Selector<String> tagSelector) {
        var tag = tagSelector.getValue();
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException(translate("kubejs_recipe_ui.settings.no_tags"));
        }
        return tag;
    }

    /** 创建固定高度的字段说明标签。 */
    private static Label fieldLabel(String translationKey, Object... arguments) {
        var label = new Label();
        label.setText(Component.translatable(translationKey, arguments));
        label.layout(style -> style.height(10));
        return label;
    }

    /** @return 原料类型对应的本地化名称 */
    private static Component modeText(IngredientSpec.Kind kind) {
        return Component.translatable(switch (kind) {
            case ITEM -> "kubejs_recipe_ui.settings.mode_item";
            case TAG -> "kubejs_recipe_ui.settings.mode_tag";
            case ALTERNATIVES -> "kubejs_recipe_ui.settings.mode_alternatives";
        });
    }

    /** 将原料模型还原为设置弹窗中的主值文本。 */
    private static String valueText(IngredientSpec specification) {
        if (specification == null) {
            return "";
        }
        return switch (specification.kind()) {
            case ITEM -> specification.value();
            case TAG -> "#" + specification.value();
            case ALTERNATIVES -> specification.alternatives().stream()
                    .map(RecipeEditorUi::alternativeText)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
        };
    }

    /** 将替代材料列表转换为逗号分隔文本。 */
    private static String alternativeText(IngredientSpec specification) {
        return specification.kind() == IngredientSpec.Kind.TAG
                ? "#" + specification.value()
                : specification.value() + specification.componentPatch();
    }

    /** 根据设置弹窗字段创建经过校验的原料模型。 */
    private static IngredientSpec createSpecification(IngredientSpec.Kind kind, String value, String components) {
        return switch (kind) {
            case ITEM -> IngredientSpec.item(value, components);
            case TAG -> IngredientSpec.tag(value);
            case ALTERNATIVES -> {
                var alternatives = new ArrayList<IngredientSpec>();
                Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(entry -> !entry.isEmpty())
                        .map(IngredientSpec::parseAlternative)
                        .forEach(alternatives::add);
                yield IngredientSpec.alternatives(alternatives);
            }
        };
    }

    /** 将代码编辑器的行数组合并为平台无关的换行文本。 */
    private static String editorCode(CodeEditor editor) {
        return String.join("\n", editor.getValue());
    }

    /** 创建适合浅色 JEI 风格背景的高对比 JavaScript 语法配色。 */
    private static StyleManager createLightCodeStyle() {
        var styleManager = new StyleManager();
        styleManager.setDefaultStyle(codeTextStyle(0x24292F));
        styleManager.getStyleMap().put(TokenTypes.KEYWORD.name, codeTextStyle(0x0000FF));
        styleManager.getStyleMap().put(TokenTypes.IDENTIFIER.name, codeTextStyle(0x24292F));
        styleManager.getStyleMap().put(TokenTypes.STRING.name, codeTextStyle(0xA31515));
        styleManager.getStyleMap().put(TokenTypes.COMMENT.name, codeTextStyle(0x008000));
        styleManager.getStyleMap().put(TokenTypes.NUMBER.name, codeTextStyle(0x098658));
        styleManager.getStyleMap().put(TokenTypes.OPERATOR.name, codeTextStyle(0x24292F));
        return styleManager;
    }

    private static Style codeTextStyle(int rgb) {
        return Style.EMPTY.withColor(rgb);
    }

    /** 创建滚动速度较慢的长文本标签，确保下拉选项可读。 */
    private static Label slowRollingLabel(String text) {
        var label = new Label();
        label.setText(text == null ? "" : text);
        label.textStyle(style -> style
                .textWrap(TextWrap.HOVER_ROLL)
                .textAlignVertical(Vertical.CENTER)
                .rollSpeed(0.15F)
        );
        label.layout(style -> style.widthPercent(100).height(16));
        label.setOverflowVisible(false);
        return label;
    }

    /** 将提示同时显示在游戏聊天栏和当前 LDLib2 界面中。 */
    private static void showInGameMessage(Player player, String translationKey) {
        var message = Component.translatable(translationKey);
        player.sendSystemMessage(message);
        Dialog.showNotification(message.getString(), 3.0F);
    }

    /** 显示服务端返回的写入结果，成功和失败都会给玩家明确反馈。 */
    private static void showWriteResult(Player player, Object result, String fallbackKey) {
        var translationKey = result instanceof String value && !value.isBlank() ? value : fallbackKey;
        showInGameMessage(player, translationKey);
    }

    /**
     * 从当前会话生成预览代码，并把校验错误转换为界面通知。
     */
    private static void generateRecipe(
            RecipeEditorSession session,
            RecipeWriteMode writeMode,
            RecipeDeleteMode deleteMode,
            boolean includeInputComponents,
            boolean includeOutputComponents,
            java.util.function.Consumer<String> consumer
    ) {
        try {
            var recipe = session.createDefinition(includeInputComponents, includeOutputComponents);
            var code = generateStandardRecipe(
                    session,
                    recipe,
                    writeMode,
                    deleteMode,
                    includeInputComponents,
                    includeOutputComponents
            );
            LOGGER.info(
                    "[工作台编辑器] 代码生成成功: 类型={}, 写入方式={}, 删除依据={}, 原料组件={}, 输出组件={}, 代码长度={}",
                    session.getActiveType(),
                    writeMode,
                    deleteMode,
                    includeInputComponents,
                    includeOutputComponents,
                    code.length()
            );
            consumer.accept(code);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("[工作台编辑器] 代码生成失败: {}", exception.getMessage());
            var message = exception.getMessage();
            Dialog.showNotification(message != null && message.startsWith("kubejs_recipe_ui.")
                    ? translate(message)
                    : message, 3.0F);
        }
    }

    /** 生成普通 KubeJS 配方事件代码，并保留既有的删除条件提取规则。*/
    private static String generateStandardRecipe(
            RecipeEditorSession session,
            RecipeDefinition recipe,
            RecipeWriteMode writeMode,
            RecipeDeleteMode deleteMode,
            boolean includeInputComponents,
            boolean includeOutputComponents
    ) {
        var deleteValue = session.createDeleteValue(deleteMode, includeInputComponents, includeOutputComponents);
        return writeMode == RecipeWriteMode.REMOVE_ONLY
                ? KubeJsRecipeGenerator.generateRemove(deleteMode, deleteValue)
                : KubeJsRecipeGenerator.generate(recipe, writeMode, deleteMode, deleteValue);
    }

    /** 创建带标题和滚动内容区域的通用模态弹窗。 */
    private static Dialog createModal(String titleKey, float maxWidth, float maxHeight) {
        var dialog = new Dialog()
                .setTitle(translate(titleKey))
                .darkenBackground()
                .setClickOutsideClose(false);
        // 原版按钮样式的绘制层级高于 LDLib 对话框默认值，需确保模态框始终位于主界面上方。
        dialog.style(style -> style.zIndex(100));
        dialog.overlay.layout(style -> style
                .widthPercent(90)
                .maxWidth(maxWidth)
                .heightPercent(90)
                .maxHeight(maxHeight)
        );
        dialog.titleBar.layout(style -> style.height(24).flexShrink(0));
        dialog.contentContainer.layout(style -> style.flex(1).minHeight(0));
        dialog.buttonContainer.layout(style -> style.height(32).flexShrink(0));
        return dialog;
    }

    /** 立即解析翻译键，用于 Selector 等只接受字符串的控件。 */
    private static String translate(String translationKey) {
        return Component.translatable(translationKey).getString();
    }

}
