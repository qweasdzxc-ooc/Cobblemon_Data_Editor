package com.xiaoyu.cobblemoneditor.client

import com.xiaoyu.cobblemoneditor.client.widget.Dropdown
import com.xiaoyu.cobblemoneditor.data.CategoryCatalog
import com.xiaoyu.cobblemoneditor.data.EffectBricks
import com.xiaoyu.cobblemoneditor.data.MoveFlagCatalog
import com.xiaoyu.cobblemoneditor.data.PriorityCatalog
import com.xiaoyu.cobblemoneditor.data.TargetCatalog
import com.xiaoyu.cobblemoneditor.data.TypeCatalog
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class MoveEditorScreen : Screen(Component.literal("招式编辑器")) {

    private val dropdowns = mutableListOf<Dropdown>()
    private val removeButtons = mutableListOf<Button>()
    private lateinit var idBox: EditBox
    private lateinit var nameBox: EditBox
    private lateinit var powerBox: EditBox
    private lateinit var accBox: EditBox
    private lateinit var ppBox: EditBox
    private lateinit var paramsBox: EditBox
    private lateinit var brickDropdown: Dropdown
    private lateinit var presetDropdown: Dropdown

    private var type = "electric"
    private var category = "special"
    private var priority = "0"
    private var target = "normal"
    private var flag = "none"
    private var currentBrick = "secondary_status"
    private val bricks = mutableListOf<Pair<String, String>>()
    private val maxBricks = 6

    // 标签列（窄）在左，控件列在右
    private val labelX get() = width / 2 - 170
    private val fieldX get() = width / 2 - 170 + 60

    override fun init() {
        val left = width / 2 - 170
        val bx = fieldX

        idBox = EditBox(this.font, bx, 22, 100, 16, Component.empty()); idBox.setMaxLength(32)
        nameBox = EditBox(this.font, bx + 108, 22, 100, 16, Component.empty()); nameBox.setMaxLength(64)
        powerBox = EditBox(this.font, bx, 42, 40, 16, Component.empty()); powerBox.setMaxLength(3)
        accBox = EditBox(this.font, bx + 48, 42, 40, 16, Component.empty()); accBox.setMaxLength(3)
        ppBox = EditBox(this.font, bx + 96, 42, 40, 16, Component.empty()); ppBox.setMaxLength(3)
        addRenderableWidget(idBox); addRenderableWidget(nameBox)
        addRenderableWidget(powerBox); addRenderableWidget(accBox); addRenderableWidget(ppBox)

        fun dd(y: Int, w: Int, labels: List<String>, values: List<String>, cur: String, cb: (String) -> Unit): Dropdown {
            val d = Dropdown(bx, y, w, 16, labels, values, 10) { cb(it) }
            d.setSelectedByValue(cur); addRenderableWidget(d); dropdowns.add(d); return d
        }
        dd(62, 90, TypeCatalog.TYPES.map { it.second }, TypeCatalog.TYPES.map { it.first }, type) { type = it }
        dd(82, 90, CategoryCatalog.CATEGORIES.map { it.second }, CategoryCatalog.CATEGORIES.map { it.first }, category) { category = it }
        dd(102, 90, PriorityCatalog.OPTIONS.map { it.first }, PriorityCatalog.OPTIONS.map { it.second.toString() }, priority) { priority = it }
        dd(122, 90, TargetCatalog.OPTIONS.map { it.first }, TargetCatalog.OPTIONS.map { it.second }, target) { target = it }
        dd(142, 90, MoveFlagCatalog.OPTIONS.map { it.first }, MoveFlagCatalog.OPTIONS.map { it.second }, flag) { flag = it }

        brickDropdown = Dropdown(bx, 166, 150, 16, EffectBricks.ALL.map { it.displayName }, EffectBricks.ALL.map { it.id }, 10) { v ->
            currentBrick = v
            paramsBox.value = EffectBricks.byId(v)?.presets?.firstOrNull()?.first ?: ""
            rebuildPresets()
        }
        addRenderableWidget(brickDropdown); dropdowns.add(brickDropdown)

        paramsBox = EditBox(this.font, bx, 188, 150, 16, Component.empty()); paramsBox.setMaxLength(100)
        addRenderableWidget(paramsBox)
        rebuildPresets()

        addRenderableWidget(Button.builder(Component.literal("+ 添加")) { addBrick() }.bounds(bx + 158, 188, 54, 16).build())
        rebuildRemoveButtons()

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
        presetDropdown = Dropdown(fieldX + 158, 166, 102, 16, presets.map { it.second }, presets.map { it.first }, 8) { v -> paramsBox.value = v }
        addRenderableWidget(presetDropdown); dropdowns.add(presetDropdown)
    }

    private fun rebuildRemoveButtons() {
        for (b in removeButtons) removeWidget(b)
        removeButtons.clear()
        val left = width / 2 - 170
        bricks.forEachIndexed { i, _ ->
            val y = 232 + i * 18
            val btn = Button.builder(Component.literal("✕")) { removeBrick(i) }.bounds(left + 250, y, 18, 16).build()
            addRenderableWidget(btn)
            removeButtons.add(btn)
        }
    }

    private fun addBrick() {
        if (bricks.size >= maxBricks) { msg("最多添加 $maxBricks 条效果"); return }
        bricks.add(currentBrick to paramsBox.value.trim())
        rebuildRemoveButtons()
    }

    private fun removeBrick(i: Int) {
        if (i in bricks.indices) { bricks.removeAt(i); rebuildRemoveButtons() }
    }

    private fun save() {
        val id = idBox.value.trim().lowercase()
        if (id.isEmpty()) { msg("请填写招式 id（英文）"); return }
        val parts = ArrayList<String>()
        val name = nameBox.value.trim(); if (name.isNotEmpty()) parts.add("name=" + name)
        val power = powerBox.value.trim(); if (power.isNotEmpty()) { if (power.toIntOrNull() == null) { msg("威力必须是数字"); return }; parts.add("power=" + power) }
        val acc = accBox.value.trim(); if (acc.isNotEmpty()) parts.add("accuracy=" + acc)
        val pp = ppBox.value.trim(); if (pp.isNotEmpty()) parts.add("pp=" + pp)
        parts.add("type=" + type)
        parts.add("category=" + category)
        parts.add("priority=" + priority)
        parts.add("target=" + target)
        parts.add("flags=" + flag)
        if (bricks.isNotEmpty()) {
            parts.add("bricks=" + bricks.joinToString("|") { (b, p) -> b + (if (p.isEmpty()) "" else ":" + p) })
        }
        val spec = parts.joinToString("&")
        val player = Minecraft.getInstance().player
        if (player == null) { msg("未连接服务器"); return }
        player.connection.sendCommand("cobbleeditor move add " + id + " " + spec)
        onClose()
    }

    private fun msg(s: String) { Minecraft.getInstance().player?.displayClientMessage(Component.literal(s), false) }

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
        g.drawCenteredString(this.font, this.title, width / 2, 6, 0xFFFFFF)
        g.drawString(this.font, "id:", left, 26, 0xAAAAAA)
        g.drawString(this.font, "名称:", left + 112, 26, 0xAAAAAA)
        g.drawString(this.font, "威力:", left, 46, 0xAAAAAA)
        g.drawString(this.font, "命中:", left + 50, 46, 0xAAAAAA)
        g.drawString(this.font, "pp:", left + 98, 46, 0xAAAAAA)
        g.drawString(this.font, "属性:", left, 66, 0xAAAAAA)
        g.drawString(this.font, "分类:", left, 86, 0xAAAAAA)
        g.drawString(this.font, "优先:", left, 106, 0xAAAAAA)
        g.drawString(this.font, "目标:", left, 126, 0xAAAAAA)
        g.drawString(this.font, "子标签:", left, 146, 0xAAAAAA)
        g.drawString(this.font, "积木:", left, 170, 0xAAAAAA)
        g.drawString(this.font, "参数:", left, 192, 0xAAAAAA)
        g.drawString(this.font, "已添加(最多" + maxBricks + "条):", left, 216, 0xAAAAAA)
        bricks.forEachIndexed { i, (b, params) ->
            val brick = EffectBricks.byId(b)
            val label = (i + 1).toString() + ". " + (brick?.displayName ?: b) + (if (params.isEmpty()) "" else " (" + params + ")")
            g.drawString(this.font, label, left + 4, 234 + i * 18, 0xFFFFFF)
        }
        for (d in dropdowns) d.renderExpanded(g, mx, my)
    }

    override fun isPauseScreen(): Boolean = false
}
