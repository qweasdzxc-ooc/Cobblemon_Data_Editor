package com.xiaoyu.cobblemoneditor.client

import com.xiaoyu.cobblemoneditor.client.widget.Dropdown
import com.xiaoyu.cobblemoneditor.data.CategoryCatalog
import com.xiaoyu.cobblemoneditor.data.EffectBricks
import com.xiaoyu.cobblemoneditor.data.MoveBrickKind
import com.xiaoyu.cobblemoneditor.data.MoveFlagCatalog
import com.xiaoyu.cobblemoneditor.data.PriorityCatalog
import com.xiaoyu.cobblemoneditor.data.TargetCatalog
import com.xiaoyu.cobblemoneditor.data.TypeCatalog
import com.xiaoyu.cobblemoneditor.data.validateBrickParams
import com.xiaoyu.cobblemoneditor.data.isSaneRawJs
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.util.Base64

class MoveEditorScreen : Screen(Component.literal("招式编辑器")) {

    private val dropdowns = mutableListOf<Dropdown>()
    private val removeButtons = mutableListOf<Button>()
    private val speciesList: List<Triple<String, String, String>> = ClientCatalogs.species()
    private lateinit var idBox: EditBox
    private lateinit var nameBox: EditBox
    private lateinit var descBox: EditBox
    private lateinit var powerBox: EditBox
    private lateinit var accBox: EditBox
    private lateinit var ppBox: EditBox
    private lateinit var paramsBox: EditBox
    private lateinit var brickDropdown: Dropdown
    private lateinit var presetDropdown: Dropdown
    private lateinit var assignSearchBox: EditBox
    private lateinit var assignSpeciesDropdown: Dropdown
    private lateinit var assignMethodDropdown: Dropdown
    private lateinit var assignLevelBox: EditBox

    private var type = "electric"
    private var category = "special"
    private var priority = "0"
    private var target = "normal"
    private var flag = "none"
    private var currentBrick = "secondary_status"
    private val bricks = mutableListOf<Pair<String, String>>()
    private val maxBricks = 12
    private var assignSpecies = ""
    private var assignMethod = "level"
    private var lastAssignSearch = ""

    // 标签列（窄）在左，控件列在右；每行的标签都绘制在控件左侧的间隙里，不重叠。
    private val labelX get() = width / 2 - 170
    private val fieldX get() = width / 2 - 110

