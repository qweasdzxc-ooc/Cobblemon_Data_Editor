package com.xiaoyu.cobblemoneditor.client.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component

/**
 * 下拉选择框（支持滚动）。onChange 必须是最后一个参数，方便用尾随 lambda。
 */
class Dropdown(
    x: Int, y: Int, w: Int, h: Int,
    private val labels: List<String>,
    private val values: List<String>,
    private val maxVisible: Int = 20,
    private val onChange: (String) -> Unit,
) : AbstractWidget(x, y, w, h, Component.literal("")) {

    private var selectedIndex = 0
    private var scroll = 0
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
        if (open) {
            val n = labels.size
            scroll = (selectedIndex - maxVisible + 1).coerceIn(0, (n - maxVisible).coerceAtLeast(0))
        }
    }

    fun handleScroll(verticalAmount: Double) {
        if (!open) return
        val n = labels.size
        if (n <= maxVisible) return
        scroll = (scroll - verticalAmount.toInt()).coerceIn(0, n - maxVisible)
    }

    fun handleExpandedClick(mx: Double, my: Double): Boolean {
        if (!open) return false
        val visible = minOf(maxVisible, labels.size)
        val idx = scroll + ((my - (y + height)) / height).toInt()
        if (mx >= x && mx <= x + width && my >= y + height && my < y + height + visible * height && idx in labels.indices) {
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
    }

    fun renderExpanded(g: GuiGraphics, mx: Int, my: Int) {
        if (!open) return
        val font = Minecraft.getInstance().font
        val visible = minOf(maxVisible, labels.size)
        val end = minOf(scroll + visible, labels.size)
        for (k in scroll until end) {
            val oy = y + height + (k - scroll) * height
            val oh = mx >= x && mx <= x + width && my >= oy && my < oy + height
            g.fill(x, oy, x + width, oy + height, if (oh) 0xFF4A4A4A.toInt() else 0xFF2E2E2E.toInt())
            g.renderOutline(x, oy, width, height, 0xFF777777.toInt())
            g.drawCenteredString(font, Component.literal(labels[k]), x + width / 2, oy + (height - 8) / 2, 0xFFFFFFFF.toInt())
        }
    }
}
