package com.xiaoyu.cobblemoneditor.client

import com.cobblemon.mod.common.api.abilities.Abilities
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Files
import java.nio.file.Path

/** 客户端从 Cobblemon 运行时读全精灵/全特性列表；中文名优先 lang，其次内置名表，最后从 .js shortDesc 兜底。 */
object ClientCatalogs {

    /** 内置官方中文名表（打包自 Cobblemon zh_cn.json，lang 缺失时的兜底） */
    private val speciesNames: Map<String, String> by lazy { loadCatalog("species_names.json") }
    private val moveNames: Map<String, String> by lazy { loadCatalog("move_names.json") }
    private val abilityNames: Map<String, String> by lazy { loadCatalog("ability_names.json") }

    /** 精灵：(英文id, 英文名, 中文名)，按图鉴号排序 */
    fun species(): List<Triple<String, String, String>> =
        PokemonSpecies.implemented
            .sortedBy { it.nationalPokedexNumber }
            .map {
                val zh = it.translatedName.string
                val display = if (zh.startsWith("cobblemon.species.")) speciesNames[it.name] ?: zh else zh
                Triple(it.resourceIdentifier.path, it.name, display)
            }

    /** 特性：id -> 显示名（lang 优先，其次内置名表，再 displayName，最后 .js shortDesc，兜底 id） */
    fun abilities(): List<Pair<String, String>> =
        Abilities.all()
            .sortedBy { it.name }
            .map { a ->
                val key = "cobblemon.ability." + a.name
                val zh = I18n.get(key)
                val display = when {
                    zh != key -> zh
                    abilityNames.containsKey(a.name) -> abilityNames[a.name]!!
                    !a.displayName.isNullOrBlank() -> a.displayName
                    else -> readShortDesc(a.name) ?: a.name
                }
                a.name to display
            }

    /** 招式：id -> 显示名（lang 优先，其次内置名表，兜底 id） */
    fun moves(): List<Pair<String, String>> =
        Moves.all()
            .sortedBy { it.name }
            .map { m ->
                val key = "cobblemon.move." + m.name
                val zh = I18n.get(key)
                val display = if (zh != key) zh else moveNames[m.name] ?: m.name
                m.name to display
            }

    private fun loadCatalog(file: String): Map<String, String> {
        val stream = ClientCatalogs::class.java.getResourceAsStream("/cobblemoneditor/catalog/$file") ?: return emptyMap()
        return try {
            Gson().fromJson(stream.reader(), object : TypeToken<Map<String, String>>() {}.type)
        } catch (_: Exception) {
            emptyMap()
        } finally {
            try { stream.close() } catch (_: Exception) { }
        }
    }

    private fun readShortDesc(id: String): String? {
        val file = abilityJsFile(id) ?: return null
        if (!Files.exists(file)) return null
        return try {
            extractJsString(Files.readString(file), "shortDesc")
        } catch (_: Exception) {
            null
        }
    }

    private fun readDesc(id: String): String? {
        val file = abilityJsFile(id) ?: return null
        if (!Files.exists(file)) return null
        return try {
            extractJsString(Files.readString(file), "desc")
        } catch (_: Exception) {
            null
        }
    }

    private fun extractJsString(text: String, key: String): String? {
        val marker = key + ":"
        val idx = text.indexOf(marker)
        if (idx < 0) return null
        val after = text.substring(idx + marker.length).trimStart()
        if (!after.startsWith('"')) return null
        val end = after.indexOf('"', 1)
        if (end <= 0) return null
        return after.substring(1, end).takeIf { it.isNotBlank() }
    }
    private fun abilityJsFile(id: String): Path? {
        val server = Minecraft.getInstance().singleplayerServer ?: return null
        return server.getWorldPath(LevelResource.ROOT)
            .resolve("datapacks").resolve("cobblemoneditor")
            .resolve("data").resolve("cobblemoneditor").resolve("abilities").resolve("$id.js")
    }
}
