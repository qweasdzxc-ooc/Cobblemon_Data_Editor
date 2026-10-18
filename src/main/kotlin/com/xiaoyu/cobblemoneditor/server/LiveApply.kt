package com.xiaoyu.cobblemoneditor.server

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.abilities.Abilities
import com.cobblemon.mod.common.api.abilities.CommonAbility
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import net.minecraft.server.MinecraftServer

/**
 * 实时生效管线（借鉴精灵工坊 WorkshopRuntime 的设计，直接用 Cobblemon 编译期 API）：
 * 保存命令只写数据包文件并把手动补丁加入队列（queue），**不自动重载**；玩家在主菜单点
 * 「加载修改」（/cobbleeditor load）时才会执行：重新扫描数据包 → 把 cobblemoneditor 包移到
 * 启用列表末尾（最后加载，防止被其它包覆盖）→ server.reloadResources(enabled) 异步重载 →
 * Cobblemon 自带的 reload 监听器会把 moves 目录与 abilities 目录下的 .js 重新注册进内存
 * （Moves.getByName / Abilities.get 立即可命中）→ 重载完成后 applyLive 把种族值/特性池/学习表
 * 直接注入内存中的 Species 对象 → sync 全部在线玩家。
 *
 * 仍需要重进世界的部分：新精灵（species 目录的 .json，PokemonSpecies 注册表不热重载）、
 * 中文显示名（resources.zip 世界资源包在进入世界时加载）。
 * 学习表升级部分在内存中只注入 TM 表（与工坊相同做法），完整学习表重进世界后按文件生效。
 */
object LiveApply {

    /** 一次保存中需要注入内存的精灵补丁数据。 */
    data class SpeciesPatchData(
        val speciesName: String,
        val stats: Map<String, Int>,
        val abilities: List<String>,
        val moves: List<String>,
    )

    private val pendingPatches = mutableListOf<SpeciesPatchData>()
    private var reloadInFlight = false

    /** 保存成功后调用：只把精灵补丁加入待应用队列，不触发重载（重载由玩家点「加载修改」手动触发）。 */
    fun queue(patches: List<SpeciesPatchData>) {
        synchronized(pendingPatches) { pendingPatches.addAll(patches) }
    }

    /** 手动「加载」入口：重新扫描数据包 + 重载资源，完成后把队列里的补丁注入内存并同步玩家。 */
    fun reloadAndApply(server: MinecraftServer): Boolean {
        if (reloadInFlight) return false
        drain(server)
        return true
    }

    private fun drain(server: MinecraftServer) {
        if (reloadInFlight) return
        reloadInFlight = true
        try {
            server.packRepository.reload()
            val enabled = ArrayList(server.packRepository.selectedIds)
            enabled.remove(EditorCommands.PACK_ID)
            enabled.add(EditorCommands.PACK_ID)
            server.reloadResources(enabled).whenComplete { _, _ ->
                server.execute {
                    try {
                        val batch: List<SpeciesPatchData>
                        synchronized(pendingPatches) {
                            batch = pendingPatches.toList()
                            pendingPatches.clear()
                        }
                        for (p in batch) applyLive(p)
                        syncPlayers(server)
                    } catch (_: Exception) {
                    } finally {
                        reloadInFlight = false
                        synchronized(pendingPatches) {
                            if (pendingPatches.isNotEmpty()) drain(server)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            reloadInFlight = false
        }
    }

    private fun applyLive(p: SpeciesPatchData) {
        val species = PokemonSpecies.getByName(p.speciesName) ?: return
        for ((key, value) in p.stats) {
            val stat = when (key) {
                "hp" -> Stats.HP
                "attack" -> Stats.ATTACK
                "defence" -> Stats.DEFENCE
                "special_attack" -> Stats.SPECIAL_ATTACK
                "special_defence" -> Stats.SPECIAL_DEFENCE
                "speed" -> Stats.SPEED
                else -> null
            } ?: continue
            species.baseStats[stat] = value
        }
        for (a in p.abilities) {
            val hidden = a.startsWith("h:")
            val id = a.removePrefix("h:")
            val template = Abilities.get(id) ?: continue
            if (species.abilities.any { it.template.name == id }) continue
            val priority = if (hidden) Priority.LOW else Priority.NORMAL
            species.abilities.add(priority, CommonAbility(template))
        }
        for (m in p.moves) {
            val moveName = m.substringAfter(':', "")
            val template = Moves.getByName(moveName) ?: continue
            val tm = species.moves.tmMoves as MutableList<MoveTemplate>
            if (tm.none { it.name == moveName }) tm.add(template)
        }
    }

    private fun syncPlayers(server: MinecraftServer) {
        for (player in server.playerList.players) {
            try {
                PokemonSpecies.sync(player)
            } catch (_: Exception) {
            }
        }
    }
}
