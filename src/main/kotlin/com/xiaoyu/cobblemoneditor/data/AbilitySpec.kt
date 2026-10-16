package com.xiaoyu.cobblemoneditor.data

/** 特性定义（框架初版：名称/描述；效果积木后续加）。 */
data class AbilitySpec(
    val id: String,
    val displayName: String,
    val description: String,
)

/**
 * 特性：解析 + 生成 Showdown JS。
 *   /cobbleeditor ability add <id> name=...&desc=...
 */
object AbilitySpecFactory {

    fun parse(spec: String): AbilitySpec? {
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
        return AbilitySpec(id, name, desc)
    }

    fun renderJs(a: AbilitySpec): String {
        val sb = StringBuilder()
        sb.append("// 由 Cobblemon Data Editor 生成\n")
        sb.append("// 注意：数据格式为框架初版，需在游戏内校准\n")
        sb.append("exports.BattleAbilities = exports.BattleAbilities || {};\n")
        sb.append("exports.BattleAbilities['").append(a.id).append("'] = {\n")
        sb.append("  name: \"").append(esc(a.displayName)).append("\",\n")
        sb.append("  desc: \"").append(esc(a.description)).append("\"\n")
        sb.append("};\n")
        return sb.toString()
    }

    private fun esc(s: String) = s.replace("\"", "'")
}
