package com.github.jun8848.kubejsrecipeui.client;

import com.github.jun8848.kubejsrecipeui.recipe.IngredientSpec;
import com.github.jun8848.kubejsrecipeui.recipe.KubeJsRecipeGenerator;
import com.github.jun8848.kubejsrecipeui.recipe.RecipeWriteService;
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
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.Languages;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;

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
                .height(222)
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
                .height(222)
                .paddingAll(9)
                .gapAll(5)
        );
        main.lss("flex-direction", "column");
        main.addClass("panel_bg");

        var title = new Label();
        title.setText(Component.translatable(WorkstationType.CRAFTING.translationKey()));
        title.layout(style -> style.height(12));
        main.addChild(title);

        var editorArea = new UIElement();
        editorArea.layout(style -> style.widthPercent(100).height(62));
        editorArea.lss("flex-direction", "column");
        var panels = new EnumMap<WorkstationType, UIElement>(WorkstationType.class);
        for (var type : WorkstationType.values()) {
            var panel = createWorkstationPanel(root, holder, session, type);
            panel.setDisplay(type == WorkstationType.CRAFTING);
            panels.put(type, panel);
            editorArea.addChild(panel);
        }
        main.addChild(editorArea);

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
        captureClick(previewButton, "生成预览", event -> generate(session, code -> showPreview(root, code)));

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
        captureClick(writeButton, "写入脚本", event -> generate(session, code -> writeRpc.send(
                result -> Dialog.showNotification(translate((String) result), 2.5F),
                code
        )));

        actions.addChildren(modeButton, previewButton, writeButton);
        main.addChild(actions);

        var inventoryLabel = new Label();
        inventoryLabel.setText(Component.translatable("container.inventory"));
        inventoryLabel.layout(style -> style.height(11).marginTop(1));
        main.addChild(inventoryLabel);

        var inventorySlots = new InventorySlots();
        inventorySlots.layout(style -> style.marginHorizontalAuto());
        main.addChild(inventorySlots);

        var workstationButtons = new EnumMap<WorkstationType, Button>(WorkstationType.class);
        for (var type : WorkstationType.values()) {
            var button = new Button();
            button.setText("");
            button.addPreIcon(new ItemStackTexture(type.icon()));
            button.layout(style -> style.width(20).height(20));
            button.style(style -> style.tooltips(Component.translatable(type.translationKey())));
            button.setActive(type != WorkstationType.CRAFTING);
            captureClick(button, "切换工作方块: " + type, event -> {
                session.setActiveType(type);
                title.setText(Component.translatable(type.translationKey()));
                panels.forEach((panelType, panel) -> panel.setDisplay(panelType == type));
                workstationButtons.forEach((buttonType, other) -> other.setActive(buttonType != type));
                modeButton.setDisplay(type == WorkstationType.CRAFTING);
            });
            workstationButtons.put(type, button);
            sidebar.addChild(button);
        }

        root.addChildren(sidebar, main);

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
        var panel = workstationRow(8);
        var values = new UIElement();
        values.layout(style -> style.width(92).height(48).gapAll(4));
        values.lss("flex-direction", "column");

        var experience = numericField(Float.toString(session.getExperience(type)), 0.0F, 10000.0F, 48);
        experience.registerValueListener(value -> session.setExperience(type, parseFloat(value, 0.0F)));
        var cookingTime = numericField(Integer.toString(session.getCookingTime(type)), 1, 72000, 48);
        cookingTime.registerValueListener(value -> session.setCookingTime(type, parseInt(value, 200)));

        values.addChildren(
                labeledField("kubejs_recipe_ui.cooking.experience", experience),
                labeledField("kubejs_recipe_ui.cooking.time", cookingTime)
        );
        panel.addChildren(
                createInputSlot(root, holder, session, type, 0, "kubejs_recipe_ui.slot.ingredient"),
                recipeArrow(),
                createOutputSlot(root, holder, session, type),
                values
        );
        return panel;
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

    private static void showPreview(UIElement root, String code) {
        var editor = new CodeEditor();
        editor.setLanguage(Languages.JAVASCRIPT);
        editor.setValue(code.split("\\R", -1), false);
        editor.layout(style -> style.widthPercent(100).heightPercent(100));

        var dialog = createModal("kubejs_recipe_ui.preview.title", 430, 285);
        dialog.addContent(editor);

        var copy = actionButton(Component.translatable("kubejs_recipe_ui.action.copy"), 80);
        copy.addPreIcon(Icons.COPY);
        captureClick(copy, "复制生成代码", event -> {
            ClipboardManager.INSTANCE.copyDirect(code);
            Dialog.showNotification(translate("kubejs_recipe_ui.message.copied"), 2.0F);
        });
        var close = actionButton(Component.translatable("gui.done"), 80);
        captureClick(close, "关闭代码预览", event -> dialog.close());
        dialog.addButton(copy).addButton(close).show(root);
    }

    private static void generate(RecipeEditorSession session, java.util.function.Consumer<String> consumer) {
        try {
            var definition = session.createDefinition();
            var code = KubeJsRecipeGenerator.generate(definition);
            LOGGER.info(
                    "[工作台编辑器] 代码生成成功: 类型={}, 工作台模式={}, 已用原料槽位={}, 代码长度={}",
                    definition.type(),
                    definition.type() == WorkstationType.CRAFTING
                            ? (definition.shaped() ? "有序" : "无序")
                            : "不适用",
                    definition.inputs().stream().filter(java.util.Objects::nonNull).count(),
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
