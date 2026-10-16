package com.xiaoyu.cobblemoneditor.client

import com.cobblemon.mod.common.api.abilities.Abilities
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import net.minecraft.client.resources.language.I18n

/** 客户端从 Cobblemon 运行时读全精灵/全特性列表。 */
object ClientCatalogs {

    /** 精灵：(英文id, 英文名, 中文名)，按图鉴号排序 */
    fun species(): List<Triple<String, String, String>> =
        PokemonSpecies.implemented
            .sortedBy { it.nationalPokedexNumber }
            .map { Triple(it.resourceIdentifier.path, it.name, it.translatedName.string) }

    /** 特性：id -> 中文名（找不到翻译时回退英文显示名） */
    fun abilities(): List<Pair<String, String>> =
        Abilities.all()
            .sortedBy { it.name }
            .map { a ->
                val key = "cobblemon.ability." + a.name
                val zh = I18n.get(key)
                a.name to (if (zh == key) a.displayName else zh)
            }
}