    override fun init() {
        val left = width / 2 - 170
        val bx = fieldX

        // 第 1 行：id + 名称
        idBox = EditBox(this.font, bx, 18, 100, 16, Component.empty()); idBox.setMaxLength(32)
        idBox.setHint(Component.literal("英文id(可留空)"))
        nameBox = EditBox(this.font, bx + 128, 18, 110, 16, Component.empty()); nameBox.setMaxLength(64)
        nameBox.setHint(Component.literal("招式中文名"))
        addRenderableWidget(idBox); addRenderableWidget(nameBox)

        // 第 2 行：描述
        descBox = EditBox(this.font, bx, 40, 238, 16, Component.empty()); descBox.setMaxLength(200)
        descBox.setHint(Component.literal("招式描述（显示在图鉴/招式详情里）"))
        addRenderableWidget(descBox)

        // 第 3-4 行：属性/分类/优先/目标 两列下拉
        fun dd(x: Int, y: Int, w: Int, labels: List<String>, values: List<String>, cur: String, cb: (String) -> Unit): Dropdown {
            val d = Dropdown(x, y, w, 16, labels, values, 10) { cb(it) }
            d.setSelectedByValue(cur); addRenderableWidget(d); dropdowns.add(d); return d
        }
        dd(bx, 62, 90, TypeCatalog.TYPES.map { it.second }, TypeCatalog.TYPES.map { it.first }, type) { type = it }
        dd(bx + 128, 62, 90, CategoryCatalog.CATEGORIES.map { it.second }, CategoryCatalog.CATEGORIES.map { it.first }, category) { category = it }
        dd(bx, 84, 90, PriorityCatalog.OPTIONS.map { it.first }, PriorityCatalog.OPTIONS.map { it.second.toString() }, priority) { priority = it }
        dd(bx + 128, 84, 90, TargetCatalog.OPTIONS.map { it.first }, TargetCatalog.OPTIONS.map { it.second }, target) { target = it }
        dd(bx, 106, 90, MoveFlagCatalog.OPTIONS.map { it.first }, MoveFlagCatalog.OPTIONS.map { it.second }, flag) { flag = it }

        // 第 5 行：威力/命中/pp（标签画在输入框之间）
        powerBox = EditBox(this.font, bx, 128, 40, 16, Component.empty()); powerBox.setMaxLength(3); powerBox.setHint(Component.literal("0"))
        accBox = EditBox(this.font, bx + 88, 128, 40, 16, Component.empty()); accBox.setMaxLength(3); accBox.setHint(Component.literal("100"))
        ppBox = EditBox(this.font, bx + 168, 128, 40, 16, Component.empty()); ppBox.setMaxLength(3); ppBox.setHint(Component.literal("10"))
        addRenderableWidget(powerBox); addRenderableWidget(accBox); addRenderableWidget(ppBox)

        // 第 6 行：积木 + 预设
        brickDropdown = Dropdown(bx, 150, 150, 16, EffectBricks.ALL.map { it.displayName }, EffectBricks.ALL.map { it.id }, 10) { v ->
            currentBrick = v
            paramsBox.value = EffectBricks.byId(v)?.presets?.firstOrNull()?.first ?: ""
            rebuildPresets()
        }
        addRenderableWidget(brickDropdown); dropdowns.add(brickDropdown)

        // 第 7 行：参数 + 添加
        paramsBox = EditBox(this.font, bx, 172, 150, 16, Component.empty()); paramsBox.setMaxLength(4000)
        addRenderableWidget(paramsBox)
        rebuildPresets()
        addRenderableWidget(Button.builder(Component.literal("+ 添加")) { addBrick() }.bounds(bx + 158, 172, 54, 16).build())
        rebuildRemoveButtons()

        // 第 8 行：赋予精灵（把当前招式写入选中精灵的学习表）
        assignSearchBox = EditBox(this.font, bx, 306, 56, 16, Component.empty()); assignSearchBox.setMaxLength(64)
        assignSearchBox.setHint(Component.literal("搜索精灵"))
        addRenderableWidget(assignSearchBox)
        rebuildAssignSpeciesDropdown()
        val methodLabels = listOf("等级", "蛋招式", "招式机", "传授", "遗传", "特殊")
        val methodValues = listOf("level", "egg", "tm", "tutor", "legacy", "special")
        assignMethodDropdown = Dropdown(bx + 146, 306, 56, 16, methodLabels, methodValues, 7) { assignMethod = it }
        assignMethodDropdown.setSelectedByValue("level")
        addRenderableWidget(assignMethodDropdown); dropdowns.add(assignMethodDropdown)
        assignLevelBox = EditBox(this.font, bx + 208, 306, 20, 16, Component.empty()); assignLevelBox.setMaxLength(3)
        assignLevelBox.setHint(Component.literal("级"))
        addRenderableWidget(assignLevelBox)
        addRenderableWidget(Button.builder(Component.literal("赋予")) { assignToSpecies() }.bounds(bx + 234, 306, 40, 16).build())

        val by = height - 32
        addRenderableWidget(Button.builder(Component.literal("保存")) { save() }.bounds(left, by, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("返回")) { Minecraft.getInstance().setScreen(EditorHubScreen()) }.bounds(left + 76, by, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("关闭")) { onClose() }.bounds(left + 152, by, 68, 20).build())
    }

