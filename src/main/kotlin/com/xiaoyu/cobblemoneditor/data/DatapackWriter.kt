package com.xiaoyu.cobblemoneditor.data

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object DatapackWriter {
    private const val PACK_FORMAT = 48
    /** 每个文件保留的备份份数（更早的自动删除） */
    private const val BACKUP_KEEP = 10

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
            pruneBackups(dir, fileName)
            Result.Written(file)
        } catch (e: Exception) {
            Result.Failed(e.message ?: "write failed")
        }
    }

    /** 同一文件的备份只保留最近 BACKUP_KEEP 份，更早的删除。 */
    private fun pruneBackups(dir: Path, fileName: String) {
        try {
            val prefix = "$fileName.bak-"
            Files.newDirectoryStream(dir) { p -> p.fileName.toString().startsWith(prefix) }.use { stream ->
                val sorted = stream.sortedByDescending { backupTime(it.fileName.toString().substring(prefix.length)) }
                for (old in sorted.drop(BACKUP_KEEP)) {
                    Files.deleteIfExists(old)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun backupTime(suffix: String): Long = suffix.toLongOrNull() ?: 0L

    fun writeSpeciesAddition(packRoot: Path, namespace: String, speciesId: String, json: String): Result =
        writeWithBackup(packRoot, "data/$namespace/species_additions", "$speciesId.json", json)

    fun writeSpecies(packRoot: Path, namespace: String, speciesId: String, json: String): Result =
        writeWithBackup(packRoot, "data/$namespace/species", "$speciesId.json", json)

    fun writeMove(packRoot: Path, namespace: String, moveId: String, js: String): Result =
        writeWithBackup(packRoot, "data/$namespace/moves", "$moveId.js", js)

    fun writeAbility(packRoot: Path, namespace: String, abilityId: String, js: String): Result =
        writeWithBackup(packRoot, "data/$namespace/abilities", "$abilityId.js", js)

    fun writeHeldItem(packRoot: Path, namespace: String, itemId: String, js: String): Result =
        writeWithBackup(packRoot, "data/$namespace/held_items", "$itemId.js", js)
}
