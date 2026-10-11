package com.xiaoyu.cobblemoneditor.data

/**
 * 种族值补丁。null = 不改该项。
 * 输出为 Cobblemon 的 species_additions JSON 补丁（不覆写整个 species 文件）。
 */
data class StatPatch(
    val hp: Int? = null,
    val attack: Int? = null,
    val defence: Int? = null,
    val specialAttack: Int? = null,
    val specialDefence: Int? = null,
    val speed: Int? = null,
) {
    val isEmpty: Boolean
        get() = listOfNotNull(hp, attack, defence, specialAttack, specialDefence, speed).isEmpty()
}

object SpeciesPatch {
    const val MIN = 0
    const val MAX = 255

    /** 输入别名 -> JSON key（与 Cobblemon species JSON 的 baseStats 键一致） */
    private val ALIASES = mapOf(
        "hp" to "hp",
        "attack" to "attack", "atk" to "attack",
        "defence" to "defence", "defense" to "defence", "def" to "defence",
        "special_attack" to "special_attack", "spa" to "special_attack", "spatk" to "special_attack",
        "special_defence" to "special_defence", "special_defense" to "special_defence", "spd" to "special_defence", "spdef" to "special_defence",
        "speed" to "speed", "spe" to "speed",
    )

    /** 解析 "speed=120,hp=40" 形式的规格串；返回 规范key->值 或 null（格式错误）。 */
    fun parseSpec(spec: String): LinkedHashMap<String, Int>? {
        val out = LinkedHashMap<String, Int>()
        for (part in spec.split(',')) {
            val t = part.trim()
            if (t.isEmpty()) continue
            val eq = t.indexOf('=')
            if (eq <= 0) return null
            val rawKey = t.substring(0, eq).trim().lowercase()
            val rawVal = t.substring(eq + 1).trim()
            val key = ALIASES[rawKey] ?: return null
            val value = rawVal.toIntOrNull() ?: return null
            if (value < MIN || value > MAX) return null
            out[key] = value
        }
        return if (out.isEmpty()) null else out
    }

    /** 校验并生成 species_additions JSON。返回 null 表示校验失败。 */
    fun build(speciesId: String, spec: String): String? {
        if (!speciesId.matches(Regex("[a-z0-9_]+"))) return null
        val stats = parseSpec(spec) ?: return null
        return buildJson(speciesId, stats)
    }

    fun buildJson(speciesId: String, stats: Map<String, Int>): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"target\": \"cobblemon:").append(speciesId).append("\",\n")
        sb.append("  \"baseStats\": {\n")
        val entries = stats.entries.toList()
        entries.forEachIndexed { i, e ->
            sb.append("    \"").append(e.key).append("\": ").append(e.value)
            if (i < entries.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  }\n")
        sb.append("}\n")
        return sb.toString()
    }
}
