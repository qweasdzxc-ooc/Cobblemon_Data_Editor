package com.xiaoyu.cobblemoneditor.data

/** 能力效果积木：生成 Showdown 能力事件处理器（单行 JS）。 */
data class AbilityEffectBrick(
    val id: String,
    val displayName: String,
    val paramHints: List<String>,
    val presets: List<Pair<String, String>>,
    val render: (List<String>) -> String,
)

object AbilityEffects {
    val ALL: List<AbilityEffectBrick> = listOf(
        AbilityEffectBrick("immune_type", "免疫某属性", listOf("属性(首字母大写)"),
            listOf("Fire" to "免疫火", "Water" to "免疫水", "Ground" to "免疫地面", "Electric" to "免疫电", "Grass" to "免疫草"),
            { p -> "onTryHit(target, source, move) { if (move.type === '" + p[0] + "') { this.add('-immune', target); return null; } }" }),
        AbilityEffectBrick("start_boost", "出场提升能力", listOf("属性(atk/def/spa/spd/spe)", "级数"),
            listOf("atk:1" to "攻击+1", "spa:1" to "特攻+1", "spe:1" to "速度+1", "atk:2" to "攻击+2"),
            { p -> "onStart(pokemon) { this.boost({ " + p[0] + ": " + p[1] + " }, pokemon); }" }),
        AbilityEffectBrick("start_weather", "出场改变天气", listOf("天气"),
            listOf("sunnyday" to "大晴天", "raindance" to "下雨", "sandstorm" to "沙暴", "hail" to "冰雹"),
            { p -> "onStart(pokemon) { this.field.setWeather('" + p[0] + "'); }" }),
        AbilityEffectBrick("contact_damage", "接触反伤", listOf("反伤分母(8=1/8)"),
            listOf("8" to "反伤1/8", "4" to "反伤1/4"),
            { p -> "onDamagingHit(damage, target, source, move) { if (move.flags['contact']) { this.damage(source.baseMaxhp / " + p[0] + ", source, target); } }" }),
        AbilityEffectBrick("type_power_boost", "属性威力加成", listOf("属性", "倍率(1.5=1.5倍)"),
            listOf("Fire:1.5" to "火系×1.5", "Water:1.5" to "水系×1.5", "Grass:1.5" to "草系×1.5"),
            { p -> "onBasePower(basePower, attacker, defender, move) { if (move.type === '" + p[0] + "') return this.chainModify(" + p[1] + "); }" }),
    )

    fun byId(id: String): AbilityEffectBrick? = ALL.firstOrNull { it.id == id }
    fun ids(): List<String> = ALL.map { it.id }
}
