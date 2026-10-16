package com.xiaoyu.cobblemoneditor.data

data class ParsedAbilityEffect(val brick: AbilityEffectBrick, val params: List<String>)

data class AbilitySpec(
    val id: String,
    val displayName: String,
    val description: String,
    val effects: List<ParsedAbilityEffect>,
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
        val effects = ArrayList<ParsedAbilityEffect>()
        m["effects"]?.split('|')?.forEach { b ->
            val t2 = b.trim()
            if (t2.isEmpty()) return@forEach
            val parts = t2.split(':')
            val brick = AbilityEffects.byId(parts[0].trim()) ?: return null
            effects.add(ParsedAbilityEffect(brick, parts.drop(1).map { it.trim() }))
        }
        return AbilitySpec(id, name, desc, effects)
    }

    // 裸对象字面量（同 MoveSpec）
    fun renderJs(a: AbilitySpec): String {
        val members = ArrayList<String>()
        members.add("name: " + '"' + a.displayName + '"')
        members.add("desc: " + '"' + a.description + '"')
        for (e in a.effects) members.add(e.brick.render(e.params))
        return "{ " + members.joinToString(", ") + " }"
    }
}
