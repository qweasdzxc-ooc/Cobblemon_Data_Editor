package com.xiaoyu.cobblemoneditor.data

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * 把补丁写进存档的 datapack：<save>/datapacks/<pack>/data/<ns>/species_additions/<id>.json
 * 写入前对同名旧文件做一次备份。
 */
object DatapackWriter {
    // 1.21.1 的 pack_format
    private const val PACK_FORMAT = 48

    sealed interface Result {
        data class Written(val path: Path) : Result
        data class Failed(val message: String) : Result
    }

    fun ensurePackMeta(packRoot: Path) {
        try {
            Files.createDirectories(packRoot)
            val meta = packRoot.resolve("pack.mcmeta")
            if (!Files.exists(meta)) {
                Files.writeString(
                    meta,
                    "{\n  \"pack\": {\n    \"pack_format\": $PACK_FORMAT,\n    \"description\": \"Cobblemon Data Editor\"\n  }\n}\n"
                )
            }
        } catch (_: Exception) {
            // best effort
        }
    }

    fun writeSpeciesAddition(packRoot: Path, namespace: String, speciesId: String, json: String): Result {
        return try {
            ensurePackMeta(packRoot)
            val dir = packRoot.resolve("data/$namespace/species_additions")
            Files.createDirectories(dir)
            val file = dir.resolve("$speciesId.json")
            if (Files.exists(file)) {
                Files.copy(
                    file,
                    dir.resolve("$speciesId.json.bak-${System.currentTimeMillis()}"),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            Files.writeString(file, json)
            Result.Written(file)
        } catch (e: Exception) {
            Result.Failed(e.message ?: "write failed")
        }
    }
}
