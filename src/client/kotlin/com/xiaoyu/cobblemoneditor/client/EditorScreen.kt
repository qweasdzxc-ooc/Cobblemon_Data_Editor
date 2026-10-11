package com.xiaoyu.cobblemoneditor.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * 种族值编辑界面。填写后点「保存」→ 发送 /cobbleeditor set 指令到服务端。
 */
class EditorScreen : Screen(Component.literal("Cobblemon Data Editor")) {

    private lateinit var speciesBox: EditBox
    private val fields = LinkedHashMap<String, EditBox>()

    private val statKeys = linkedMapOf(
        "hp" to "HP",
        "attack" to "攻击",
        "defence" to "防御",
        "special_attack" to "特攻",
        "special_defence" to "特防",
        "speed" to "速度",
    )

    override fun init() {
        speciesBox = EditBox(this.font, width / 2 - 100, 30, 200, 18, Component.literal("species"))
        speciesBox.setMaxLength(64)
        addRenderableWidget(speciesBox)

        var y = 56
        for ((key, label) in statKeys) {
            val box = EditBox(this.font, width / 2 - 40, y - 4, 70, 16, Component.literal(label))
            box.setMaxLength(3)
            fields[key] = box
            addRenderableWidget(box)
            y += 22
        }

        addRenderableWidget(
            Button.builder(Component.literal("保存")) { save() }
                .bounds(width / 2 - 100, y + 6, 90, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("关闭")) { onClose() }
                .bounds(width / 2 + 10, y + 6, 90, 20).build()
        )
    }

    private fun save() {
        val species = speciesBox.value.trim().lowercase()
        if (species.isEmpty()) {
            msg("请先填精灵 id，例如 pikachu")
            return
        }
        val parts = ArrayList<String>()
        for ((key, box) in fields) {
            val v = box.value.trim()
            if (v.isEmpty()) continue
            if (v.toIntOrNull() == null) {
                msg("$key 必须是数字")
                return
            }
            parts.add("$key=$v")
        }
        if (parts.isEmpty()) {
            msg("至少填一项种族值")
            return
        }
        val spec = parts.joinToString(",")
        val player = Minecraft.getInstance().player
        if (player == null) {
            msg("未连接服务器")
            return
        }
        player.connection.sendCommand("cobbleeditor set $species $spec")
        onClose()
    }

    private fun msg(s: String) {
        Minecraft.getInstance().player?.displayClientMessage(Component.literal(s), false)
    }

    override fun render(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(g, mouseX, mouseY, partialTick)
        g.drawCenteredString(this.font, this.title, width / 2, 14, 0xFFFFFF)
        g.drawString(this.font, "精灵 id:", width / 2 - 160, 35, 0xAAAAAA)
        var y = 58
        for ((_, label) in statKeys) {
            g.drawString(this.font, label + ":", width / 2 - 100, y, 0xAAAAAA)
            y += 22
        }
        g.drawString(this.font, "留空 = 不改该项；保存后重进世界生效", width / 2 - 100, height - 24, 0x888888)
    }

    override fun isPauseScreen(): Boolean = false
}
