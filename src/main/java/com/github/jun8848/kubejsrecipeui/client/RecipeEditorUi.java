package com.github.jun8848.kubejsrecipeui.client;

import com.github.jun8848.kubejsrecipeui.event.EventDescriptor;
import com.github.jun8848.kubejsrecipeui.event.EventMethodCatalog;
import com.github.jun8848.kubejsrecipeui.event.EventMethodOption;
import com.github.jun8848.kubejsrecipeui.event.EventScriptGenerator;
import com.github.jun8848.kubejsrecipeui.event.EventTargetCatalog;
import com.github.jun8848.kubejsrecipeui.event.EventTargetOption;
import com.github.jun8848.kubejsrecipeui.event.EventWriteService;
import com.github.jun8848.kubejsrecipeui.event.KubeJsEventCatalog;
import com.github.jun8848.kubejsrecipeui.recipe.IngredientSpec;
import com.github.jun8848.kubejsrecipeui.recipe.KubeJsRecipeGenerator;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeDeleteMode;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeSourceInfo;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeWriteService;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeWriteMode;
import com.github.jun8848.kubejsrecipeui.recipe.WorkstationType;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
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
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.Languages;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;

public final class RecipeEditorUi {
    private static final float SLOT_SIZE = 18.0F;
    private static final Logger LOGGER = LogUtils.getLogger();

    private RecipeEditorUi() {
    }

