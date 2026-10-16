package com.xiaoyu.cobblemoneditor.data

object TypeCatalog {
    val TYPES: List<Pair<String, String>> = listOf(
        "normal" to "一般", "fighting" to "格斗", "flying" to "飞行", "poison" to "毒",
        "ground" to "地面", "rock" to "岩石", "bug" to "虫", "ghost" to "幽灵", "steel" to "钢",
        "fire" to "火", "water" to "水", "grass" to "草", "electric" to "电", "psychic" to "超能力",
        "ice" to "冰", "dragon" to "龙", "dark" to "恶", "fairy" to "妖精",
    )
    fun zh(id: String): String = TYPES.firstOrNull { it.first == id }?.second ?: id
    fun byIndex(i: Int): Pair<String, String> { val n = TYPES.size; return TYPES[((i % n) + n) % n] }
}

object CategoryCatalog {
    val CATEGORIES: List<Pair<String, String>> = listOf("physical" to "物理", "special" to "特殊", "status" to "变化")
    fun zh(id: String): String = CATEGORIES.firstOrNull { it.first == id }?.second ?: id
    fun byIndex(i: Int): Pair<String, String> { val n = CATEGORIES.size; return CATEGORIES[((i % n) + n) % n] }
}

object PriorityCatalog {
    val OPTIONS: List<Pair<String, Int>> = listOf("先制" to 1, "普通" to 0, "后手" to -1)
    fun zh(v: Int): String = OPTIONS.firstOrNull { it.second == v }?.first ?: v.toString()
    fun byIndex(i: Int): Pair<String, Int> { val n = OPTIONS.size; return OPTIONS[((i % n) + n) % n] }
}

object TargetCatalog {
    val OPTIONS: List<Pair<String, String>> = listOf(
        "单体" to "normal", "全体" to "allAdjacentFoes", "己方全场" to "allySide",
        "对方全场" to "foeSide", "场地" to "all",
    )
    fun zh(v: String): String = OPTIONS.firstOrNull { it.second == v }?.first ?: v
    fun byIndex(i: Int): Pair<String, String> { val n = OPTIONS.size; return OPTIONS[((i % n) + n) % n] }
}

object MoveFlagCatalog {
    val FLAGS: List<Pair<String, String>> = listOf(
        "拳类" to "punch", "咬碎类" to "bite", "斩击类" to "slicing", "声音类" to "sound",
        "粉末类" to "powder", "波动类" to "pulse", "舞蹈招式" to "dance", "弹丸/炸弹" to "bullet",
    )
    fun byIndex(i: Int): Pair<String, String> { val n = FLAGS.size; return FLAGS[((i % n) + n) % n] }
}
