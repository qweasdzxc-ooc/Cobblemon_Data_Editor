package com.xiaoyu.cobblemoneditor.client

import com.xiaoyu.cobblemoneditor.client.widget.Dropdown
import com.xiaoyu.cobblemoneditor.data.TypeCatalog
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * 新建精灵界面：生成 data/cobblemoneditor/species/<id>.json。
 * 六维留空=50；学习表留空=1:tackle；高级字段（身高/体重/捕获率等）可用 /cobbleeditor species add 命令设置。
 */
class NewSpeciesScreen : Screen(Component.literal("新建精灵")) {

    private val dropdowns = mutableListOf<Dropdown>()
    private val abilitiesList: List<Pair<String, String>> = ClientCatalogs.abilities()
    private val movesList: List<Pair<String, String>> = ClientCatalogs.moves()
    private val statBoxes = LinkedHashMap<String, EditBox>()
    private val methods = listOf(
        "等级" to "level", "蛋招式" to "egg", "招式机" to "tm",
        "传授" to "tutor", "遗传" to "legacy", "特殊" to "special",
    )

    private lateinit var idBox: EditBox
    private lateinit var dexBox: EditBox
    private lateinit var abilitySearchBox: EditBox
    private lateinit var moveSearchBox: EditBox
    private lateinit var levelBox: EditBox
    private lateinit var movesBox: EditBox
    private lateinit var abilityDropdown1: Dropdown
    private lateinit var abilityDropdown2: Dropdown
    private lateinit var hiddenAbilityDropdown: Dropdown
    private lateinit var moveDropdown: Dropdown
    private lateinit var methodDropdown: Dropdown

    private var type1 = ""
    private var type2 = ""
    private var ability1 = ""
    private var ability2 = ""
    private var hiddenAbility = ""
    private var method = "level"
    private var selectedMove = ""
    private var lastAbilitySearch = ""
    private var lastMoveSearch = ""

    private val stats = listOf(
        "hp" to "HP", "attack" to "攻击", "defence" to "防御",
        "special_attack" to "特攻", "special_defence" to "特防", "speed" to "速度",
    )

    private val labelX get() = width / 2 - 160
    private val fieldX get() = width / 2 - 88

    override fun init() {
        val left = width / 2 - 160

        idBox = EditBox(this.font, fieldX, 14, 88, 14, Component.empty())
        idBox.setMaxLength(32)
        idBox.setHint(Component.literal("英文id"))
        addRenderableWidget(idBox)
        dexBox = EditBox(this.font, fieldX + 98, 14, 40, 14, Component.empty())
        dexBox.setMaxLength(5)
        dexBox.setHint(Component.literal("9001"))
        addRenderableWidget(dexBox)

        val typeLabels = listOf("无") + TypeCatalog.TYPES.map { it.second }
        val typeValues = listOf("") + TypeCatalog.TYPES.map { it.first }
        val d1 = Dropdown(fieldX, 32, 92, 14, typeLabels, typeValues, 10) { type1 = it }
        addRenderableWidget(d1); dropdowns.add(d1)
        val d2 = Dropdown(fieldX + 110, 32, 92, 14, typeLabels, typeValues, 10) { type2 = it }
        addRenderableWidget(d2); dropdowns.add(d2)

        var i = 0
        for ((k, _) in stats) {
            val col = i % 2; val row = i / 2
            val box = EditBox(this.font, fieldX + col * 150, 50 + row * 18, 46, 14, Component.empty())
            box.setMaxLength(3); addRenderableWidget(box); statBoxes[k] = box
            i++
        }

        abilitySearchBox = EditBox(this.font, fieldX, 104, 190, 14, Component.empty())
        abilitySearchBox.setMaxLength(64)
        addRenderableWidget(abilitySearchBox)
        rebuildAbilityDropdowns()

        moveSearchBox = EditBox(this.font, fieldX, 158, 90, 14, Component.empty())
        moveSearchBox.setMaxLength(64)
        addRenderableWidget(moveSearchBox)
        methodDropdown = Dropdown(fieldX + 98, 158, 64, 14, methods.map { it.first }, methods.map { it.second }, 6) { method = it }
        addRenderableWidget(methodDropdown); dropdowns.add(methodDropdown)
        levelBox = EditBox(this.font, fieldX + 170, 158, 26, 14, Component.empty())
        levelBox.setMaxLength(3)
        levelBox.setHint(Component.literal("1"))
        addRenderableWidget(levelBox)
        rebuildMoveDropdown()

        addRenderableWidget(Button.builder(Component.literal("添加")) { addMove() }.bounds(fieldX + 108, 176, 44, 14).build())
        addRenderableWidget(Button.builder(Component.literal("清空")) { movesBox.value = "" }.bounds(fieldX + 160, 176, 40, 14).build())
        movesBox = EditBox(this.font, fieldX, 196, 248, 14, Component.empty())
        movesBox.setMaxLength(512)
        movesBox.setHint(Component.literal("1:tackle|5:growl"))
        addRenderableWidget(movesBox)

        val by = height - 28
        addRenderableWidget(Button.builder(Component.literal("保存")) { save() }.bounds(left, by, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("返回")) { Minecraft.getInstance().setScreen(EditorHubScreen()) }.bounds(left + 76, by, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("关闭")) { onClose() }.bounds(left + 152, by, 68, 20).build())
    }

