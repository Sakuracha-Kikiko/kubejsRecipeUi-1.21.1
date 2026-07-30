package com.github.jun8848.kubejsrecipeui.client;

import com.github.jun8848.kubejsrecipeui.recipe.IngredientSpec;
import com.github.jun8848.kubejsrecipeui.recipe.ExtendedRecipeSettings;
import com.github.jun8848.kubejsrecipeui.recipe.KubeJsRecipeGenerator;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeDeleteMode;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeDefinition;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeWriteService;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeWriteMode;
import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEventBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
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
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;

/**
 * 使用 LDLib2 构建配方编辑器的完整客户端界面。
 * 该类只负责控件布局和交互，配方状态存放在 {@link RecipeEditorSession} 中。
 */
public final class RecipeEditorUi {
    private static final float SLOT_SIZE = 18.0F;
    private static final Identifier MINECRAFT_FONT = Identifier.withDefaultNamespace("default");
    private static final Logger LOGGER = LogUtils.getLogger();

    private RecipeEditorUi() {
    }

    /**
     * 创建一个包含工作站侧边栏、配方编辑区和代码预览的 ModularUI。
     */
    public static ModularUI create(BlockUIMenuType.BlockUIHolder holder) {
        var session = new RecipeEditorSession(holder.player);
        var availableTypes = WorkstationType.availableValues();
        var sidebarTypes = availableTypes.stream()
                .filter(type -> !type.isExtendedTable() || type == WorkstationType.EXTENDED_BASIC_TABLE)
                .toList();
        var root = new UIElement();
        root.layout(style -> style
                .width(560)
                .height(420)
                .gapAll(4)
        );
        root.lss("flex-direction", "column");

        var header = new UIElement();
        header.layout(style -> style
                .widthPercent(100)
                .height(26)
                .paddingHorizontal(8)
        );
        header.lss("flex-direction", "row");
        header.lss("align-items", "center");
        header.addClass("panel_bg");

        var title = new Label();
        title.setText(Component.translatable(WorkstationType.CRAFTING.translationKey()));
        title.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
        title.layout(style -> style.widthPercent(100).height(16));
        header.addChild(title);

        var contentBody = new UIElement();
        contentBody.layout(style -> style
                .widthPercent(100)
                .flex(1)
                .gapAll(4)
        );
        contentBody.lss("flex-direction", "row");

        var sidebar = new UIElement();
        sidebar.layout(style -> style
                .width(36)
                .heightPercent(100)
                .paddingAll(6)
                .gapAll(5)
        );
        sidebar.lss("flex-direction", "column");
        sidebar.lss("align-items", "center");
        sidebar.addClass("panel_bg");

        var main = new UIElement();
        main.layout(style -> style
                .flex(1)
                .heightPercent(100)
        );
        main.lss("flex-direction", "column");
        main.addClass("panel_bg");

        var recipeCanvas = new UIElement();
        recipeCanvas.layout(style -> style
                .widthPercent(100)
                .heightPercent(100)
                .paddingAll(8)
                .gapAll(5)
        );
        recipeCanvas.lss("flex-direction", "column");
        recipeCanvas.addClass("panel_bg");

        var inspectorContent = new UIElement();
        inspectorContent.layout(style -> style
                .widthPercent(100)
                .paddingAll(7)
                .gapAll(5)
        );
        inspectorContent.lss("flex-direction", "column");

        var inspector = new ScrollerView();
        inspector.layout(style -> style.widthPercent(100).heightPercent(100));
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
                editorSectionLabel("kubejs_recipe_ui.editor.inspector.recipe"),
                writeModeButton,
                switchOptionRow("kubejs_recipe_ui.recipe_components.input", inputComponents),
                switchOptionRow("kubejs_recipe_ui.recipe_components.output", outputComponents)
        );

