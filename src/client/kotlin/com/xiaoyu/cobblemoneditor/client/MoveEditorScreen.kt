package com.xiaoyu.cobblemoneditor.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class MoveEditorScreen : Screen(Component.literal("招式编辑器")) {

    private lateinit var idBox: EditBox
    private lateinit var nameBox: EditBox
    private lateinit var typeBox: EditBox
    private lateinit var categoryBox: EditBox
    private lateinit var powerBox: EditBox
    private lateinit var accBox: EditBox
    private lateinit var ppBox: EditBox
    private lateinit var prioBox: EditBox
    private lateinit var targetBox: EditBox
    private lateinit var bricksBox: EditBox

    override fun init() {
        val left = width / 2 - 120
        val bx = left + 70
        fun box(y: Int, w: Int = 90): EditBox {
            val b = EditBox(this.font, bx, y, w, 16, Component.empty())
            addRenderableWidget(b)
            return b
        }
        idBox = box(26, 140); idBox.setMaxLength(32)
        nameBox = box(46, 140); nameBox.setMaxLength(64)
        typeBox = box(66, 90); typeBox.setMaxLength(16)
        categoryBox = box(86, 90); categoryBox.setMaxLength(10)
        powerBox = box(106, 60); powerBox.setMaxLength(3)
        accBox = box(126, 60); accBox.setMaxLength(3)
        ppBox = box(146, 60); ppBox.setMaxLength(3)
        prioBox = box(166, 60); prioBox.setMaxLength(3)
        targetBox = box(186, 90); targetBox.setMaxLength(16)
        bricksBox = box(206, 220); bricksBox.setMaxLength(400)

        addRenderableWidget(Button.builder(Component.literal("保存")) { save() }.bounds(left, 236, 90, 20).build())
        addRenderableWidget(Button.builder(Component.literal("关闭")) { onClose() }.bounds(left + 110, 236, 90, 20).build())
    }

    private fun save() {
        val id = idBox.value.trim().lowercase()
        if (id.isEmpty()) { msg("请填招式 id"); return }
        val parts = ArrayList<String>()
        val name = nameBox.value.trim(); if (name.isNotEmpty()) parts.add("name=$name")
        val type = typeBox.value.trim().lowercase(); if (type.isNotEmpty()) parts.add("type=$type")
        val cat = categoryBox.value.trim().lowercase(); if (cat.isNotEmpty()) parts.add("category=$cat")
        val power = powerBox.value.trim(); if (power.isNotEmpty()) { if (power.toIntOrNull() == null) { msg("威力必须是数字"); return }; parts.add("power=$power") }
        val acc = accBox.value.trim(); if (acc.isNotEmpty()) parts.add("accuracy=$acc")
        val pp = ppBox.value.trim(); if (pp.isNotEmpty()) parts.add("pp=$pp")
        val prio = prioBox.value.trim(); if (prio.isNotEmpty()) parts.add("priority=$prio")
        val target = targetBox.value.trim().lowercase(); if (target.isNotEmpty()) parts.add("target=$target")
        val bricks = bricksBox.value.trim().lowercase(); if (bricks.isNotEmpty()) parts.add("bricks=$bricks")

        val spec = parts.joinToString("&")
        val player = Minecraft.getInstance().player
        if (player == null) { msg("未连接服务器"); return }
        player.connection.sendCommand("cobbleeditor move add $id $spec")
        onClose()
    }

    private fun msg(s: String) { Minecraft.getInstance().player?.displayClientMessage(Component.literal(s), false) }

    override fun render(g: GuiGraphics, mx: Int, my: Int, p: Float) {
        super.render(g, mx, my, p)
        val left = width / 2 - 120
        g.drawCenteredString(this.font, this.title, width / 2, 10, 0xFFFFFF)
        val labels = listOf("id", "名称", "属性", "分类", "威力", "命中", "pp", "优先度", "目标", "效果积木")
        labels.forEachIndexed { i, l -> g.drawString(this.font, "$l:", left, 30 + i * 20, 0xAAAAAA) }
        g.drawString(this.font, "分类: physical/special/status | 目标: normal/self/...", left, 232, 0x666666)
        g.drawString(this.font, "积木例: secondary_status:10:par|drain:2", left, 246, 0x666666)
    }

    override fun isPauseScreen(): Boolean = false
}
