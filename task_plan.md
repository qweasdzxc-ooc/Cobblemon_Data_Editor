# Cobblemon Data Editor 完善计划（v0.2.10 → v0.3.x）

> 用户需求（2026-10-18）：补齐设计文档缺口 + 扩展积木库 + 修复游玩中遇到的问题。
> 设计文档：docs/superpowers/specs/2026-10-11-cobblemon-data-editor-design.md
> 调研报告：research/cobblemon-api-findings.md（关键事实已核实）

## 目标

| 工作流 | 内容 | 状态 |
|---|---|---|
| A | 补齐设计文档缺口：新建精灵、招式学习表编辑、冲突检测、备份保留N份 | in_progress |
| B | 扩展招式/特性效果积木库（并行，队友 brick-builder） | in_progress |
| C | 修复用户实际游玩中遇到的问题 | pending（等用户问题清单） |

## 阶段

### Phase 1 调研确认（✅ 完成，结论见 findings.md「Phase 1 验证结果」）
- [x] 1.1 moves/*.js = 裸 Showdown 对象；name 必须 ASCII；num/name 自动补；必读字段 id/num/type/category/basePower/target/pp/priority
- [x] 1.2 species_additions 的 moves = 整体替换 Learnset，格式 "1:tackle"/"egg:x"/"tm:x"…；合并需服务端读 Learnset getter 重建
- [x] 1.3 最小物种字段集确认（implemented:true 必设；name ASCII；translationKey 需资源包 → v1 显示 ASCII 名）

### Phase 2 新建精灵（in_progress）
- [ ] 2.1 data 层：`SpeciesJson.kt` 生成最小合法物种 JSON（name/dexNo/types/baseStats/abilities/moves/基本字段）
- [ ] 2.2 server：`/cobbleeditor species add <id> <spec>`（复用 spec 语法 + 必填 dexNo 等）
- [ ] 2.3 client：精灵编辑器加"新建精灵"模式
- [ ] 2.4 手测：新建 → 重进世界 → 图鉴可见

### Phase 3 招式学习表编辑
- [ ] 3.1 server：learnset spec（如 `moves=1:tackle,5:growl,egg:xxx,tm:yyy`）→ species_additions 补丁
- [ ] 3.2 client：精灵编辑器加学习表区（招式搜索 + 等级）
- [ ] 3.3 手测：给精灵加招式 → 重进 → 升级学会

### Phase 4 冲突检测 + 备份保留
- [ ] 4.1 DatapackWriter：备份只保留最近 N=10 份（自动清理旧 .bak）
- [ ] 4.2 冲突检测：招式/特性 id 与 Cobblemon 已有注册表冲突时警告；species_additions 同 target 同 namespace 覆盖警告
- [ ] 4.3 手测

### Phase 5 积木库扩展（队友并行）
- [ ] 5.1 招式积木：新增 ≥8 个（见积木任务板）
- [ ] 5.2 特性积木：新增 ≥8 个
- [ ] 5.3 构建通过（Lead 统一构建验证）

### Phase 6 构建 + 部署
- [ ] 6.1 `gradlew build` 全绿
- [ ] 6.2 版本号 bump（0.3.0）+ git 提交
- [ ] 6.3 新 jar 复制到整合包 mods 目录（替换旧 [自制]CobblemonDataEditor-*.jar）

### Phase 7 游玩问题修复（等用户清单）
- [ ] 7.1 用户提供问题清单
- [ ] 7.2 逐项修复 + 回归

## Errors Encountered

| Error | Attempt | Resolution |
|---|---|---|

## 决策记录
- 2026-10-18：工作流 A/B/C 获批；B 派给队友并行；构建验证统一由 Lead 做（避免 gradle 并发冲突）
- 2026-10-18：新建物种放在本 mod 自有 namespace（cobblemoneditor），与现有 species_additions 一致
