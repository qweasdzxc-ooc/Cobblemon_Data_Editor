package com.xiaoyu.cobblemoneditor.data

/**
 * 效果积木：把预置的 Showdown 效果片段拼进招式/特性 JS。
 * presets 提供"参数"下拉的预设选项（参数字符串 -> 显示名）。
 */
data class EffectBrick(
    val id: String,
    val displayName: String,
    val paramHints: List<String>,
    val presets: List<Pair<String, String>>,
    val render: (List<String>) -> String,
)

object EffectBricks {
    val ALL: List<EffectBrick> = listOf(
        EffectBrick("priority", "优先度", listOf("数值"), listOf("1" to "+1", "2" to "+2"),
            { p -> "priority: " + p[0] }),
        EffectBrick("secondary_status", "概率附加状态", listOf("概率%", "状态"),
            listOf("10:brn" to "灼伤10%", "30:par" to "麻痹30%", "10:psn" to "中毒10%", "10:frz" to "冰冻10%", "10:slp" to "睡眠10%"),
            { p -> "secondary: { chance: " + p[0] + ", status: " + '"' + p[1] + '"' + " }" }),
        EffectBrick("secondary_flinch", "概率畏缩", listOf("概率%"),
            listOf("10" to "畏缩10%", "30" to "畏缩30%", "100" to "畏缩100%"),
            { p -> "secondary: { chance: " + p[0] + ", volatileStatus: " + '"' + "flinch" + '"' + " }" }),
        EffectBrick("drain", "吸取回复", listOf("回复分母(2=50%)"),
            listOf("2" to "吸取50%", "3" to "吸取33%", "4" to "吸取25%"),
            { p -> "drain: [1, " + p[0] + "]" }),
        EffectBrick("heal", "回复自身", listOf("回复分母(2=50%)"),
            listOf("2" to "回复50%", "4" to "回复25%"),
            { p -> "heal: [1, " + p[0] + "]" }),
        EffectBrick("multihit", "连击", listOf("最少次数", "最多次数"),
            listOf("2:5" to "2~5次", "2:3" to "2~3次", "3:5" to "3~5次"),
            { p -> "multihit: [" + p[0] + ", " + p[1] + "]" }),
        EffectBrick("recoil", "反伤", listOf("反伤分母(4=1/4)"),
            listOf("4" to "反伤1/4", "3" to "反伤1/3", "2" to "反伤1/2"),
            { p -> "recoil: [1, " + p[0] + "]" }),
        EffectBrick("self_boost", "自身强化", listOf("属性", "级数"),
            listOf("atk:1" to "攻击+1", "def:1" to "防御+1", "spa:1" to "特攻+1", "spd:1" to "特防+1", "spe:1" to "速度+1", "atk:2" to "攻击+2"),
            { p -> "self: { boosts: { " + p[0] + ": " + p[1] + " } }" }),
        EffectBrick("foe_boost", "对手弱化", listOf("属性", "级数(正数)", "概率%"),
            listOf("def:1:100" to "降防1级100%", "atk:1:100" to "降攻1级100%", "spa:1:100" to "降特攻1级100%"),
            { p -> "secondary: { chance: " + p[2] + ", boosts: { " + p[0] + ": -" + p[1] + " } }" }),
    )

    fun byId(id: String): EffectBrick? = ALL.firstOrNull { it.id == id }
    fun ids(): List<String> = ALL.map { it.id }
}
