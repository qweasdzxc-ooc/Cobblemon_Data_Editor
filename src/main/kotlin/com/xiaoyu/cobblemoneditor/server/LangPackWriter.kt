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
 * "cobblemon.move.xxx" 之类的原始翻译键。resources.zip 在进入世界时读取，
 * 与数据包一样"重进世界后生效"。
 */
object LangPackWriter {
    private const val ZH = "assets/cobblemon/lang/zh_cn.json"
    private const val EN = "assets/cobblemon/lang/en_us.json"
    private const val ZH_NS = "assets/cobblemoneditor/lang/zh_cn.json"
    private const val EN_NS = "assets/cobblemoneditor/lang/en_us.json"
    private val gson = Gson()

    /** 招式：名称键取 displayName；描述键取 desc（缺省回落 displayName）。 */
    fun writeMove(worldDir: Path, id: String, displayName: String, desc: String): Boolean {
        val zh = displayName.ifBlank { id }
        val descVal = desc.ifBlank { zh }
        return updateZip(worldDir, mapOf(
            ZH to mapOf("cobblemon.move.$id" to zh, "cobblemon.move.$id.desc" to descVal),
            EN to mapOf("cobblemon.move.$id" to zh, "cobblemon.move.$id.desc" to descVal),
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
     * 在服务器启动时调用，让旧版本（0.2.x）保存的数据也能显示中文名，无需重新编辑。
     */
    fun syncAll(worldDir: Path, packRoot: Path) {
        val dataDir = packRoot.resolve("data").resolve(EditorCommands.PACK_ID)
        val common = mutableMapOf<String, String>()
        val species = mutableMapOf<String, String>()
        forEachFile(dataDir.resolve("moves"), "js") { name, text ->
            val zh = extractJsString(text, "shortDesc") ?: name
            val desc = extractJsString(text, "desc") ?: zh
            common["cobblemon.move.$name"] = zh
            common["cobblemon.move.$name.desc"] = desc
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
        if (common.isEmpty() && species.isEmpty()) return
        updateZip(worldDir, mapOf(
            ZH to common,
            EN to common,
            ZH_NS to species,
            EN_NS to species,
        ))
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
     * 合并写入 resources.zip：保留包内其它条目，仅替换/新增 lang 文件；
     * 覆盖前把旧包备份为 resources.zip.bak-<epochMillis>。返回是否成功。
     */
    private fun updateZip(worldDir: Path, langUpdates: Map<String, Map<String, String>>): Boolean {
        val zipFile = worldDir.resolve("resources.zip")
        val entries = LinkedHashMap<String, ByteArray>()
        try {
            if (Files.exists(zipFile)) {
                ZipInputStream(Files.newInputStream(zipFile)).use { zis ->
                    var e = zis.nextEntry
                    while (e != null) {
                        if (!e.isDirectory) entries[e.name] = zis.readBytes()
                        zis.closeEntry()
                        e = zis.nextEntry
                    }
                }
            }
            // 世界资源包必须带 pack.mcmeta，否则进入世界时提示"无法应用服务器资源包"
            if (!entries.containsKey("pack.mcmeta")) {
                entries["pack.mcmeta"] =
                    "{\"pack\":{\"pack_format\":34,\"description\":\"Cobblemon Data Editor \u4e2d\u6587\u663e\u793a\"}}".toByteArray(Charsets.UTF_8)
            }
            for ((entryName, updates) in langUpdates) {
                if (updates.isEmpty()) continue
                val old = entries[entryName]
                val obj: JsonObject = if (old != null) {
                    try {
                        JsonParser.parseString(String(old, Charsets.UTF_8)).asJsonObject
                    } catch (_: Exception) {
                        JsonObject()
                    }
                } else {
                    JsonObject()
                }
                for ((k, v) in updates) obj.addProperty(k, v)
                entries[entryName] = gson.toJson(obj).toByteArray(Charsets.UTF_8)
            }
            if (Files.exists(zipFile)) {
                try {
                    Files.copy(zipFile, worldDir.resolve("resources.zip.bak-" + System.currentTimeMillis()), StandardCopyOption.REPLACE_EXISTING)
                } catch (_: Exception) {
                }
            }
            val tmp = worldDir.resolve("resources.zip.tmp")
            ZipOutputStream(Files.newOutputStream(tmp)).use { zos ->
                for ((entryName, bytes) in entries) {
                    zos.putNextEntry(ZipEntry(entryName))
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }
            try {
                Files.move(tmp, zipFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: Exception) {
                Files.move(tmp, zipFile, StandardCopyOption.REPLACE_EXISTING)
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }
}
