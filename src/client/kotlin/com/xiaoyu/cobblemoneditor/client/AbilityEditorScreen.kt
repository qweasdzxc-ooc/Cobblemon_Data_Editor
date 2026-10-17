package com.xiaoyu.cobblemoneditor.client

import com.xiaoyu.cobblemoneditor.client.widget.Dropdown
import com.xiaoyu.cobblemoneditor.data.AbilityEffects
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class AbilityEditorScreen : Screen(Component.literal("特性编辑器")) {

    private val dropdowns = mutableListOf<Dropdown>()
    private val removeButtons = mutableListOf<Button>()
    private lateinit var idBox: EditBox
    private lateinit var nameBox: EditBox
    private lateinit var descBox: EditBox
    private lateinit var paramsBox: EditBox
    private lateinit var brickDropdown: Dropdown
    private lateinit var presetDropdown: Dropdown
    private var currentBrick = "start_boost"
    private val effects = mutableListOf<Pair<String, String>>()
    private val maxEffects = 6

    // 标签列（窄）在左，控件列在右，互不重叠
    private val labelX get() = width / 2 - 160
    private val fieldX get() = width / 2 - 160 + 56

    override fun init() {
        val left = width / 2 - 160
        val bx = fieldX

        idBox = EditBox(this.font, bx, 36, 260, 16, Component.empty()); idBox.setMaxLength(32)
        nameBox = EditBox(this.font, bx, 58, 260, 16, Component.empty()); nameBox.setMaxLength(64)
        descBox = EditBox(this.font, bx, 80, 260, 16, Component.empty()); descBox.setMaxLength(200)
        addRenderableWidget(idBox); addRenderableWidget(nameBox); addRenderableWidget(descBox)

        brickDropdown = Dropdown(bx, 118, 150, 16, AbilityEffects.ALL.map { it.displayName }, AbilityEffects.ALL.map { it.id }, 10) { v ->
            currentBrick = v
            paramsBox.value = AbilityEffects.byId(v)?.presets?.firstOrNull()?.first ?: ""
            rebuildPresets()
        }
        addRenderableWidget(brickDropdown); dropdowns.add(brickDropdown)

        paramsBox = EditBox(this.font, bx, 140, 150, 16, Component.empty()); paramsBox.setMaxLength(100)
        addRenderableWidget(paramsBox)
        rebuildPresets()

        addRenderableWidget(Button.builder(Component.literal("+ 添加")) { addEffect() }.bounds(bx + 160, 140, 54, 16).build())

        rebuildRemoveButtons()

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
            val y = 184 + i * 18
            val btn = Button.builder(Component.literal("✕")) { removeEffect(i) }.bounds(left + 250, y, 18, 16).build()
            addRenderableWidget(btn)
            removeButtons.add(btn)
        }
    }

    private fun addEffect() {
        if (effects.size >= maxEffects) { msg("最多添加 $maxEffects 条效果"); return }
        effects.add(currentBrick to paramsBox.value.trim())
        rebuildRemoveButtons()
    }

    private fun removeEffect(i: Int) {
        if (i in effects.indices) { effects.removeAt(i); rebuildRemoveButtons() }
    }

    private fun save() {
        val id = idBox.value.trim().lowercase()
        if (id.isEmpty()) { msg("请填写 id（英文）"); return }
        val parts = ArrayList<String>()
        val name = nameBox.value.trim(); if (name.isNotEmpty()) parts.add("name=" + name)
        val desc = descBox.value.trim(); if (desc.isNotEmpty()) parts.add("desc=" + desc)
        if (effects.isNotEmpty()) {
            parts.add("effects=" + effects.joinToString("|") { (b, p) -> b + (if (p.isEmpty()) "" else ":" + p) })
        }
        if (parts.isEmpty()) { msg("至少填一项内容"); return }
        val spec = parts.joinToString("&")
        val player = Minecraft.getInstance().player
        if (player == null) { msg("未连接服务器"); return }
        player.connection.sendCommand("cobbleeditor ability add " + id + " " + spec)
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
        val left = width / 2 - 160
        g.drawCenteredString(this.font, this.title, width / 2, 10, 0xFFFFFF)
        g.drawString(this.font, "id:", left, 40, 0xAAAAAA)
        g.drawString(this.font, "名称:", left, 62, 0xAAAAAA)
        g.drawString(this.font, "描述:", left, 84, 0xAAAAAA)
        g.drawString(this.font, "效果:", left, 122, 0xAAAAAA)
        g.drawString(this.font, "参数:", left, 144, 0xAAAAAA)
        g.drawString(this.font, "已添加(最多" + maxEffects + "条):", left, 168, 0xAAAAAA)
        effects.forEachIndexed { i, (b, params) ->
            val brick = AbilityEffects.byId(b)
            val label = (i + 1).toString() + ". " + (brick?.displayName ?: b) + (if (params.isEmpty()) "" else " (" + params + ")")
            g.drawString(this.font, label, left + 4, 186 + i * 18, 0xFFFFFF)
        }
        for (d in dropdowns) d.renderExpanded(g, mx, my)
    }

    override fun isPauseScreen(): Boolean = false
}
