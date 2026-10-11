package com.xiaoyu.cobblemoneditor.server

import com.mojang.brigadier.arguments.StringArgumentType
import com.xiaoyu.cobblemoneditor.data.DatapackWriter
import com.xiaoyu.cobblemoneditor.data.SpeciesPatch
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.world.level.storage.LevelResource

/**
 * 服务端指令：/cobbleeditor set <species> <spec>
 * 例：/cobbleeditor set pikachu speed=120,hp=40
 * 仅 OP（permission level >= 2）可用。
 */
object EditorCommands {
    const val PACK_ID = "cobblemoneditor"

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                Commands.literal("cobbleeditor")
                    .requires { it.hasPermission(2) }
                    .then(
                        Commands.literal("set")
                            .then(
                                Commands.argument("species", StringArgumentType.word())
                                    .then(
                                        Commands.argument("spec", StringArgumentType.greedyString())
                                            .executes { ctx ->
                                                runSet(
                                                    ctx.source,
                                                    StringArgumentType.getString(ctx, "species"),
                                                    StringArgumentType.getString(ctx, "spec"),
                                                )
                                            }
                                    )
                            )
                    )
            )
        }
    }

    private fun runSet(source: CommandSourceStack, species: String, spec: String): Int {
        val json = SpeciesPatch.build(species, spec)
        if (json == null) {
            source.sendFailure(
                Component.literal("参数无效。用法: /cobbleeditor set <species> <stat=value,...>  例: speed=120,hp=40")
            )
            return 0
        }
        val server = source.server
        val packRoot = server.getWorldPath(LevelResource.ROOT).resolve("datapacks").resolve(PACK_ID)
        return when (val r = DatapackWriter.writeSpeciesAddition(packRoot, PACK_ID, species, json)) {
            is DatapackWriter.Result.Written -> {
                source.sendSuccess(
                    { Component.literal("已保存 $species 的种族值补丁，重进世界后生效。\n文件: ${r.path}") },
                    false
                )
                1
            }
            is DatapackWriter.Result.Failed -> {
                source.sendFailure(Component.literal("写入失败: ${r.message}"))
                0
            }
        }
    }
}
