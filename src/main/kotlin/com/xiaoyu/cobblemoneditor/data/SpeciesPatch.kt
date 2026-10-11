package com.xiaoyu.cobblemoneditor.data

/**
 * 编辑规格解析 + species_additions JSON 生成。
 *
 * 规格串：逗号分隔的 key=value 列表，例如：
 *   speed=120,hp=40,primaryType=electric,secondaryType=flying,abilities=static|h:lightningrod
 * - 种族值键：hp/attack/defence/special_attack/special_defence/speed（0..255）
 * - 属性键：primaryType（别名 type1）、secondaryType（别名 type2）；值用类型 id（小写），
 *           "none"/"null"/"" 表示该属性为空（无第二属性）
 * - 特性键：abilities；值用 | 分隔的列表，例如 static|h:lightningrod（h: 前缀=隐藏特性）
 */
object SpeciesPatch {
    const val MIN = 0
    const val MAX = 255

    private val STAT_ALIASES = mapOf(
        "hp" to "hp",
        "attack" to "attack", "atk" to "attack",
        "defence" to "defence", "defense" to "defence", "def" to "defence",
        "special_attack" to "special_attack", "spa" to "special_attack", "spatk" to "special_attack",
        "special_defence" to "special_defence", "special_defense" to "special_defence", "spd" to "special_defence", "spdef" to "special_defence",
        "speed" to "speed", "spe" to "speed",
    )

    data class Parsed(
        val stats: LinkedHashMap<String, Int>,
        val primaryTypeSpecified: Boolean,
        val primaryType: String?,
        val secondaryTypeSpecified: Boolean,
        val secondaryType: String?,
        val abilities: List<String>?,
    )

    private fun normType(v: String): String? {
        val t = v.trim().lowercase()
        if (t.isEmpty() || t == "none" || t == "null" || t == "无" || t == "-") return null
        return if (t.matches(Regex("[a-z0-9_]+"))) t else null
    }

    fun parseSpec(spec: String): Parsed? {
        val stats = LinkedHashMap<String, Int>()
        var primarySpecified = false; var primary: String? = null
        var secondarySpecified = false; var secondary: String? = null
        var abilities: List<String>? = null

        for (part in spec.split(',')) {
            val t = part.trim()
            if (t.isEmpty()) continue
            val eq = t.indexOf('=')
            if (eq <= 0) return null
            val rawKey = t.substring(0, eq).trim().lowercase()
            val rawVal = t.substring(eq + 1).trim()

            when {
                STAT_ALIASES.containsKey(rawKey) -> {
                    val value = rawVal.toIntOrNull() ?: return null
                    if (value < MIN || value > MAX) return null
                    stats[STAT_ALIASES.getValue(rawKey)] = value
                }
                rawKey == "primarytype" || rawKey == "type1" -> {
                    primarySpecified = true
                    primary = normType(rawVal) ?: if (normType(rawVal) == null && rawVal.trim().isNotEmpty()) return null else null
                }
                rawKey == "secondarytype" || rawKey == "type2" -> {
                    secondarySpecified = true
                    val n = normType(rawVal)
                    secondary = n
                    if (n == null && rawVal.trim().isNotEmpty() &&
                        !listOf("none", "null", "无", "-").contains(rawVal.trim().lowercase())) return null
                }
                rawKey == "abilities" || rawKey == "ability" -> {
                    val list = rawVal.split('|').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                    if (list.isEmpty()) return null
                    if (list.any { !it.matches(Regex("h?:?[a-z0-9_]+")) }) return null
                    abilities = list
                }
                else -> return null
            }
        }
        if (stats.isEmpty() && !primarySpecified && !secondarySpecified && abilities == null) return null
        return Parsed(stats, primarySpecified, primary, secondarySpecified, secondary, abilities)
    }

    fun build(speciesId: String, spec: String): String? {
        if (!speciesId.matches(Regex("[a-z0-9_]+"))) return null
        val parsed = parseSpec(spec) ?: return null
        return buildJson(speciesId, parsed)
    }

    fun buildJson(speciesId: String, p: Parsed): String {
        val sections = ArrayList<String>()
        if (p.stats.isNotEmpty()) {
            val entries = p.stats.entries.toList()
            val sb = StringBuilder()
            sb.append("  \"baseStats\": {\n")
            entries.forEachIndexed { i, e ->
                sb.append("    \"").append(e.key).append("\": ").append(e.value)
                if (i < entries.size - 1) sb.append(",")
                sb.append("\n")
            }
            sb.append("  }")
            sections.add(sb.toString())
        }
        if (p.primaryTypeSpecified) {
            sections.add("  \"primaryType\": " + jsonNullableString(p.primaryType))
        }
        if (p.secondaryTypeSpecified) {
            sections.add("  \"secondaryType\": " + jsonNullableString(p.secondaryType))
        }
        if (p.abilities != null) {
            val arr = p.abilities.joinToString(", ") { "\"" + it + "\"" }
            sections.add("  \"abilities\": [$arr]")
        }
        return "{\n  \"target\": \"cobblemon:$speciesId\",\n" + sections.joinToString(",\n") + "\n}\n"
    }

    private fun jsonNullableString(v: String?): String = if (v == null) "null" else "\"$v\""
}
