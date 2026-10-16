package com.xiaoyu.cobblemoneditor.client

import com.xiaoyu.cobblemoneditor.data.CategoryCatalog
import com.xiaoyu.cobblemoneditor.data.EffectBricks
import com.xiaoyu.cobblemoneditor.data.MoveFlagCatalog
import com.xiaoyu.cobblemoneditor.data.PriorityCatalog
import com.xiaoyu.cobblemoneditor.data.TargetCatalog
import com.xiaoyu.cobblemoneditor.data.TypeCatalog
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class MoveEditorScreen : Screen(Component.literal("招式编辑器")) {

    private lateinit var idBox: EditBox
    private lateinit var nameBox: EditBox
    private lateinit var powerBox: EditBox
    private lateinit var accBox: EditBox
    private lateinit var ppBox: EditBox
    private lateinit var bricksParamsBox: EditBox

    private var type = "electric"
    private var category = "special"
    private var priority = 0
    private var target = "normal"
    private var brickId = "secondary_status"
    private val selectedFlags = mutableSetOf<String>()

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

        addRenderableWidget(CycleButton.builder { v: String -> Component.literal(TypeCatalog.zh(v)) }
            .withValues(TypeCatalog.TYPES.map { it.first })
            .withInitialValue(type)
            .create(left + 100, 124, 96, 16, Component.literal("属性"), { _, v -> type = v }))

        addRenderableWidget(CycleButton.builder { v: String -> Component.literal(CategoryCatalog.zh(v)) }
            .withValues(CategoryCatalog.CATEGORIES.map { it.first })
            .withInitialValue(category)
            .create(left + 100, 144, 96, 16, Component.literal("分类"), { _, v -> category = v }))

        addRenderableWidget(CycleButton.builder { v: Int -> Component.literal(PriorityCatalog.zh(v)) }
            .withValues(PriorityCatalog.OPTIONS.map { it.second })
            .withInitialValue(priority)
            .create(left + 100, 164, 96, 16, Component.literal("优先度"), { _, v -> priority = v }))

        addRenderableWidget(CycleButton.builder { v: String -> Component.literal(TargetCatalog.zh(v)) }
            .withValues(TargetCatalog.OPTIONS.map { it.second })
            .withInitialValue(target)
            .create(left + 100, 184, 96, 16, Component.literal("目标"), { _, v -> target = v }))

        var fx = left + 100; var fy = 204
        for (f in MoveFlagCatalog.FLAGS) {
            addRenderableWidget(CycleButton.builder<Boolean> { on -> Component.literal(f.first + (if (on) " ✓" else " ✗")) }
                .withValues(listOf(false, true))
                .withInitialValue(false)
                .create(fx, fy, 48, 16, Component.literal(f.first), { _, on -> if (on) selectedFlags.add(f.second) else selectedFlags.remove(f.second) }))
            fx += 52
            if (fx > left + 308) { fx = left + 100; fy += 18 }
        }

        addRenderableWidget(CycleButton.builder { v: String -> Component.literal(EffectBricks.byId(v)?.displayName ?: v) }
            .withValues(EffectBricks.ids())
            .withInitialValue(brickId)
            .create(left + 100, fy + 2, 150, 16, Component.literal("效果积木"), { _, v -> brickId = v }))

        bricksParamsBox = EditBox(this.font, left + 100, fy + 20, 150, 16, Component.empty())
        bricksParamsBox.setMaxLength(100); addRenderableWidget(bricksParamsBox)

        addRenderableWidget(Button.builder(Component.literal("保存")) { save() }.bounds(left, fy + 42, 90, 20).build())
        addRenderableWidget(Button.builder(Component.literal("关闭")) { onClose() }.bounds(left + 110, fy + 42, 90, 20).build())
    }

    private fun save() {
        val id = idBox.value.trim().lowercase()
        if (id.isEmpty()) { msg("请填招式 id"); return }
        val parts = ArrayList<String>()
        val name = nameBox.value.trim(); if (name.isNotEmpty()) parts.add("name=$name")
        val power = powerBox.value.trim(); if (power.isNotEmpty()) { if (power.toIntOrNull() == null) { msg("威力必须是数字"); return }; parts.add("power=$power") }
        val acc = accBox.value.trim(); if (acc.isNotEmpty()) parts.add("accuracy=$acc")
        val pp = ppBox.value.trim(); if (pp.isNotEmpty()) parts.add("pp=$pp")
        parts.add("type=$type")
        parts.add("category=$category")
        parts.add("priority=$priority")
        parts.add("target=$target")
        if (selectedFlags.isNotEmpty()) parts.add("flags=${selectedFlags.joinToString(",")}")
        val bp = bricksParamsBox.value.trim()
        parts.add(if (bp.isEmpty()) "bricks=$brickId" else "bricks=$brickId:$bp")
        val spec = parts.joinToString("&")
        val player = Minecraft.getInstance().player
        if (player == null) { msg("未连接服务器"); return }
        player.connection.sendCommand("cobbleeditor move add $id $spec")
        onClose()
    }

    private fun msg(s: String) { Minecraft.getInstance().player?.displayClientMessage(Component.literal(s), false) }

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
        g.drawString(this.font, "积木参数(如 10:par):", left, 226, 0xAAAAAA)
    }

    override fun isPauseScreen(): Boolean = false
}