        // 四个 Extended Crafting 工作台共用一个侧栏入口，通过下拉框切换实际网格和独立编辑状态。
        var extendedTableByDisplay = new HashMap<String, WorkstationType>();
        var extendedTableDisplay = new EnumMap<WorkstationType, String>(WorkstationType.class);
        var extendedTableCandidates = availableTypes.stream()
                .filter(WorkstationType::isExtendedTable)
                .map(type -> {
                    var display = translate(extendedTableLevelTranslationKey(type));
                    extendedTableByDisplay.put(display, type);
                    extendedTableDisplay.put(type, display);
                    return display;
                })
                .toList();
        var selectedExtendedTable = new WorkstationType[]{WorkstationType.EXTENDED_BASIC_TABLE};
        var extendedTableSelector = new Selector<String>();
        extendedTableSelector.dialog.style(style -> style.zIndex(200));
        extendedTableSelector.setCandidates(extendedTableCandidates);
        if (!extendedTableCandidates.isEmpty()) {
            extendedTableSelector.setSelected(extendedTableCandidates.getFirst());
        }
        extendedTableSelector.setCandidateUIProvider(RecipeEditorUi::slowRollingLabel);
        extendedTableSelector.layout(style -> style.width(115).height(20));
        var extendedTableSelectorRow = labeledField("kubejs_recipe_ui.extended.table_level", extendedTableSelector);
        extendedTableSelectorRow.layout(style -> style.widthPercent(100).height(20));
        extendedTableSelectorRow.setDisplay(false);
        inspectorContent.addChild(extendedTableSelectorRow);

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
        codePreview.textAreaStyle(style -> style.font(MINECRAFT_FONT));
        codePreview.setValue(new String[0], false);
        codePreview.layout(style -> style.widthPercent(100).heightPercent(100));

        captureClick(previewButton, "生成代码预览", event -> generateRecipe(
                session,
                writeMode[0],
                deleteMode[0],
                inputComponents.isOn(),
                outputComponents.isOn(),
                code -> codePreview.setValue(code.split("\\R", -1), false)
        ));

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
        editorArea.layout(style -> style.widthPercent(100).height(182));
        editorArea.lss("flex-direction", "column");
        // 所有工作站面板只创建一次，侧边栏切换时仅改变可见性以保留控件状态。
        var panels = new EnumMap<WorkstationType, UIElement>(WorkstationType.class);
        for (var type : availableTypes) {
            var panel = createWorkstationPanel(root, holder, session, type);
            panel.setDisplay(type == WorkstationType.CRAFTING);
            panels.put(type, panel);
            editorArea.addChild(panel);
        }
        recipeCanvas.addChild(editorArea);

        var inventoryLabel = new Label();
        inventoryLabel.setText(Component.translatable("container.inventory"));
        inventoryLabel.layout(style -> style.height(11).marginTop(1));
        recipeCanvas.addChild(inventoryLabel);

        var inventorySlots = new InventorySlots();
        inventorySlots.layout(style -> style.marginHorizontalAuto());
        recipeCanvas.addChild(inventorySlots);

        var sourceContent = new UIElement();
        sourceContent.layout(style -> style
                .widthPercent(100)
                .heightPercent(100)
                .paddingAll(6)
                .gapAll(3)
        );
        sourceContent.lss("flex-direction", "column");
        sourceContent.addChildren(
                sourceInfoRow("kubejs_recipe_ui.editor.source.id", sourceIdValue),
                sourceInfoRow("kubejs_recipe_ui.editor.source.type", sourceTypeValue),
                sourceInfoRow("kubejs_recipe_ui.editor.source.mod", sourceModValue),
                sourceInfoRow("kubejs_recipe_ui.editor.source.group", sourceGroupValue)
        );

        var codeContent = new UIElement();
        codeContent.layout(style -> style.widthPercent(100).heightPercent(100).paddingAll(4));
        codeContent.addChild(codePreview);

        var bottomTabs = new TabView();
        bottomTabs.layout(style -> style.widthPercent(100).heightPercent(100));
        bottomTabs.addTab(
                new Tab().setText(Component.translatable("kubejs_recipe_ui.editor.tab.code")),
                codeContent
        );
        bottomTabs.addTab(
                new Tab().setText(Component.translatable("kubejs_recipe_ui.editor.tab.source")),
                sourceContent
        );

        var workspaceSplit = new SplitView.Horizontal();
        workspaceSplit.setPercentage(66).left(recipeCanvas).right(inspector);
        workspaceSplit.layout(style -> style.widthPercent(100).heightPercent(100));

        var editorSplit = new SplitView.Vertical();
        editorSplit.setPercentage(78).top(workspaceSplit).bottom(bottomTabs);
        editorSplit.layout(style -> style.widthPercent(100).heightPercent(100));
        main.addChild(editorSplit);

