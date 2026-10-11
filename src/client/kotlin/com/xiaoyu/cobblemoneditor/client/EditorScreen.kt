package com.xiaoyu.cobblemoneditor.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Cobblemon 数据编辑界面（阶段 1+3-lite）：
 * 种族值 / 属性 / 特性列表。填写后点「保存」→ 发送 /cobbleeditor set 指令到服务端。
 */
class EditorScreen : Screen(Component.literal("Cobblemon Data Editor")) {

    private lateinit var speciesBox: EditBox
    private val fields = LinkedHashMap<String, EditBox>()
    private lateinit var primaryTypeBox: EditBox
    private lateinit var secondaryTypeBox: EditBox
    private lateinit var abilitiesBox: EditBox

    private val statKeys = linkedMapOf(
        "hp" to "HP",
        "attack" to "攻击",
        "defence" to "防御",
        "special_attack" to "特攻",
        "special_defence" to "特防",
        "speed" to "速度",
    )

    override fun init() {
        val left = width / 2 - 150
        speciesBox = EditBox(this.font, left, 26, 120, 16, Component.literal("species"))
        speciesBox.setMaxLength(64)
        addRenderableWidget(speciesBox)

        // 六项种族值：两列
        var i = 0
        for ((key, _) in statKeys) {
            val col = i % 2
            val row = i / 2
            val x = left + col * 150 + 60
            val y = 54 + row * 20
            val box = EditBox(this.font, x, y, 50, 16, Component.literal(key))
            box.setMaxLength(3)
            fields[key] = box
            addRenderableWidget(box)
            i++
        }

        val y1 = 54 + 3 * 20 + 6
        primaryTypeBox = EditBox(this.font, left + 60, y1, 90, 16, Component.literal("type1"))
        primaryTypeBox.setMaxLength(24)
        addRenderableWidget(primaryTypeBox)

        secondaryTypeBox = EditBox(this.font, left + 60, y1 + 20, 90, 16, Component.literal("type2"))
        secondaryTypeBox.setMaxLength(24)
        addRenderableWidget(secondaryTypeBox)

        abilitiesBox = EditBox(this.font, left + 60, y1 + 40, 220, 16, Component.literal("abilities"))
        abilitiesBox.setMaxLength(200)
        addRenderableWidget(abilitiesBox)

        addRenderableWidget(
            Button.builder(Component.literal("保存")) { save() }
                .bounds(left, y1 + 66, 90, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("关闭")) { onClose() }
                .bounds(left + 100, y1 + 66, 90, 20).build()
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
        val t1 = primaryTypeBox.value.trim().lowercase()
        if (t1.isNotEmpty()) parts.add("primaryType=$t1")
        val t2 = secondaryTypeBox.value.trim().lowercase()
        if (t2.isNotEmpty()) parts.add("secondaryType=$t2")
        val ab = abilitiesBox.value.trim().lowercase()
        if (ab.isNotEmpty()) parts.add("abilities=$ab")

        if (parts.isEmpty()) {
            msg("至少填一项内容")
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
        val left = width / 2 - 150
        g.drawCenteredString(this.font, this.title, width / 2, 10, 0xFFFFFF)
        g.drawString(this.font, "精灵 id:", left - 55, 30, 0xAAAAAA)

        var i = 0
        for ((_, label) in statKeys) {
            val col = i % 2
            val row = i / 2
            g.drawString(this.font, "$label:", left + col * 150, 58 + row * 20, 0xAAAAAA)
            i++
        }
        val y1 = 54 + 3 * 20 + 6
        g.drawString(this.font, "属性1:", left, y1 + 4, 0xAAAAAA)
        g.drawString(this.font, "属性2:", left, y1 + 24, 0xAAAAAA)
        g.drawString(this.font, "特性(用|分隔, h:前缀=隐藏):", left - 10, y1 + 44, 0xAAAAAA)

        g.drawString(this.font, "留空=不改；保存后需重进世界生效", left, y1 + 92, 0x888888)
        g.drawString(this.font, "例: 速度120 / 属性2=flying / 特性=static|h:lightningrod", left, y1 + 104, 0x666666)
    }

    override fun isPauseScreen(): Boolean = false
}