    private fun rebuildPresets() {
        if (::presetDropdown.isInitialized) removeWidget(presetDropdown)
        val b = EffectBricks.byId(currentBrick)
        val presets = b?.presets ?: emptyList()
        if (presets.isEmpty()) return
        presetDropdown = Dropdown(fieldX + 158, 150, 102, 16, presets.map { it.second }, presets.map { it.first }, 8) { v -> paramsBox.value = v }
        addRenderableWidget(presetDropdown); dropdowns.add(presetDropdown)
    }

    private fun rebuildRemoveButtons() {
        for (b in removeButtons) removeWidget(b)
        removeButtons.clear()
        val left = width / 2 - 170
        bricks.forEachIndexed { i, _ ->
            val col = i / 6
            val row = i % 6
            val btn = Button.builder(Component.literal("✕")) { removeBrick(i) }.bounds(left + if (col == 0) 150 else 300, 198 + row * 18, 18, 16).build()
            addRenderableWidget(btn)
            removeButtons.add(btn)
        }
    }

    private fun addBrick() {
        if (bricks.size >= maxBricks) { msg("最多添加 $maxBricks 条效果"); return }
        val brick = EffectBricks.byId(currentBrick) ?: return
        val raw = paramsBox.value.trim()
        // 自定义 JS 整段作为一个参数（不按冒号切分）
        val params = if (raw.isEmpty()) emptyList()
            else if (brick.kind == MoveBrickKind.RAW_HANDLER) listOf(raw)
            else raw.split(':').map { it.trim() }
        if (!validateBrickParams(brick.paramHints, params)) {
            msg("参数不完整或格式错误（" + brick.paramHints.joinToString("/") + "）；示例：" + brick.presets.firstOrNull()?.first.orEmpty())
            return
        }
        if (brick.kind == MoveBrickKind.RAW_HANDLER && !isSaneRawJs(raw)) {
            msg("自定义JS无效：必须是完整的JS事件处理函数（如 onTryHit(target, source, move) { return null; }），效果名不能直接当JS")
            return
        }
        bricks.add(currentBrick to raw)
        rebuildRemoveButtons()
    }

