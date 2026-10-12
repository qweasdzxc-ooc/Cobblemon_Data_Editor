package com.xiaoyu.cobblemoneditor.server

import com.mojang.brigadier.arguments.StringArgumentType
import com.xiaoyu.cobblemoneditor.data.DatapackWriter
import com.xiaoyu.cobblemoneditor.data.MoveSpecFactory
import com.xiaoyu.cobblemoneditor.data.SpeciesPatch
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.world.level.storage.LevelResource

object EditorCommands {
    const val PACK_ID = "cobblemoneditor"

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            val root = Commands.literal("cobbleeditor").requires { it.hasPermission(2) }

            val setCmd = Commands.literal("set")
                .then(
                    Commands.argument("species", StringArgumentType.word())
                        .then(
                            Commands.argument("spec", StringArgumentType.greedyString())
                                .executes { ctx ->
                                    runSet(ctx.source, StringArgumentType.getString(ctx, "species"), StringArgumentType.getString(ctx, "spec"))
                                }
                        )
                )

            val moveCmd = Commands.literal("move")
                .then(
                    Commands.literal("add")
                        .then(
                            Commands.argument("id", StringArgumentType.word())
                                .then(
                                    Commands.argument("spec", StringArgumentType.greedyString())
                                        .executes { ctx ->
                                            runMoveAdd(ctx.source, StringArgumentType.getString(ctx, "id"), StringArgumentType.getString(ctx, "spec"))
                                        }
                                )
                        )
                )

            dispatcher.register(root.then(setCmd).then(moveCmd))
        }
    }

    private fun packRoot(source: CommandSourceStack) =
        source.server.getWorldPath(LevelResource.ROOT).resolve("datapacks").resolve(PACK_ID)

    private fun runSet(source: CommandSourceStack, species: String, spec: String): Int {
        val json = SpeciesPatch.build(species, spec)
        if (json == null) {
            source.sendFailure(Component.literal("参数无效。用法: /cobbleeditor set <species> <key=value,...>  例: speed=120,secondaryType=flying"))
            return 0
        }
        return when (val r = DatapackWriter.writeSpeciesAddition(packRoot(source), PACK_ID, species, json)) {
            is DatapackWriter.Result.Written -> {
                source.sendSuccess({ Component.literal("已保存 $species 补丁，重进世界后生效。 文件: ${r.path}") }, false)
                1
            }
            is DatapackWriter.Result.Failed -> {
                source.sendFailure(Component.literal("写入失败: ${r.message}"))
                0
            }
        }
    }

    private fun runMoveAdd(source: CommandSourceStack, id: String, spec: String): Int {
        val fullSpec = if (spec.contains("id=")) spec else "id=$id&$spec"
        val move = MoveSpecFactory.parse(fullSpec)
        if (move == null) {
            source.sendFailure(Component.literal("招式参数无效。用法: /cobbleeditor move add <id> name=...&type=electric&category=special&power=80&accuracy=100&pp=10&target=normal[&bricks=secondary_status:10:par|drain:2]"))
            return 0
        }
        val js = MoveSpecFactory.renderJs(move)
        return when (val r = DatapackWriter.writeMove(packRoot(source), PACK_ID, move.id, js)) {
            is DatapackWriter.Result.Written -> {
                source.sendSuccess({ Component.literal("已保存招式 ${move.id}，重进世界后生效。 文件: ${r.path}") }, false)
                1
            }
            is DatapackWriter.Result.Failed -> {
                source.sendFailure(Component.literal("写入失败: ${r.message}"))
                0
            }
        }
    }
}
