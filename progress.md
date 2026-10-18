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

### 构建与部署记录（v0.3.1）
- 构建失败 2 次：①AbilityPoolMerger KDoc 内 data/<ns>/species/** 的 /** 触发 Kotlin 嵌套块注释（Unclosed comment）→ 改为单星 ②JsonArray 未 smart-cast（takeIf 后需 .asJsonArray）→ mapNotNull 未解析。第三次 ✅ BUILD SUCCESSFUL 6s
- jar 校验：fabric.mod.json version=0.3.1，含 AbilityPoolMerger/SpeciesFactory/NewSpeciesScreen .class ✅
- 部署：mods/[自制]CobblemonDataEditor-0.3.1.jar（170747B），0.3.0 移入 _mods_backup_20261007_234644
- git commit df2b0ed（feat: ability pool merge semantics + fixes）

## 2026-10-18 主动代码自检（客户端界面）
- 巡检：latest.log 仍停在 10/17 22:42（用户未再游玩）；数据包文件全部健康
- 修 bug：EditorScreen 切换精灵时未清空上一精灵的编辑状态（六维/属性/特性/学习表会泄漏到新精灵上，保存即串改数据）→ 新增 onSpeciesChanged() 全量重置，类型下拉提升为字段
- 修：NewSpeciesScreen id 必须以字母开头（与 SpeciesFactory name 规则一致，避免服务端报错晦涩）；特性槽标签"不改"→"无"
- 构建 ✅ 4s，部署 0.3.1（171209B），commit 43f2c91 + e5c28d0

## 2026-10-18 渲染管线终审（对照 Cobblemon 读回代码）
- 逐项核验 MoveSpec.renderJs/AbilitySpec.renderJs 输出 vs Cobblemon 读回（Moves.kt/Abilities.kt）：ability 读回只需 id ✅；多 secondary 合并 secondaries ✅；同触发时机 ability handler 合并 ✅；accuracy:true→-1.0 ✅
- ⚠️ 抓到真 bug：Moves.kt:77 `jsMove.get("basePower").asDouble` 无条件取值 → 编辑器生成的状态招式（无 basePower 键）会被注册表静默丢弃，文件在但游戏里永远看不到。官方状态招式都带 basePower:0 才幸免
- 修复：MoveSpec.renderJs 统一输出 basePower（状态=0）；findings.md 记录该陷阱；构建 ✅ 部署 ✅ commit 4c49ad6

## 2026-10-18 积木参数校验（双端）
- 隐患：积木参数个数不符/数字槽位填非数字 → 服务端渲染阶段 IndexOutOfBounds 崩溃，或生成语法错误的 JS 文件
- 修复：EffectBrick.kt 新增共享 validateBrickParams(hints, params)（个数必须等于提示数；含"概率/级数/分母/次数/倍率/等级/会心/阈值/加成"关键词的槽位必须是数字）；MoveSpecFactory/AbilitySpecFactory.parse 强制校验；两个编辑器界面添加积木时即时校验并提示示例
- 顺带收紧：MoveSpec parse 的 accuracy/pp/priority 非数字不再静默回落默认值而是拒绝
- 构建 ✅（5s/3s 两轮），部署 174527B，commit fdd8bbd

## 2026-10-18 Phase 7 用户实测反馈修复（v0.3.2）
- 用户反馈：①界面输入框仍有文本遮挡（截图=精灵编辑器，选中妙蛙种子）②自制特性/招式游戏内显示 raw lang key（cobblemon.ability.mianyihuo / cobblemon.move.fengren），中文名与描述都不显示（数据包无法提供 lang，lang 属资源包）
- 遮挡根因：render() 里 drawString 在 super.render 之后，部分标签画在输入框/下拉框上。修复：EditorScreen 属性2/特性2 标签移至下拉框之间（属性2 left+166、特性2 left+174，下拉宽度 110→100、第二个下拉右移 fieldX+132/fieldX+124）；NewSpeciesScreen 同型修复（属性2 left+166、特性2 left+174）；MoveEditorScreen 删除压在威力框上的"命中:"等三标签，改用 EditBox 占位提示（威力/命中/pp）；NewSpeciesScreen"来源:/等级:"两标签同样改占位提示
- lang 修复：新增 server/LangPackWriter.kt —— 写世界根目录 resources.zip（Minecraft 世界自带资源包，进世界自动加载）：cobblemon.move.<id>(.desc) / cobblemon.ability.<id>(.desc) / cobblemoneditor.species.<id>.name，zh_cn+en_us 双写、合并保留包内其它条目、覆盖前备份 resources.zip.bak-<epoch>
- EditorCommands 三命令保存成功后自动写入 lang（消息附"（显示名已写入世界资源包）"）；CobblemonEditor 注册 SERVER_STARTED → LangPackWriter.syncAll 全量扫描数据包重建 lang（旧版 0.2.x 数据无需重编辑即显示中文）
- SpeciesFactory 新增 namezh= 键（中文显示名，≤32 字符，禁 , " \ |）；NewSpeciesScreen 新增中文名输入框（可选）
- 待办：构建部署 0.3.2、向用户汇报并请求复测

## 2026-10-18 12:43 v0.3.2 部署完成
- 用户关闭游戏后：mods 装入 [自制]CobblemonDataEditor-0.3.2.jar（183886B）
- 0.3.1 归档至 _mods_backup_20261007_234644

## 2026-10-18 14:55 v0.3.3 六项反馈全部实现 + 部署
- ①特性搜索重置丢失已选值：rebuildAbilityDropdowns 把已确认值追加进过滤下拉（extra 列表）
- ②招式编辑器全重排（标签画在控件间隙、描述框 desc=、保存时写入 lang .desc 键）+ 赋予精灵区（招式/特性编辑器底部，走 cobbleeditor set moves=/abilities= 合并语义）
- ③精灵编辑器选中即预填六维/属性/特性/学习表（读 Species.baseStats/primaryType/abilities/moves）；三属性+：Cobblemon 数据模型只有 primaryType/secondaryType 两槽，无法支持（未实现，需向用户说明）
- ④特性编辑器：新触发（onModifyAtk 等 20 个）与新积木（40 个）落地
- ⑤携带道具：HeldItemEditorScreen + HeldItemBricks(10) + HeldItemSpec + /cobbleeditor item add/give（HeldItemEffectComponent 绑定到任意物品，编辑器一键给予）
- ⑥积木库大扩展：招式 35 积木（含 raw_handler 自定义 JS、自定义陷阱 condition 注入、官方陷阱 preset 9 个）；特性 40 积木（含 raw_effect）；raw 参数经 URL-safe Base64 传输（解决 ":"/"|"/"&" 语法冲突）
- 关键技术考证：side condition 由 dex-conditions.js 按 id 从招式 condition 字段解析（sideCondition:'{MOVEID}'+condition 注入=自定义陷阱正确写法）
- 构建：remapJar OOM（-Xmx2G 不够）→ gradle.properties 提到 -Xmx4G + 重启 daemon 后 BUILD SUCCESSFUL；误提交的 752MB hprof 已从仓库移除并 ignore *.hprof
- 部署：mods 装入 [自制]CobblemonDataEditor-0.3.3.jar（222115B，71 entries，fabric.mod.json=0.3.3），0.3.2 归档备份目录；commit d86d4a0 + 1459cb4
- 待办：用户复测六项反馈；item 2/4 的 UI 联调问题留待反馈

## 2026-10-18 Phase 9 工坊设计整合（v0.3.4）
- 用户请求：学习 E:\PCL\自制修改mod\cobblemon_1_cobblem\源码\mod（精灵工坊）的优秀设计整合进本项目；不加任何模组物品；实现方式与我们有区别的，一律按我们的来
- 子代理深度分析 14 个 Java 文件全部源码，10 节报告；定稿整合清单并逐项落地：
- 9.1 实时生效管线：新增 server/LiveApply.kt —— 保存后重扫数据包→cobblemoneditor 包移到启用列表末尾→reloadResources→Cobblemon reload 监听器重读 moves/abilities 的 .js→直接注入内存（种族值 baseStats.put / 特性池 CommonAbility+LOWEST / 学习表 TM 注入）+ PokemonSpecies.sync 同步在线玩家。新精灵与中文名仍需重进世界（PokemonSpecies 不热重载、世界资源包进世界时加载）
- 9.5 原子写入：DatapackWriter.writeWithBackup 改临时文件+ATOMIC_MOVE（回退普通 move）
- 9.6 单元测试：新增 src/test/kotlin/.../SpecParsingTest.kt 12 个 JUnit5 测试（SpeciesFactory/SpeciesPatch/MoveSpec/AbilitySpec/HeldItemSpec 纯逻辑，全部通过）；build.gradle 加 junit-jupiter
- 9.3 中文名目录：从 Cobblemon zh_cn.json 提取内置名表（species 1025/moves 1875/abilities 628）打包进 jar（resources/cobblemoneditor/catalog/），ClientCatalogs 兜底显示中文名（保留 resources.zip 增量机制为主）
- 9.2 构建稳健性：processResources 加 filteringCharset='UTF-8'、fabric.mod.json 改行过滤替换 version（避免 expand 破坏 JSON 转义）、JavaCompile encoding=UTF-8
- 明确不学：终端物品+物品组（违反无物品约束）、全反射桥接（我们有编译依赖直接调用）、他们的 JS 生成器（方向不同）、聊天命令回显（保持我们的屏幕内交互）
- 构建：034a 失败（LiveApply KDoc 含 moves/*.js 嵌套块注释）→ 034b ✅ BUILD SUCCESSFUL 58s（含 test 任务全绿）
- 部署：mods 装入 [自制]CobblemonDataEditor-0.3.4.jar（300510B，fabric.mod.json=0.3.4，含 LiveApply/catalog 资源），0.3.3 归档备份目录

---

## 2026-10-18 17:5x v0.3.5 五问题修复轮

用户实测反馈 5 个问题，逐项处理：

1. **世界资源包警告**（无法应用服务器资源包）——resources.zip 缺 pack.mcmeta，LangPackWriter 写入时自动补齐（pack_format 34）。
2. **特性 id 中文报错**——客户端保存/赋予前自动生成合法 ASCII id（"c"+名称 hash 6 位），中文名填名称框即可；服务端保持严格校验。
3. **效果数量上限**——招式积木 6→12、特性积木 6→12、道具积木 6→8，已添加列表改双列渲染。
4. **anystag 日志**——[扩展-任意标签]anytag-1.0.1.jar 自带数据包加载的 info 日志，整合包原生行为，无害无需修。
5. **参数显示不全**——三编辑器增加"参数说明"行（paramHints），已添加条目把参数解析成预设中文名显示。

构建 BUILD SUCCESSFUL、部署 [自制]CobblemonDataEditor-0.3.5.jar（304457B）、README 同步更新并推送。
## 2026-10-18 20:1x v0.3.6 万毒穿心炸服修复
- 根因：用户把"效果名:参数"（contact_poison:100、boost_on_faint:atk:1,spe:1）当作自定义JS填进 raw 参数框，
  渲染器原样输出后生成非法 JS（boost_on_faint:atk:1 → "Expected comma but found :"），
  Cobblemon Abilities.reload 拼全量注册表 eval 失败 → 数据包加载失败 → 无法进入世界。
- 修复：①存档 c597391.js 已重写为合法 JS（坏文件备份 .bak-broken-20261018），
  接触剧毒/击倒强化按真实积木重建 ②新增 isSaneRawJs 校验（括号配平+顶层裸冒号拒绝），服务端与客户端双端拦截
  ③新增特性积木 faint_boost（onSourceAfterFaint 击倒对手后提升）④v0.3.6 构建部署。
- 教训：任何会拼进 Cobblemon 注册表 JS 的用户输入都必须做语法级防呆；数据包 eval 失败=世界无法进入（致命）。
## v0.3.7 —— 新特性炸档与中文名缺失修复（2026-10-18 晚）
- 问题1：新特性保存后进世界报数据包错误。根因=我们自己的 AbilityTrigger.ON_MODIFY_MOVE 参数串写成 "move, pokemon, target, move"（重复形参），GraalVM strict mode 直接拒绝整个特性注册表求值 → 进世界炸档。已改正确签名 "move, pokemon, target, source"；新增 JUnit 测试断言所有触发时机参数无重复；用户存档 c517273.js 已备份并修复。
- 问题2：新特性不显示中文名/描述。根因=游戏运行期间客户端全程锁定 resources.zip，保存时的 lang 写入全部失败。重构：保存只写数据包；lang 由 syncAll 统一负责——SERVER_STARTED 补写（内容无变化时不写文件）+ SERVER_STOPPED 后台线程趁锁释放后补写（最多 30 秒重试）；写入失败清理临时文件。live 存档的 resources.zip 已手工补齐 c597391/c517273 条目。
- 问题3：进世界二次资源加载。根因=syncAll 每次启动都重写 resources.zip 触发资源管理器重载。修复=内容与现包一致时跳过写入（不改变文件时间戳 → 不触发重载）。
- 部署：v0.3.7 jar 已装入整合包 mods（0.3.6 归档），Minecraft 关闭状态部署无锁。
## v0.3.8 —— 手动加载（2026-10-18 21:2x）
- 用户请求：保存后自动加载改成手动加载，加一个「加载」按钮。
- 实现：保存命令只写数据包文件（runSet 把补丁入队 queue，runMoveAdd/runAbilityAdd/runItemAdd 不再触发重载）；新增 /cobbleeditor load 命令（LiveApply.reloadAndApply：重扫数据包→包移启用列表末尾→reloadResources→应用补丁队列→sync 玩家）；主菜单（EditorHubScreen）新增「加载修改（应用保存的编辑）」按钮，点击发送 cobbleeditor load 并关屏。
- 保存提示文案全部改为「点击主菜单『加载修改』后生效」。
- 部署：v0.3.8 jar 已装入 mods（0.3.7 归档），Minecraft 关闭状态部署。