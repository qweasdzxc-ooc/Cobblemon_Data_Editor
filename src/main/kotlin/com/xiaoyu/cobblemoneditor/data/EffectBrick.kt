package com.xiaoyu.cobblemoneditor.data

/** 招式积木类型：决定 render 结果如何拼进招式对象字面量。 */
enum class MoveBrickKind { FIELD, SECONDARY, HANDLER }

/**
 * 招式效果积木。
 * - FIELD：render 返回完整 "key: value" 片段
 * - SECONDARY：render 返回 "{...}" 值片段，多个会合并成 secondaries 数组
 * - HANDLER：render 返回 "handler(...) {...}" 完整事件处理器
 */
data class EffectBrick(
    val id: String,
    val displayName: String,
    val paramHints: List<String>,
    val presets: List<Pair<String, String>>,
    val kind: MoveBrickKind,
    val render: (List<String>) -> String,
)

object EffectBricks {
    val ALL: List<EffectBrick> = listOf(
        EffectBrick("secondary_status", "概率附加状态", listOf("概率%", "状态"),
            listOf("10:brn" to "灼伤10%", "30:par" to "麻痹30%", "10:psn" to "中毒10%", "10:frz" to "冰冻10%", "10:slp" to "睡眠10%"),
            MoveBrickKind.SECONDARY, { p -> "{ chance: " + p[0] + ", status: '" + p[1] + "' }" }),
        EffectBrick("secondary_flinch", "概率畏缩", listOf("概率%"),
            listOf("10" to "畏缩10%", "30" to "畏缩30%", "100" to "畏缩100%"),
            MoveBrickKind.SECONDARY, { p -> "{ chance: " + p[0] + ", volatileStatus: 'flinch' }" }),
        EffectBrick("foe_boost", "概率弱化对手", listOf("属性", "级数(正数)", "概率%"),
            listOf("def:1:100" to "降防1级100%", "atk:1:100" to "降攻1级100%", "spa:1:100" to "降特攻1级100%"),
            MoveBrickKind.SECONDARY, { p -> "{ chance: " + p[2] + ", boosts: { " + p[0] + ": -" + p[1] + " } }" }),
        EffectBrick("drain", "吸取回复", listOf("回复分母(2=50%)"),
            listOf("2" to "吸取50%", "3" to "吸取33%", "4" to "吸取25%"),
            MoveBrickKind.FIELD, { p -> "drain: [1, " + p[0] + "]" }),
        EffectBrick("heal", "回复自身", listOf("回复分母(2=50%)"),
            listOf("2" to "回复50%", "4" to "回复25%"),
            MoveBrickKind.FIELD, { p -> "heal: [1, " + p[0] + "]" }),
        EffectBrick("multihit", "连击", listOf("最少次数", "最多次数"),
            listOf("2:5" to "2~5次", "2:3" to "2~3次", "3:5" to "3~5次"),
            MoveBrickKind.FIELD, { p -> "multihit: [" + p[0] + ", " + p[1] + "]" }),
        EffectBrick("recoil", "反伤", listOf("反伤分母(4=1/4)"),
            listOf("4" to "反伤1/4", "3" to "反伤1/3", "2" to "反伤1/2"),
            MoveBrickKind.FIELD, { p -> "recoil: [1, " + p[0] + "]" }),
        EffectBrick("self_boost", "自身强化", listOf("属性", "级数"),
            listOf("atk:1" to "攻击+1", "def:1" to "防御+1", "spa:1" to "特攻+1", "spd:1" to "特防+1", "spe:1" to "速度+1", "atk:2" to "攻击+2"),
            MoveBrickKind.FIELD, { p -> "self: { boosts: { " + p[0] + ": " + p[1] + " } }" }),
        EffectBrick("weather_power_boost", "天气时威力加成", listOf("天气", "倍率"),
            listOf("sunnyday:1.5" to "晴天×1.5", "raindance:1.5" to "雨天×1.5", "sandstorm:1.3" to "沙暴×1.3"),
            MoveBrickKind.HANDLER, { p -> "onBasePower(basePower, attacker, defender, move) { if (this.field.isWeather('" + p[0] + "')) return this.chainModify(" + p[1] + "); }" }),
    )

    fun byId(id: String): EffectBrick? = ALL.firstOrNull { it.id == id }
}
