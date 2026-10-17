package com.xiaoyu.cobblemoneditor.server

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.abilities.Abilities
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies

/** 保存前的冲突检测（注册表级：覆盖官方数据/图鉴号占用）。 */
object ConflictChecker {

    /** 图鉴号是否已被占用（官方 + 自制命名空间），返回占用者 "ns:name"。 */
    fun dexTaken(dex: Int): String? {
        for (ns in listOf(Cobblemon.MODID, EditorCommands.PACK_ID)) {
            val s = PokemonSpecies.getByPokedexNumber(dex, ns) ?: continue
            return "$ns:${s.name}"
        }
        return null
    }

    /** 是否与官方精灵同名（自制命名空间下不会覆盖官方数据，仅提醒）。 */
    fun officialSpeciesNameTaken(id: String): Boolean =
        PokemonSpecies.getByName(id) != null

    /** 招式 id 是否已存在（保存后将覆盖同名招式数据）。 */
    fun moveNameTaken(id: String): Boolean = Moves.all().any { it.name == id }

    /** 特性 id 是否已存在（保存后将覆盖同名特性数据）。 */
    fun abilityNameTaken(id: String): Boolean = Abilities.all().any { it.name == id }
}
