package com.xiaoyu.cobblemoneditor.client

import com.xiaoyu.cobblemoneditor.data.TypeCatalog
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class EditorScreen : Screen(Component.literal("精灵编辑器")) {

    private val speciesList: List<Pair<String, String>> = ClientCatalogs.species()
    private val abilitiesList: List<Pair<String, String>> = ClientCatalogs.abilities()
    private var speciesIdx = 0
    private var type1: String? = null
    private var type2: String? = null
    private var ability: String? = null
    private val statBoxes = LinkedHashMap<String, EditBox>()

    private val stats = listOf(
        "hp" to "HP", "attack" to "攻击", "defence" to "防御",
        "special_attack" to "特攻", "special_defence" to "特防", "speed" to "速度",
    )

    override fun init() {
        val left = width / 2 - 150

        if (speciesList.isNotEmpty()) {
            addRenderableWidget(CycleButton.builder<Int> { i -> Component.literal(speciesList[i].second) }
                .withValues((0 until speciesList.size).toList())
                .withInitialValue(speciesIdx)
                .create(left, 20, 180, 18, Component.literal("精灵"), { _, i -> speciesIdx = i }))
        }

        var i = 0
        for ((k, _) in stats) {
            val col = i % 2; val row = i / 2
            val box = EditBox(this.font, left + 60 + col * 150, 44 + row * 20, 50, 16, Component.empty())
            box.setMaxLength(3); addRenderableWidget(box); statBoxes[k] = box
            i++
        }

        val typeOpts: List<String?> = listOf(null) + TypeCatalog.TYPES.map { it.first }
        addRenderableWidget(CycleButton.builder<String?> { t -> Component.literal(if (t == null) "不添加" else TypeCatalog.zh(t!!)) }
            .withValues(typeOpts)
            .withInitialValue(type1)
            .create(left, 106, 90, 16, Component.literal("属性1"), { _, v -> type1 = v }))
        addRenderableWidget(CycleButton.builder<String?> { t -> Component.literal(if (t == null) "不添加" else TypeCatalog.zh(t!!)) }
            .withValues(typeOpts)
            .withInitialValue(type2)
            .create(left + 100, 106, 90, 16, Component.literal("属性2"), { _, v -> type2 = v }))

        val abOpts: List<String?> = listOf(null) + abilitiesList.map { it.first }
        addRenderableWidget(CycleButton.builder<String?> { a -> Component.literal(if (a == null) "不改" else (abilitiesList.firstOrNull { it.first == a }?.second ?: a!!)) }
            .withValues(abOpts)
            .withInitialValue(ability)
            .create(left, 128, 200, 16, Component.literal("特性"), { _, v -> ability = v }))

        addRenderableWidget(Button.builder(Component.literal("保存")) { save() }.bounds(left, 152, 90, 20).build())
        addRenderableWidget(Button.builder(Component.literal("关闭")) { onClose() }.bounds(left + 110, 152, 90, 20).build())
    }

    private fun save() {
        if (speciesList.isEmpty()) { msg("未加载到精灵列表"); return }
        val speciesId = speciesList[speciesIdx].first
        val parts = ArrayList<String>()
        for ((k, box) in statBoxes) {
            val v = box.value.trim(); if (v.isEmpty()) continue
            if (v.toIntOrNull() == null) { msg("$k 必须是数字"); return }
            parts.add("$k=$v")
        }
        type1?.let { parts.add("primaryType=$it") }
        type2?.let { parts.add("secondaryType=$it") }
        ability?.let { parts.add("abilities=$it") }
        if (parts.isEmpty()) { msg("至少填一项内容"); return }
        val spec = parts.joinToString(",")
        val player = Minecraft.getInstance().player
        if (player == null) { msg("未连接服务器"); return }
        player.connection.sendCommand("cobbleeditor set $speciesId $spec")
        onClose()
    }

    private fun msg(s: String) { Minecraft.getInstance().player?.displayClientMessage(Component.literal(s), false) }

    override fun render(g: GuiGraphics, mx: Int, my: Int, p: Float) {
        super.render(g, mx, my, p)
        val left = width / 2 - 150
        g.drawCenteredString(this.font, this.title, width / 2, 8, 0xFFFFFF)
        var i = 0
        for ((_, label) in stats) {
            val col = i % 2; val row = i / 2
            g.drawString(this.font, "$label:", left + col * 150, 48 + row * 20, 0xAAAAAA)
            i++
        }
        g.drawString(this.font, "属性1:", left, 110, 0xAAAAAA)
        g.drawString(this.font, "属性2:", left + 100, 110, 0xAAAAAA)
        g.drawString(this.font, "特性:", left, 132, 0xAAAAAA)
        g.drawString(this.font, "留空=不改该项；保存后重进世界生效", left, 176, 0x888888)
    }

    override fun isPauseScreen(): Boolean = false
}
