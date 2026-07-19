package com.github.jun8848.kubejsrecipeui.block;

import com.github.jun8848.kubejsrecipeui.client.RecipeEditorUi;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 工作台编辑器方块，将玩家交互转交给 LDLib2 的方块 UI 菜单系统。
 */
public final class RecipeEditorBlock extends Block implements BlockUIMenuType.BlockUI {
    public RecipeEditorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** 仅在服务端打开菜单，客户端返回成功结果以完成手部动画和交互确认。 */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && BlockUIMenuType.openUI(serverPlayer, pos)) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** 为每次打开方块创建独立的配方编辑界面和会话。 */
    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return RecipeEditorUi.create(holder);
    }

    /** @return 菜单顶部显示的本地化方块名称 */
    @Override
    public Component getUIDisplayName(BlockUIMenuType.BlockUIHolder holder) {
        return Component.translatable("container.kubejs_recipe_ui.recipe_editor");
    }
}
