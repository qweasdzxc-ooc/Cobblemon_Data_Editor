package com.xiaoyu.cobblemoneditor.client

import com.xiaoyu.cobblemoneditor.client.widget.Dropdown
import com.xiaoyu.cobblemoneditor.data.EffectBricks
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class AbilityEditorScreen : Screen(Component.literal("特性编辑器")) {

    private val dropdowns = mutableListOf<Dropdown>()
    private lateinit var idBox: EditBox
    private lateinit var nameBox: EditBox
    private lateinit var descBox: EditBox
    private var brickId = "secondary_status"
    private var paramsValue = "10:brn"
    private lateinit var paramsDropdown: Dropdown

    override fun init() {
        val left = width / 2 - 130
        val bx = left + 70
        idBox = EditBox(this.font, bx, 40, 180, 16, Component.empty()); idBox.setMaxLength(32)
        nameBox = EditBox(this.font, bx, 64, 180, 16, Component.empty()); nameBox.setMaxLength(64)
        descBox = EditBox(this.font, bx, 88, 250, 16, Component.empty()); descBox.setMaxLength(200)
        addRenderableWidget(idBox); addRenderableWidget(nameBox); addRenderableWidget(descBox)

        val d1 = Dropdown(bx, 112, 150, 16, EffectBricks.ALL.map { it.displayName }, EffectBricks.ALL.map { it.id }) { v ->
            brickId = v
            val b = EffectBricks.byId(v)
            paramsValue = b?.presets?.firstOrNull()?.first ?: ""
            rebuildParamsDropdown(132)
        }
        addRenderableWidget(d1); dropdowns.add(d1)
        rebuildParamsDropdown(132)

        addRenderableWidget(Button.builder(Component.literal("保存")) { save() }.bounds(left, 158, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("返回")) { Minecraft.getInstance().setScreen(EditorHubScreen()) }.bounds(left + 76, 158, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("关闭")) { onClose() }.bounds(left + 152, 158, 68, 20).build())
    }

    private fun rebuildParamsDropdown(y: Int) {
        if (::paramsDropdown.isInitialized) removeWidget(paramsDropdown)
        val b = EffectBricks.byId(brickId)
        val presets = b?.presets ?: emptyList()
        if (presets.isEmpty()) return
        paramsDropdown = Dropdown(width / 2 - 130 + 70, y, 150, 16, presets.map { it.second }, presets.map { it.first }) { paramsValue = it }
        paramsDropdown.setSelectedByValue(paramsValue)
        addRenderableWidget(paramsDropdown); dropdowns.add(paramsDropdown)
    }

    private fun save() {
        val id = idBox.value.trim().lowercase()
        if (id.isEmpty()) { msg("请填特性 id"); return }
        val parts = ArrayList<String>()
        val name = nameBox.value.trim(); if (name.isNotEmpty()) parts.add("name=" + name)
        val desc = descBox.value.trim(); if (desc.isNotEmpty()) parts.add("desc=" + desc)
        parts.add("bricks=" + brickId + (if (paramsValue.isEmpty()) "" else ":" + paramsValue))
        val spec = parts.joinToString("&")
        val player = Minecraft.getInstance().player
        if (player == null) { msg("未连接服务器"); return }
        player.connection.sendCommand("cobbleeditor ability add " + id + " " + spec)
        onClose()
    }

    private fun msg(s: String) { Minecraft.getInstance().player?.displayClientMessage(Component.literal(s), false) }

    override fun mouseClicked(mx: Double, my: Double, button: Int): Boolean {
        for (d in dropdowns) if (d.open) { d.handleExpandedClick(mx, my); return true }
        return super.mouseClicked(mx, my, button)
    }

    override fun render(g: GuiGraphics, mx: Int, my: Int, p: Float) {
        super.render(g, mx, my, p)
        val left = width / 2 - 130
        g.drawCenteredString(this.font, this.title, width / 2, 14, 0xFFFFFF)
        g.drawString(this.font, "id:", left, 44, 0xAAAAAA)
        g.drawString(this.font, "名称:", left, 68, 0xAAAAAA)
        g.drawString(this.font, "描述:", left, 92, 0xAAAAAA)
        g.drawString(this.font, "效果积木:", left, 116, 0xAAAAAA)
        g.drawString(this.font, "积木参数:", left, 136, 0xAAAAAA)
        g.drawString(this.font, "(能力效果积木为占位，待接入)", left, 180, 0x666666)
    }

    override fun isPauseScreen(): Boolean = false
}