    public static ModularUI create(BlockUIMenuType.BlockUIHolder holder) {
        var session = new RecipeEditorSession(holder.player);
        var root = new UIElement();
        root.layout(style -> style
                .width(290)
                .height(445)
                .gapAll(4)
        );
        root.lss("flex-direction", "row");

        var sidebar = new UIElement();
        sidebar.layout(style -> style
                .width(28)
                .heightPercent(100)
                .paddingAll(4)
                .gapAll(4)
        );
        sidebar.lss("flex-direction", "column");
        sidebar.lss("align-items", "center");
        sidebar.addClass("panel_bg");

        var main = new UIElement();
        main.layout(style -> style
                .width(258)
                .height(445)
                .paddingAll(9)
                .gapAll(5)
        );
        main.lss("flex-direction", "column");
        main.addClass("panel_bg");

        var title = new Label();
        title.setText(Component.translatable(WorkstationType.CRAFTING.translationKey()));
        title.layout(style -> style.height(16));
        main.addChild(title);

        var writeMode = new RecipeWriteMode[]{RecipeWriteMode.ADD_ONLY};
        var writeModeButton = actionButton(Component.translatable(writeMode[0].translationKey()), 88);
        captureClick(writeModeButton, "切换配方写入方式", event -> {
            writeMode[0] = writeMode[0].next();
            writeModeButton.setText(Component.translatable(writeMode[0].translationKey()));
        });

        var inputComponents = componentToggle("kubejs_recipe_ui.recipe_components.input", true, 72);
        var outputComponents = componentToggle("kubejs_recipe_ui.recipe_components.output", true, 72);
        var recipeOptions = new UIElement();
        recipeOptions.layout(style -> style.widthPercent(100).height(20).gapAll(4));
        recipeOptions.lss("flex-direction", "row");
        recipeOptions.addChildren(writeModeButton, inputComponents, outputComponents);
        main.addChild(recipeOptions);

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
        deleteModeSelector.layout(style -> style.width(92).height(20));
        deleteModeSelector.setOnValueChanged(value -> {
            var selected = deleteModeByDisplay.get(value);
            if (selected != null) {
                deleteMode[0] = selected;
            }
        });

        var recipeIdField = new TextField();
        recipeIdField.setAnyString();
        recipeIdField.layout(style -> style.flex(1).height(20));
        recipeIdField.textFieldStyle(style -> style.placeholder(
                Component.translatable("kubejs_recipe_ui.jei.recipe_id.placeholder")
        ));
        recipeIdField.registerValueListener(session::setRecipeId);
        var deleteOptions = new UIElement();
        deleteOptions.layout(style -> style.widthPercent(100).height(20).gapAll(4));
        deleteOptions.lss("flex-direction", "row");
        deleteOptions.addChildren(deleteModeSelector, recipeIdField);
        main.addChild(deleteOptions);

        var actions = new UIElement();
        actions.layout(style -> style
                .height(22)
                .widthPercent(100)
                .gapAll(4)
        );
        actions.lss("flex-direction", "row");

        var modeButton = actionButton(Component.translatable("kubejs_recipe_ui.action.shaped"), 74);
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

        var codePreview = new CodeEditor();
        codePreview.setLanguage(Languages.JAVASCRIPT);
        codePreview.setValue(new String[0], false);
        codePreview.layout(style -> style.widthPercent(100).height(82));

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
                Dialog.showNotification(translate("kubejs_recipe_ui.message.preview_required"), 3.0F);
                return;
            }
            writeRpc.send(
                    result -> Dialog.showNotification(translate((String) result), 2.5F),
                    code
            );
        });

        actions.addChildren(modeButton, previewButton, writeButton);
        main.addChild(actions);

        var editorArea = new UIElement();
        editorArea.layout(style -> style.widthPercent(100).height(112));
        editorArea.lss("flex-direction", "column");
        var panels = new EnumMap<WorkstationType, UIElement>(WorkstationType.class);
        for (var type : WorkstationType.values()) {
            var panel = createWorkstationPanel(root, holder, session, type);
            panel.setDisplay(type == WorkstationType.CRAFTING);
            panels.put(type, panel);
            editorArea.addChild(panel);
        }
        main.addChild(editorArea);

        var previewLabel = new Label();
        previewLabel.setText(Component.translatable("kubejs_recipe_ui.preview.inline_title"));
        previewLabel.layout(style -> style.height(11));
        main.addChildren(previewLabel, codePreview);

        var inventoryLabel = new Label();
        inventoryLabel.setText(Component.translatable("container.inventory"));
        inventoryLabel.layout(style -> style.height(11).marginTop(1));
        main.addChild(inventoryLabel);

        var inventorySlots = new InventorySlots();
        inventorySlots.layout(style -> style.marginHorizontalAuto());
        main.addChild(inventorySlots);

        var eventMain = createEventEditorPanel(holder);
        eventMain.setDisplay(false);
        var eventButtonReference = new Button[1];

        var workstationButtons = new EnumMap<WorkstationType, Button>(WorkstationType.class);
        for (var type : WorkstationType.values()) {
            var button = new Button();
            button.setText("");
            button.addPreIcon(new ItemStackTexture(type.icon()));
            button.layout(style -> style.width(20).height(20));
            button.style(style -> style.tooltips(Component.translatable(type.translationKey())));
            button.setActive(type != WorkstationType.CRAFTING);
            captureClick(button, "切换工作方块: " + type, event -> {
                main.setDisplay(true);
                eventMain.setDisplay(false);
                if (eventButtonReference[0] != null) {
                    eventButtonReference[0].setActive(true);
                }
                session.setActiveType(type);
                title.setText(Component.translatable(type.translationKey()));
                panels.forEach((panelType, panel) -> panel.setDisplay(panelType == type));
                workstationButtons.forEach((buttonType, other) -> other.setActive(buttonType != type));
                modeButton.setDisplay(type == WorkstationType.CRAFTING);
            });
            workstationButtons.put(type, button);
            sidebar.addChild(button);
        }

        var jeiBridge = new RecipeEditorJeiBridge((
                type,
                inputs,
                output,
                sourceInfo,
                shaped,
                craftingWidth,
                craftingHeight,
                experience,
                cookingTime
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
            for (var index = 0; index < type.inputCount(); index++) {
                session.setInputStack(type, index, ItemStack.EMPTY);
            }

            if (type == WorkstationType.CRAFTING && shaped && inputs.size() != 9) {
                for (var index = 0; index < inputs.size() && index < craftingWidth * craftingHeight; index++) {
                    var row = index / Math.max(1, craftingWidth);
                    var column = index % Math.max(1, craftingWidth);
                    var target = row * 3 + column;
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
            main.setDisplay(true);
            eventMain.setDisplay(false);
            if (eventButtonReference[0] != null) {
                eventButtonReference[0].setActive(true);
            }
            title.setText(Component.translatable(type.translationKey()));
            panels.forEach((panelType, panel) -> panel.setDisplay(panelType == type));
            workstationButtons.forEach((buttonType, button) -> button.setActive(buttonType != type));
            modeButton.setDisplay(type == WorkstationType.CRAFTING);
            modeButton.setText(Component.translatable(shaped
                    ? "kubejs_recipe_ui.action.shaped"
                    : "kubejs_recipe_ui.action.shapeless"));
            Dialog.showNotification(translate("kubejs_recipe_ui.jei.transfer.success"), 2.5F);
            LOGGER.info("[工作台编辑器] JEI 配方已填充: 类型={}, ID={}", type, sourceInfo.id());
        });
        root.addChild(jeiBridge);

        var eventButton = new Button();
        eventButton.setText("");
        eventButton.addPreIcon(new ItemStackTexture(Items.COMMAND_BLOCK));
        eventButton.layout(style -> style.width(20).height(20));
        eventButton.style(style -> style.tooltips(Component.translatable("kubejs_recipe_ui.event.editor.title")));
        eventButtonReference[0] = eventButton;
        captureClick(eventButton, "切换到事件编辑器", event -> {
            main.setDisplay(false);
            eventMain.setDisplay(true);
            workstationButtons.values().forEach(button -> button.setActive(true));
            eventButton.setActive(false);
        });
        sidebar.addChild(eventButton);

        root.addChildren(sidebar, main, eventMain);

        return new ModularUI(
                UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC)),
                holder.player
        );
    }

    private static UIElement createWorkstationPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session,
            WorkstationType type
    ) {
        return switch (type) {
            case CRAFTING -> createCraftingPanel(root, holder, session);
            case FURNACE, SMOKER, BLAST_FURNACE -> createCookingPanel(root, holder, session, type);
            case STONECUTTER -> createSingleInputPanel(root, holder, session, type);
            case SMITHING -> createSmithingPanel(root, holder, session);
        };
    }

    private static UIElement createCraftingPanel(
            UIElement root,
            BlockUIMenuType.BlockUIHolder holder,
            RecipeEditorSession session
    ) {
        var panel = workstationRow(14);
        var grid = new UIElement();
        grid.layout(style -> style.width(58).height(58).gapAll(2));
        grid.lss("flex-direction", "row");
        grid.lss("flex-wrap", "wrap");
        for (var index = 0; index < 9; index++) {
            grid.addChild(createInputSlot(root, holder, session, WorkstationType.CRAFTING, index, null));
        }
        panel.addChildren(grid, recipeArrow(), createOutputSlot(root, holder, session, WorkstationType.CRAFTING));
        return panel;
    }

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

    private static UIElement workstationRow(float gap) {
        var panel = new UIElement();
        panel.layout(style -> style.widthPercent(100).height(62).gapAll(gap));
        panel.lss("flex-direction", "row");
        panel.lss("align-items", "center");
        panel.lss("justify-content", "center");
        return panel;
    }

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

    private static UIElement recipeArrow() {
        var arrow = new UIElement();
        arrow.layout(style -> style.width(24).height(16));
        arrow.style(style -> style.background(Icons.RIGHT_ARROW_NO_BAR));
        return arrow;
    }

    private static String inputSlotId(WorkstationType type, int slotIndex) {
        return "kubejs_recipe_ui#input_" + type.name().toLowerCase(java.util.Locale.ROOT) + "_" + slotIndex;
    }

    private static String outputSlotId(WorkstationType type) {
        return "kubejs_recipe_ui#output_" + type.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static String cookingExperienceId(WorkstationType type) {
        return "kubejs_recipe_ui#experience_" + type.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static String cookingTimeId(WorkstationType type) {
        return "kubejs_recipe_ui#cooking_time_" + type.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static Label symbol(String text) {
        var label = new Label();
        label.setText(text);
        label.layout(style -> style.width(8).height(10));
        return label;
    }

    private static UIElement labeledField(String key, TextField field) {
        var row = new UIElement();
        row.layout(style -> style.widthPercent(100).height(20).gapAll(3));
        row.lss("flex-direction", "row");
        row.lss("align-items", "center");
        var label = fieldLabel(key);
        label.layout(style -> style.width(38).height(10));
        row.addChildren(label, field);
        return row;
    }

    private static TextField numericField(String value, float min, float max, float width) {
        var field = new TextField();
        field.setNumbersOnlyFloat(min, max);
        field.setText(value);
        field.layout(style -> style.width(width).height(20));
        return field;
    }

    private static TextField numericField(String value, int min, int max, float width) {
        var field = new TextField();
        field.setNumbersOnlyInt(min, max);
        field.setText(value);
        field.layout(style -> style.width(width).height(20));
        return field;
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Button actionButton(Component text, float width) {
        var button = new Button();
        button.setText(text);
        button.layout(style -> style.width(width).height(20));
        return button;
    }

    private static Toggle componentToggle(String translationKey, boolean enabled, float width) {
        var toggle = new Toggle();
        toggle.toggleLabel.setText(Component.translatable(translationKey));
        toggle.setOn(enabled, false);
        toggle.layout(style -> style.width(width).height(18));
        toggle.style(style -> style.tooltips(Component.translatable(translationKey + ".tooltip")));
        return toggle;
    }

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

    private static String selectedTag(Selector<String> tagSelector) {
        var tag = tagSelector.getValue();
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException(translate("kubejs_recipe_ui.settings.no_tags"));
        }
        return tag;
    }

    private static Label fieldLabel(String translationKey, Object... arguments) {
        var label = new Label();
        label.setText(Component.translatable(translationKey, arguments));
        label.layout(style -> style.height(10));
        return label;
    }

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

    private static Component modeText(IngredientSpec.Kind kind) {
        return Component.translatable(switch (kind) {
            case ITEM -> "kubejs_recipe_ui.settings.mode_item";
            case TAG -> "kubejs_recipe_ui.settings.mode_tag";
            case ALTERNATIVES -> "kubejs_recipe_ui.settings.mode_alternatives";
        });
    }

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

    private static String alternativeText(IngredientSpec specification) {
        return specification.kind() == IngredientSpec.Kind.TAG
                ? "#" + specification.value()
                : specification.value() + specification.componentPatch();
    }

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

    private static UIElement createEventEditorPanel(BlockUIMenuType.BlockUIHolder holder) {
        var main = new UIElement();
        main.layout(style -> style
                .width(258)
                .height(445)
                .paddingAll(9)
                .gapAll(1)
        );
        main.lss("flex-direction", "column");
        main.addClass("panel_bg");

        var title = new Label();
        title.setText(Component.translatable("kubejs_recipe_ui.event.editor.title"));
        title.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
        title.layout(style -> style.height(16));
        main.addChild(title);

        var events = KubeJsEventCatalog.all();
        if (events.isEmpty()) {
            var empty = new Label();
            empty.setText(Component.translatable("kubejs_recipe_ui.event.message.empty"));
            empty.layout(style -> style.widthPercent(100).height(20));
            main.addChild(empty);
            return main;
        }

        var eventByDisplay = new HashMap<String, EventDescriptor>();
        var eventCandidates = events.stream().map(event -> {
            var display = eventDisplay(event);
            eventByDisplay.put(display, event);
            return display;
        }).toList();
        var selectedEvent = new EventDescriptor[]{events.getFirst()};
        var selectedType = new ScriptType[]{events.getFirst().scriptTypes().getFirst()};
        var selectedMethod = new EventMethodOption[1];

        var eventSelector = new Selector<String>();
        eventSelector.dialog.style(style -> style.zIndex(200));
        eventSelector.setCandidates(eventCandidates).setSelected(eventCandidates.getFirst());
        eventSelector.setCandidateUIProvider(RecipeEditorUi::slowRollingLabel);
        eventSelector.layout(style -> style.widthPercent(100).height(20));

        var typeByDisplay = new HashMap<String, ScriptType>();
        var typeSelector = new Selector<String>();
        typeSelector.dialog.style(style -> style.zIndex(200));
        typeSelector.layout(style -> style.widthPercent(100).height(20));

        var methodSelector = new Selector<EventMethodOption>();
        methodSelector.dialog.style(style -> style.zIndex(200));
        methodSelector.setCandidateUIProvider(method -> slowRollingLabel(
                method == null ? "" : eventMethodDisplay(method)
        ));
        methodSelector.layout(style -> style.widthPercent(100).height(20));

        var targetLabel = fieldLabel("kubejs_recipe_ui.event.target");
        var targetField = new TextField();
        targetField.setAnyString();
        targetField.layout(style -> style.widthPercent(100).height(20));
        targetField.textFieldStyle(style -> style.placeholder(
                Component.translatable("kubejs_recipe_ui.event.target.placeholder")
        ));

        var targetSelector = new Selector<EventTargetOption>();
        targetSelector.dialog.style(style -> style.zIndex(200));
        targetSelector.setCandidateUIProvider(option -> slowRollingLabel(
                option == null ? "" : option.displayText()
        ));
        targetSelector.layout(style -> style.widthPercent(100).height(20));
        var targetCandidates = new java.util.concurrent.atomic.AtomicReference<>(
                java.util.List.<EventTargetOption>of()
        );
        targetSelector.setOnValueChanged(option -> {
            if (option != null) {
                targetField.setText(option.id());
            }
        });

        var targetRow = new UIElement();
        targetRow.layout(style -> style.widthPercent(100).height(42).gapAll(2));
        targetRow.lss("flex-direction", "column");
        targetRow.addChildren(targetField, targetSelector);

        var editor = new CodeEditor();
        editor.setLanguage(Languages.JAVASCRIPT);
        editor.layout(style -> style.widthPercent(100).height(190));

        Runnable updateCode = () -> {
            try {
                var code = EventScriptGenerator.generate(
                        selectedEvent[0],
                        selectedType[0],
                        targetField.getText(),
                        selectedMethod[0]
                );
                editor.setValue(code.split("\\R", -1), false);
            } catch (IllegalArgumentException exception) {
                Dialog.showNotification(exception.getMessage(), 3.0F);
            }
        };
        java.util.function.Consumer<EventDescriptor> updateEvent = event -> {
            selectedEvent[0] = event;
            typeByDisplay.clear();
            var candidates = event.scriptTypes().stream().map(type -> {
                var display = scriptTypeText(type);
                typeByDisplay.put(display, type);
                return display;
            }).toList();
            selectedType[0] = event.scriptTypes().getFirst();
            typeSelector.setCandidates(candidates).setSelected(candidates.getFirst());
            var methods = new ArrayList<EventMethodOption>();
            methods.add(EventMethodOption.NONE);
            methods.addAll(EventMethodCatalog.candidates(event));
            selectedMethod[0] = null;
            methodSelector.setCandidates(methods).setSelected(EventMethodOption.NONE, false);
            var targets = EventTargetCatalog.candidates(event.targetKind(), holder.player.registryAccess());
            targetCandidates.set(targets);
            var visibleTargets = filterTargetCandidates(targets, "");
            targetSelector.setCandidates(visibleTargets);
            targetSelector.setDisplay(event.supportsTarget() && !targets.isEmpty());
            targetLabel.setDisplay(event.supportsTarget());
            targetRow.setDisplay(event.supportsTarget());
            var initialTarget = event.targetRequired() && !targets.isEmpty()
                    ? targets.getFirst().id()
                    : event.targetRequired() ? "minecraft:target" : "";
            targetField.setText(initialTarget);
            if (!visibleTargets.isEmpty()) {
                targetSelector.setSelected(visibleTargets.getFirst(), false);
            }
            updateCode.run();
        };
        eventSelector.setOnValueChanged(value -> {
            var event = eventByDisplay.get(value);
            if (event != null) {
                updateEvent.accept(event);
            }
        });
        typeSelector.setOnValueChanged(value -> {
            var type = typeByDisplay.get(value);
            if (type != null) {
                selectedType[0] = type;
                updateCode.run();
            }
        });
        methodSelector.setOnValueChanged(method -> {
            if (method != null) {
                selectedMethod[0] = method == EventMethodOption.NONE ? null : method;
                updateCode.run();
            }
        });
        targetField.registerValueListener(value -> {
            var candidates = filterTargetCandidates(targetCandidates.get(), value);
            targetSelector.setCandidates(candidates);
            if (!candidates.isEmpty()) {
                targetSelector.setSelected(candidates.getFirst(), false);
            }
            if (!selectedEvent[0].targetRequired() || !value.isBlank()) {
                updateCode.run();
            }
        });
        updateEvent.accept(selectedEvent[0]);

        main.addChildren(
                fieldLabel("kubejs_recipe_ui.event.select"), eventSelector,
                fieldLabel("kubejs_recipe_ui.event.script_type"), typeSelector,
                targetLabel, targetRow,
                fieldLabel("kubejs_recipe_ui.event.method"), methodSelector
        );

        var actions = new UIElement();
        actions.layout(style -> style.widthPercent(100).height(22).gapAll(4));
        actions.lss("flex-direction", "row");
        var regenerate = actionButton(Component.translatable("kubejs_recipe_ui.event.action.regenerate"), 70);
        captureClick(regenerate, "重新生成事件骨架", event -> updateCode.run());
        var copy = actionButton(Component.translatable("kubejs_recipe_ui.action.copy"), 60);
        captureClick(copy, "复制事件代码", event -> ClipboardManager.INSTANCE.copyDirect(editorCode(editor)));
        var write = actionButton(Component.translatable("kubejs_recipe_ui.action.write"), 60);
        var writeRpc = write.addRPCEvent(RPCEventBuilder.simple(
                String.class,
                String.class,
                payload -> holder.player instanceof ServerPlayer serverPlayer
                        ? EventWriteService.write(serverPlayer, payload)
                        : EventWriteService.FAILED
        ));
        captureClick(write, "写入事件脚本", event -> {
            var payload = selectedType[0].name() + "\n" + editorCode(editor);
            writeRpc.send(result -> Dialog.showNotification(translate((String) result), 2.5F), payload);
        });
        actions.addChildren(regenerate, copy, write);

        var previewLabel = new Label();
        previewLabel.setText(Component.translatable("kubejs_recipe_ui.preview.inline_title"));
        previewLabel.layout(style -> style.height(11));
        main.addChildren(actions, previewLabel, editor);
        return main;
    }

    private static String editorCode(CodeEditor editor) {
        return String.join("\n", editor.getValue());
    }

    private static String eventDisplay(EventDescriptor event) {
        var groupKey = "kubejs_recipe_ui.event.group." + event.group();
        var nameKey = "kubejs_recipe_ui.event.name." + event.name();
        var group = translate(groupKey);
        var name = translate(nameKey);
        group = group.equals(groupKey) ? event.group() : group;
        name = name.equals(nameKey) ? translateEventName(event.name()) : name;
        group = group.replaceFirst("(?i)\\s*events?$", "").replaceFirst("事件$", "");
        return Component.translatable(
                "kubejs_recipe_ui.event.display",
                event.id(),
                group + name
        ).getString();
    }

    private static String eventMethodDisplay(EventMethodOption method) {
        if (method == EventMethodOption.NONE) {
            return translate("kubejs_recipe_ui.event.method.none");
        }
        var key = "kubejs_recipe_ui.event.method." + method.name();
        var translated = translate(key);
        if (translated.equals(key)) {
            translated = translateMethodName(method.name());
        }
        return method.displayText(translated);
    }

    private static String translateMethodName(String name) {
        var translated = switch (name) {
            case "cancel" -> "取消事件";
            case "success" -> "成功结束事件";
            case "exit" -> "结束事件";
            case "getBlock" -> "获取方块";
            case "getEntity" -> "获取实体";
            case "getPlayer" -> "获取玩家";
            case "getLevel" -> "获取世界";
            case "getItem" -> "获取物品";
            case "getFacing" -> "获取方向";
            case "getHand" -> "获取交互手";
            case "getXp" -> "获取经验";
            case "setXp" -> "设置经验";
            case "getItems" -> "获取物品列表";
            case "addItem" -> "添加物品";
            case "removeItem" -> "移除物品";
            case "containsItem" -> "检查物品";
            default -> null;
        };
        if (translated != null) {
            return translated;
        }
        for (var prefix : new String[][]{
                {"get", "获取"},
                {"set", "设置"},
                {"add", "添加"},
                {"remove", "移除"},
                {"modify", "修改"},
                {"replace", "替换"},
                {"contains", "检查是否包含"},
                {"is", "是否"},
                {"has", "是否拥有"},
                {"can", "是否可以"}
        }) {
            if (name.startsWith(prefix[0]) && name.length() > prefix[0].length()) {
                return prefix[1] + translateEventName(name.substring(prefix[0].length()));
            }
        }
        return humanizeEventName(name);
    }

    private static String humanizeEventName(String name) {
        var spaced = name.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        if (spaced.isEmpty()) {
            return name;
        }
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private static String translateEventName(String name) {
        var words = name.replaceAll("([a-z0-9])([A-Z])", "$1 $2").split("\\s+");
        var translated = new StringBuilder();
        for (var word : words) {
            // 第三方事件没有语言条目时，按事件名中的常见英文词语生成中文兜底名称。
            var part = switch (word.toLowerCase(java.util.Locale.ROOT)) {
                case "add", "added" -> "添加";
                case "advancement" -> "进度";
                case "after" -> "后";
                case "assets" -> "资源";
                case "atlas" -> "图集";
                case "basic" -> "基础";
                case "before" -> "前";
                case "block" -> "方块";
                case "broken" -> "破坏";
                case "can" -> "可";
                case "categories" -> "分类";
                case "changed" -> "变化";
                case "chat" -> "聊天";
                case "check" -> "检查";
                case "chest" -> "箱子";
                case "clicked" -> "点击";
                case "closed" -> "关闭";
                case "command" -> "命令";
                case "completely" -> "彻底";
                case "creative" -> "创造模式";
                case "data" -> "数据";
                case "death" -> "死亡";
                case "debug" -> "调试";
                case "decorate" -> "装饰";
                case "destroyed" -> "损坏";
                case "detector" -> "探测器";
                case "dropped", "drops" -> "掉落";
                case "dynamic" -> "动态";
                case "eaten" -> "食用";
                case "entity" -> "实体";
                case "entries" -> "条目";
                case "explosion" -> "爆炸";
                case "falling" -> "下落";
                case "farmland" -> "农田";
                case "first" -> "首次";
                case "food" -> "食物";
                case "generate" -> "生成";
                case "group" -> "分组";
                case "highlight" -> "高亮";
                case "hurt" -> "受伤";
                case "in" -> "登录";
                case "info", "information" -> "信息";
                case "ingredient" -> "原料";
                case "interacted" -> "交互";
                case "inventory" -> "背包";
                case "lang" -> "语言文件";
                case "left" -> "左键";
                case "loaded" -> "加载";
                case "logged" -> "";
                case "mapping" -> "映射";
                case "menu" -> "菜单";
                case "model" -> "模型";
                case "modification", "modify" -> "修改";
                case "opened" -> "打开";
                case "out" -> "退出";
                case "particle" -> "粒子";
                case "pick", "picked" -> "拾取";
                case "placed" -> "放置";
                case "post" -> "后";
                case "powered" -> "通电";
                case "pressed" -> "按下";
                case "properties" -> "属性";
                case "provider" -> "提供器";
                case "public" -> "公开";
                case "random" -> "随机";
                case "received" -> "接收";
                case "recipe", "recipes" -> "配方";
                case "register", "registry" -> "注册";
                case "released" -> "松开";
                case "remove", "removed" -> "移除";
                case "renderer" -> "渲染器";
                case "respawned" -> "重生";
                case "result" -> "结果";
                case "right" -> "右键";
                case "saved" -> "保存";
                case "schema" -> "架构";
                case "screen" -> "界面";
                case "serializers" -> "序列化器";
                case "smelted" -> "烧炼";
                case "spawn", "spawned" -> "生成";
                case "special" -> "特殊";
                case "sprite" -> "纹理";
                case "stage" -> "游戏阶段";
                case "started" -> "开始";
                case "stopped" -> "停止";
                case "subtypes" -> "子类型";
                case "tab" -> "物品栏";
                case "tags" -> "标签";
                case "tick" -> "刻更新";
                case "tier" -> "等级";
                case "tool" -> "工具";
                case "tooltips" -> "提示文本";
                case "trampled" -> "被踩踏";
                case "unloaded" -> "卸载";
                case "unpowered" -> "断电";
                case "up" -> "";
                default -> null;
            };
            if (part == null) {
                return humanizeEventName(name);
            }
            translated.append(part);
        }
        return translated.isEmpty() ? humanizeEventName(name) : translated.toString();
    }

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

    private static java.util.List<EventTargetOption> filterTargetCandidates(
            java.util.List<EventTargetOption> candidates,
            String query
    ) {
        var normalized = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> normalized.isEmpty()
                        || candidate.id().toLowerCase(java.util.Locale.ROOT).contains(normalized)
                        || candidate.name().toLowerCase(java.util.Locale.ROOT).contains(normalized))
                .limit(200)
                .toList();
    }

    private static String scriptTypeText(ScriptType type) {
        return translate("kubejs_recipe_ui.event.script_type." + type.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static void generateRecipe(
            RecipeEditorSession session,
            RecipeWriteMode writeMode,
            RecipeDeleteMode deleteMode,
            boolean includeInputComponents,
            boolean includeOutputComponents,
            java.util.function.Consumer<String> consumer
    ) {
        try {
            var deleteValue = session.createDeleteValue(
                    deleteMode,
                    includeInputComponents,
                    includeOutputComponents
            );
            var code = writeMode == RecipeWriteMode.REMOVE_ONLY
                    ? KubeJsRecipeGenerator.generateRemove(deleteMode, deleteValue)
                    : KubeJsRecipeGenerator.generate(
                            session.createDefinition(includeInputComponents, includeOutputComponents),
                            writeMode,
                            deleteMode,
                            deleteValue
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
            Dialog.showNotification(exception.getMessage(), 3.0F);
        }
    }

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

    private static String translate(String translationKey) {
        return Component.translatable(translationKey).getString();
    }

}
