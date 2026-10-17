package com.xiaoyu.cobblemoneditor.server

import com.mojang.brigadier.arguments.StringArgumentType
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.xiaoyu.cobblemoneditor.data.AbilityPoolMerger
import com.xiaoyu.cobblemoneditor.data.AbilitySpecFactory
import com.xiaoyu.cobblemoneditor.data.DatapackWriter
import com.xiaoyu.cobblemoneditor.data.LearnsetMerger
import com.xiaoyu.cobblemoneditor.data.MoveSpecFactory
import com.xiaoyu.cobblemoneditor.data.SpeciesFactory
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

            val speciesCmd = Commands.literal("species")
                .then(
                    Commands.literal("add")
                        .then(
                            Commands.argument("id", StringArgumentType.word())
                                .then(
                                    Commands.argument("spec", StringArgumentType.greedyString())
                                        .executes { ctx ->
                                            runSpeciesAdd(ctx.source, StringArgumentType.getString(ctx, "id"), StringArgumentType.getString(ctx, "spec"))
                                        }
                                )
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

            val abilityCmd = Commands.literal("ability")
                .then(
                    Commands.literal("add")
                        .then(
                            Commands.argument("id", StringArgumentType.word())
                                .then(
                                    Commands.argument("spec", StringArgumentType.greedyString())
                                        .executes { ctx ->
                                            runAbilityAdd(ctx.source, StringArgumentType.getString(ctx, "id"), StringArgumentType.getString(ctx, "spec"))
                                        }
                                )
                        )
                )

            dispatcher.register(root.then(setCmd).then(speciesCmd).then(moveCmd).then(abilityCmd))
        }
    }

    private fun packRoot(source: CommandSourceStack) =
        source.server.getWorldPath(LevelResource.ROOT).resolve("datapacks").resolve(PACK_ID)

    private fun runSet(source: CommandSourceStack, species: String, spec: String): Int {
        var parsed = SpeciesPatch.parseSpec(spec)
        if (parsed == null) {
            source.sendFailure(Component.literal("参数无效。用法: /cobbleeditor set <species> <key=value,...>（键: hp/atk/def/spa/spd/spe/primaryType/secondaryType/abilities/abilitiesReplace/moves/movesReplace）"))
            return 0
        }
        // abilities= 为合并模式：官方物种 JSON ∪ 运行时现有池 ∪ 用户条目（防止整体替换冲掉官方特性）
        if (parsed.mergeAbilities != null) {
            val speciesObj = PokemonSpecies.getByName(species)
            val official = AbilityPoolMerger.fromSpeciesJson(source.server, speciesObj?.name ?: species)
            val runtime = AbilityPoolMerger.read(speciesObj?.abilities)
            parsed = parsed.copy(
                mergeAbilities = null,
                replaceAbilities = AbilityPoolMerger.merge(official, runtime, parsed.mergeAbilities!!)
            )
        }
        // moves= 为合并模式：与注册表中该精灵现有学习表取并集（查不到注册表则退化为仅用户条目）
        if (parsed.mergeMoves != null) {
            val existing = PokemonSpecies.getByName(species)?.moves
            parsed = parsed.copy(replaceMoves = LearnsetMerger.merge(existing, parsed.mergeMoves!!))
        }
        val json = SpeciesPatch.buildJson(species, parsed)
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

    private fun runSpeciesAdd(source: CommandSourceStack, id: String, spec: String): Int {
        val species = SpeciesFactory.parse(id, spec)
        if (species == null) {
            source.sendFailure(Component.literal("精灵参数无效。用法: /cobbleeditor species add <id> dex=9001&type1=electric&abilities=static[&type2=..&hp=..&atk=..&def=..&spa=..&spd=..&spe=..&moves=1:tackle|5:growl|egg:xxx|tm:yyy&...]"))
            return 0
        }
        val json = SpeciesFactory.buildJson(species)
        return when (val r = DatapackWriter.writeSpecies(packRoot(source), PACK_ID, species.id, json)) {
            is DatapackWriter.Result.Written -> {
                var note = ""
                ConflictChecker.dexTaken(species.dex)?.let { note += " ⚠ 图鉴号 ${species.dex} 已被 $it 占用，可能造成图鉴冲突" }
                if (ConflictChecker.officialSpeciesNameTaken(species.id)) note += " ⚠ 名称与官方精灵同名（自制命名空间下不会覆盖官方数据）"
                source.sendSuccess({ Component.literal("已创建新精灵 ${species.id}（图鉴号 ${species.dex}），重进世界后生效。$note 文件: ${r.path}") }, false)
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
            source.sendFailure(Component.literal("招式参数无效。用法: /cobbleeditor move add <id> name=...&type=electric&category=special&power=80&accuracy=100&pp=10&target=normal[&bricks=...]"))
            return 0
        }
        val js = MoveSpecFactory.renderJs(move)
        return when (val r = DatapackWriter.writeMove(packRoot(source), PACK_ID, move.id, js)) {
            is DatapackWriter.Result.Written -> {
                val note = if (ConflictChecker.moveNameTaken(move.id)) " ⚠ 已有同名招式，重进世界后本次保存将覆盖其数据" else ""
                source.sendSuccess({ Component.literal("已保存招式 ${move.id}，重进世界后生效。$note 文件: ${r.path}") }, false)
                1
            }
            is DatapackWriter.Result.Failed -> {
                source.sendFailure(Component.literal("写入失败: ${r.message}"))
                0
            }
        }
    }

    private fun runAbilityAdd(source: CommandSourceStack, id: String, spec: String): Int {
        val fullSpec = if (spec.contains("id=")) spec else "id=$id&$spec"
        val ability = AbilitySpecFactory.parse(fullSpec)
        if (ability == null) {
            source.sendFailure(Component.literal("特性参数无效。用法: /cobbleeditor ability add <id> name=...&desc=..."))
            return 0
        }
        val js = AbilitySpecFactory.renderJs(ability)
        return when (val r = DatapackWriter.writeAbility(packRoot(source), PACK_ID, ability.id, js)) {
            is DatapackWriter.Result.Written -> {
                val note = if (ConflictChecker.abilityNameTaken(ability.id)) " ⚠ 已有同名特性，重进世界后本次保存将覆盖其数据" else ""
                source.sendSuccess({ Component.literal("已保存特性 ${ability.id}，重进世界后生效。$note 文件: ${r.path}") }, false)
                1
            }
            is DatapackWriter.Result.Failed -> {
                source.sendFailure(Component.literal("写入失败: ${r.message}"))
                0
            }
        }
    }
}
