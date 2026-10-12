package com.xiaoyu.cobblemoneditor.data

/** 一个已解析的效果积木（含参数）。 */
data class ParsedBrick(val brick: EffectBrick, val params: List<String>)

/** 招式定义。 */
data class MoveSpec(
    val id: String,
    val displayName: String,
    val type: String,
    val category: String,
    val basePower: Int?,
    val accuracy: Int,
    val pp: Int,
    val priority: Int,
    val target: String,
    val bricks: List<ParsedBrick>,
)

/**
 * 招式：解析 + 生成 Showdown JS。
 * 命令规格（框架初版）：
 *   /cobbleeditor move add <id> name=...&type=electric&category=special&power=80&accuracy=100&pp=10&priority=0&target=normal&bricks=secondary_status:10:par|drain:2
 */
object MoveSpecFactory {

    private val CATEGORIES = setOf("physical", "special", "status")

    fun parse(spec: String): MoveSpec? {
        val m = LinkedHashMap<String, String>()
        for (part in spec.split('&')) {
            val t = part.trim()
            if (t.isEmpty()) continue
            val eq = t.indexOf('=')
            if (eq <= 0) return null
            m[t.substring(0, eq).trim().lowercase()] = t.substring(eq + 1).trim()
        }
        val id = m["id"] ?: return null
        if (!id.matches(Regex("[a-z0-9_]+"))) return null
        val name = m["name"] ?: id
        val type = (m["type"] ?: "normal").lowercase()
        val category = (m["category"] ?: "physical").lowercase()
        if (category !in CATEGORIES) return null
        val basePower = if (category == "status") null else (m["power"]?.toIntOrNull() ?: return null)
        val accuracy = m["accuracy"]?.toIntOrNull() ?: 100
        val pp = m["pp"]?.toIntOrNull() ?: 10
        val priority = m["priority"]?.toIntOrNull() ?: 0
        val target = m["target"] ?: "normal"

        val bricks = ArrayList<ParsedBrick>()
        m["bricks"]?.split('|')?.forEach { b ->
            val t2 = b.trim()
            if (t2.isEmpty()) return@forEach
            val parts = t2.split(':')
            val brick = EffectBricks.byId(parts[0].trim()) ?: return null
            bricks.add(ParsedBrick(brick, parts.drop(1).map { it.trim() }))
        }
        return MoveSpec(id, name, type, category, basePower, accuracy, pp, priority, target, bricks)
    }

    fun renderJs(m: MoveSpec): String {
        val fields = ArrayList<String>()
        fields.add("num: -1")
        fields.add("name: \"${escape(m.displayName)}\"")
        fields.add("type: \"${cap(m.type)}\"")
        fields.add("category: \"${cap(m.category)}\"")
        m.basePower?.let { fields.add("basePower: $it") }
        fields.add("accuracy: ${m.accuracy}")
        fields.add("pp: ${m.pp}")
        fields.add("priority: ${m.priority}")
        fields.add("target: \"${m.target}\"")
        fields.add("flags: {}")
        m.bricks.forEach { b -> fields.add(b.brick.render(b.params)) }

        val sb = StringBuilder()
        sb.append("// 由 Cobblemon Data Editor 生成\n")
        sb.append("// 注意：数据格式为框架初版，需在游戏内校准\n")
        sb.append("exports.BattleMovedex = exports.BattleMovedex || {};\n")
        sb.append("exports.BattleMovedex['").append(m.id).append("'] = {\n")
        fields.forEachIndexed { i, f ->
            sb.append("  ").append(f)
            if (i < fields.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("};\n")
        return sb.toString()
    }

    private fun cap(s: String) = s.replaceFirstChar { it.uppercaseChar() }

    private fun escape(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
}
