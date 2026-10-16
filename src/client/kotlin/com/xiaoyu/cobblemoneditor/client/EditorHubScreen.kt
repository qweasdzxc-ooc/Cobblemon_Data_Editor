package com.xiaoyu.cobblemoneditor.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class EditorHubScreen : Screen(Component.literal("Cobblemon Data Editor")) {

    override fun init() {
        val cx = width / 2 - 100
        addRenderableWidget(
            Button.builder(Component.literal("精灵：种族值/属性/特性")) { Minecraft.getInstance().setScreen(EditorScreen()) }
                .bounds(cx, 40, 200, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("招式")) { Minecraft.getInstance().setScreen(MoveEditorScreen()) }
                .bounds(cx, 68, 200, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("特性")) { Minecraft.getInstance().setScreen(AbilityEditorScreen()) }
                .bounds(cx, 96, 200, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("关闭")) { onClose() }
                .bounds(cx, 130, 200, 20).build()
        )
    }

    override fun render(g: GuiGraphics, mx: Int, my: Int, p: Float) {
        super.render(g, mx, my, p)
        g.drawCenteredString(this.font, this.title, width / 2, 14, 0xFFFFFF)
    }

    override fun isPauseScreen(): Boolean = false
}
