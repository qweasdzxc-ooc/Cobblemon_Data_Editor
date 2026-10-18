package com.xiaoyu.cobblemoneditor.client

import com.xiaoyu.cobblemoneditor.client.widget.Dropdown
import com.xiaoyu.cobblemoneditor.data.HeldItemBrick
import com.xiaoyu.cobblemoneditor.data.HeldItemBricks
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * 携带道具效果编辑器：定义道具效果（data/cobblemoneditor/held_items 下的 .js 文件），
 * 并把效果绑定到任意 MC 物品（cobblemon:held_item_effect 组件）后直接获得。
 */
class HeldItemEditorScreen : Screen(Component.literal("携带道具 · 编辑器")) {
    private val labelX = width / 2 - 170
    private val fieldX = width / 2 - 110
    private val left = labelX
    private val bx = fieldX

    private lateinit var idBox: EditBox
    private lateinit var nameBox: EditBox
    private lateinit var descBox: EditBox
    private lateinit var paramsBox: EditBox
    private lateinit var itemBox: EditBox

    private val dropdowns = mutableListOf<Dropdown>()
    private lateinit var brickDropdown: Dropdown
    private lateinit var presetDropdown: Dropdown
    private val added = mutableListOf<Pair<HeldItemBrick, List<String>>>()
    private val removeButtons = mutableListOf<Button>()
    private var status = ""

    private fun rebuildBrickDropdown() {
        val labels = HeldItemBricks.ALL.map { it.displayName }
        val values = HeldItemBricks.ALL.map { it.id }
        val old = if (::brickDropdown.isInitialized) brickDropdown.selectedValue else null
        brickDropdown = Dropdown(bx, 62, 150, 14, labels, values, 10) { onBrickChanged() }
        addRenderableWidget(brickDropdown); dropdowns.add(brickDropdown)
        if (old != null) brickDropdown.setSelectedByValue(old)
        rebuildPresetDropdown()
    }

    private fun rebuildPresetDropdown() {
        if (::presetDropdown.isInitialized) removeWidget(presetDropdown)
        val brick = HeldItemBricks.byId(brickDropdown.selectedValue)
        val presets = brick?.presets ?: emptyList()
        val labels = if (presets.isEmpty()) listOf("(无预设)") else presets.map { it.joinToString(":") }
        val values = if (presets.isEmpty()) listOf("") else presets.map { it.joinToString(":") }
        presetDropdown = Dropdown(bx + 158, 62, 102, 14, labels, values, 6) { preset ->
            if (preset.isNotEmpty()) paramsBox.value = preset
        }
        addRenderableWidget(presetDropdown); dropdowns.add(presetDropdown)
    }

    private fun onBrickChanged() {
        rebuildPresetDropdown()
        val brick = HeldItemBricks.byId(brickDropdown.selectedValue)
        paramsBox.setHint(Component.literal(brick?.paramHints?.joinToString(":") ?: "参数"))
        brick?.presets?.firstOrNull()?.let { paramsBox.value = it.joinToString(":") }
    }

    private fun rebuildRemoveButtons() {
        for (b in removeButtons) removeWidget(b)
        removeButtons.clear()
        var y = 122
        for ((brick, _) in added) {
            val idx = added.indexOfFirst { it.first.id == brick.id }
            val btn = Button.builder(Component.literal("✕")) {
                if (idx in added.indices) {
                    added.removeAt(idx)
                    rebuildRemoveButtons()
                }
            }.bounds(left + 250, y - 4, 20, 12).build()
            removeButtons.add(btn)
            addRenderableWidget(btn)
            y += 16
        }
    }

    override fun init() {
        idBox = EditBox(this.font, bx, 18, 100, 14, Component.empty())
        idBox.setMaxLength(32)
        idBox.setHint(Component.literal("效果id(小写字母数字)"))
        addRenderableWidget(idBox)
        nameBox = EditBox(this.font, bx + 128, 18, 110, 14, Component.empty())
        nameBox.setMaxLength(32)
        nameBox.setHint(Component.literal("中文名(可选)"))
        addRenderableWidget(nameBox)
        descBox = EditBox(this.font, bx, 40, 238, 14, Component.empty())
        descBox.setMaxLength(200)
        descBox.setHint(Component.literal("描述(可选)"))
        addRenderableWidget(descBox)
        paramsBox = EditBox(this.font, bx, 84, 150, 14, Component.empty())
        paramsBox.setMaxLength(64)
        paramsBox.setHint(Component.literal("参数 如 80:fire"))
        addRenderableWidget(paramsBox)

        rebuildBrickDropdown()
        addRenderableWidget(Button.builder(Component.literal("添加")) { addBrick() }.bounds(bx + 158, 84, 54, 14).build())
        addRenderableWidget(Button.builder(Component.literal("清空")) {
            added.clear()
            rebuildRemoveButtons()
        }.bounds(bx + 218, 84, 40, 14).build())

        itemBox = EditBox(this.font, bx, 196, 180, 14, Component.empty())
        itemBox.setMaxLength(64)
        itemBox.setHint(Component.literal("minecraft:diamond"))
        addRenderableWidget(itemBox)
        addRenderableWidget(Button.builder(Component.literal("给我一个")) { giveItem() }.bounds(bx + 188, 196, 70, 14).build())

        addRenderableWidget(Button.builder(Component.literal("保存")) { save() }.bounds(width / 2 - 118, height - 28, 72, 20).build())
        addRenderableWidget(Button.builder(Component.literal("返回")) { onClose() }.bounds(width / 2 - 38, height - 28, 72, 20).build())
        addRenderableWidget(Button.builder(Component.literal("关闭")) { Minecraft.getInstance().setScreen(null) }.bounds(width / 2 + 42, height - 28, 72, 20).build())
    }

