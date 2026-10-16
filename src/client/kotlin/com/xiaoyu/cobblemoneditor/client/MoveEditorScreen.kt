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
    private lateinit var idBox: EditBox
    private lateinit var nameBox: EditBox
    private lateinit var powerBox: EditBox
    private lateinit var accBox: EditBox
    private lateinit var ppBox: EditBox

    private var type = "electric"
    private var category = "special"
    private var priority = "0"
    private var target = "normal"
    private var brickId = "secondary_status"
    private var paramsValue = "10:brn"
    private val selectedFlags = mutableSetOf<String>()
    private lateinit var paramsDropdown: Dropdown

    override fun init() {
        val left = width / 2 - 160
        fun txt(y: Int, w: Int, maxLen: Int): EditBox {
            val b = EditBox(this.font, left + 100, y, w, 16, Component.empty())
            b.setMaxLength(maxLen); addRenderableWidget(b); return b
        }
        idBox = txt(22, 110, 32)
        nameBox = txt(42, 110, 64)
        powerBox = txt(62, 50, 3)
        accBox = txt(82, 50, 3)
        ppBox = txt(102, 50, 3)

        fun dd(y: Int, w: Int, labels: List<String>, values: List<String>, cur: String, cb: (String) -> Unit): Dropdown {
            val d = Dropdown(left + 100, y, w, 16, labels, values, cb)
            d.setSelectedByValue(cur); addRenderableWidget(d); dropdowns.add(d); return d
        }

        dd(124, 96, TypeCatalog.TYPES.map { it.second }, TypeCatalog.TYPES.map { it.first }, type) { type = it }
        dd(144, 96, CategoryCatalog.CATEGORIES.map { it.second }, CategoryCatalog.CATEGORIES.map { it.first }, category) { category = it }
        dd(164, 96, PriorityCatalog.OPTIONS.map { it.first }, PriorityCatalog.OPTIONS.map { it.second.toString() }, priority) { priority = it }
        dd(184, 96, TargetCatalog.OPTIONS.map { it.first }, TargetCatalog.OPTIONS.map { it.second }, target) { target = it }

        var fx = left + 100; var fy = 204
        for (f in MoveFlagCatalog.FLAGS) {
            val flagId = f.second
            addRenderableWidget(Button.builder(Component.literal(f.first + (if (flagId in selectedFlags) " ✓" else ""))) {
                if (flagId in selectedFlags) selectedFlags.remove(flagId) else selectedFlags.add(flagId)
            }.bounds(fx, fy, 48, 16).build())
            fx += 52
            if (fx > left + 308) { fx = left + 100; fy += 18 }
        }

        dd(fy + 2, 150, EffectBricks.ALL.map { it.displayName }, EffectBricks.ALL.map { it.id }, brickId) { v ->
            brickId = v
            val b = EffectBricks.byId(v)
            paramsValue = b?.presets?.firstOrNull()?.first ?: ""
            rebuildParamsDropdown(fy + 20)
        }
        rebuildParamsDropdown(fy + 20)

        addRenderableWidget(Button.builder(Component.literal("保存")) { save() }.bounds(left, fy + 42, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("返回")) { Minecraft.getInstance().setScreen(EditorHubScreen()) }.bounds(left + 76, fy + 42, 68, 20).build())
        addRenderableWidget(Button.builder(Component.literal("关闭")) { onClose() }.bounds(left + 152, fy + 42, 68, 20).build())
    }

    private fun rebuildParamsDropdown(y: Int) {
        if (::paramsDropdown.isInitialized) removeWidget(paramsDropdown)
        val b = EffectBricks.byId(brickId)
        val presets = b?.presets ?: emptyList()
        if (presets.isEmpty()) return
        paramsDropdown = Dropdown(width / 2 - 160 + 100, y, 150, 16, presets.map { it.second }, presets.map { it.first }) { paramsValue = it }
        paramsDropdown.setSelectedByValue(paramsValue)
        addRenderableWidget(paramsDropdown); dropdowns.add(paramsDropdown)
    }

    private fun save() {
        val id = idBox.value.trim().lowercase()
        if (id.isEmpty()) { msg("请填招式 id"); return }
        val parts = ArrayList<String>()
        val name = nameBox.value.trim(); if (name.isNotEmpty()) parts.add("name=" + name)
        val power = powerBox.value.trim(); if (power.isNotEmpty()) { if (power.toIntOrNull() == null) { msg("威力必须是数字"); return }; parts.add("power=" + power) }
        val acc = accBox.value.trim(); if (acc.isNotEmpty()) parts.add("accuracy=" + acc)
        val pp = ppBox.value.trim(); if (pp.isNotEmpty()) parts.add("pp=" + pp)
        parts.add("type=" + type)
        parts.add("category=" + category)
        parts.add("priority=" + priority)
        parts.add("target=" + target)
        if (selectedFlags.isNotEmpty()) parts.add("flags=" + selectedFlags.joinToString(","))
        parts.add("bricks=" + brickId + (if (paramsValue.isEmpty()) "" else ":" + paramsValue))
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

    override fun render(g: GuiGraphics, mx: Int, my: Int, p: Float) {
        super.render(g, mx, my, p)
        val left = width / 2 - 160
        g.drawCenteredString(this.font, this.title, width / 2, 8, 0xFFFFFF)
        g.drawString(this.font, "id:", left, 26, 0xAAAAAA)
        g.drawString(this.font, "名称:", left, 46, 0xAAAAAA)
        g.drawString(this.font, "威力:", left, 66, 0xAAAAAA)
        g.drawString(this.font, "命中:", left, 86, 0xAAAAAA)
        g.drawString(this.font, "pp:", left, 106, 0xAAAAAA)
        g.drawString(this.font, "属性:", left, 128, 0xAAAAAA)
        g.drawString(this.font, "分类:", left, 148, 0xAAAAAA)
        g.drawString(this.font, "优先度:", left, 168, 0xAAAAAA)
        g.drawString(this.font, "目标:", left, 188, 0xAAAAAA)
        g.drawString(this.font, "子标签:", left, 208, 0xAAAAAA)
    }

    override fun isPauseScreen(): Boolean = false
}
