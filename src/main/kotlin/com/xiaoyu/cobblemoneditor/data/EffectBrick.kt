package com.xiaoyu.cobblemoneditor.data

/** 招式积木类型：决定 render 结果如何拼进招式对象字面量。 */
enum class MoveBrickKind { FIELD, SECONDARY, HANDLER, RAW_HANDLER }

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
 * - RAW_HANDLER：render 原样返回参数内容（用户输入含函数签名的完整 JS 处理器）
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
            listOf("def:1:100" to "降防1级100%", "atk:1:100" to "降攻1级100%", "spa:1:100" to "降特攻1级100%", "spd:1:100" to "降特防1级100%", "spe:1:100" to "降速1级100%"),
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
        EffectBrick("side_condition", "场地状态(保护/陷阱)", listOf("状态"),
            listOf("reflect" to "反射壁", "lightscreen" to "光墙", "safeguard" to "神秘守护", "mist" to "白雾", "auroraveil" to "极光幕",
                "spikes" to "撒菱", "toxicspikes" to "毒菱", "stealthrock" to "隐形岩", "stickyweb" to "粘粘网"),
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
        EffectBrick("foe_stat_reset", "清除对手强化", listOf(),
            listOf("" to "清除之烟(清除目标强化)"),
            MoveBrickKind.HANDLER, { _ -> """onHit(target) {
				target.clearBoosts();
				this.add("-clearboost", target);
			}""" }),
        EffectBrick("stat_swap", "交换双攻强化", listOf(),
            listOf("" to "力量交换(交换物攻特攻强化)"),
            MoveBrickKind.HANDLER, { _ -> """onHit(target, source) {
				const targetBoosts = {};
				const sourceBoosts = {};
				const atkSpa = ['atk', 'spa'];
				for (const stat of atkSpa) {
					targetBoosts[stat] = target.boosts[stat];
					sourceBoosts[stat] = source.boosts[stat];
				}
				source.setBoost(targetBoosts);
				target.setBoost(sourceBoosts);
				this.add("-swapboost", source, target, 'atk, spa', '[from] move: Power Swap');
			}""" }),
        EffectBrick("stat_copy", "复制对手强化", listOf(),
            listOf("" to "自我暗示(复制对手能力变化)"),
            MoveBrickKind.HANDLER, { _ -> """onHit(target, source) {
				let i;
				for (i in target.boosts) {
					source.boosts[i] = target.boosts[i];
				}
				this.add("-copyboost", source, target, '[from] move: Psych Up');
			}""" }),
        EffectBrick("hazard_spikes", "设置自定义陷阱", listOf("陷阱显示名", "每层伤害分母(8=1/8)"),
            listOf("Spikes:8" to "撒菱(1/8)", "Stealth Rock:8" to "岩钉(1/8)", "Toxic Spikes:8" to "毒菱(1/8)", "Sticky Web:8" to "粘网(1/8)"),
            MoveBrickKind.FIELD, { p -> """sideCondition: '{MOVEID}',
			condition: {
				onSideStart(side) { this.add("-sidestart", side, '${p[0]}'); this.effectState.layers = 1; },
				onSideRestart(side) { if (this.effectState.layers >= 3) return false; this.add("-sidestart", side, '${p[0]}'); this.effectState.layers++; },
				onSwitchIn(pokemon) { if (!pokemon.isGrounded()) return; this.damage(pokemon.baseMaxhp / ${p[1]} * this.effectState.layers); }
			}""" }),
        EffectBrick("hazard_clear", "清除场地障碍", listOf(),
            listOf("" to "清除浓雾(清除全场场地状态)"),
            MoveBrickKind.HANDLER, { _ -> """onHit(target, source, move) {
				const removeTarget = ['reflect', 'lightscreen', 'auroraveil', 'safeguard', 'mist', 'spikes', 'toxicspikes', 'stealthrock', 'stickyweb'];
				for (const targetCondition of removeTarget) {
					if (target.side.removeSideCondition(targetCondition)) {
						this.add("-sideend", target.side, this.dex.conditions.get(targetCondition).name, '[from] move: Defog', '[of] ' + source);
					}
				}
				this.field.clearTerrain();
			}""" }),
        EffectBrick("self_heal_status", "治愈自身异常", listOf("回复分母(2=1/2)"),
            listOf("2" to "治愈异常并回复50%", "4" to "治愈异常并回复25%"),
            MoveBrickKind.HANDLER, { p -> """onHit(target, source) {
				if (!source.status) return false;
				source.cureStatus();
				this.heal(source.baseMaxhp / """ + p[0] + """, source);
			}""" }),
        EffectBrick("sacrifice_move", "自爆", listOf(),
            listOf("" to "大爆炸(使用后自身濒死)"),
            MoveBrickKind.FIELD, { "selfdestruct: 'always'" }),
        EffectBrick("mind_blown", "惊爆反噬", listOf(),
            listOf("" to "惊爆大头(使用后损失一半HP)"),
            MoveBrickKind.FIELD, { "mindBlownRecoil: true" }),
        EffectBrick("will_crit", "必定会心", listOf(),
            listOf("" to "冰息/山岚摔(必定会心)"),
            MoveBrickKind.FIELD, { "willCrit: true" }),
        EffectBrick("smart_target", "智能目标", listOf(),
            listOf("" to "巨龙双箭(每击可重新瞄准)"),
            MoveBrickKind.FIELD, { "smartTarget: true" }),
        EffectBrick("thaws_target", "解除冰冻", listOf(),
            listOf("" to "热水(解除目标冰冻)"),
            MoveBrickKind.FIELD, { "thawsTarget: true" }),
        EffectBrick("raw_handler", "自定义JS", listOf("完整JS(含函数签名)"),
            listOf(
                "onTryHit(target, source, move) { if (move.type === 'Ground') { this.add('-immune', target); return null; } }" to "免疫地面系",
                "onBasePower(basePower, attacker, defender, move) { return this.chainModify(1.5); }" to "威力×1.5",
                "onHit(target, source, move) { this.heal(source.baseMaxhp / 2, source); }" to "命中回复50%",
            ),
            MoveBrickKind.RAW_HANDLER, { p -> p[0] }),
    )

    fun byId(id: String): EffectBrick? = ALL.firstOrNull { it.id == id }
}
