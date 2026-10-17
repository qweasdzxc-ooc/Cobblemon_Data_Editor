package com.xiaoyu.cobblemoneditor.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class EditorHubScreen : Screen(Component.literal("Cobblemon Data Editor")) {

    override fun init() {
        val cx = width / 2 - 120
        val w = 240
        addRenderableWidget(Button.builder(Component.literal("精灵 · 编辑（种族值/属性/特性）")) { Minecraft.getInstance().setScreen(EditorScreen()) }.bounds(cx, 52, w, 22).build())
        addRenderableWidget(Button.builder(Component.literal("精灵 · 新建")) { Minecraft.getInstance().setScreen(NewSpeciesScreen()) }.bounds(cx, 80, w, 22).build())
        addRenderableWidget(Button.builder(Component.literal("招式 · 编辑器")) { Minecraft.getInstance().setScreen(MoveEditorScreen()) }.bounds(cx, 108, w, 22).build())
        addRenderableWidget(Button.builder(Component.literal("特性 · 编辑器")) { Minecraft.getInstance().setScreen(AbilityEditorScreen()) }.bounds(cx, 136, w, 22).build())
        addRenderableWidget(Button.builder(Component.literal("关闭")) { onClose() }.bounds(cx, 164, w, 22).build())
    }

    override fun render(g: GuiGraphics, mx: Int, my: Int, p: Float) {
        g.fill(0, 0, width, height, 0xC0101010.toInt())
        super.render(g, mx, my, p)
        g.drawCenteredString(this.font, this.title, width / 2, 26, 0xFFFFFFFF.toInt())
        g.drawCenteredString(this.font, "选择要编辑的内容（保存后重进世界生效）", width / 2, 40, 0xFFAAAAAA.toInt())
    }

    override fun isPauseScreen(): Boolean = false
}
