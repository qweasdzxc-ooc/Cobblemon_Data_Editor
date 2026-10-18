package com.xiaoyu.cobblemoneditor.data

/**
 * 携带道具（held item）效果积木库。
 *
 * 依据：Showdown data/items.js（research\temp\x\data\cobblemon\showdown.zip 提取，
 * 另存 E:\AGI\DeepSeek Harness\showdown-items.js）。道具事件签名与特性不同：
 *   onModifyX(stat, pokemon) —— 第二参是持有者，不是 attacker/defender；
 *   onImmunity(type, pokemon)、onResidual(pokemon)、onStart(pokemon)、onEat(pokemon)、
 *   onUpdate(pokemon)、onTakeItem(item, pokemon)、onModifyDamage(damage, source, target, move)。
 * 道具字段：name/spritenum/fling{basePower}/isBerry/naturalGift{basePower,type}。
 */
data class HeldItemBrick(
    val id: String,
    val displayName: String,
    val paramHints: List<String>,
    val presets: List<List<String>>,
    /** 返回要放进道具对象的键值/方法字符串（FIELD 放对象体，HANDLER 放方法区）。 */
    val render: (List<String>) -> String,
)

object HeldItemBricks {
    val ALL: List<HeldItemBrick> = listOf(
        HeldItemBrick("is_berry", "树果(可自然食用)", emptyList(), listOf(emptyList())) { _ -> "isBerry: true" },
        HeldItemBrick("natural_gift", "自然恩惠", listOf("威力", "属性"), listOf(listOf("80", "fire"))) { p ->
            "naturalGift: { basePower: ${p[0]}, type: '${p[1]}' }"
        },
        HeldItemBrick("fling_power", "投掷威力", listOf("威力"), listOf(listOf("10"))) { p -> "fling: { basePower: ${p[0]} }" },
        HeldItemBrick("residual_heal", "每回合回复HP", listOf("分母"), listOf(listOf("16"))) { p ->
            "onResidual(pokemon) { this.heal(pokemon.baseMaxhp / ${p[0]}); }"
        },
        HeldItemBrick("start_boost", "出场能力提升", listOf("属性", "级数"), listOf(listOf("atk", "1"))) { p ->
            "onStart(pokemon) { this.boost({ ${p[0]}: ${p[1]} }, pokemon); }"
        },
        HeldItemBrick("eat_when_low", "低HP自动食用", listOf("触发HP分母", "治疗分母"), listOf(listOf("4", "4"))) { p ->
            "onUpdate(pokemon) { if (pokemon.hp <= pokemon.maxhp / ${p[0]} && !pokemon.status) { pokemon.eatItem(); } }\n" +
                "  onEat(pokemon) { this.heal(pokemon.baseMaxhp / ${p[1]}); }"
        },
        HeldItemBrick("take_item_boost", "受击后能力提升(消耗)", listOf("属性", "级数"), listOf(listOf("spa", "1"))) { p ->
            "onTakeItem(item, pokemon) { this.boost({ ${p[0]}: ${p[1]} }, pokemon); }"
        },
        HeldItemBrick("stat_multiplier", "能力倍率", listOf("属性", "倍率"), listOf(listOf("atk", "1.5"))) { p ->
            val handler = when (p[0]) {
                "atk" -> "onModifyAtk"; "spa" -> "onModifySpA"; "def" -> "onModifyDef"
                "spd" -> "onModifySpD"; "spe" -> "onModifySpe"; else -> "onModifyAtk"
            }
            val stat = if (p[0] in listOf("atk", "spa", "def", "spd", "spe")) p[0] else "atk"
            "$handler($stat, pokemon) { return this.chainModify(${p[1]}); }"
        },
        HeldItemBrick("damage_multiplier", "受到伤害倍率", listOf("倍率"), listOf(listOf("0.5"))) { p ->
            "onModifyDamage(damage, source, target, move) { return this.chainModify(${p[0]}); }"
        },
        HeldItemBrick("immunity", "免疫指定类型/天气", listOf("免疫对象"), listOf(listOf("fire"))) { p ->
            "onImmunity(type, pokemon) { if (type === '${p[0]}') return false; }"
        },
    )

    fun byId(id: String): HeldItemBrick? = ALL.find { it.id == id }

    /** 参数校验：个数=提示数，数字位必须是数字（复用 EffectBrick.kt 的共享校验）。 */
    fun validParams(brick: HeldItemBrick, params: List<String>): Boolean =
        validateBrickParams(brick.paramHints, params)
}
