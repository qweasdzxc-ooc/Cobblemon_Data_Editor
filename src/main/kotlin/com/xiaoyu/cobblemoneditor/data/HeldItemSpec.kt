package com.xiaoyu.cobblemoneditor.data

/**
 * 携带道具（held item）规格解析与 Showdown 渲染。
 *
 * 语法：id=<ascii>&name=<中文名>&desc=<描述>&bricks=<积木id>:<参数>:<参数>|...
 * 输出：data/<ns>/held_items/<id>.js —— 裸 Showdown 道具对象字面量。
 * 字段：name（必须 ASCII=id，中文名放 shortDesc）、shortDesc、desc、积木渲染结果。
 * 效果绑定：游戏内物品需带 cobblemon:held_item_effect 组件（showdownId=<id>）。
 */
data class HeldItemSpec(
    val id: String,
    val displayName: String,
    val description: String,
    val bricks: List<Pair<HeldItemBrick, List<String>>>,
)

object HeldItemSpecFactory {
    fun parse(id: String, spec: String): HeldItemSpec? {
        if (!id.matches(Regex("[a-z0-9]+"))) return null
        var name = ""
        var desc = ""
        val bricks = mutableListOf<Pair<HeldItemBrick, List<String>>>()
        for (part in spec.split("&")) {
            if (part.isEmpty()) continue
            val eq = part.indexOf('=')
            if (eq <= 0) return null
            val key = part.substring(0, eq)
            val value = part.substring(eq + 1)
            when (key) {
                "name" -> {
                    if (value.length > 32 || value.any { it.isISOControl() }) return null
                    name = value
                }
                "desc" -> {
                    if (value.length > 200 || value.any { it.isISOControl() }) return null
                    desc = value
                }
                "bricks" -> {
                    for (brickPart in value.split("|")) {
                        if (brickPart.isEmpty()) continue
                        val seg = brickPart.split(":")
                        val b = HeldItemBricks.byId(seg[0]) ?: return null
                        val params = seg.drop(1)
                        if (!HeldItemBricks.validParams(b, params)) return null
                        bricks.add(b to params)
                    }
                }
                else -> return null
            }
        }
        return HeldItemSpec(id, name.ifBlank { id }, desc, bricks)
    }

    fun renderJs(item: HeldItemSpec): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  name: \"${item.id}\",\n")
        sb.append("  shortDesc: \"${item.displayName}\",\n")
        sb.append("  desc: \"${item.description.ifBlank { item.displayName }}\",\n")
        var first = true
        for ((brick, params) in item.bricks) {
            val rendered = brick.render(params)
            val lines = rendered.split("\n")
            for (line in lines) {
                if (first) {
                    first = false
                } else {
                    sb.append(",\n")
                }
                sb.append("  ").append(line.trim())
            }
        }
        sb.append("\n}\n")
        return sb.toString()
    }
}
