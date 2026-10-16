package com.xiaoyu.cobblemoneditor.data

data class ParsedBrick(val brick: EffectBrick, val params: List<String>)

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
    val flags: List<String>,
    val bricks: List<ParsedBrick>,
)

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
        val flags = m["flags"]?.split(',')?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() } ?: emptyList()

        val bricks = ArrayList<ParsedBrick>()
        m["bricks"]?.split('|')?.forEach { b ->
            val t2 = b.trim()
            if (t2.isEmpty()) return@forEach
            val parts = t2.split(':')
            val brick = EffectBricks.byId(parts[0].trim()) ?: return null
            bricks.add(ParsedBrick(brick, parts.drop(1).map { it.trim() }))
        }
        return MoveSpec(id, name, type, category, basePower, accuracy, pp, priority, target, flags, bricks)
    }

    // 生成裸对象字面量（Cobblemon 会把它嵌进 {"id": <这里>} 求值），不能有 exports/注释
    fun renderJs(m: MoveSpec): String {
        val fields = ArrayList<String>()
        fields.add("num: -1")
        fields.add("name: " + '"' + m.displayName + '"')
        fields.add("type: " + '"' + cap(m.type) + '"')
        fields.add("category: " + '"' + cap(m.category) + '"')
        m.basePower?.let { fields.add("basePower: " + it) }
        fields.add("accuracy: " + m.accuracy)
        fields.add("pp: " + m.pp)
        fields.add("priority: " + m.priority)
        fields.add("target: " + '"' + m.target + '"')
        fields.add("flags: " + renderFlags(m.flags))
        m.bricks.forEach { b -> fields.add(b.brick.render(b.params)) }
        return "{ " + fields.joinToString(", ") + " }"
    }

    private fun renderFlags(flags: List<String>): String =
        if (flags.isEmpty()) "{}" else "{ " + flags.joinToString(", ") { it + ": 1" } + " }"

    private fun cap(s: String) = s.replaceFirstChar { it.uppercaseChar() }
}
