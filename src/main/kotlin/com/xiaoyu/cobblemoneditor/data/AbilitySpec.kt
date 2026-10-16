package com.xiaoyu.cobblemoneditor.data

data class AbilitySpec(
    val id: String,
    val displayName: String,
    val description: String,
    val bricksNote: String?,
)

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
        return AbilitySpec(id, name, desc, m["bricks"])
    }

    fun renderJs(a: AbilitySpec): String {
        val sb = StringBuilder()
        sb.appendLine("// 由 Cobblemon Data Editor 生成")
        sb.appendLine("// 注意：数据格式为框架初版，需在游戏内校准")
        sb.appendLine("exports.BattleAbilities = exports.BattleAbilities || {};")
        sb.appendLine("exports.BattleAbilities['" + a.id + "'] = {")
        sb.appendLine("  name: " + '"' + a.displayName + '"' + ",")
        sb.appendLine("  desc: " + '"' + a.description + '"')
        if (a.bricksNote != null) {
            sb.appendLine("  // 效果积木(占位,待接入 Showdown 能力事件): " + a.bricksNote)
        }
        sb.appendLine("};")
        return sb.toString()
    }
}
