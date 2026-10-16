package com.xiaoyu.cobblemoneditor.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class AbilityEditorScreen : Screen(Component.literal("特性编辑器")) {

    private lateinit var idBox: EditBox
    private lateinit var nameBox: EditBox
    private lateinit var descBox: EditBox

    override fun init() {
        val left = width / 2 - 130
        val bx = left + 70
        idBox = EditBox(this.font, bx, 40, 180, 16, Component.empty()); idBox.setMaxLength(32)
        nameBox = EditBox(this.font, bx, 64, 180, 16, Component.empty()); nameBox.setMaxLength(64)
        descBox = EditBox(this.font, bx, 88, 250, 16, Component.empty()); descBox.setMaxLength(200)
        addRenderableWidget(idBox); addRenderableWidget(nameBox); addRenderableWidget(descBox)
        addRenderableWidget(Button.builder(Component.literal("保存")) { save() }.bounds(left, 120, 90, 20).build())
        addRenderableWidget(Button.builder(Component.literal("关闭")) { onClose() }.bounds(left + 110, 120, 90, 20).build())
    }

    private fun save() {
        val id = idBox.value.trim().lowercase()
        if (id.isEmpty()) { msg("请填特性 id"); return }
        val parts = ArrayList<String>()
        val name = nameBox.value.trim(); if (name.isNotEmpty()) parts.add("name=$name")
        val desc = descBox.value.trim(); if (desc.isNotEmpty()) parts.add("desc=$desc")
        val spec = parts.joinToString("&")
        val player = Minecraft.getInstance().player
        if (player == null) { msg("未连接服务器"); return }
        player.connection.sendCommand("cobbleeditor ability add $id $spec")
        onClose()
    }

    private fun msg(s: String) { Minecraft.getInstance().player?.displayClientMessage(Component.literal(s), false) }

    override fun render(g: GuiGraphics, mx: Int, my: Int, p: Float) {
        super.render(g, mx, my, p)
        val left = width / 2 - 130
        g.drawCenteredString(this.font, this.title, width / 2, 14, 0xFFFFFF)
        g.drawString(this.font, "id:", left, 44, 0xAAAAAA)
        g.drawString(this.font, "名称:", left, 68, 0xAAAAAA)
        g.drawString(this.font, "描述:", left, 92, 0xAAAAAA)
        g.drawString(this.font, "(效果积木：后续版本加入)", left, 150, 0x666666)
    }

    override fun isPauseScreen(): Boolean = false
}
