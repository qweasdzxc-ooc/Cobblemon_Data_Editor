package com.xiaoyu.cobblemoneditor.client.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component

/**
 * 真正的下拉选择框：点开显示选项列表，点选后收起。
 * 展开态的点击由所在 Screen 的 mouseClicked 转发到 handleExpandedClick。
 */
class Dropdown(
    x: Int, y: Int, w: Int, h: Int,
    private val labels: List<String>,
    private val values: List<String>,
    private val onChange: (String) -> Unit,
) : AbstractWidget(x, y, w, h, Component.literal("")) {

    private var selectedIndex = 0
    var open = false
        private set

    val selectedValue: String get() = values[selectedIndex]

    fun setSelectedByValue(value: String?) {
        val i = values.indexOf(value)
        if (i >= 0) selectedIndex = i
    }

    override fun getMessage(): Component = Component.literal(labels[selectedIndex])

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput)
    }

    override fun onClick(mx: Double, my: Double) {
        open = !open
    }

    /** 展开态点击：选中某项或收起。返回 true 表示已消费该点击。 */
    fun handleExpandedClick(mx: Double, my: Double): Boolean {
        if (!open) return false
        val idx = ((my - (y + height)) / height).toInt()
        if (mx >= x && mx <= x + width && idx in labels.indices && my >= y + height) {
            selectedIndex = idx
            open = false
            onChange(values[idx])
        } else {
            open = false
        }
        return true
    }

    override fun renderWidget(g: GuiGraphics, mx: Int, my: Int, partialTick: Float) {
        val font = Minecraft.getInstance().font
        val hover = mx >= x && mx < x + width && my >= y && my < y + height
        g.fill(x, y, x + width, y + height, if (hover || open) 0xFF3A3A3A.toInt() else 0xFF262626.toInt())
        g.renderOutline(x, y, width, height, 0xFFAAAAAA.toInt())
        g.drawCenteredString(font, Component.literal(labels[selectedIndex] + (if (open) " ▲" else " ▼")), x + width / 2, y + (height - 8) / 2, 0xFFFFFFFF.toInt())

        if (open) {
            for (i in labels.indices) {
                val oy = y + height + i * height
                val oh = mx >= x && mx <= x + width && my >= oy && my < oy + height
                g.fill(x, oy, x + width, oy + height, if (oh) 0xFF4A4A4A.toInt() else 0xFF2E2E2E.toInt())
                g.renderOutline(x, oy, width, height, 0xFF777777.toInt())
                g.drawCenteredString(font, Component.literal(labels[i]), x + width / 2, oy + (height - 8) / 2, 0xFFFFFFFF.toInt())
            }
        }
    }
}
