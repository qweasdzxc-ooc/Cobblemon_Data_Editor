package com.xiaoyu.cobblemoneditor.data

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object DatapackWriter {
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
                Files.writeString(meta, """{"pack":{"pack_format":$PACK_FORMAT,"description":"Cobblemon Data Editor"}}""")
            }
        } catch (_: Exception) {
        }
    }

    private fun writeWithBackup(packRoot: Path, relDir: String, fileName: String, content: String): Result {
        return try {
            ensurePackMeta(packRoot)
            val dir = packRoot.resolve(relDir)
            Files.createDirectories(dir)
            val file = dir.resolve(fileName)
            if (Files.exists(file)) {
                Files.copy(file, dir.resolve("$fileName.bak-${System.currentTimeMillis()}"), StandardCopyOption.REPLACE_EXISTING)
            }
            Files.writeString(file, content)
            Result.Written(file)
        } catch (e: Exception) {
            Result.Failed(e.message ?: "write failed")
        }
    }

    fun writeSpeciesAddition(packRoot: Path, namespace: String, speciesId: String, json: String): Result =
        writeWithBackup(packRoot, "data/$namespace/species_additions", "$speciesId.json", json)

    fun writeMove(packRoot: Path, namespace: String, moveId: String, js: String): Result =
        writeWithBackup(packRoot, "data/$namespace/moves", "$moveId.js", js)

    fun writeAbility(packRoot: Path, namespace: String, abilityId: String, js: String): Result =
        writeWithBackup(packRoot, "data/$namespace/abilities", "$abilityId.js", js)
}
