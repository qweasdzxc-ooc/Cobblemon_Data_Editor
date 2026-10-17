package com.xiaoyu.cobblemoneditor.client

import com.cobblemon.mod.common.api.abilities.Abilities
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Files
import java.nio.file.Path

/** 客户端从 Cobblemon 运行时读全精灵/全特性列表。 */
object ClientCatalogs {

    /** 精灵：(英文id, 英文名, 中文名)，按图鉴号排序 */
    fun species(): List<Triple<String, String, String>> =
        PokemonSpecies.implemented
            .sortedBy { it.nationalPokedexNumber }
            .map { Triple(it.resourceIdentifier.path, it.name, it.translatedName.string) }

    /** 特性：id -> 显示名（优先 lang，其次 displayName，最后从 .js 的 shortDesc 读中文名，兜底 id） */
    fun abilities(): List<Pair<String, String>> =
        Abilities.all()
            .sortedBy { it.name }
            .map { a ->
                val key = "cobblemon.ability." + a.name
                val zh = I18n.get(key)
                val display = when {
                    zh != key -> zh
                    !a.displayName.isNullOrBlank() -> a.displayName
                    else -> readShortDesc(a.name) ?: a.name
                }
                a.name to display
            }

    private fun readShortDesc(id: String): String? {
        val file = abilityJsFile(id) ?: return null
        if (!Files.exists(file)) return null
        return try {
            val text = Files.readString(file)
            Regex("""shortDesc:s*"([^"]*)"""").find(text)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun abilityJsFile(id: String): Path? {
        val server = Minecraft.getInstance().singleplayerServer ?: return null
        return server.getWorldPath(LevelResource.ROOT)
            .resolve("datapacks").resolve("cobblemoneditor")
            .resolve("data").resolve("cobblemoneditor").resolve("abilities").resolve("$id.js")
    }
}