    private fun addBrick() {
        val brick = HeldItemBricks.byId(brickDropdown.selectedValue)
        if (brick == null) {
            status = "请先选择积木"
            return
        }
        val params = paramsBox.value.split(":").map { it.trim() }.filter { it.isNotEmpty() }
        if (!HeldItemBricks.validParams(brick, params)) {
            status = "参数无效（需要 ${brick.paramHints.joinToString("/")}）"
            return
        }
        if (added.size >= 6) {
            status = "最多 6 个效果"
            return
        }
        if (added.none { it.first.id == brick.id }) {
            added.add(brick to params)
            rebuildRemoveButtons()
        }
        status = ""
    }

    private fun save() {
        val id = idBox.value.trim()
        if (!id.matches(Regex("[a-z0-9]+"))) {
            status = "效果id 必须是小写字母数字"
            return
        }
        val parts = mutableListOf<String>()
        if (nameBox.value.isNotBlank()) parts.add("name=" + nameBox.value.trim())
        if (descBox.value.isNotBlank()) parts.add("desc=" + descBox.value.trim())
        if (added.isNotEmpty()) {
            parts.add("bricks=" + added.joinToString("|") { (b, p) -> if (p.isEmpty()) b.id else b.id + ":" + p.joinToString(":") })
        }
        sendCommand("cobbleeditor item add $id ${parts.joinToString("&")}")
        status = "已发送保存命令"
    }

    private fun giveItem() {
        val id = idBox.value.trim()
        val itemId = itemBox.value.trim()
        if (!id.matches(Regex("[a-z0-9]+"))) {
            status = "先填写有效的效果id"
            return
        }
        if (itemId.isEmpty()) {
            status = "填写载体物品（如 minecraft:diamond）"
            return
        }
        sendCommand("cobbleeditor item give $id $itemId")
        status = "已发送给予命令"
    }

    private fun sendCommand(cmd: String) {
        val player = Minecraft.getInstance().player ?: return
        player.connection.sendCommand(cmd)
    }

    override fun render(g: GuiGraphics, mx: Int, my: Int, delta: Float) {
        super.render(g, mx, my, delta)
        g.drawString(this.font, "效果id:", left, 22, 0xAAAAAA)
        g.drawString(this.font, "中文名:", left + 134, 22, 0xAAAAAA)
        g.drawString(this.font, "描述:", left, 44, 0xAAAAAA)
        g.drawString(this.font, "积木:", left, 66, 0xAAAAAA)
        g.drawString(this.font, "预设:", left + 190, 66, 0xAAAAAA)
        g.drawString(this.font, "参数:", left, 88, 0xAAAAAA)
        g.drawString(this.font, "已添加(最多6条):", left, 108, 0xAAAAAA)
        g.drawString(this.font, "载体物品:", left, 200, 0xAAAAAA)
        g.drawString(this.font, "保存效果后点“给我一个”获得带组件的物品，放进精灵携带槽生效", left, 220, 0x888888)
        var y = 122
        for ((brick, params) in added) {
            val text = brick.displayName + if (params.isEmpty()) "" else "（" + params.joinToString(":") + "）"
            g.drawString(this.font, text, left + 4, y, 0xCCCCCC)
            y += 16
        }
        g.drawString(this.font, status, left, height - 52, 0xFFFF55)
        for (d in dropdowns) d.renderExpanded(g, mx, my)
    }

    override fun mouseClicked(mx: Double, my: Double, button: Int): Boolean {
        for (d in dropdowns) if (d.open) { d.handleExpandedClick(mx, my); return true }
        return super.mouseClicked(mx, my, button)
    }

    override fun mouseScrolled(mx: Double, my: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        for (d in dropdowns) if (d.open) { d.handleScroll(verticalAmount); return true }
        return super.mouseScrolled(mx, my, horizontalAmount, verticalAmount)
    }

    override fun isPauseScreen(): Boolean = false
}