        var workstationButtons = new EnumMap<WorkstationType, Button>(WorkstationType.class);
        for (var type : sidebarTypes) {
            var button = new Button();
            button.setText("");
            button.addPreIcon(new ItemStackTexture(type.icon()));
            button.layout(style -> style.width(20).height(20));
            button.style(style -> style.tooltips(Component.translatable(
                    type == WorkstationType.EXTENDED_BASIC_TABLE
                            ? "kubejs_recipe_ui.workstation.extended_table"
                            : type.translationKey()
            )));
            button.setActive(type != WorkstationType.CRAFTING);
            captureClick(button, "切换工作方块: " + type, event -> {
                var targetType = type == WorkstationType.EXTENDED_BASIC_TABLE
                        ? selectedExtendedTable[0]
                        : type;
                main.setDisplay(true);
                session.setActiveType(targetType);
                title.setText(Component.translatable(targetType.translationKey()));
                showWorkstationPanel(panels, targetType);
                updateWorkstationButtons(workstationButtons, targetType);
                extendedTableSelectorRow.setDisplay(targetType.isExtendedTable());
                if (targetType.isExtendedTable()) {
                    extendedTableSelector.setSelected(extendedTableDisplay.get(targetType));
                }
                modeButton.setDisplay(targetType.supportsShaped());
                modeButton.setText(Component.translatable(session.isShaped()
                        ? "kubejs_recipe_ui.action.shaped"
                        : "kubejs_recipe_ui.action.shapeless"));
            });
            workstationButtons.put(type, button);
            sidebar.addChild(button);
        }

        extendedTableSelector.setOnValueChanged(value -> {
            var type = extendedTableByDisplay.get(value);
            if (type == null) {
                return;
            }
            selectedExtendedTable[0] = type;
            session.setActiveType(type);
            title.setText(Component.translatable(type.translationKey()));
            showWorkstationPanel(panels, type);
            updateWorkstationButtons(workstationButtons, type);
            modeButton.setDisplay(true);
            modeButton.setText(Component.translatable(session.isShaped()
                    ? "kubejs_recipe_ui.action.shaped"
                    : "kubejs_recipe_ui.action.shapeless"));
        });

