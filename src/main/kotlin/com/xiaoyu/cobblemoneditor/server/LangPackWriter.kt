package com.xiaoyu.cobblemoneditor.server

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 把自制招式/特性/精灵的显示名写入世界根目录的 resources.zip（Minecraft 世界自带资源包机制，
 * 进入世界时自动加载）。lang 键与 Cobblemon 官方 zh_cn.json 一致：
 *   cobblemon.move.<id> / cobblemon.move.<id>.desc
 *   cobblemon.ability.<id> / cobblemon.ability.<id>.desc
 *   cobblemoneditor.species.<id>.name（新物种 namespace=cobblemoneditor）
 *
 * 数据包无法提供 lang（lang 属于资源包），因此没有这些条目时游戏内会显示
 * "cobblemon.move.xxx" 之类的原始翻译键。
 *
 * 关键约束（实测）：游戏运行期间客户端全程持有 resources.zip 的文件锁，任何替换都会失败；
 * 而且资源管理器检测到包文件变化会触发第二次全量资源加载。因此写入策略是：
 *   - 内容与现包完全一致时不写文件（无变化 → 无重载）；
 *   - 写入失败时清理临时文件并返回 false，由调用方决定重试时机；
 *   - 权威写入时机是 SERVER_STOPPED（客户端释放锁之后，后台线程重试）。
 */
object LangPackWriter {
    private const val ZH = "assets/cobblemon/lang/zh_cn.json"
    private const val EN = "assets/cobblemon/lang/en_us.json"
    private const val ZH_NS = "assets/cobblemoneditor/lang/zh_cn.json"
    private const val EN_NS = "assets/cobblemoneditor/lang/en_us.json"
    private const val PACK_MCMETA = "pack.mcmeta"
    private const val PACK_FORMAT = 34 // Minecraft 1.21.1 资源包格式
    private const val MAX_RETRY = 60 // 后台同步最多重试次数（每次间隔 1 秒）
    private const val INITIAL_DELAY_MS = 5000L // 首次尝试前的等待，等客户端完成卸载、停止监听资源包
    private val gson = Gson()

    fun writeMove(worldDir: Path, id: String, displayName: String, desc: String): Boolean {
        val zh = displayName.ifBlank { id }
        val d = desc.ifBlank { zh }
        return updateZip(worldDir, mapOf(
            ZH to mapOf("cobblemon.move.$id" to zh, "cobblemon.move.$id.desc" to d),
            EN to mapOf("cobblemon.move.$id" to zh, "cobblemon.move.$id.desc" to d),
        ))
    }

    fun writeAbility(worldDir: Path, id: String, displayName: String, description: String): Boolean {
        val zh = displayName.ifBlank { id }
        val desc = description.ifBlank { zh }
        return updateZip(worldDir, mapOf(
            ZH to mapOf("cobblemon.ability.$id" to zh, "cobblemon.ability.$id.desc" to desc),
            EN to mapOf("cobblemon.ability.$id" to zh, "cobblemon.ability.$id.desc" to desc),
        ))
    }

    fun writeSpecies(worldDir: Path, id: String, displayName: String): Boolean {
        val zh = displayName.ifBlank { id }
        return updateZip(worldDir, mapOf(
            ZH_NS to mapOf("cobblemoneditor.species.$id.name" to zh),
            EN_NS to mapOf("cobblemoneditor.species.$id.name" to zh),
        ))
    }

    /**
     * 全量同步：扫描数据包内所有已保存的招式/特性/精灵文件，重建全部 lang 条目。
     * 返回是否已把世界资源包更新到位（内容无变化或写入成功都返回 true）。
     */
    fun syncAll(worldDir: Path, packRoot: Path): Boolean {
        val dataDir = packRoot.resolve("data").resolve(EditorCommands.PACK_ID)
        val common = mutableMapOf<String, String>()
        val species = mutableMapOf<String, String>()
        forEachFile(dataDir.resolve("moves"), "js") { name, text ->
            val zh = extractJsString(text, "shortDesc") ?: name
            common["cobblemon.move.$name"] = zh
            common["cobblemon.move.$name.desc"] = extractJsString(text, "desc") ?: zh
        }
        forEachFile(dataDir.resolve("abilities"), "js") { name, text ->
            val zh = extractJsString(text, "shortDesc") ?: name
            val desc = extractJsString(text, "desc") ?: zh
            common["cobblemon.ability.$name"] = zh
            common["cobblemon.ability.$name.desc"] = desc
        }
        forEachFile(dataDir.resolve("species"), "json") { name, text ->
            species["cobblemoneditor.species.$name.name"] = extractJsonString(text, "name") ?: name
        }
        return updateZip(worldDir, mapOf(
            ZH to common,
            EN to common,
            ZH_NS to species,
            EN_NS to species,
        ))
    }

    /**
     * 后台线程反复尝试 syncAll：世界退出后客户端会释放 resources.zip 的文件锁，
     * 趁隙写入。先等 INITIAL_DELAY_MS 再试——等待期间客户端完成世界卸载并停止
     * 监听世界资源包，此时写入不会触发"退出世界时的二次资源加载"。
     * 之后每次间隔 1 秒，最多 MAX_RETRY 次。用于 SERVER_STOPPED。
     */
    fun syncInBackground(worldDir: Path, packRoot: Path) {
        val t = Thread({
            try {
                Thread.sleep(INITIAL_DELAY_MS)
            } catch (_: InterruptedException) {
                return@Thread
            }
            var attempt = 0
            while (attempt < MAX_RETRY) {
                if (syncAll(worldDir, packRoot)) return@Thread
                attempt++
                try {
                    Thread.sleep(1000)
                } catch (_: InterruptedException) {
                    return@Thread
                }
            }
        }, "CobblemonEditor-LangSync")
        t.isDaemon = true
        t.start()
    }

