package com.xiaoyu.cobblemoneditor.client

import com.xiaoyu.cobblemoneditor.client.widget.Dropdown
import com.xiaoyu.cobblemoneditor.data.TypeCatalog
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class EditorScreen : Screen(Component.literal("精灵编辑器")) {

    private val dropdowns = mutableListOf<Dropdown>()
    private val speciesList: List<Pair<String, String>> = ClientCatalogs.species()
    private val abilitiesList: List<Pair<String, String>> = ClientCatalogs.abilities()
    private val statBoxes = LinkedHashMap<String, EditBox>()
    private var speciesId = ""
    private var type1 = ""
    private var type2 = ""
    private var ability1 = ""
    private var ability2 = ""
    private var hiddenAbility = ""

    private val stats = listOf(
        "hp" to "HP", "attack" to "攻击", "defence" to "防御",
        "special_attack" to "特攻", "special_defence" to "特防", "speed" to "速度",
    )

    override fun init() {
        val left = width / 2 - 150

        if (speciesList.isNotEmpty()) {
            speciesId = speciesList[0].first
            val d = Dropdown(left, 18, 180, 16, speciesList.map { it.second }, speciesList.map { it.first }) { speciesId = it }
            addRenderableWidget(d); dropdowns.add(d)
        }

        var i = 0
        for ((k, _) in stats) {
            val col = i % 2; val row = i / 2
            val box = EditBox(this.font, left + 60 + col * 150, 38 + row * 18, 50, 14, Component.empty())
            box.setMaxLength(3); addRenderableWidget(box); statBoxes[k] = box
            i++
        }

        val typeLabels = listOf("不添加") + TypeCatalog.TYPES.map { it.second }
        val typeValues = listOf("") + TypeCatalog.TYPES.map { it.first }
        val d1 = Dropdown(left, 96, 90, 14, typeLabels, typeValues) { type1 = it }
        addRenderableWidget(d1); dropdowns.add(d1)
        val d2 = Dropdown(left + 100, 96, 90, 14, typeLabels, typeValues) { type2 = it }
        addRenderableWidget(d2); dropdowns.add(d2)

        val abLabels = listOf("不改") + abilitiesList.map { it.second }
        val abValues = listOf("") + abilitiesList.map { it.first }
        val a1 = Dropdown(left, 116, 110, 14, abLabels, abValues) { ability1 = it }
        addRenderableWidget(a1); dropdowns.add(a1)
        val a2 = Dropdown(left + 120, 116, 110, 14, abLabels, abValues) { ability2 = it }
        addRenderableWidget(a2); dropdowns.add(a2)
        val a3 = Dropdown(left, 136, 230, 14, abLabels, abValues) { hiddenAbility = it }
        addRenderableWidget(a3); dropdowns.add(a3)

        addRenderableWidget(Button.builder(Component.literal("保存")) { save() }.bounds(left, 158, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("返回")) { Minecraft.getInstance().setScreen(EditorHubScreen()) }.bounds(left + 76, 158, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("关闭")) { onClose() }.bounds(left + 152, 158, 68, 20).build())
    }

    private fun save() {
        if (speciesList.isEmpty()) { msg("未加载到精灵列表"); return }
        val parts = ArrayList<String>()
        for ((k, box) in statBoxes) {
            val v = box.value.trim(); if (v.isEmpty()) continue
            if (v.toIntOrNull() == null) { msg(k + " 必须是数字"); return }
            parts.add(k + "=" + v)
        }
        if (type1.isNotEmpty()) parts.add("primaryType=" + type1)
        if (type2.isNotEmpty()) parts.add("secondaryType=" + type2)
        val ab = mutableListOf<String>()
        if (ability1.isNotEmpty()) ab.add(ability1)
        if (ability2.isNotEmpty()) ab.add(ability2)
        if (hiddenAbility.isNotEmpty()) ab.add("h:" + hiddenAbility)
        if (ab.isNotEmpty()) parts.add("abilities=" + ab.joinToString("|"))
        if (parts.isEmpty()) { msg("至少填一项内容"); return }
        val spec = parts.joinToString(",")
        val player = Minecraft.getInstance().player
        if (player == null) { msg("未连接服务器"); return }
        player.connection.sendCommand("cobbleeditor set " + speciesId + " " + spec)
        onClose()
    }

    private fun msg(s: String) { Minecraft.getInstance().player?.displayClientMessage(Component.literal(s), false) }

    override fun mouseClicked(mx: Double, my: Double, button: Int): Boolean {
        for (d in dropdowns) if (d.open) { d.handleExpandedClick(mx, my); return true }
        return super.mouseClicked(mx, my, button)
    }

    override fun render(g: GuiGraphics, mx: Int, my: Int, p: Float) {
        super.render(g, mx, my, p)
        val left = width / 2 - 150
        g.drawCenteredString(this.font, this.title, width / 2, 6, 0xFFFFFF)
        var i = 0
        for ((_, label) in stats) {
            val col = i % 2; val row = i / 2
            g.drawString(this.font, label + ":", left + col * 150, 42 + row * 18, 0xAAAAAA)
            i++
        }
        g.drawString(this.font, "属性1:", left, 100, 0xAAAAAA)
        g.drawString(this.font, "属性2:", left + 100, 100, 0xAAAAAA)
        g.drawString(this.font, "特性1:", left, 120, 0xAAAAAA)
        g.drawString(this.font, "特性2:", left + 120, 120, 0xAAAAAA)
        g.drawString(this.font, "隐藏特性:", left, 140, 0xAAAAAA)
        g.drawString(this.font, "留空=不改；保存后重进世界生效", left, 182, 0x888888)
        for (d in dropdowns) d.renderExpanded(g, mx, my)
    }

    override fun isPauseScreen(): Boolean = false
}
