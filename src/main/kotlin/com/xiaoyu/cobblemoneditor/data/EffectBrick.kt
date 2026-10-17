package com.xiaoyu.cobblemoneditor.data

/** 招式积木类型：决定 render 结果如何拼进招式对象字面量。 */
enum class MoveBrickKind { FIELD, SECONDARY, HANDLER }

/**
 * 校验积木参数是否合法：个数必须与 paramHints 完全一致；
 * 提示含数值关键词的槽位必须是数字（否则渲染阶段会越界崩溃或生成语法错误的 JS）。
 */
fun validateBrickParams(hints: List<String>, params: List<String>): Boolean {
    if (params.size != hints.size) return false
    val numericKeywords = listOf("概率", "级数", "分母", "次数", "倍率", "等级", "会心", "阈值", "加成")
    return hints.indices.all { i ->
        val v = params[i]
        if (v.isEmpty()) false
        else if (numericKeywords.any { hints[i].contains(it) }) v.toDoubleOrNull() != null
        else true
    }
}

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
        EffectBrick("status_field", "附加异常状态", listOf("状态"),
            listOf("par" to "麻痹", "brn" to "灼伤", "psn" to "中毒", "frz" to "冰冻", "slp" to "睡眠"),
            MoveBrickKind.FIELD, { p -> "status: '" + p[0] + "'" }),
        EffectBrick("volatile_status", "附加变化状态", listOf("变化状态"),
            listOf("confusion" to "混乱", "flinch" to "畏缩", "partiallytrapped" to "束缚"),
            MoveBrickKind.FIELD, { p -> "volatileStatus: '" + p[0] + "'" }),
        EffectBrick("weather_set", "改变天气", listOf("天气"),
            listOf("sunnyday" to "大晴天", "raindance" to "下雨", "sandstorm" to "沙暴", "snow" to "冰雹"),
            MoveBrickKind.FIELD, { p -> "weather: '" + p[0] + "'" }),
        EffectBrick("terrain_set", "改变场地", listOf("场地"),
            listOf("electricterrain" to "电气场地", "grassyterrain" to "青草场地", "mistyterrain" to "薄雾场地", "psychicterrain" to "精神场地"),
            MoveBrickKind.FIELD, { p -> "terrain: '" + p[0] + "'" }),
        EffectBrick("side_condition", "场地保护状态", listOf("状态"),
            listOf("reflect" to "反射壁", "lightscreen" to "光墙", "safeguard" to "神秘守护", "mist" to "白雾"),
            MoveBrickKind.FIELD, { p -> "sideCondition: '" + p[0] + "'" }),
        EffectBrick("crit_ratio", "会心等级", listOf("会心等级(2=易会心,3=极会心)"),
            listOf("1" to "普通(1/24)", "2" to "易会心(1/8)", "3" to "极会心(1/2)"),
            MoveBrickKind.FIELD, { p -> "critRatio: " + p[0] }),
        EffectBrick("force_switch", "强制换下对手", listOf(),
            listOf("" to "吼叫"),
            MoveBrickKind.FIELD, { "forceSwitch: true" }),
        EffectBrick("self_switch", "使用后换下自己", listOf(),
            listOf("" to "急速折返"),
            MoveBrickKind.FIELD, { "selfSwitch: true" }),
        EffectBrick("ohko", "一击必杀", listOf(),
            listOf("" to "地裂"),
            MoveBrickKind.FIELD, { "ohko: true" }),
        EffectBrick("always_hit", "必中", listOf(),
            listOf("" to "高速星星"),
            MoveBrickKind.FIELD, { "accuracy: true" }),
        EffectBrick("breaks_protect", "穿透守住", listOf(),
            listOf("" to "佯攻"),
            MoveBrickKind.FIELD, { "breaksProtect: true" }),
        EffectBrick("no_pp_boosts", "无视PP上限", listOf(),
            listOf("" to "复活祝福"),
            MoveBrickKind.FIELD, { "noPPBoosts: true" }),
        EffectBrick("twoturn_charge", "蓄力攻击", listOf(),
            listOf("" to "神鸟猛击(蓄力一回合)"),
            MoveBrickKind.HANDLER, { _ -> """onTryMove(attacker, defender, move) {
				if (attacker.removeVolatile(move.id)) {
					return;
				}
				this.add("-prepare", attacker, move.name);
				if (!this.runEvent("ChargeMove", attacker, defender, move)) {
					return;
				}
				attacker.addVolatile("twoturnmove", defender);
				return null;
			}""" }),
        EffectBrick("clear_all_boosts", "清除全场能力变化", listOf(),
            listOf("" to "黑雾"),
            MoveBrickKind.HANDLER, { _ -> """onHitField() {
				this.add("-clearallboost");
				for (const pokemon of this.getAllActive()) {
					pokemon.clearBoosts();
				}
			}""" }),
    )

    fun byId(id: String): EffectBrick? = ALL.firstOrNull { it.id == id }
}