    private fun forEachFile(dir: Path, ext: String, fn: (String, String) -> Unit) {
        if (!Files.isDirectory(dir)) return
        Files.newDirectoryStream(dir) { p -> p.fileName.toString().endsWith(".$ext") }.use { stream ->
            for (p in stream) {
                val name = p.fileName.toString().removeSuffix(".$ext")
                try {
                    fn(name, Files.readString(p))
                } catch (_: Exception) {
                }
            }
        }
    }

    /** 从生成的 .js 里取字符串值（支持单双引号），如 shortDesc: "风刃"。 */
    private fun extractJsString(text: String, key: String): String? {
        val marker = key + ":"
        val idx = text.indexOf(marker)
        if (idx < 0) return null
        val after = text.substring(idx + marker.length).trimStart()
        val quote = after.firstOrNull()
        if (quote != '"' && quote != '\'') return null
        val end = after.indexOf(quote, 1)
        if (end <= 0) return null
        return after.substring(1, end).takeIf { it.isNotBlank() }
    }

    /** 从生成的物种 .json 里取第一个 "name" 字符串值。 */
    private fun extractJsonString(text: String, key: String): String? {
        val marker = "\"$key\""
        val idx = text.indexOf(marker)
        if (idx < 0) return null
        val colon = text.indexOf(':', idx + marker.length)
        if (colon < 0) return null
        val q1 = text.indexOf('"', colon + 1)
        if (q1 < 0) return null
        val q2 = text.indexOf('"', q1 + 1)
        if (q2 < 0) return null
        return text.substring(q1 + 1, q2).takeIf { it.isNotBlank() }
    }

    /**
     * 合并写入 resources.zip：
     * - 保留包内其它条目，仅替换/新增 lang 文件（缺 pack.mcmeta 时补写）；
     * - 合并结果与现包完全一致时不写文件（避免触发资源重载）；
     * - 覆盖前把旧包备份为 resources.zip.bak-<epochMillis>；
     * - 写入走临时文件 + 原子替换，替换失败最多重试 3 次（间隔 500ms），
     *   失败时清理临时文件并返回 false。
     */
    private fun updateZip(worldDir: Path, langUpdates: Map<String, Map<String, String>>): Boolean {
        val zipFile = worldDir.resolve("resources.zip")
        val tmpFile = worldDir.resolve("resources.zip.tmp")
        try {
            // 清理上次失败遗留的临时文件
            Files.deleteIfExists(tmpFile)
            val existing = readZip(zipFile)
            val merged = existing.toMutableMap()
            for ((entryName, updates) in langUpdates) {
                if (updates.isEmpty()) continue
                val obj = parseLang(merged[entryName])
                for ((k, v) in updates) obj.addProperty(k, v)
                merged[entryName] = gson.toJson(obj).toByteArray(Charsets.UTF_8)
            }
            if (!merged.containsKey(PACK_MCMETA)) {
                merged[PACK_MCMETA] = """{"pack":{"pack_format":$PACK_FORMAT,"description":"Cobblemon Data Editor 中文显示"}}"""
                    .toByteArray(Charsets.UTF_8)
            }
            if (existing.isNotEmpty() && zipUnchanged(existing, merged)) return true
            if (Files.exists(zipFile)) {
                try {
                    Files.copy(zipFile, worldDir.resolve("resources.zip.bak-" + System.currentTimeMillis()), StandardCopyOption.REPLACE_EXISTING)
                } catch (_: Exception) {
                }
            }
            writeZip(tmpFile, merged)
            return if (moveWithRetry(tmpFile, zipFile)) {
                true
            } else {
                try {
                    Files.deleteIfExists(tmpFile)
                } catch (_: Exception) {
                }
                false
            }
        } catch (_: Exception) {
            try {
                Files.deleteIfExists(tmpFile)
            } catch (_: Exception) {
            }
            return false
        }
    }

    private fun readZip(zipFile: Path): Map<String, ByteArray> {
        if (!Files.exists(zipFile)) return emptyMap()
        val entries = LinkedHashMap<String, ByteArray>()
        ZipInputStream(Files.newInputStream(zipFile)).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                if (!e.isDirectory) entries[e.name] = zis.readBytes()
                zis.closeEntry()
                e = zis.nextEntry
            }
        }
        return entries
    }

    private fun writeZip(file: Path, entries: Map<String, ByteArray>) {
        ZipOutputStream(Files.newOutputStream(file)).use { zos ->
            for ((name, bytes) in entries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
    }

    private fun parseLang(bytes: ByteArray?): JsonObject {
        if (bytes == null) return JsonObject()
        return try {
            JsonParser.parseString(String(bytes, Charsets.UTF_8)).asJsonObject
        } catch (_: Exception) {
            JsonObject()
        }
    }

    private fun zipUnchanged(a: Map<String, ByteArray>, b: Map<String, ByteArray>): Boolean {
        if (a.size != b.size) return false
        for ((k, v) in a) {
            val other = b[k] ?: return false
            if (!v.contentEquals(other)) return false
        }
        return true
    }

    private fun moveWithRetry(tmp: Path, target: Path): Boolean {
        repeat(3) { i ->
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                return true
            } catch (_: Exception) {
                try {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING)
                    return true
                } catch (_: Exception) {
                    if (i < 2) {
                        try {
                            Thread.sleep(500)
                        } catch (_: InterruptedException) {
                        }
                    }
                }
            }
        }
        return false
    }
}
