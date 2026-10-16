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
    private val speciesList: List<Triple<String, String, String>> = ClientCatalogs.species()
    private val abilitiesList: List<Pair<String, String>> = ClientCatalogs.abilities()
    private val statBoxes = LinkedHashMap<String, EditBox>()
    private lateinit var speciesSearchBox: EditBox
    private lateinit var abilitySearchBox: EditBox
    private lateinit var speciesDropdown: Dropdown
    private lateinit var abilityDropdown1: Dropdown
    private lateinit var abilityDropdown2: Dropdown
    private lateinit var abilityDropdown3: Dropdown
    private var speciesId = ""
    private var lastSearch = ""
    private var lastAbilitySearch = ""
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

        speciesSearchBox = EditBox(this.font, left + 150, 18, 120, 16, Component.empty())
        speciesSearchBox.setMaxLength(64)
        addRenderableWidget(speciesSearchBox)
        if (speciesList.isNotEmpty()) speciesId = speciesList[0].first
        rebuildSpeciesDropdown()

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

        abilitySearchBox = EditBox(this.font, left, 114, 230, 14, Component.empty())
        abilitySearchBox.setMaxLength(64)
        addRenderableWidget(abilitySearchBox)
        rebuildAbilityDropdowns()

        addRenderableWidget(Button.builder(Component.literal("保存")) { save() }.bounds(left, 168, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("返回")) { Minecraft.getInstance().setScreen(EditorHubScreen()) }.bounds(left + 76, 168, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("关闭")) { onClose() }.bounds(left + 152, 168, 68, 20).build())
    }

    private fun filteredSpecies(): List<Triple<String, String, String>> {
        val q = speciesSearchBox.value.trim().lowercase()
        if (q.isEmpty()) return speciesList
        return speciesList.filter { (id, en, zh) -> id.contains(q) || en.lowercase().contains(q) || zh.contains(q) }
    }

    private fun rebuildSpeciesDropdown() {
        if (::speciesDropdown.isInitialized) removeWidget(speciesDropdown)
        val f = filteredSpecies()
        if (f.isEmpty()) return
        if (f.none { it.first == speciesId }) speciesId = f[0].first
        speciesDropdown = Dropdown(width / 2 - 150, 18, 140, 16, f.map { it.third }, f.map { it.first }, 10) { speciesId = it }
        speciesDropdown.setSelectedByValue(speciesId)
        addRenderableWidget(speciesDropdown); dropdowns.add(speciesDropdown)
    }

    private fun filteredAbilities(): List<Pair<String, String>> {
        val q = abilitySearchBox.value.trim().lowercase()
        if (q.isEmpty()) return abilitiesList
        return abilitiesList.filter { (id, zh) -> id.contains(q) || zh.contains(q) }
    }

    private fun rebuildAbilityDropdowns() {
        if (::abilityDropdown1.isInitialized) removeWidget(abilityDropdown1)
        if (::abilityDropdown2.isInitialized) removeWidget(abilityDropdown2)
        if (::abilityDropdown3.isInitialized) removeWidget(abilityDropdown3)
        val f = filteredAbilities()
        val labels = listOf("不改") + f.map { it.second }
        val values = listOf("") + f.map { it.first }
        if (ability1.isNotEmpty() && f.none { it.first == ability1 }) ability1 = ""
        if (ability2.isNotEmpty() && f.none { it.first == ability2 }) ability2 = ""
        if (hiddenAbility.isNotEmpty() && f.none { it.first == hiddenAbility }) hiddenAbility = ""
        val left = width / 2 - 150
        abilityDropdown1 = Dropdown(left, 132, 110, 14, labels, values, 10) { ability1 = it }
        abilityDropdown1.setSelectedByValue(ability1)
        addRenderableWidget(abilityDropdown1); dropdowns.add(abilityDropdown1)
        abilityDropdown2 = Dropdown(left + 120, 132, 110, 14, labels, values, 10) { ability2 = it }
        abilityDropdown2.setSelectedByValue(ability2)
        addRenderableWidget(abilityDropdown2); dropdowns.add(abilityDropdown2)
        abilityDropdown3 = Dropdown(left, 150, 230, 14, labels, values, 10) { hiddenAbility = it }
        abilityDropdown3.setSelectedByValue(hiddenAbility)
        addRenderableWidget(abilityDropdown3); dropdowns.add(abilityDropdown3)
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

    override fun tick() {
        val q1 = speciesSearchBox.value.trim().lowercase()
        if (q1 != lastSearch) { lastSearch = q1; rebuildSpeciesDropdown() }
        val q2 = abilitySearchBox.value.trim().lowercase()
        if (q2 != lastAbilitySearch) { lastAbilitySearch = q2; rebuildAbilityDropdowns() }
    }

    override fun mouseClicked(mx: Double, my: Double, button: Int): Boolean {
        for (d in dropdowns) if (d.open) { d.handleExpandedClick(mx, my); return true }
        return super.mouseClicked(mx, my, button)
    }

    override fun mouseScrolled(mx: Double, my: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        for (d in dropdowns) if (d.open) { d.handleScroll(verticalAmount); return true }
        return super.mouseScrolled(mx, my, horizontalAmount, verticalAmount)
    }

    override fun render(g: GuiGraphics, mx: Int, my: Int, p: Float) {
        g.fill(0, 0, width, height, 0xC0101010.toInt())
        super.render(g, mx, my, p)
        val left = width / 2 - 150
        g.drawCenteredString(this.font, this.title, width / 2, 4, 0xFFFFFF)
        g.drawString(this.font, "精灵搜索:", left, 22, 0xAAAAAA)
        var i = 0
        for ((_, label) in stats) {
            val col = i % 2; val row = i / 2
            g.drawString(this.font, label + ":", left + col * 150, 42 + row * 18, 0xAAAAAA)
            i++
        }
        g.drawString(this.font, "属性1:", left, 100, 0xAAAAAA)
        g.drawString(this.font, "属性2:", left + 100, 100, 0xAAAAAA)
        g.drawString(this.font, "特性搜索:", left, 118, 0xAAAAAA)
        g.drawString(this.font, "特性1:", left, 136, 0xAAAAAA)
        g.drawString(this.font, "特性2:", left + 120, 136, 0xAAAAAA)
        g.drawString(this.font, "隐藏特性:", left, 154, 0xAAAAAA)
        g.drawString(this.font, "留空=不改；保存后重进世界生效", left, 190, 0x888888)
        for (d in dropdowns) d.renderExpanded(g, mx, my)
    }

    override fun isPauseScreen(): Boolean = false
}