    private fun removeBrick(i: Int) {
        if (i in bricks.indices) { bricks.removeAt(i); rebuildRemoveButtons() }
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

    /** 把当前招式加入选中精灵的学习表（服务端合并语义，无需重填全部招式）。 */
    private fun assignToSpecies() {
        val rawId = idBox.value.trim().lowercase()
        val moveId = if (rawId.isEmpty() || !rawId.matches(Regex("[a-z0-9_]+"))) {
            val gen = ClientCatalogs.autoId(nameBox.value.trim())
            idBox.value = gen
            msg("招式 id 已自动生成: $gen（id 只能用小写英文/数字/下划线，中文名请填在名称框）")
            gen
        } else rawId
        if (assignSpecies.isEmpty()) { msg("请选择要赋予的精灵"); return }
        val entry = if (assignMethod == "level") {
            val lvl = assignLevelBox.value.trim().toIntOrNull()
            if (lvl == null || lvl < 1 || lvl > 100) { msg("等级必须是 1-100"); return }
            "$lvl:$moveId"
        } else "$assignMethod:$moveId"
        val player = Minecraft.getInstance().player
        if (player == null) { msg("未连接服务器"); return }
        player.connection.sendCommand("cobbleeditor set " + assignSpecies + " moves=" + entry)
        msg("已把 $moveId 加入 $assignSpecies 的学习表（$entry），重进世界后生效")
    }

    private fun save() {
        var id = idBox.value.trim().lowercase()
        if (id.isEmpty() || !id.matches(Regex("[a-z0-9_]+"))) {
            val gen = ClientCatalogs.autoId(nameBox.value.trim())
            id = gen
            idBox.value = gen
            msg("招式 id 已自动生成: $gen（id 只能用小写英文/数字/下划线，中文名请填在名称框）")
        }
        val parts = ArrayList<String>()
        val name = nameBox.value.trim(); if (name.isNotEmpty()) parts.add("name=" + name)
        val desc = descBox.value.trim(); if (desc.isNotEmpty()) parts.add("desc=" + desc)
        val power = powerBox.value.trim(); if (power.isNotEmpty()) { if (power.toIntOrNull() == null) { msg("威力必须是数字"); return }; parts.add("power=" + power) }
        val acc = accBox.value.trim(); if (acc.isNotEmpty()) { if (acc.toIntOrNull() == null) { msg("命中必须是数字"); return }; parts.add("accuracy=" + acc) }
        val pp = ppBox.value.trim(); if (pp.isNotEmpty()) parts.add("pp=" + pp)
        parts.add("type=" + type)
        parts.add("category=" + category)
        parts.add("priority=" + priority)
        parts.add("target=" + target)
        parts.add("flags=" + flag)
        if (bricks.isNotEmpty()) {
            parts.add("bricks=" + bricks.joinToString("|") { (b, p) ->
                if (p.isEmpty()) b else {
                    val brick = EffectBricks.byId(b)
                    // 自定义 JS 参数 base64 编码（URL 安全无填充），防止冒号/竖线破坏 spec 语法
                    b + ":" + if (brick?.kind == MoveBrickKind.RAW_HANDLER) encodeRaw(p) else p
                }
            })
        }
        val spec = parts.joinToString("&")
        val player = Minecraft.getInstance().player
        if (player == null) { msg("未连接服务器"); return }
        player.connection.sendCommand("cobbleeditor move add " + id + " " + spec)
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
        val left = width / 2 - 170
        g.drawCenteredString(this.font, this.title, width / 2, 2, 0xFFFFFF)
        g.drawString(this.font, "id:", left, 22, 0xAAAAAA)
        g.drawString(this.font, "名称:", left + 120, 22, 0xAAAAAA)
        g.drawString(this.font, "描述:", left, 44, 0xAAAAAA)
        g.drawString(this.font, "属性:", left, 66, 0xAAAAAA)
        g.drawString(this.font, "分类:", left + 118, 66, 0xAAAAAA)
        g.drawString(this.font, "优先:", left, 88, 0xAAAAAA)
        g.drawString(this.font, "目标:", left + 118, 88, 0xAAAAAA)
        g.drawString(this.font, "子标签:", left, 110, 0xAAAAAA)
        g.drawString(this.font, "威力:", left, 132, 0xAAAAAA)
        g.drawString(this.font, "命中:", left + 114, 132, 0xAAAAAA)
        g.drawString(this.font, "pp:", left + 202, 132, 0xAAAAAA)
        g.drawString(this.font, "积木:", left, 154, 0xAAAAAA)
        g.drawString(this.font, "参数:", left, 176, 0xAAAAAA)
        val hintBrick = EffectBricks.byId(currentBrick)
        val hintText = "参数说明: " + (hintBrick?.paramHints?.takeIf { it.isNotEmpty() }?.joinToString("/") ?: "该效果无参数")
        g.drawString(this.font, hintText, left + 4, 188, 0x888888)
        g.drawString(this.font, "赋予:", left, 310, 0xAAAAAA)
        bricks.forEachIndexed { i, (b, params) ->
            val brick = EffectBricks.byId(b)
            val presetName = if (params.isNotEmpty()) brick?.presets?.firstOrNull { it.first == params }?.second else null
            val tail = presetName?.let { "（" + it + "）" } ?: if (params.isEmpty()) "" else " (" + params + ")"
            val label = clip((i + 1).toString() + ". " + (brick?.displayName ?: b) + tail, 144)
            val col = i / 6
            val row = i % 6
            g.drawString(this.font, label, left + 4 + col * 150, 200 + row * 18, 0xFFFFFF)
        }
        for (d in dropdowns) d.renderExpanded(g, mx, my)
    }

    override fun isPauseScreen(): Boolean = false
}
