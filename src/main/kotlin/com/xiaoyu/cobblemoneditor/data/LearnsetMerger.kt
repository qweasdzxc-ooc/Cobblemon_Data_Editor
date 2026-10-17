package com.xiaoyu.cobblemoneditor.data

import com.cobblemon.mod.common.api.pokemon.moves.Learnset

/**
 * 把 Learnset 读取/合并为 species JSON 的 moves 数组格式："<method>:<moveId>"。
 * method 可为等级（1..100）、egg、tm、tutor、legacy、special。
 */
object LearnsetMerger {

    /** 读取现有学习表为字符串列表（等级组在前按等级升序，之后按 egg/tm/tutor/legacy/special）。 */
    fun read(learnset: Learnset?): List<String> {
        if (learnset == null) return emptyList()
        val out = ArrayList<String>()
        for ((level, templates) in learnset.levelUpMoves.entries.sortedBy { it.key }) {
            for (t in templates) out.add("$level:${t.name}")
        }
        for (t in learnset.eggMoves) out.add("egg:${t.name}")
        for (t in learnset.tmMoves) out.add("tm:${t.name}")
        for (t in learnset.tutorMoves) out.add("tutor:${t.name}")
        for (t in learnset.legacyMoves) out.add("legacy:${t.name}")
        for (t in learnset.specialMoves) out.add("special:${t.name}")
        return out
    }

    /** 现有学习表 ∪ 用户条目（去重；用户新增条目追加在后）。 */
    fun merge(existing: Learnset?, userEntries: List<String>): List<String> {
        val merged = read(existing).toMutableList()
        for (e in userEntries) if (e !in merged) merged.add(e)
        return merged
    }
}
