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
 * - 学习表键：moves（与现有学习表合并）、movesReplace（整体替换）；
 *   值用 | 分隔的 "<method>:<moveId>" 列表，例如 1:tackle|5:growl|egg:leechseed|tm:thunderbolt
 *   （method 可为等级 1..100、egg、tm、tutor、legacy、special）
 */
object SpeciesPatch {
    const val MIN = 0
    const val MAX = 255

    private val MOVE_METHODS = setOf("egg", "tm", "tutor", "legacy", "special")

    internal val STAT_ALIASES = mapOf(
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
        val mergeMoves: List<String>?,
        val replaceMoves: List<String>?,
    )

    private fun normType(v: String): String? {
        val t = v.trim().lowercase()
        if (t.isEmpty() || t == "none" || t == "null" || t == "无" || t == "-") return null
        return if (t.matches(Regex("[a-z0-9_]+"))) t else null
    }

    /** 解析 "<method>:<moveId>" 列表（| 分隔），非法返回 null；去重保序。 */
    private fun parseMoveEntries(raw: String): List<String>? {
        val entries = raw.split('|').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (entries.isEmpty()) return null
        val out = LinkedHashSet<String>()
        for (e in entries) {
            val idx = e.indexOf(':')
            if (idx <= 0) return null
            val method = e.substring(0, idx)
            val moveId = e.substring(idx + 1)
            if (!moveId.matches(Regex("[a-z0-9]+"))) return null
            if (method in MOVE_METHODS) {
                out.add(e)
            } else {
                val lvl = method.toIntOrNull() ?: return null
                if (lvl < 1 || lvl > 100) return null
                out.add(e)
            }
        }
        return out.toList()
    }

    fun parseSpec(spec: String): Parsed? {
        val stats = LinkedHashMap<String, Int>()
        var primarySpecified = false; var primary: String? = null
        var secondarySpecified = false; var secondary: String? = null
        var abilities: List<String>? = null
        var mergeMoves: List<String>? = null
        var replaceMoves: List<String>? = null

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
                rawKey == "moves" || rawKey == "learnset" -> {
                    mergeMoves = parseMoveEntries(rawVal) ?: return null
                }
                rawKey == "movesreplace" || rawKey == "learnsetreplace" -> {
                    replaceMoves = parseMoveEntries(rawVal) ?: return null
                }
                else -> return null
            }
        }
        if (stats.isEmpty() && !primarySpecified && !secondarySpecified &&
            abilities == null && mergeMoves == null && replaceMoves == null) return null
        return Parsed(stats, primarySpecified, primary, secondarySpecified, secondary, abilities, mergeMoves, replaceMoves)
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
        val moves = p.replaceMoves ?: p.mergeMoves
        if (moves != null) {
            val arr = moves.joinToString(", ") { "\"" + it + "\"" }
            sections.add("  \"moves\": [$arr]")
        }
        return "{\n  \"target\": \"cobblemon:$speciesId\",\n" + sections.joinToString(",\n") + "\n}\n"
    }

    private fun jsonNullableString(v: String?): String = if (v == null) "null" else "\"$v\""
}
