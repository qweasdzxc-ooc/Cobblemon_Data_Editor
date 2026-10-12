package com.xiaoyu.cobblemoneditor.data

/**
 * 效果积木：把预置的 Showdown 效果片段拼进招式/特性 JS。
 * 每个积木有一组数字/字符串参数，render 生成"对象内一行"的 Showdown 字段。
 * 注意：Showdown 字段格式为"框架优先"的初版，具体到游戏里再逐个校准。
 */
data class EffectBrick(
    val id: String,
    val displayName: String,
    val paramHints: List<String>,
    val render: (List<String>) -> String,
)

object EffectBricks {
    val ALL: List<EffectBrick> = listOf(
        EffectBrick("priority", "优先度", listOf("数值"), { p -> "priority: ${p[0]}" }),
        EffectBrick(
            "secondary_status", "概率附加状态", listOf("概率%", "状态(brn/par/psn/slp/frz)"),
            { p -> "secondary: { chance: ${p[0]}, status: \"${p[1]}\" }" }
        ),
        EffectBrick(
            "secondary_flinch", "概率畏缩", listOf("概率%"),
            { p -> "secondary: { chance: ${p[0]}, volatileStatus: \"flinch\" }" }
        ),
        EffectBrick("drain", "吸取回复", listOf("回复分母(2=50%)"), { p -> "drain: [1, ${p[0]}]" }),
        EffectBrick("heal", "回复自身", listOf("回复分母(2=50%)"), { p -> "heal: [1, ${p[0]}]" }),
        EffectBrick("multihit", "连击", listOf("最少次数", "最多次数"), { p -> "multihit: [${p[0]}, ${p[1]}]" }),
        EffectBrick("recoil", "反伤", listOf("反伤分母(4=1/4)"), { p -> "recoil: [1, ${p[0]}]" }),
        EffectBrick(
            "self_boost", "自身强化", listOf("属性(atk/def/spa/spd/spe)", "级数"),
            { p -> "self: { boosts: { ${p[0]}: ${p[1]} } }" }
        ),
        EffectBrick(
            "foe_boost", "对手弱化", listOf("属性(atk/def/spa/spd/spe)", "级数(正数)", "概率%"),
            { p -> "secondary: { chance: ${p[2]}, boosts: { ${p[0]}: -${p[1]} } }" }
        ),
    )

    fun byId(id: String): EffectBrick? = ALL.firstOrNull { it.id == id }
    fun ids(): List<String> = ALL.map { it.id }
}
