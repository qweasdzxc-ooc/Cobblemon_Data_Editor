package com.xiaoyu.cobblemoneditor.client

import com.xiaoyu.cobblemoneditor.client.widget.Dropdown
import com.xiaoyu.cobblemoneditor.data.AbilityEffects
import com.xiaoyu.cobblemoneditor.data.validateBrickParams
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.util.Base64

class AbilityEditorScreen : Screen(Component.literal("特性编辑器")) {

    private val dropdowns = mutableListOf<Dropdown>()
    private val removeButtons = mutableListOf<Button>()
    private val speciesList: List<Triple<String, String, String>> = ClientCatalogs.species()
    private lateinit var idBox: EditBox
    private lateinit var nameBox: EditBox
    private lateinit var descBox: EditBox
    private lateinit var paramsBox: EditBox
    private lateinit var brickDropdown: Dropdown
    private lateinit var presetDropdown: Dropdown
    private lateinit var assignSearchBox: EditBox
    private lateinit var assignSpeciesDropdown: Dropdown
    private lateinit var assignSlotDropdown: Dropdown
    private var currentBrick = "start_boost"
    private val effects = mutableListOf<Pair<String, String>>()
    private val maxEffects = 12
    private var assignSpecies = ""
    private var assignSlot = ""
    private var lastAssignSearch = ""

    // 标签列（窄）在左，控件列在右，互不重叠
    private val labelX get() = width / 2 - 160
    private val fieldX get() = width / 2 - 160 + 56

    override fun init() {
        val left = width / 2 - 160
        val bx = fieldX

        idBox = EditBox(this.font, bx, 36, 260, 16, Component.empty()); idBox.setMaxLength(32)
        idBox.setHint(Component.literal("英文id(可留空自动生成)"))
        nameBox = EditBox(this.font, bx, 58, 260, 16, Component.empty()); nameBox.setMaxLength(64)
        descBox = EditBox(this.font, bx, 80, 260, 16, Component.empty()); descBox.setMaxLength(200)
        addRenderableWidget(idBox); addRenderableWidget(nameBox); addRenderableWidget(descBox)

        brickDropdown = Dropdown(bx, 118, 150, 16, AbilityEffects.ALL.map { it.displayName }, AbilityEffects.ALL.map { it.id }, 10) { v ->
            currentBrick = v
            paramsBox.value = AbilityEffects.byId(v)?.presets?.firstOrNull()?.first ?: ""
            rebuildPresets()
        }
        addRenderableWidget(brickDropdown); dropdowns.add(brickDropdown)

        paramsBox = EditBox(this.font, bx, 140, 150, 16, Component.empty()); paramsBox.setMaxLength(4000)
        addRenderableWidget(paramsBox)
        rebuildPresets()

        addRenderableWidget(Button.builder(Component.literal("+ 添加")) { addEffect() }.bounds(bx + 160, 140, 54, 16).build())

        rebuildRemoveButtons()

        // 赋予精灵：把当前特性加入选中精灵的特性池
        assignSearchBox = EditBox(this.font, bx, 306, 56, 16, Component.empty()); assignSearchBox.setMaxLength(64)
        assignSearchBox.setHint(Component.literal("搜索精灵"))
        addRenderableWidget(assignSearchBox)
        rebuildAssignSpeciesDropdown()
        val slotLabels = listOf("特性1", "特性2", "隐藏特性")
        val slotValues = listOf("", "", "h:")
        assignSlotDropdown = Dropdown(bx + 146, 306, 60, 16, slotLabels, slotValues, 4) { assignSlot = it }
        assignSlotDropdown.setSelectedByValue("")
        addRenderableWidget(assignSlotDropdown); dropdowns.add(assignSlotDropdown)
        addRenderableWidget(Button.builder(Component.literal("赋予")) { assignToSpecies() }.bounds(bx + 212, 306, 44, 16).build())

        val by = height - 32
        addRenderableWidget(Button.builder(Component.literal("保存")) { save() }.bounds(left, by, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("返回")) { Minecraft.getInstance().setScreen(EditorHubScreen()) }.bounds(left + 76, by, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("关闭")) { onClose() }.bounds(left + 152, by, 68, 20).build())
    }

