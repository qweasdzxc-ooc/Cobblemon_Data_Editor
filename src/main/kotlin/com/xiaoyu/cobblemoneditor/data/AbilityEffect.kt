package com.xiaoyu.cobblemoneditor.data

/** 触发时机：决定效果积木生成到哪个 Showdown 事件处理器。 */
enum class AbilityTrigger(
    val handler: String,
    val params: String,
    val displayName: String,
) {
    ON_START("onStart", "pokemon", "出场/战斗开始"),
    ON_BEFORE_MOVE("onBeforeMove", "source, target, move", "招式发动前"),
    ON_RESIDUAL("onResidual", "pokemon", "回合结束"),
    ON_TRY_HIT("onTryHit", "target, source, move", "受到攻击时(免疫)"),
    ON_DAMAGING_HIT("onDamagingHit", "damage, target, source, move", "受到伤害后"),
    ON_BASE_POWER("onBasePower", "basePower, attacker, defender, move", "威力计算时"),
    ON_UPDATE("onUpdate", "pokemon", "每回合检查"),
}

/** 能力效果积木：触发时机 + 动作片段（动作会拼进对应事件处理器函数体）。 */
data class AbilityEffectBrick(
    val id: String,
    val displayName: String,
    val trigger: AbilityTrigger,
    val paramHints: List<String>,
    val presets: List<Pair<String, String>>,
    val action: (List<String>) -> String,
)

object AbilityEffects {
    val ALL: List<AbilityEffectBrick> = listOf(
        AbilityEffectBrick("start_boost", "出场提升能力", AbilityTrigger.ON_START,
            listOf("属性", "级数"),
            listOf("atk:1" to "攻击+1", "def:1" to "防御+1", "spa:1" to "特攻+1", "spd:1" to "特防+1", "spe:1" to "速度+1", "atk:2" to "攻击+2"),
            { p -> "this.boost({ " + p[0] + ": " + p[1] + " }, pokemon);" }),
        AbilityEffectBrick("start_weather", "出场改变天气", AbilityTrigger.ON_START,
            listOf("天气"),
            listOf("sunnyday" to "大晴天", "raindance" to "下雨", "sandstorm" to "沙暴", "snow" to "冰雹"),
            { p -> "this.field.setWeather('" + p[0] + "');" }),
        AbilityEffectBrick("weather_boost", "有天气时提升能力", AbilityTrigger.ON_START,
            listOf("天气", "属性", "级数"),
            listOf("sunnyday:atk:1" to "晴天攻击+1", "raindance:spa:1" to "雨天特攻+1", "sandstorm:spd:1" to "沙暴特防+1", "snow:spe:1" to "冰雹速度+1"),
            { p -> "if (this.field.isWeather('" + p[0] + "')) { this.boost({ " + p[1] + ": " + p[2] + " }, pokemon); }" }),
        AbilityEffectBrick("before_boost", "发动招式前提升能力", AbilityTrigger.ON_BEFORE_MOVE,
            listOf("属性", "级数"),
            listOf("atk:1" to "攻击+1", "spa:1" to "特攻+1", "spe:1" to "速度+1"),
            { p -> "this.boost({ " + p[0] + ": " + p[1] + " }, source);" }),
        AbilityEffectBrick("before_heal", "发动招式前回复", AbilityTrigger.ON_BEFORE_MOVE,
            listOf("回复分母(8=1/8)"),
            listOf("8" to "回复1/8", "4" to "回复1/4"),
            { p -> "this.heal(source.baseMaxhp / " + p[0] + ", source);" }),
        AbilityEffectBrick("immune_type", "免疫某属性", AbilityTrigger.ON_TRY_HIT,
            listOf("属性(首字母大写)"),
            listOf("Fire" to "免疫火", "Water" to "免疫水", "Ground" to "免疫地面", "Electric" to "免疫电", "Grass" to "免疫草", "Steel" to "免疫钢", "Poison" to "免疫毒", "Ice" to "免疫冰"),
            { p -> "if (move.type === '" + p[0] + "') { this.add('-immune', target); return null; }" }),
        AbilityEffectBrick("contact_damage", "接触反伤", AbilityTrigger.ON_DAMAGING_HIT,
            listOf("反伤分母(8=1/8)"),
            listOf("8" to "反伤1/8", "4" to "反伤1/4"),
            { p -> "if (move.flags['contact']) { this.damage(source.baseMaxhp / " + p[0] + ", source, target); }" }),
        AbilityEffectBrick("hit_boost", "受到伤害后提升能力", AbilityTrigger.ON_DAMAGING_HIT,
            listOf("属性", "级数"),
            listOf("atk:1" to "攻击+1", "def:1" to "防御+1", "spa:1" to "特攻+1", "spd:1" to "特防+1", "spe:1" to "速度+1"),
            { p -> "this.boost({ " + p[0] + ": " + p[1] + " }, target);" }),
        AbilityEffectBrick("type_power_boost", "属性威力加成", AbilityTrigger.ON_BASE_POWER,
            listOf("属性", "倍率(1.5=1.5倍)"),
            listOf("Fire:1.5" to "火系×1.5", "Water:1.5" to "水系×1.5", "Grass:1.5" to "草系×1.5", "Electric:1.5" to "电系×1.5"),
            { p -> "if (move.type === '" + p[0] + "') return this.chainModify(" + p[1] + ");" }),
        AbilityEffectBrick("residual_heal", "回合结束回复", AbilityTrigger.ON_RESIDUAL,
            listOf("回复分母(8=1/8)"),
            listOf("8" to "回复1/8", "16" to "回复1/16", "4" to "回复1/4"),
            { p -> "this.heal(pokemon.baseMaxhp / " + p[0] + ", pokemon);" }),
        AbilityEffectBrick("low_hp_boost", "血量低于阈值提升能力", AbilityTrigger.ON_UPDATE,
            listOf("阈值%(50/30)", "属性", "级数"),
            listOf("50:atk:1" to "血量<50%攻击+1", "30:atk:1" to "血量<30%攻击+1", "50:spe:1" to "血量<50%速度+1"),
            { p -> "if (pokemon.hp <= pokemon.maxhp * " + p[0] + " / 100 && this.effectState.triggered === false) { this.boost({ " + p[1] + ": " + p[2] + " }, pokemon); this.effectState.triggered = true; }" }),
        AbilityEffectBrick("low_hp_heal", "血量低于阈值回复", AbilityTrigger.ON_UPDATE,
            listOf("阈值%(50/30)", "回复分母(4=1/4)"),
            listOf("30:4" to "血量<30%回复1/4", "50:4" to "血量<50%回复1/4", "30:2" to "血量<30%回复1/2"),
            { p -> "if (pokemon.hp <= pokemon.maxhp * " + p[0] + " / 100 && this.effectState.triggered === false) { this.heal(pokemon.baseMaxhp / " + p[1] + ", pokemon); this.effectState.triggered = true; }" }),
    )

    fun byId(id: String): AbilityEffectBrick? = ALL.firstOrNull { it.id == id }
}