    private fun filteredAbilities(): List<Pair<String, String>> {
        val q = abilitySearchBox.value.trim().lowercase()
        if (q.isEmpty()) return abilitiesList
        return abilitiesList.filter { (id, zh) -> id.contains(q) || zh.contains(q) }
    }

    private fun rebuildAbilityDropdowns() {
        if (::abilityDropdown1.isInitialized) removeWidget(abilityDropdown1)
        if (::abilityDropdown2.isInitialized) removeWidget(abilityDropdown2)
        if (::hiddenAbilityDropdown.isInitialized) removeWidget(hiddenAbilityDropdown)
        val f = filteredAbilities()
        val labels = listOf("不改") + f.map { it.second }
        val values = listOf("") + f.map { it.first }
        if (ability1.isNotEmpty() && f.none { it.first == ability1 }) ability1 = ""
        if (ability2.isNotEmpty() && f.none { it.first == ability2 }) ability2 = ""
        if (hiddenAbility.isNotEmpty() && f.none { it.first == hiddenAbility }) hiddenAbility = ""
        abilityDropdown1 = Dropdown(fieldX, 122, 110, 14, labels, values, 10) { ability1 = it }
        abilityDropdown1.setSelectedByValue(ability1)
        addRenderableWidget(abilityDropdown1); dropdowns.add(abilityDropdown1)
        abilityDropdown2 = Dropdown(fieldX + 120, 122, 110, 14, labels, values, 10) { ability2 = it }
        abilityDropdown2.setSelectedByValue(ability2)
        addRenderableWidget(abilityDropdown2); dropdowns.add(abilityDropdown2)
        hiddenAbilityDropdown = Dropdown(fieldX, 140, 190, 14, labels, values, 10) { hiddenAbility = it }
        hiddenAbilityDropdown.setSelectedByValue(hiddenAbility)
        addRenderableWidget(hiddenAbilityDropdown); dropdowns.add(hiddenAbilityDropdown)
    }

    private fun filteredMoves(): List<Pair<String, String>> {
        val q = moveSearchBox.value.trim().lowercase()
        if (q.isEmpty()) return movesList
        return movesList.filter { (id, zh) -> id.contains(q) || zh.contains(q) }
    }

    private fun rebuildMoveDropdown() {
        if (::moveDropdown.isInitialized) removeWidget(moveDropdown)
        val f = filteredMoves()
        if (f.isEmpty()) { selectedMove = ""; return }
        if (selectedMove.isNotEmpty() && f.none { it.first == selectedMove }) selectedMove = ""
        moveDropdown = Dropdown(fieldX, 176, 100, 14, f.map { it.second }, f.map { it.first }, 10) { selectedMove = it }
        moveDropdown.setSelectedByValue(selectedMove)
        addRenderableWidget(moveDropdown); dropdowns.add(moveDropdown)
    }

    private fun parseMovesBox(): LinkedHashSet<String> =
        movesBox.value.split('|').map { it.trim() }.filter { it.isNotEmpty() }.toCollection(LinkedHashSet())