    private fun rebuildPresets() {
        if (::presetDropdown.isInitialized) removeWidget(presetDropdown)
        val b = AbilityEffects.byId(currentBrick)
        val presets = b?.presets ?: emptyList()
        if (presets.isEmpty()) return
        presetDropdown = Dropdown(fieldX + 158, 118, 102, 16, presets.map { it.second }, presets.map { it.first }, 8) { v -> paramsBox.value = v }
        addRenderableWidget(presetDropdown); dropdowns.add(presetDropdown)
    }

    private fun rebuildRemoveButtons() {
        for (b in removeButtons) removeWidget(b)
        removeButtons.clear()
        val left = width / 2 - 160
        effects.forEachIndexed { i, _ ->
            val col = i / 6
            val row = i % 6
            val btn = Button.builder(Component.literal("✕")) { removeEffect(i) }.bounds(left + if (col == 0) 150 else 300, 184 + row * 18, 18, 16).build()
            addRenderableWidget(btn)
            removeButtons.add(btn)
        }
    }

    private fun addEffect() {
        if (effects.size >= maxEffects) { msg("最多添加 $maxEffects 条效果"); return }
        val brick = AbilityEffects.byId(currentBrick) ?: return
        val raw = paramsBox.value.trim()
        // 自定义 JS 整段作为一个参数（不按冒号切分）
        val params = if (raw.isEmpty()) emptyList()
            else if (brick.id == "raw_effect") listOf(raw)
            else raw.split(':').map { it.trim() }
        if (!validateBrickParams(brick.paramHints, params)) {
            msg("参数不完整或格式错误（" + brick.paramHints.joinToString("/") + "）；示例：" + brick.presets.firstOrNull()?.first.orEmpty())
            return
        }
        effects.add(currentBrick to raw)
        rebuildRemoveButtons()
    }

    private fun removeEffect(i: Int) {
        if (i in effects.indices) { effects.removeAt(i); rebuildRemoveButtons() }
    }

    private fun filteredAssignSpecies(): List<Triple<String, String, String>> {
        val q = assignSearchBox.value.trim().lowercase()
        if (q.isEmpty()) return speciesList
        return speciesList.filter { (id, en, zh) -> id.contains(q) || en.lowercase().contains(q) || zh.contains(q) }
    }

    private fun rebuildAssignSpeciesDropdown() {
        if (::assignSpeciesDropdown.isInitialized) removeWidget(assignSpeciesDropdown)
        val f = filteredAssignSpecies()
        if (f.isEmpty()) return
        if (f.none { it.first == assignSpecies }) assignSpecies = f[0].first
        assignSpeciesDropdown = Dropdown(fieldX + 62, 306, 78, 16, f.map { it.third }, f.map { it.first }, 8) { assignSpecies = it }
        assignSpeciesDropdown.setSelectedByValue(assignSpecies)
        addRenderableWidget(assignSpeciesDropdown); dropdowns.add(assignSpeciesDropdown)
    }

    /** 把当前特性加入选中精灵的特性池（服务端合并语义）。 */
    private fun assignToSpecies() {
        val rawId = idBox.value.trim().lowercase()
        val abilityId = if (rawId.isEmpty() || !rawId.matches(Regex("[a-z0-9_]+"))) {
            val gen = ClientCatalogs.autoId(nameBox.value.trim())
            idBox.value = gen
            msg("特性 id 已自动生成: $gen（id 只能用小写英文/数字/下划线，中文名请填在名称框）")
            gen
        } else rawId
        if (assignSpecies.isEmpty()) { msg("请选择要赋予的精灵"); return }
        val player = Minecraft.getInstance().player
        if (player == null) { msg("未连接服务器"); return }
        player.connection.sendCommand("cobbleeditor set " + assignSpecies + " abilities=" + assignSlot + abilityId)
        msg("已把 $abilityId 加入 $assignSpecies 的特性池（$assignSlot$abilityId），重进世界后生效")
    }

