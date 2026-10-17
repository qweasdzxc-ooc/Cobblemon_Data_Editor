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
            val params = parts.drop(1).map { it.trim() }
            if (!validateBrickParams(brick.paramHints, params)) return null
            effects.add(ParsedAbilityEffect(brick, params))
        }
        return AbilitySpec(id, name, desc, effects)
    }

    /**
     * 生成裸对象字面量（Cobblemon 会把它嵌进对象求值）。
     * 关键：name 必须是 ASCII（Showdown 用 toID(name) 生成 id，中文会被过滤成空 → 找不到）。
     * 中文显示名放 shortDesc；多条同名事件处理器按触发时机合并成一个函数体。
     */
    fun renderJs(a: AbilitySpec): String {
        val byTrigger = LinkedHashMap<AbilityTrigger, MutableList<ParsedAbilityEffect>>()
        for (e in a.effects) byTrigger.getOrPut(e.brick.trigger) { mutableListOf() }.add(e)

        val members = ArrayList<String>()
        members.add("name: " + '"' + a.id + '"')
        members.add("flags: {}")
        members.add("rating: 3")
        val zh = esc(if (a.displayName.isNotEmpty() && a.displayName != a.id) a.displayName else "")
        val fullDesc = esc(if (a.description.isNotEmpty()) a.description else a.displayName)
        if (zh.isNotEmpty()) members.add("shortDesc: " + '"' + zh + '"')
        if (fullDesc.isNotEmpty()) members.add("desc: " + '"' + fullDesc + '"')
        for ((trigger, list) in byTrigger) {
            val body = list.joinToString(" ") { it.brick.action(it.params) }
            members.add(trigger.handler + "(" + trigger.params + ") { " + body + " }")
        }
        return "{ " + members.joinToString(", ") + " }"
    }

    private fun esc(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ")
}
