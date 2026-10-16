package com.xiaoyu.cobblemoneditor.client

import com.xiaoyu.cobblemoneditor.data.EffectBricks
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class AbilityEditorScreen : Screen(Component.literal("特性编辑器")) {

    private lateinit var idBox: EditBox
    private lateinit var nameBox: EditBox
    private lateinit var descBox: EditBox
    private lateinit var bricksParamsBox: EditBox
    private var brickId = "secondary_status"

    override fun init() {
        val left = width / 2 - 130
        val bx = left + 70
        idBox = EditBox(this.font, bx, 40, 180, 16, Component.empty()); idBox.setMaxLength(32)
        nameBox = EditBox(this.font, bx, 64, 180, 16, Component.empty()); nameBox.setMaxLength(64)
        descBox = EditBox(this.font, bx, 88, 250, 16, Component.empty()); descBox.setMaxLength(200)
        addRenderableWidget(idBox); addRenderableWidget(nameBox); addRenderableWidget(descBox)

        addRenderableWidget(CycleButton.builder { v: String -> Component.literal(EffectBricks.byId(v)?.displayName ?: v) }
            .withValues(EffectBricks.ids())
            .withInitialValue(brickId)
            .create(bx, 112, 150, 16, Component.literal("效果积木"), { _, v -> brickId = v }))

        bricksParamsBox = EditBox(this.font, bx, 132, 150, 16, Component.empty()); bricksParamsBox.setMaxLength(100)
        addRenderableWidget(bricksParamsBox)

        addRenderableWidget(Button.builder(Component.literal("保存")) { save() }.bounds(left, 158, 90, 20).build())
        addRenderableWidget(Button.builder(Component.literal("关闭")) { onClose() }.bounds(left + 110, 158, 90, 20).build())
    }

    private fun save() {
        val id = idBox.value.trim().lowercase()
        if (id.isEmpty()) { msg("请填特性 id"); return }
        val parts = ArrayList<String>()
        val name = nameBox.value.trim(); if (name.isNotEmpty()) parts.add("name=$name")
        val desc = descBox.value.trim(); if (desc.isNotEmpty()) parts.add("desc=$desc")
        val bp = bricksParamsBox.value.trim()
        parts.add(if (bp.isEmpty()) "bricks=$brickId" else "bricks=$brickId:$bp")
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
        g.drawString(this.font, "效果积木:", left, 116, 0xAAAAAA)
        g.drawString(this.font, "积木参数(如 10:par):", left, 136, 0xAAAAAA)
        g.drawString(this.font, "(能力效果积木为占位，待接入)", left, 180, 0x666666)
    }

    override fun isPauseScreen(): Boolean = false
}
