package com.xiaoyu.cobblemoneditor.client

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.pokemon.Species
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
    private lateinit var movesBox: EditBox
    private lateinit var speciesDropdown: Dropdown
    private lateinit var typeDropdown1: Dropdown
    private lateinit var typeDropdown2: Dropdown
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

    // 布局常量：标签列在左，控件列在右，互不重叠
    private val labelX get() = width / 2 - 160
    private val fieldX get() = width / 2 - 160 + 72

    override fun init() {
        val left = width / 2 - 160

        // 搜索行：搜索框 + 精灵下拉
        speciesSearchBox = EditBox(this.font, fieldX, 16, 100, 16, Component.empty())
        speciesSearchBox.setMaxLength(64)
        addRenderableWidget(speciesSearchBox)
        if (speciesList.isNotEmpty()) speciesId = speciesList[0].first
        rebuildSpeciesDropdown()

        // 六维：2 列 × 3 行
        var i = 0
        for ((k, _) in stats) {
            val col = i % 2; val row = i / 2
            val bx = fieldX + col * 150
            val by = 56 + row * 18
            val box = EditBox(this.font, bx, by, 46, 14, Component.empty())
            box.setMaxLength(3); addRenderableWidget(box); statBoxes[k] = box
            i++
        }

        // 属性
        val typeLabels = listOf("不添加") + TypeCatalog.TYPES.map { it.second }
        val typeValues = listOf("") + TypeCatalog.TYPES.map { it.first }
        typeDropdown1 = Dropdown(fieldX, 112, 92, 14, typeLabels, typeValues, 10) { type1 = it }
        addRenderableWidget(typeDropdown1); dropdowns.add(typeDropdown1)
        typeDropdown2 = Dropdown(fieldX + 124, 112, 92, 14, typeLabels, typeValues, 10) { type2 = it }
        addRenderableWidget(typeDropdown2); dropdowns.add(typeDropdown2)

        // 特性
        abilitySearchBox = EditBox(this.font, fieldX, 132, 190, 14, Component.empty())
        abilitySearchBox.setMaxLength(64)
        addRenderableWidget(abilitySearchBox)
        rebuildAbilityDropdowns()

        // 学习表（预填当前选中精灵的完整学习表，可直接编辑；留空=不改）
        movesBox = EditBox(this.font, fieldX, 184, 190, 14, Component.empty())
        movesBox.setMaxLength(2048)
        movesBox.setHint(Component.literal("1:tackle|5:growl|egg:xx|tm:yy"))
        addRenderableWidget(movesBox)

        // 初始选中第一个精灵：预填它的六维/属性/特性/学习表
        onSpeciesChanged()

        // 按钮
        val by = height - 28
        addRenderableWidget(Button.builder(Component.literal("保存")) { save() }.bounds(left, by, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("返回")) { Minecraft.getInstance().setScreen(EditorHubScreen()) }.bounds(left + 76, by, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("关闭")) { onClose() }.bounds(left + 152, by, 68, 20).build())
    }

    private fun filteredSpecies(): List<Triple<String, String, String>> {
        val q = speciesSearchBox.value.trim().lowercase()
        if (q.isEmpty()) return speciesList
        return speciesList.filter { (id, en, zh) -> id.contains(q) || en.lowercase().contains(q) || zh.contains(q) }
    }

    private fun currentSpecies(): Species? =
        PokemonSpecies.implemented.find { it.resourceIdentifier.path == speciesId }

    /**
     * 切换精灵时：六维/属性/特性/学习表全部预填为该精灵的当前值
     * （用户要求"选择的精灵显示已有的六维、特性、属性"）。
     */
    private fun onSpeciesChanged() {
        val s = currentSpecies()
        for ((k, box) in statBoxes) box.value = s?.baseStats?.get(statByKey(k))?.toString() ?: ""
        type1 = typeValue(s?.primaryType); type2 = typeValue(s?.secondaryType)
        if (::typeDropdown1.isInitialized) {
            typeDropdown1.setSelectedByValue(type1); typeDropdown2.setSelectedByValue(type2)
        }
        val ab = currentAbilities(s)
        ability1 = ab.first.getOrNull(0) ?: ""; ability2 = ab.first.getOrNull(1) ?: ""; hiddenAbility = ab.second.getOrNull(0) ?: ""
        if (::abilityDropdown1.isInitialized) rebuildAbilityDropdowns()
        if (::movesBox.isInitialized) movesBox.value = currentLearnset()
    }

    private fun statByKey(k: String): Stats = when (k) {
        "hp" -> Stats.HP; "attack" -> Stats.ATTACK; "defence" -> Stats.DEFENCE
        "special_attack" -> Stats.SPECIAL_ATTACK; "special_defence" -> Stats.SPECIAL_DEFENCE
        else -> Stats.SPEED
    }

    /** ElementalType -> 数据包小写键（与 TypeCatalog 一致）。 */
    private fun typeValue(t: ElementalType?): String {
        if (t == null) return ""
        val n = t.name
        if (TypeCatalog.TYPES.any { it.first == n }) return n
        return t.showdownId.lowercase()
    }

    /** 读 AbilityPool：第一个列表=常驻特性(≤2)，第二个=隐藏特性(≤1)。LOW/LOWEST 都是隐藏。 */
    private fun currentAbilities(s: Species?): Pair<List<String>, List<String>> {
        if (s == null) return emptyList<String>() to emptyList<String>()
        val normal = mutableListOf<String>(); val hidden = mutableListOf<String>()
        for (pa in s.abilities) {
            val n = pa.template.name
            if (pa.priority == Priority.LOW || pa.priority == Priority.LOWEST) hidden.add(n) else normal.add(n)
        }
        return normal.take(2) to hidden.take(1)
    }

    private fun rebuildSpeciesDropdown() {
        if (::speciesDropdown.isInitialized) removeWidget(speciesDropdown)
        val f = filteredSpecies()
        if (f.isEmpty()) return
        if (f.none { it.first == speciesId }) { speciesId = f[0].first; onSpeciesChanged() }
        speciesDropdown = Dropdown(fieldX + 110, 16, 100, 16, f.map { it.third }, f.map { it.first }, 10) { speciesId = it; onSpeciesChanged() }
        speciesDropdown.setSelectedByValue(speciesId)
        addRenderableWidget(speciesDropdown); dropdowns.add(speciesDropdown)
    }

    /** 当前选中精灵的完整学习表（"1:tackle|egg:xx|tm:yy"），查不到返回空串。 */
    private fun currentLearnset(): String {
        val s = PokemonSpecies.implemented.find { it.resourceIdentifier.path == speciesId } ?: return ""
        val ls = s.moves
        val parts = mutableListOf<String>()
        for ((level, templates) in ls.levelUpMoves.entries.sortedBy { it.key })
            for (t in templates) parts.add("$level:${t.name}")
        for (t in ls.eggMoves) parts.add("egg:${t.name}")
        for (t in ls.tmMoves) parts.add("tm:${t.name}")
        for (t in ls.tutorMoves) parts.add("tutor:${t.name}")
        for (t in ls.legacyMoves) parts.add("legacy:${t.name}")
        for (t in ls.specialMoves) parts.add("special:${t.name}")
        return parts.joinToString("|")
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
        // 修复：已确认的特性即使不在当前搜索结果里也保留在下拉列表中，
        // 避免"再搜索时已确认的特性全部跳回【不改】"。
        val confirmed = listOf(ability1, ability2, hiddenAbility).filter { it.isNotEmpty() }
        val extra = confirmed.filter { c -> f.none { it.first == c } }
            .map { c -> abilitiesList.firstOrNull { it.first == c } ?: (c to c) }
        val all = f + extra
        val labels = listOf("不改") + all.map { it.second }
        val values = listOf("") + all.map { it.first }
        abilityDropdown1 = Dropdown(fieldX, 152, 100, 14, labels, values, 10) { ability1 = it }
        abilityDropdown1.setSelectedByValue(ability1)
        addRenderableWidget(abilityDropdown1); dropdowns.add(abilityDropdown1)
        abilityDropdown2 = Dropdown(fieldX + 132, 152, 100, 14, labels, values, 10) { ability2 = it }
        abilityDropdown2.setSelectedByValue(ability2)
        addRenderableWidget(abilityDropdown2); dropdowns.add(abilityDropdown2)
        abilityDropdown3 = Dropdown(fieldX, 170, 190, 14, labels, values, 10) { hiddenAbility = it }
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
        val movesRaw = movesBox.value.trim()
        if (movesRaw.isNotEmpty()) {
            val entries = movesRaw.split('|').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            for (e in entries) {
                val idx = e.indexOf(':')
                if (idx <= 0 || !e.substring(idx + 1).matches(Regex("[a-z0-9]+"))) {
                    msg("学习表格式错误: $e（应为 1:tackle|egg:xx|tm:yy）"); return
                }
                val method = e.substring(0, idx)
                if (method !in setOf("egg", "tm", "tutor", "legacy", "special")) {
                    val lvl = method.toIntOrNull()
                    if (lvl == null || lvl < 1 || lvl > 100) { msg("学习表等级无效: $e（等级 1-100）"); return }
                }
            }
            parts.add("movesReplace=" + entries.joinToString("|"))
        }
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
        val left = width / 2 - 160
        g.drawCenteredString(this.font, this.title, width / 2, 4, 0xFFFFFF)
        g.drawString(this.font, "精灵:", left, 20, 0xAAAAAA)
        var i = 0
        for ((_, label) in stats) {
            val col = i % 2; val row = i / 2
            g.drawString(this.font, label + ":", left + col * 150, 60 + row * 18, 0xAAAAAA)
            i++
        }
        g.drawString(this.font, "属性1:", left + 42, 116, 0xAAAAAA)
        g.drawString(this.font, "属性2:", left + 166, 116, 0xAAAAAA)
        g.drawString(this.font, "特性搜索:", left, 136, 0xAAAAAA)
        g.drawString(this.font, "特性1:", left + 42, 156, 0xAAAAAA)
        g.drawString(this.font, "特性2:", left + 174, 156, 0xAAAAAA)
        g.drawString(this.font, "隐藏特性:", left, 174, 0xAAAAAA)
        g.drawString(this.font, "学习表:", left, 188, 0xAAAAAA)
        g.drawString(this.font, "留空=不改；保存后重进世界生效", left, 202, 0x888888)
        for (d in dropdowns) d.renderExpanded(g, mx, my)
    }

    override fun isPauseScreen(): Boolean = false
}
