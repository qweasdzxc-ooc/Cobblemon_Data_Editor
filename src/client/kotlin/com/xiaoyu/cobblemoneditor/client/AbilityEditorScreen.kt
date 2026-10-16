package com.xiaoyu.cobblemoneditor.client

import com.xiaoyu.cobblemoneditor.client.widget.Dropdown
import com.xiaoyu.cobblemoneditor.data.AbilityEffects
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
    private lateinit var paramsBox: EditBox
    private var effectId = "immune_type"
    private lateinit var presetsDropdown: Dropdown

    override fun init() {
        val left = width / 2 - 130
        val bx = left + 80
        idBox = EditBox(this.font, bx, 40, 180, 16, Component.empty()); idBox.setMaxLength(32)
        nameBox = EditBox(this.font, bx, 64, 180, 16, Component.empty()); nameBox.setMaxLength(64)
        descBox = EditBox(this.font, bx, 88, 250, 16, Component.empty()); descBox.setMaxLength(200)
        addRenderableWidget(idBox); addRenderableWidget(nameBox); addRenderableWidget(descBox)

        val d1 = Dropdown(bx, 112, 150, 16, AbilityEffects.ALL.map { it.displayName }, AbilityEffects.ALL.map { it.id }) { v ->
            effectId = v
            paramsBox.value = AbilityEffects.byId(v)?.presets?.firstOrNull()?.first ?: ""
            rebuildPresetsDropdown(132)
        }
        addRenderableWidget(d1); dropdowns.add(d1)

        paramsBox = EditBox(this.font, bx, 132, 150, 16, Component.empty()); paramsBox.setMaxLength(100)
        addRenderableWidget(paramsBox)
        paramsBox.value = AbilityEffects.byId(effectId)?.presets?.firstOrNull()?.first ?: ""
        rebuildPresetsDropdown(152)

        addRenderableWidget(Button.builder(Component.literal("保存")) { save() }.bounds(left, 178, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("返回")) { Minecraft.getInstance().setScreen(EditorHubScreen()) }.bounds(left + 76, 178, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("关闭")) { onClose() }.bounds(left + 152, 178, 68, 20).build())
    }

    private fun rebuildPresetsDropdown(y: Int) {
        if (::presetsDropdown.isInitialized) removeWidget(presetsDropdown)
        val b = AbilityEffects.byId(effectId)
        val presets = b?.presets ?: emptyList()
        if (presets.isEmpty()) return
        presetsDropdown = Dropdown(width / 2 - 130 + 80, y, 150, 16, presets.map { it.second }, presets.map { it.first }) { v -> paramsBox.value = v }
        addRenderableWidget(presetsDropdown); dropdowns.add(presetsDropdown)
    }

    private fun save() {
        val id = idBox.value.trim().lowercase()
        if (id.isEmpty()) { msg("请填特性 id"); return }
        val parts = ArrayList<String>()
        val name = nameBox.value.trim(); if (name.isNotEmpty()) parts.add("name=" + name)
        val desc = descBox.value.trim(); if (desc.isNotEmpty()) parts.add("desc=" + desc)
        val pv = paramsBox.value.trim()
        parts.add("effects=" + effectId + (if (pv.isEmpty()) "" else ":" + pv))
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
        g.drawString(this.font, "效果:", left, 116, 0xAAAAAA)
        g.drawString(this.font, "参数(可改):", left, 136, 0xAAAAAA)
        g.drawString(this.font, "预设:", left, 156, 0xAAAAAA)
        for (d in dropdowns) d.renderExpanded(g, mx, my)
    }

    override fun isPauseScreen(): Boolean = false
}