        // JEI 转移回调同时更新会话和已经挂载的槽位控件。
        var jeiBridge = new RecipeEditorJeiBridge((
                type,
                inputs,
                output,
                sourceInfo,
                shaped,
                craftingWidth,
                craftingHeight,
                experience,
                cookingTime,
                extendedSettings
        ) -> {
            session.setActiveType(type);
            session.setSourceInfo(sourceInfo);
            session.setShaped(shaped);
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
            session.setExtendedSettings(type, extendedSettings);
            if (type.isExtendedTable()) {
                selectedExtendedTable[0] = type;
                extendedTableSelector.setSelected(extendedTableDisplay.get(type));
            }
            extendedTableSelectorRow.setDisplay(type.isExtendedTable());
            updateNumericField(root, extendedFieldId(type, "power_cost"), extendedSettings.powerCost());
            updateNumericField(root, extendedFieldId(type, "power_rate"), extendedSettings.powerRate());
            updateNumericField(root, extendedFieldId(type, "ingredient_count"), extendedSettings.ingredientCount());
            updateNumericField(root, extendedFieldId(type, "crafting_time"), extendedSettings.craftingTime());
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

        // MODERN 主题默认使用仅含拉丁字形的 JetBrains Mono；覆盖字体以正确显示中文翻译。
        root.addLocalStylesheet("""
                text, label,
                text-field:host,
                text-area,
                .__tag-field_text-field__,
                .__search-component_text-field__ {
                  font: minecraft:default;
                }
                """);

        return new ModularUI(
                UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MODERN)),
                holder.player
        );
    }

    /** 只显示当前工作站对应的编辑面板。 */
    private static void showWorkstationPanel(
            EnumMap<WorkstationType, UIElement> panels,
            WorkstationType activeType
    ) {
        panels.forEach((type, panel) -> panel.setDisplay(type == activeType));
    }

    /** 四级扩展工作台共用基础工作台按钮，因此按钮选中状态按工作站族判断。 */
    private static void updateWorkstationButtons(
            EnumMap<WorkstationType, Button> buttons,
            WorkstationType activeType
    ) {
        buttons.forEach((type, button) -> {
            var selected = type == activeType
                    || type == WorkstationType.EXTENDED_BASIC_TABLE && activeType.isExtendedTable();
            button.setActive(!selected);
        });
    }

    /** @return Extended Crafting 工作台等级下拉框使用的简短翻译键 */
    private static String extendedTableLevelTranslationKey(WorkstationType type) {
        return switch (type) {
            case EXTENDED_BASIC_TABLE -> "kubejs_recipe_ui.extended.table_level.basic";
            case EXTENDED_ADVANCED_TABLE -> "kubejs_recipe_ui.extended.table_level.advanced";
            case EXTENDED_ELITE_TABLE -> "kubejs_recipe_ui.extended.table_level.elite";
            case EXTENDED_ULTIMATE_TABLE -> "kubejs_recipe_ui.extended.table_level.ultimate";
            default -> throw new IllegalArgumentException("不是 Extended Crafting 工作台类型: " + type);
        };
    }

    /** 根据工作站类型分派到对应的原版风格面板构建器。 */
    private static UIElement createWorkstationPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session,
            WorkstationType type
    ) {
        return switch (type) {
            case CRAFTING -> createCraftingPanel(root, holder, session);
            case FURNACE, SMOKER, BLAST_FURNACE -> createCookingPanel(root, holder, session, type);
            case CAMPFIRE -> createCampfirePanel(root, holder, session);
            case STONECUTTER -> createSingleInputPanel(root, holder, session, type);
            case SMITHING -> createSmithingPanel(root, holder, session);
            case EXTENDED_BASIC_TABLE, EXTENDED_ADVANCED_TABLE, EXTENDED_ELITE_TABLE, EXTENDED_ULTIMATE_TABLE ->
                    createExtendedTablePanel(root, holder, session, type);
            case EXTENDED_COMBINATION -> createCombinationPanel(root, holder, session);
            case EXTENDED_COMPRESSOR -> createCompressorPanel(root, holder, session);
            case EXTENDED_ENDER_CRAFTER -> createEnderCrafterPanel(root, holder, session);
            case EXTENDED_FLUX_CRAFTER -> createFluxCrafterPanel(root, holder, session);
        };
    }

    /** 创建 3x3 工作台输入网格和输出槽。 */
    private static UIElement createCraftingPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session
    ) {
        var panel = workstationRow(14);
        var grid = createInputGrid(root, holder, session, WorkstationType.CRAFTING, 0, 9, 3, null);
        panel.addChildren(grid, recipeArrow(), createOutputSlot(root, holder, session, WorkstationType.CRAFTING));
        return panel;
    }

    /** 创建 Extended Crafting 的 3x3、5x5、7x7 或 9x9 工作台网格。 */
    private static UIElement createExtendedTablePanel(
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
                createOutputSlot(root, holder, session, type)
        );
        return panel;
    }

    /** 创建组合合成的中心材料、48 个基座材料以及能量参数。 */
    private static UIElement createCombinationPanel(
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
                createOutputSlot(root, holder, session, type)
        );
        panel.addChildren(recipe, createPowerFields(session, type, "kubejs_recipe_ui.extended.power_cost"));
        return panel;
    }

    /** 创建压缩机材料、催化剂、数量和能量参数。 */
    private static UIElement createCompressorPanel(
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
                createOutputSlot(root, holder, session, type)
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
    private static UIElement createEnderCrafterPanel(
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
                createOutputSlot(root, holder, session, type)
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
    private static UIElement createFluxCrafterPanel(
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
                createOutputSlot(root, holder, session, type)
        );
        panel.addChildren(recipe, createPowerFields(session, type, "kubejs_recipe_ui.extended.power_required"));
        return panel;
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
    private static UIElement createCookingPanel(
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
        panel.layout(style -> style.widthPercent(100).height(108));
        panel.lss("flex-direction", "column");

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
        var output = createOutputSlot(root, holder, session, type);
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
    private static UIElement createCampfirePanel(
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
        panel.layout(style -> style.widthPercent(100).height(88));
        panel.lss("flex-direction", "column");

        var recipe = workstationRow(14);
        recipe.addChildren(
                createInputSlot(root, holder, session, type, 0, "kubejs_recipe_ui.slot.ingredient"),
                recipeArrow(),
                createOutputSlot(root, holder, session, type)
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
    private static UIElement createSingleInputPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session,
            WorkstationType type
    ) {
        var panel = workstationRow(14);
        panel.addChildren(
                createInputSlot(root, holder, session, type, 0, "kubejs_recipe_ui.slot.ingredient"),
                recipeArrow(),
                createOutputSlot(root, holder, session, type)
        );
        return panel;
    }

    /** 创建锻造台的模板、基础物品、追加材料和输出槽布局。 */
    private static UIElement createSmithingPanel(
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
                createOutputSlot(root, holder, session, type)
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

    /** 创建支持复制光标物品、清空和数量设置的输出幻影槽。 */
    private static ItemSlot createOutputSlot(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session,
            WorkstationType type
    ) {
        var output = new ItemSlot();
        output.xeiPhantom();
        output.setId(outputSlotId(type));
        output.layout(style -> style.width(SLOT_SIZE).height(SLOT_SIZE));
        output.style(style -> style.tooltips(Component.translatable("kubejs_recipe_ui.slot.output")));
        output.registerValueListener(stack -> session.setOutputStack(type, stack));
        output.bind(DataBindingBuilder.itemStack(
                () -> session.getOutputStack(type),
                stack -> session.setOutputStack(type, stack)
        ).build());
        output.addEventListener("mouseDown", event -> {
            var carried = holder.player.containerMenu.getCarried();
            if (event.button == 0 && !carried.isEmpty()) {
                event.stopImmediatePropagation();
                output.setItem(carried.copy());
                LOGGER.info("[工作台编辑器] {} 从玩家光标复制输出物品: {} x{}", type, carried.getItem(), carried.getCount());
                return;
            }
            if (event.button == 0) {
                event.stopImmediatePropagation();
                output.setItem(ItemStack.EMPTY);
                LOGGER.info("[工作台编辑器] {} 左键清空输出槽位", type);
                return;
            }
            if (event.button != 1) {
                return;
            }
            if (session.getOutputStack(type).isEmpty()) {
                LOGGER.info("[工作台编辑器] {} 右键输出槽位: 空槽位，不打开数量设置", type);
                return;
            }
            event.stopImmediatePropagation();
            showOutputSettings(root, output, session);
        }, true);
        return output;
    }

    /** 创建复用 LDLib2 原版风格纹理的配方箭头。 */
    private static UIElement recipeArrow() {
        var arrow = new UIElement();
        arrow.layout(style -> style.width(24).height(16));
        arrow.style(style -> style.background(Icons.RIGHT_ARROW_NO_BAR));
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
        return button;
    }

    /** 创建控制输入或输出数据组件是否参与生成的 LDLib2 现代开关。 */
    private static Switch componentSwitch(String translationKey, boolean enabled) {
        var control = new Switch();
        control.setOn(enabled, false);
        control.layout(style -> style.width(26).height(14));
        control.style(style -> style.tooltips(Component.translatable(translationKey + ".tooltip")));
        return control;
    }

    /** 将现代开关与可翻译标签组合成属性检查器中的一行。 */
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

    /** 创建现代编辑器属性检查器的分组标题。 */
    private static Label editorSectionLabel(String translationKey) {
        var label = new Label();
        label.setText(Component.translatable(translationKey));
        label.layout(style -> style.widthPercent(100).height(14).marginTop(2));
        label.textStyle(style -> style.textAlignVertical(Vertical.CENTER));
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

    /** 打开输出数量设置弹窗，并同步更新槽位和会话。 */
    private static void showOutputSettings(UIElement root, ItemSlot outputSlot, RecipeEditorSession session) {
        var countField = new TextField();
        countField.setNumbersOnlyInt(1, 99);
        countField.setText(Integer.toString(session.getOutputStack().getCount()));
        countField.layout(style -> style.width(110).height(20));

        var content = new UIElement();
        content.layout(style -> style
                .widthPercent(100)
                .paddingAll(6)
                .gapAll(5)
        );
        content.lss("flex-direction", "column");
        content.addChildren(fieldLabel("kubejs_recipe_ui.output_settings.count"), countField);

        var dialog = createModal("kubejs_recipe_ui.output_settings.title", 220, 125);
        dialog.addContent(content);

        var save = actionButton(Component.translatable("gui.done"), 70);
        captureClick(save, "保存输出数量", event -> {
            try {
                var count = Integer.parseInt(countField.getText());
                session.setOutputCount(count);
                outputSlot.setItem(session.getOutputStack());
                dialog.close();
            } catch (IllegalArgumentException exception) {
                Dialog.showNotification(translate("kubejs_recipe_ui.output_settings.invalid_count"), 3.0F);
            }
        });
        var cancel = actionButton(Component.translatable("gui.cancel"), 70);
        captureClick(cancel, "取消输出数量设置", event -> dialog.close());
        dialog.addButton(save).addButton(cancel).show(root);
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
