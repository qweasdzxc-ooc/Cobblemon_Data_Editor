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
        addRenderableWidget(Button.builder(Component.literal("精灵 · 编辑（种族值/属性/特性）")) { Minecraft.getInstance().setScreen(EditorScreen()) }.bounds(cx, 48, w, 22).build())
        addRenderableWidget(Button.builder(Component.literal("精灵 · 新建")) { Minecraft.getInstance().setScreen(NewSpeciesScreen()) }.bounds(cx, 74, w, 22).build())
        addRenderableWidget(Button.builder(Component.literal("招式 · 编辑器")) { Minecraft.getInstance().setScreen(MoveEditorScreen()) }.bounds(cx, 100, w, 22).build())
        addRenderableWidget(Button.builder(Component.literal("特性 · 编辑器")) { Minecraft.getInstance().setScreen(AbilityEditorScreen()) }.bounds(cx, 126, w, 22).build())
        addRenderableWidget(Button.builder(Component.literal("道具 · 编辑器")) { Minecraft.getInstance().setScreen(HeldItemEditorScreen()) }.bounds(cx, 152, w, 22).build())
        addRenderableWidget(Button.builder(Component.literal("加载修改（应用保存的编辑）")) {
            Minecraft.getInstance().player?.connection?.sendCommand("cobbleeditor load")
            onClose()
        }.bounds(cx, 178, w, 22).build())
        addRenderableWidget(Button.builder(Component.literal("关闭")) { onClose() }.bounds(cx, 204, w, 22).build())
    }

    override fun render(g: GuiGraphics, mx: Int, my: Int, p: Float) {
        g.fill(0, 0, width, height, 0xC0101010.toInt())
        super.render(g, mx, my, p)
        g.drawCenteredString(this.font, this.title, width / 2, 26, 0xFFFFFFFF.toInt())
        g.drawCenteredString(this.font, "保存只写文件；点『加载修改』应用（无需重进世界）", width / 2, 40, 0xFFAAAAAA.toInt())
    }

    override fun isPauseScreen(): Boolean = false
}
