package com.xiaoyu.cobblemoneditor.data

/**
 * 新建精灵：spec 解析 + 完整物种 JSON 生成（data/<ns>/species/<id>.json）。
 *
 * 规格串：逗号分隔 key=value，列表类值用 | 分隔，例如：
 *   dex=9001,type1=electric,type2=flying,abilities=static|h:voltabsorb,moves=1:tackle|5:growl|egg:xxx|tm:yyy
 *
 * 必填：dex、type1/primaryType、abilities（≥1 个）。
 * 可选（带默认值）：name（缺省=id，ASCII）、nameZh（中文显示名，写入世界资源包 lang）、
 * type2/secondaryType、种族值 hp/atk/def/spa/spd/spe（缺省 50）、
 * moves（缺省 1:tackle）、catchRate(45)、maleRatio(0.5，-1=无性别)、height 米(1.0)、weight 千克(10.0)、
 * eggCycles(20)、eggGroups(monster)、experienceGroup(medium_fast)、baseExperienceYield(64)、
 * baseFriendship(50)、baseScale(1.0)。
 *
 * 生成时固定 implemented=true（Species 默认 false，不设进不了图鉴/不可用）。
 * 注意：显示名翻译键 "<ns>.species.<id>.name" 需要资源包 lang 才能本地化，
 * 无 lang 时游戏内显示兜底为翻译键文本；name 字段本身走 Showdown 桥接。
 */
object SpeciesFactory {
    private val EXPERIENCE_GROUPS = setOf("erratic", "fast", "medium_fast", "medium_slow", "slow", "fluctuating")
    private val LEARNSET_METHODS = setOf("egg", "tm", "tutor", "legacy", "special")
    private val STAT_ORDER = listOf("hp", "attack", "defence", "special_attack", "special_defence", "speed")

    data class NewSpecies(
        val id: String,
        val name: String,
        val nameZh: String?,
        val dex: Int,
        val primaryType: String,
        val secondaryType: String?,
        val baseStats: LinkedHashMap<String, Int>,
        val abilities: List<String>,
        val moves: List<String>,
        val catchRate: Int,
        val maleRatio: Double,
        val heightDm: Int,
        val weightHg: Int,
        val eggCycles: Int,
        val eggGroups: List<String>,
        val experienceGroup: String,
        val baseExperienceYield: Int,
        val baseFriendship: Int,
        val baseScale: Double,
    )

    fun parse(id: String, spec: String): NewSpecies? {
        if (!id.matches(Regex("[a-z0-9_]+"))) return null
        var name: String? = null
        var nameZh: String? = null
        var dex: Int? = null
        var primary: String? = null
        var secondary: String? = null
        val stats = LinkedHashMap<String, Int>()
        var abilities: List<String>? = null
        var moves: List<String>? = null
        var catchRate = 45
        var maleRatio = 0.5
        var heightDm = 10
        var weightHg = 100
        var eggCycles = 20
        var eggGroups = listOf("monster")
        var experienceGroup = "medium_fast"
        var baseExperienceYield = 64
        var baseFriendship = 50
        var baseScale = 1.0

        for (part in spec.split(',')) {
            val t = part.trim()
            if (t.isEmpty()) continue
            val eq = t.indexOf('=')
            if (eq <= 0) return null
            val key = t.substring(0, eq).trim().lowercase()
            val value = t.substring(eq + 1).trim()

            when {
                SpeciesPatch.STAT_ALIASES.containsKey(key) -> {
                    val v = value.toIntOrNull() ?: return null
                    if (v !in 0..255) return null
                    stats[SpeciesPatch.STAT_ALIASES.getValue(key)] = v
                }
                key == "dex" || key == "dexno" -> {
                    val v = value.toIntOrNull() ?: return null
                    if (v !in 1..99999) return null
                    dex = v
                }
                key == "name" -> {
                    if (!value.matches(Regex("[A-Za-z][A-Za-z0-9]*"))) return null
                    name = value
                }
                key == "namezh" -> {
                    if (value.isEmpty() || value.length > 32) return null
                    // 逗号/引号/反斜杠/竖线会破坏命令与 JSON，控制字符不允许
                    if (value.any { it == ',' || it == '"' || it == '\\' || it == '|' || it.isISOControl() }) return null
                    nameZh = value
                }
                key == "type1" || key == "primarytype" -> {
                    val v = value.lowercase()
                    if (TypeCatalog.TYPES.none { it.first == v }) return null
                    primary = v
                }
                key == "type2" || key == "secondarytype" -> {
                    val v = value.lowercase()
                    if (v.isEmpty() || v == "none" || v == "null" || v == "无" || v == "-") {
                        secondary = null
                    } else {
                        if (TypeCatalog.TYPES.none { it.first == v }) return null
                        secondary = v
                    }
                }
                key == "abilities" || key == "ability" -> {
                    val list = value.split('|').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                    if (list.isEmpty() || list.any { !it.matches(Regex("h?:?[a-z0-9_]+")) }) return null
                    abilities = list.distinct()
                }
                key == "moves" || key == "learnset" -> {
                    val list = value.split('|').map { it.trim() }.filter { it.isNotEmpty() }
                    if (list.isEmpty()) return null
                    val out = LinkedHashSet<String>()
                    for (entry in list) {
                        val colon = entry.indexOf(':')
                        if (colon <= 0) return null
                        val method = entry.substring(0, colon)
                        val moveId = entry.substring(colon + 1)
                        val ok = when {
                            method.all { c -> c.isDigit() } ->
                                (method.toIntOrNull() ?: return null) in 1..100 && moveId.matches(Regex("[a-z0-9]+"))
                            LEARNSET_METHODS.contains(method) -> moveId.matches(Regex("[a-z0-9]+"))
                            else -> false
                        }
                        if (!ok) return null
                        out.add(entry)
                    }
                    moves = out.toList()
                }
                key == "catchrate" -> {
                    val v = value.toIntOrNull() ?: return null
                    if (v !in 1..255) return null
                    catchRate = v
                }
                key == "maleratio" -> {
                    val v = value.toDoubleOrNull() ?: return null
                    if (v != -1.0 && (v < 0.0 || v > 1.0)) return null
                    maleRatio = v
                }
                key == "height" -> {
                    val v = value.toDoubleOrNull() ?: return null
                    if (v <= 0.0 || v > 100.0) return null
                    heightDm = Math.round(v * 10).toInt()
                }
                key == "weight" -> {
                    val v = value.toDoubleOrNull() ?: return null
                    if (v <= 0.0 || v > 10000.0) return null
                    weightHg = Math.round(v * 10).toInt()
                }
                key == "eggcycles" -> {
                    val v = value.toIntOrNull() ?: return null
                    if (v !in 1..120) return null
                    eggCycles = v
                }
                key == "egggroups" -> {
                    val list = value.split('|').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                    if (list.isEmpty() || list.any { !it.matches(Regex("[a-z0-9_]+")) }) return null
                    eggGroups = list.distinct()
                }
                key == "experiencegroup" -> {
                    val v = value.lowercase()
                    if (!EXPERIENCE_GROUPS.contains(v)) return null
                    experienceGroup = v
                }
                key == "baseexperienceyield" -> {
                    val v = value.toIntOrNull() ?: return null
                    if (v !in 0..100000) return null
                    baseExperienceYield = v
                }
                key == "basefriendship" -> {
                    val v = value.toIntOrNull() ?: return null
                    if (v !in 0..255) return null
                    baseFriendship = v
                }
                key == "basescale" -> {
                    val v = value.toDoubleOrNull() ?: return null
                    if (v <= 0.0 || v > 10.0) return null
                    baseScale = v
                }
                else -> return null
            }
        }

        val d = dex ?: return null
        val p = primary ?: return null
        val a = abilities ?: return null
        if (p == secondary) return null
        return NewSpecies(
            id = id,
            name = name ?: id,
            nameZh = nameZh,
            dex = d,
            primaryType = p,
            secondaryType = secondary,
            baseStats = stats,
            abilities = a,
            moves = moves ?: listOf("1:tackle"),
            catchRate = catchRate,
            maleRatio = maleRatio,
            heightDm = heightDm,
            weightHg = weightHg,
            eggCycles = eggCycles,
            eggGroups = eggGroups,
            experienceGroup = experienceGroup,
            baseExperienceYield = baseExperienceYield,
            baseFriendship = baseFriendship,
            baseScale = baseScale,
        )
    }