    private fun save() {
        var id = idBox.value.trim().lowercase()
        if (id.isEmpty() || !id.matches(Regex("[a-z0-9_]+"))) {
            val gen = ClientCatalogs.autoId(nameBox.value.trim())
            id = gen
            idBox.value = gen
            msg("特性 id 已自动生成: $gen（id 只能用小写英文/数字/下划线，中文名请填在名称框）")
        }
        val parts = ArrayList<String>()
        val name = nameBox.value.trim(); if (name.isNotEmpty()) parts.add("name=" + name)
        val desc = descBox.value.trim(); if (desc.isNotEmpty()) parts.add("desc=" + desc)
        if (effects.isNotEmpty()) {
            parts.add("effects=" + effects.joinToString("|") { (b, p) ->
                if (p.isEmpty()) b else {
                    // 自定义 JS 参数 base64 编码（URL 安全无填充），防止冒号/竖线破坏 spec 语法
                    b + ":" + if (b == "raw_effect") encodeRaw(p) else p
                }
            })
        }
        if (parts.isEmpty()) { msg("至少填一项内容"); return }
        val spec = parts.joinToString("&")
        val player = Minecraft.getInstance().player
        if (player == null) { msg("未连接服务器"); return }
        player.connection.sendCommand("cobbleeditor ability add " + id + " " + spec)
        onClose()
    }

    private fun msg(s: String) { Minecraft.getInstance().player?.displayClientMessage(Component.literal(s), false) }

    private fun clip(s: String, maxWidth: Int): String {
        var t = s
        while (t.length > 1 && this.font.width(t) > maxWidth) t = t.dropLast(1)
        return t
    }

    private fun encodeRaw(s: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(s.toByteArray(Charsets.UTF_8))

    override fun tick() {
        val q = assignSearchBox.value.trim().lowercase()
        if (q != lastAssignSearch) { lastAssignSearch = q; rebuildAssignSpeciesDropdown() }
    }

    override fun mouseClicked(mx: Double, my: Double, button: Int): Boolean {
        for (d in dropdowns) if (d.open) { d.handleExpandedClick(mx, my); return true }
        return super.mouseClicked(mx, my, button)
    }

    override fun mouseScrolled(mx: Double, my: Double, h: Double, v: Double): Boolean {
        for (d in dropdowns) if (d.open) { d.handleScroll(v); return true }
        return super.mouseScrolled(mx, my, h, v)
    }

    override fun render(g: GuiGraphics, mx: Int, my: Int, p: Float) {
        g.fill(0, 0, width, height, 0xC0101010.toInt())
        super.render(g, mx, my, p)
        val left = width / 2 - 160
        g.drawCenteredString(this.font, this.title, width / 2, 10, 0xFFFFFF)
        g.drawString(this.font, "id:", left, 40, 0xAAAAAA)
        g.drawString(this.font, "名称:", left, 62, 0xAAAAAA)
        g.drawString(this.font, "描述:", left, 84, 0xAAAAAA)
        g.drawString(this.font, "效果:", left, 122, 0xAAAAAA)
        g.drawString(this.font, "参数:", left, 144, 0xAAAAAA)
        val hintBrick = AbilityEffects.byId(currentBrick)
        val hintText = "参数说明: " + (hintBrick?.paramHints?.takeIf { it.isNotEmpty() }?.joinToString("/") ?: "该效果无参数")
        g.drawString(this.font, hintText, left + 4, 158, 0x888888)
        g.drawString(this.font, "已添加(最多" + maxEffects + "条):", left, 170, 0xAAAAAA)
        g.drawString(this.font, "赋予:", left, 310, 0xAAAAAA)
        effects.forEachIndexed { i, (b, params) ->
            val brick = AbilityEffects.byId(b)
            val presetName = if (params.isNotEmpty()) brick?.presets?.firstOrNull { it.first == params }?.second else null
            val tail = presetName?.let { "（" + it + "）" } ?: if (params.isEmpty()) "" else " (" + params + ")"
            val label = clip((i + 1).toString() + ". " + (brick?.displayName ?: b) + tail, 144)
            val col = i / 6
            val row = i % 6
            g.drawString(this.font, label, left + 4 + col * 150, 186 + row * 18, 0xFFFFFF)
        }
        for (d in dropdowns) d.renderExpanded(g, mx, my)
    }

    override fun isPauseScreen(): Boolean = false
}
