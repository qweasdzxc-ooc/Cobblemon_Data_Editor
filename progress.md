# 进度日志（progress）

## 2026-10-18 会话 1
- 用户批准三个方向：补齐设计缺口（新建精灵/学习表/冲突检测/备份N份）、扩展积木库、修游玩问题
- 通读全部源码（17 个 kt 文件）+ 设计文档 + 调研报告，掌握项目现状 v0.2.10
- 建立 task_plan.md / findings.md / progress.md
- 派发积木库扩展任务给队友 brick-builder（并行）
- 待办：Phase 1 调研（moves/*.js 格式等）→ Phase 2 新建精灵

## 2026-10-18 Phase 1-4 实施（同日续）
- Phase 1 验证完毕（moves/*.js = 裸 Showdown 对象、abilities/*.js 同、species_additions 补丁格式、Learnset getter API、学习表字符串格式），结论进 findings.md
- Phase 2 新建精灵：data/SpeciesFactory.kt（新建）+ 服务端 /cobbleeditor species add + 客户端 NewSpeciesScreen.kt（新建）+ 主菜单"精灵 · 新建"入口 + ClientCatalogs.moves()。构建 ✅（两次，中间修复 getDisplayName 不可用问题）
- Phase 3 学习表编辑：data/LearnsetMerger.kt（新建，Learnset→"1:tackle"字符串 + 合并）；SpeciesPatch 增 moves=/movesReplace= 键；服务端 runSet 合并（PokemonSpecies.getByName 读现有学习表）；客户端 EditorScreen 增学习表编辑框（预填当前学习表，保存发 movesReplace=完整列表）。构建待验证
- Phase 4 冲突检测+备份：ConflictChecker.kt（新建：图鉴号占用/官方同名/招式/特性同名）；EditorCommands 三命令保存后提示 ⚠ 冲突；DatapackWriter 备份保留最近 10 份（pruneBackups）。构建待验证
- brick-builder 完成 task-1：EffectBrick.kt 9→23 积木、AbilityEffect.kt 14→32 积木、AbilityTrigger 7→20 时机，全部有 Showdown 解包证据；已初步审查（语法/语义抽查通过）
- 构建：phase34 统一构建 ✅（修复 DatapackWriter 备份剪枝的 Stream.toList 推断错误后通过）

## 2026-10-18 Phase 6 版本与部署
- gradle.properties mod_version 0.2.10 → 0.3.0，构建 ✅ cobblemoneditor-0.3.0.jar（165683B）
- 部署到整合包 mods：新装 [自制]CobblemonDataEditor-0.3.0.jar；旧 [自制]CobblemonDataEditor-0.2.10.jar 移入版本目录 _mods_backup_20261007_234644 备份（可回滚）
- 验证：解包部署 jar 确认含 NewSpeciesScreen/SpeciesFactory/LearnsetMerger/ConflictChecker 类，fabric.mod.json version=0.3.0 ✅
- git 提交 9043ec2（17 files, +1033/-22）
- 待办：向用户汇报 0.3.0 新功能并收集 Phase 7 游玩问题清单

## 2026-10-18 实地数据排查（Phase 7 预研）
- 查 latest.log：用户最后一次游玩 0.2.9（10/17 21:54-22:42），全程无 cobblemoneditor 报错；会话中保存了 huomian 特性与 weedle 补丁
- 审计存档数据包 saves\新的开始\datapacks\cobblemoneditor：pikachu.json/weedle.json/huomian.js/mianhuo.js 均有效
- 修复 fengren.js（旧版 0.2.x 产出的废文件）：name "风刃"→"fengren"（ASCII，中文名会致 Showdown toID 为空→招式失效）、去掉 num:-1（注册表自动补号）、去重 priority 键；原文件备份为 fengren.js.bak-manual-20261018
- 发现待确认项：weedle.json 的 abilities 仅 ["h:huomian"]（species_additions 整体替换语义→weedle 特性池只剩隐藏特性、无常驻特性），需用户实测反馈是否异常
- 待办：等用户 Phase 7 问题清单

## 2026-10-18 特性合并语义修复（v0.3.1）
- 调研：javap 证实 AbilityPool extends PrioritizedList<PotentialAbility>（Iterable）；HiddenAbility 构造器字节码证实隐藏特性 = Priority.LOW；官方物种数据在 Cobblemon jar 内 data/cobblemon/species/generationN/<name>.json（资源管理器可读 cobblemon:species/**）
- 新增 src/main/kotlin/com/xiaoyu/cobblemoneditor/data/AbilityPoolMerger.kt：read(pool)（LOW→h: 前缀）、fromSpeciesJson(server,name)（listResources("species") 按 name 大小写不敏感匹配读官方 abilities）、merge(official,runtime,user)（三方并集去重）
- SpeciesPatch.kt：abilities→mergeAbilities（合并语义）、新增 abilitiesReplace（整体替换逃生门）
- EditorCommands.runSet：mergeAbilities 非空时执行 官方∪运行时∪用户 合并后整体写回
- 修复存档 weedle.json：["shielddust","h:runaway","h:huomian"]（原文件备份 weedle.json.bak-manual-20261018）
- 版本 0.3.0→0.3.1，构建+部署+提交待完成
