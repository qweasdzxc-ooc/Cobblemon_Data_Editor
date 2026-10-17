package com.xiaoyu.cobblemoneditor.data

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.abilities.AbilityPool
import com.google.gson.JsonParser
import net.minecraft.server.MinecraftServer

/**
 * 特性池合并工具。
 *
 * species_additions 的 "abilities" 键是整体替换语义：只写一条 h:x 会把官方特性池整个冲掉
 * （例如独角虫只剩隐藏特性、常规特性全丢）。因此把 abilities= 改成"并集合并"语义：
 *   官方物种 JSON 的 abilities ∪ 运行时现有池 ∪ 用户本次新加条目
 * 想去重为精确替换时用 abilitiesReplace=。
 */
object AbilityPoolMerger {

    /** 把运行时特性池读回字符串列表（Priority.LOW = 隐藏特性，h: 前缀；已由字节码证实） */
    fun read(pool: AbilityPool?): List<String> =
        pool?.map { pa ->
            val name = pa.template.name
            if (pa.priority == Priority.LOW) "h:$name" else name
        } ?: emptyList()

    /**
     * 从服务器资源管理器读物种 JSON（任意命名空间 data/<ns>/species/ 下）的 abilities 数组。
     * 按 "name" 字段大小写不敏感匹配；找不到返回 null，找到但无 abilities 返回空列表。
     */
    fun fromSpeciesJson(server: MinecraftServer, speciesName: String): List<String>? {
        val resources = try {
            server.resourceManager.listResources("species") { it.path.endsWith(".json") }
        } catch (_: Exception) {
            return null
        }
        for ((_, res) in resources) {
            val obj = try {
                JsonParser.parseReader(res.openAsReader()).asJsonObject
            } catch (_: Exception) {
                continue
            }
            val name = obj.get("name")?.takeIf { it.isJsonPrimitive }?.asString ?: continue
            if (!name.equals(speciesName, ignoreCase = true)) continue
            val arr = obj.get("abilities")?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
            return arr.mapNotNull { el -> if (el.isJsonPrimitive) el.asString else null }
        }
        return null
    }

    /** 三方并集去重：官方 ∪ 运行时 ∪ 用户；各来源可为 null（视为无条目）。 */
    fun merge(official: List<String>?, runtime: List<String>?, user: List<String>): List<String> {
        val out = LinkedHashSet<String>()
        if (official != null) out.addAll(official)
        if (runtime != null) out.addAll(runtime)
        out.addAll(user)
        return out.toList()
    }
}
