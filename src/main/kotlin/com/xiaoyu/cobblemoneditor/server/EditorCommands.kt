package com.xiaoyu.cobblemoneditor.server

import com.mojang.brigadier.arguments.StringArgumentType
import com.cobblemon.mod.common.CobblemonItemComponents
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.item.components.HeldItemEffectComponent
import com.cobblemon.mod.common.pokemon.Species
import com.xiaoyu.cobblemoneditor.data.AbilityPoolMerger
import com.xiaoyu.cobblemoneditor.data.AbilitySpecFactory
import com.xiaoyu.cobblemoneditor.data.DatapackWriter
import com.xiaoyu.cobblemoneditor.data.HeldItemSpecFactory
import com.xiaoyu.cobblemoneditor.data.LearnsetMerger
import com.xiaoyu.cobblemoneditor.data.MoveSpecFactory
import com.xiaoyu.cobblemoneditor.data.SpeciesFactory
import com.xiaoyu.cobblemoneditor.data.SpeciesPatch
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
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

            val itemCmd = Commands.literal("item")
                .then(
                    Commands.literal("add")
                        .then(
                            Commands.argument("id", StringArgumentType.word())
                                .then(
                                    Commands.argument("spec", StringArgumentType.greedyString())
                                        .executes { ctx ->
                                            runItemAdd(ctx.source, StringArgumentType.getString(ctx, "id"), StringArgumentType.getString(ctx, "spec"))
                                        }
                                )
                        )
                )
                .then(
                    Commands.literal("give")
                        .then(
                            Commands.argument("effectId", StringArgumentType.word())
                                .then(
                                    Commands.argument("itemId", StringArgumentType.word())
                                        .executes { ctx ->
                                            runItemGive(ctx.source, StringArgumentType.getString(ctx, "effectId"), StringArgumentType.getString(ctx, "itemId"))
                                        }
                                )
                        )
                )

            dispatcher.register(root.then(setCmd).then(speciesCmd).then(moveCmd).then(abilityCmd).then(itemCmd))
        }
    }

    private fun packRoot(source: CommandSourceStack) =
        source.server.getWorldPath(LevelResource.ROOT).resolve("datapacks").resolve(PACK_ID)

    private fun worldDir(source: CommandSourceStack) = source.server.getWorldPath(LevelResource.ROOT)

    private fun findSpecies(name: String): Species? =
        PokemonSpecies.getByName(name) ?: PokemonSpecies.getByIdentifier(ResourceLocation(PACK_ID, name))

    private fun runSet(source: CommandSourceStack, species: String, spec: String): Int {
        var parsed = SpeciesPatch.parseSpec(spec)
        if (parsed == null) {
            source.sendFailure(Component.literal("参数无效。用法: /cobbleeditor set <species> <key=value,...>（键: hp/atk/def/spa/spd/spe/primaryType/secondaryType/abilities/abilitiesReplace/moves/movesReplace）"))
            return 0
        }
        // abilities= 为合并模式：官方物种 JSON ∪ 运行时现有池 ∪ 用户条目（防止整体替换冲掉官方特性）
        if (parsed.mergeAbilities != null && parsed.replaceAbilities == null) {
            val speciesObj = findSpecies(species)
            val official = AbilityPoolMerger.fromSpeciesJson(source.server, speciesObj?.name ?: species)
            val runtime = AbilityPoolMerger.read(speciesObj?.abilities)
            parsed = parsed.copy(
                mergeAbilities = null,
                replaceAbilities = AbilityPoolMerger.merge(official, runtime, parsed.mergeAbilities!!)
            )
        }
        // moves= 为合并模式：与注册表中该精灵现有学习表取并集（查不到注册表则退化为仅用户条目）
        if (parsed.mergeMoves != null && parsed.replaceMoves == null) {
            val existing = findSpecies(species)?.moves
            parsed = parsed.copy(replaceMoves = LearnsetMerger.merge(existing, parsed.mergeMoves!!))
        }
        val json = SpeciesPatch.buildJson(species, parsed)
        return when (val r = DatapackWriter.writeSpeciesAddition(packRoot(source), PACK_ID, species, json)) {
            is DatapackWriter.Result.Written -> {
                LiveApply.request(source.server, listOf(LiveApply.SpeciesPatchData(
                    speciesName = species,
                    stats = parsed.stats,
                    abilities = parsed.replaceAbilities ?: emptyList(),
                    moves = parsed.replaceMoves ?: emptyList(),
                )))
                source.sendSuccess({ Component.literal("已保存 $species 补丁并即时应用（种族值/特性/学习表）。新精灵与中文名重进世界后生效。 文件: ${r.path}") }, false)
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
                if (LangPackWriter.writeSpecies(worldDir(source), species.id, species.nameZh ?: species.name)) {
                    note += "（显示名已写入世界资源包）"
                }
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
                var note = ""
                if (LangPackWriter.writeMove(worldDir(source), move.id, move.displayName, move.desc)) {
                    note += "（显示名已写入世界资源包）"
                }
                if (ConflictChecker.moveNameTaken(move.id)) note += " ⚠ 已有同名招式，重进世界后本次保存将覆盖其数据"
                LiveApply.request(source.server, emptyList())
                source.sendSuccess({ Component.literal("已保存招式 ${move.id}，效果即时生效。$note 文件: ${r.path}") }, false)
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
                var note = ""
                if (LangPackWriter.writeAbility(worldDir(source), ability.id, ability.displayName, ability.description)) {
                    note += "（显示名已写入世界资源包）"
                }
                if (ConflictChecker.abilityNameTaken(ability.id)) note += " ⚠ 已有同名特性，重进世界后本次保存将覆盖其数据"
                LiveApply.request(source.server, emptyList())
                source.sendSuccess({ Component.literal("已保存特性 ${ability.id}，效果即时生效。$note 文件: ${r.path}") }, false)
                1
            }
            is DatapackWriter.Result.Failed -> {
                source.sendFailure(Component.literal("写入失败: ${r.message}"))
                0
            }
        }
    }

    private fun runItemAdd(source: CommandSourceStack, id: String, spec: String): Int {
        val item = HeldItemSpecFactory.parse(id, spec)
        if (item == null) {
            source.sendFailure(Component.literal("道具参数无效。用法: /cobbleeditor item add <id> name=...&desc=...[&bricks=residual_heal:16|start_boost:atk:1|...]"))
            return 0
        }
        val js = HeldItemSpecFactory.renderJs(item)
        return when (val r = DatapackWriter.writeHeldItem(packRoot(source), PACK_ID, item.id, js)) {
            is DatapackWriter.Result.Written -> {
                LiveApply.request(source.server, emptyList())
                source.sendSuccess({ Component.literal("已保存道具效果 ${item.id}，效果即时生效。用 /cobbleeditor item give ${item.id} <物品id> 把效果绑定到物品。 文件: ${r.path}") }, false)
                1
            }
            is DatapackWriter.Result.Failed -> {
                source.sendFailure(Component.literal("写入失败: ${r.message}"))
                0
            }
        }
    }

    private fun runItemGive(source: CommandSourceStack, effectId: String, itemId: String): Int {
        if (!effectId.matches(Regex("[a-z0-9]+"))) {
            source.sendFailure(Component.literal("道具效果 id 无效（仅小写字母数字）"))
            return 0
        }
        val item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemId)).orElse(null)
        if (item == null) {
            source.sendFailure(Component.literal("物品 $itemId 不存在（示例: minecraft:diamond）"))
            return 0
        }
        return try {
            val stack = ItemStack(item)
            stack.set(CobblemonItemComponents.HELD_ITEM_EFFECT, HeldItemEffectComponent(effectId, false))
            source.playerOrException.addItem(stack)
            source.sendSuccess({ Component.literal("已获得 1 个绑定效果 $effectId 的 $itemId（放进精灵携带槽后生效）") }, false)
            1
        } catch (e: Exception) {
            source.sendFailure(Component.literal("给予失败（需要玩家执行）: ${e.message}"))
            0
        }
    }
}