    private fun addMove() {
        val moveId = selectedMove.takeIf { it.isNotEmpty() } ?: moveSearchBox.value.trim().lowercase()
        if (!moveId.matches(Regex("[a-z0-9]+"))) { msg("请先选择或输入有效招式"); return }
        val entry = when (method) {
            "level" -> {
                val lv = levelBox.value.trim().ifEmpty { "1" }
                val n = lv.toIntOrNull()
                if (n == null || n !in 1..100) { msg("等级需 1-100"); return }
                "$n:$moveId"
            }
            else -> "$method:$moveId"
        }
        val cur = parseMovesBox()
        if (cur.contains(entry)) { msg("已存在: $entry"); return }
        cur.add(entry)
        movesBox.value = cur.joinToString("|")
    }

    private fun save() {
        val id = idBox.value.trim().lowercase()
        if (!id.matches(Regex("[a-z0-9_]+"))) { msg("id 仅限小写字母/数字/下划线"); return }
        val dex = dexBox.value.trim().toIntOrNull()
        if (dex == null || dex !in 1..99999) { msg("图鉴号需 1-99999"); return }
        if (type1.isEmpty()) { msg("请选择属性1"); return }
        val parts = ArrayList<String>()
        parts.add("dex=$dex")
        parts.add("type1=$type1")
        if (type2.isNotEmpty()) parts.add("type2=$type2")
        for ((k, box) in statBoxes) {
            val v = box.value.trim(); if (v.isEmpty()) continue
            val n = v.toIntOrNull()
            if (n == null) { msg(k + " 必须是数字"); return }
            if (n !in 0..255) { msg(k + " 需 0-255"); return }
            parts.add(k + "=$v")
        }
        val ab = mutableListOf<String>()
        if (ability1.isNotEmpty()) ab.add(ability1)
        if (ability2.isNotEmpty()) ab.add(ability2)
        if (hiddenAbility.isNotEmpty()) ab.add("h:" + hiddenAbility)
        if (ab.isEmpty()) { msg("至少选 1 个特性"); return }
        parts.add("abilities=" + ab.joinToString("|"))
        val mv = parseMovesBox()
        if (mv.isNotEmpty()) parts.add("moves=" + mv.joinToString("|"))
        val player = Minecraft.getInstance().player
        if (player == null) { msg("未连接服务器"); return }
        player.connection.sendCommand("cobbleeditor species add $id " + parts.joinToString(","))
        onClose()
    }

    private fun msg(s: String) { Minecraft.getInstance().player?.displayClientMessage(Component.literal(s), false) }

    override fun tick() {
        val q1 = abilitySearchBox.value.trim().lowercase()
        if (q1 != lastAbilitySearch) { lastAbilitySearch = q1; rebuildAbilityDropdowns() }
        val q2 = moveSearchBox.value.trim().lowercase()
        if (q2 != lastMoveSearch) { lastMoveSearch = q2; rebuildMoveDropdown() }
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
        g.drawString(this.font, "id/图鉴:", left, 18, 0xAAAAAA)
        g.drawString(this.font, "属性1:", left, 36, 0xAAAAAA)
        g.drawString(this.font, "属性2:", left + 110, 36, 0xAAAAAA)
        var i = 0
        for ((_, label) in stats) {
            val col = i % 2; val row = i / 2
            g.drawString(this.font, label + ":", left + col * 150, 54 + row * 18, 0xAAAAAA)
            i++
        }
        g.drawString(this.font, "特性搜索:", left, 108, 0xAAAAAA)
        g.drawString(this.font, "特性1:", left, 126, 0xAAAAAA)
        g.drawString(this.font, "特性2:", left + 120, 126, 0xAAAAAA)
        g.drawString(this.font, "隐藏:", left, 144, 0xAAAAAA)
        g.drawString(this.font, "招式:", left, 162, 0xAAAAAA)
        g.drawString(this.font, "来源:", left + 100, 162, 0xAAAAAA)
        g.drawString(this.font, "等级:", left + 172, 162, 0xAAAAAA)
        g.drawString(this.font, "学习表:", left, 200, 0xAAAAAA)
        for (d in dropdowns) d.renderExpanded(g, mx, my)
    }

    override fun isPauseScreen(): Boolean = false
}