    /**
     * 学习表排序键：等级招式组=0（组内按等级升序），
     * egg=1/tm=2/tutor=3/legacy=4/special=5（组内按招式 id 字典序），与官方数据文件布局一致。
     */
    private fun moveGroup(method: String): Int = when (method) {
        "egg" -> 1
        "tm" -> 2
        "tutor" -> 3
        "legacy" -> 4
        "special" -> 5
        else -> 0
    }

    fun buildJson(s: NewSpecies): String {
        val statLines = STAT_ORDER.mapIndexed { i, key ->
            "    \"$key\": ${s.baseStats[key] ?: 50}" + if (i < STAT_ORDER.size - 1) "," else ""
        }
        val sortedMoves = s.moves.sortedWith(
            compareBy(
                { e -> moveGroup(e.substringBefore(':')) },
                { e -> if (e.substringBefore(':').all { c -> c.isDigit() }) e.substringBefore(':').toInt() else 0 },
                { e -> e },
            )
        )
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"name\": \"").append(s.name).append("\",\n")
        sb.append("  \"nationalPokedexNumber\": ").append(s.dex).append(",\n")
        sb.append("  \"primaryType\": \"").append(s.primaryType).append("\",\n")
        if (s.secondaryType != null) {
            sb.append("  \"secondaryType\": \"").append(s.secondaryType).append("\",\n")
        }
        sb.append("  \"baseStats\": {\n").append(statLines.joinToString("\n")).append("\n  },\n")
        sb.append("  \"abilities\": [").append(s.abilities.joinToString(", ") { "\"$it\"" }).append("],\n")
        sb.append("  \"moves\": [").append(sortedMoves.joinToString(", ") { "\"$it\"" }).append("],\n")
        sb.append("  \"implemented\": true,\n")
        sb.append("  \"maleRatio\": ").append(s.maleRatio).append(",\n")
        sb.append("  \"catchRate\": ").append(s.catchRate).append(",\n")
        sb.append("  \"height\": ").append(s.heightDm).append(",\n")
        sb.append("  \"weight\": ").append(s.weightHg).append(",\n")
        sb.append("  \"eggCycles\": ").append(s.eggCycles).append(",\n")
        sb.append("  \"eggGroups\": [").append(s.eggGroups.joinToString(", ") { "\"$it\"" }).append("],\n")
        sb.append("  \"experienceGroup\": \"").append(s.experienceGroup).append("\",\n")
        sb.append("  \"baseExperienceYield\": ").append(s.baseExperienceYield).append(",\n")
        sb.append("  \"baseFriendship\": ").append(s.baseFriendship).append(",\n")
        sb.append("  \"baseScale\": ").append(s.baseScale).append("\n")
        sb.append("}\n")
        return sb.toString()
    }
}
