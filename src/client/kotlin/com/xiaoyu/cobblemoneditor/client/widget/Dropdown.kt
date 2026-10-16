package com.xiaoyu.cobblemoneditor.client.widget

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component

/**
 * 下拉选择框。展开列表由所在 Screen 在 render() 末尾调用 renderExpanded 绘制，
 * 保证盖在其它控件之上（不被遮挡）。
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
    }

    fun renderExpanded(g: GuiGraphics, mx: Int, my: Int) {
        if (!open) return
        val font = Minecraft.getInstance().font
        for (i in labels.indices) {
            val oy = y + height + i * height
            val oh = mx >= x && mx <= x + width && my >= oy && my < oy + height
            g.fill(x, oy, x + width, oy + height, if (oh) 0xFF4A4A4A.toInt() else 0xFF2E2E2E.toInt())
            g.renderOutline(x, oy, width, height, 0xFF777777.toInt())
            g.drawCenteredString(font, Component.literal(labels[i]), x + width / 2, oy + (height - 8) / 2, 0xFFFFFFFF.toInt())
        }
    }
}
