package com.xiaoyu.cobblemoneditor.data

import java.util.Base64

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
    val desc: String = "",
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
        val desc = m["desc"] ?: ""
        val type = (m["type"] ?: "normal").lowercase()
        val category = (m["category"] ?: "physical").lowercase()
        if (category !in CATEGORIES) return null
        val basePower = if (category == "status") null else (m["power"]?.toIntOrNull() ?: return null)
        val accuracy = m["accuracy"]?.let { it.toIntOrNull() ?: return null } ?: 100
        val pp = m["pp"]?.let { it.toIntOrNull() ?: return null } ?: 10
        val priority = m["priority"]?.let { it.toIntOrNull() ?: return null } ?: 0
        val target = m["target"] ?: "normal"
        val flags = m["flags"]?.split(',')?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() && it != "none" && it != "noncontact" } ?: emptyList()

        val bricks = ArrayList<ParsedBrick>()
        m["bricks"]?.split('|')?.forEach { b ->
            val t2 = b.trim()
            if (t2.isEmpty()) return@forEach
            val parts = t2.split(':')
            val brick = EffectBricks.byId(parts[0].trim()) ?: return null
            val rawParams = parts.drop(1).map { it.trim() }
            // 自定义 JS 参数走 base64（URL 安全、无填充），避免 JS 里的冒号/竖线破坏 spec 语法
            val params = if (brick.kind == MoveBrickKind.RAW_HANDLER) rawParams.map { decodeRaw(it) } else rawParams
            if (!validateBrickParams(brick.paramHints, params)) return null
            // 自定义 JS 必须通过完整性检查，防止把注册表 eval 炸掉（世界无法进入）
            if (brick.kind == MoveBrickKind.RAW_HANDLER && !isSaneRawJs(params.getOrNull(0) ?: "")) return null
            bricks.add(ParsedBrick(brick, params))
        }
        return MoveSpec(id, name, type, category, basePower, accuracy, pp, priority, target, flags, bricks, desc)
    }

    /** 裸对象字面量。name 必须 ASCII；中文放 shortDesc；多条 secondary 合并成 secondaries。 */
    fun renderJs(m: MoveSpec): String {
        val fields = ArrayList<String>()
        fields.add("name: " + '"' + m.id + '"')
        fields.add("type: " + '"' + cap(m.type) + '"')
        fields.add("category: " + '"' + cap(m.category) + '"')
        // Moves.reload 读回时 jsMove.get("basePower").asDouble 无条件取值：
        // 状态招式若不输出该键会被跳过（注册表静默丢弃）。统一输出，状态招式补 0。
        fields.add("basePower: " + (m.basePower ?: 0))
        fields.add("accuracy: " + m.accuracy)
        fields.add("pp: " + m.pp)
        fields.add("priority: " + m.priority)
        fields.add("target: " + '"' + m.target + '"')
        fields.add("flags: " + renderFlags(m.flags))
        val zh = esc(if (m.displayName.isNotEmpty() && m.displayName != m.id) m.displayName else "")
        if (zh.isNotEmpty()) fields.add("shortDesc: " + '"' + zh + '"')
        // desc：招式描述（Showdown 数据里招式都有 desc 字段）
        val descVal = if (m.desc.isNotBlank()) m.desc else if (zh.isNotEmpty()) zh else m.id
        fields.add("desc: " + '"' + esc(descVal) + '"')

        val secondaries = ArrayList<String>()
        for (b in m.bricks) {
            // {MOVEID} 占位符：允许积木引用招式自身 id（如自定义陷阱 sideCondition 必须指向已注册招式 id）
            val rendered = b.brick.render(b.params).replace("{MOVEID}", m.id)
            when (b.brick.kind) {
                MoveBrickKind.FIELD -> fields.add(rendered)
                MoveBrickKind.HANDLER -> fields.add(rendered)
                MoveBrickKind.RAW_HANDLER -> fields.add(rendered)
                MoveBrickKind.SECONDARY -> secondaries.add(rendered)
            }
        }
        if (secondaries.size == 1) fields.add("secondary: " + secondaries[0])
        else if (secondaries.size > 1) fields.add("secondaries: [" + secondaries.joinToString(", ") + "]")
        return "{ " + fields.joinToString(", ") + " }"
    }

    private fun renderFlags(flags: List<String>): String =
        if (flags.isEmpty()) "{}" else "{ " + flags.joinToString(", ") { it + ": 1" } + " }"

    private fun cap(s: String) = s.replaceFirstChar { it.uppercaseChar() }

    private fun esc(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ")

    private fun decodeRaw(s: String): String =
        try { String(Base64.getUrlDecoder().decode(s), Charsets.UTF_8) } catch (_: Exception) { s }
}
