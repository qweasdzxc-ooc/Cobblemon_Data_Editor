# 设计文档：Cobblemon 游戏内数据编辑器（暂定名 Cobblemon Data Editor）

- **日期**：2026-10-11
- **状态**：待用户评审
- **目标平台**：Minecraft **1.21.1** · **Fabric** · Java 21
- **依赖**：Cobblemon **1.8.1+1.21.1**

---

## 1. 目的（Purpose）

做一个**游戏内图形界面（GUI）编辑器**，让**不会编程的用户**可以：

1. **新建**和**修改已有**的宝可梦：**种族值（base stats）**、**特性（abilities）**、**技能（moves）**；
2. 用**预置的"效果积木库"**拼装技能效果（不需要写代码）；
3. 改动**持久化**成数据文件，**重载世界后生效**；
4. 单机为主；服务器端权威、带校验，保证不把存档搞坏。

## 2. 目标 / 非目标

**目标（v1）**
- GUI 增改 种族值 / 特性 / 技能（含效果积木组合）
- 新增精灵、编辑已有精灵
- 写入 datapack 文件并持久化
- 应用方式：**保存 → 退出并重进世界生效**（稳）
- 服务端权威 + 严格校验 + 自动备份

**非目标（v1 不做，留待后续）**
- 「零代码凭空造任意新机制」——只有预置积木库里有的效果才能拼（缺的由我加"积木"）
- 不重启/不重进世界就秒生效（**实验性**，见 §6，留到 v2）
- 多人服务器的细粒度权限、Web 端管理
- Cobblemon 其它数据（刷新点、道具、对话等）编辑

## 3. 已核实的技术事实（来自调研报告）

报告：`research/cobblemon-api-findings.md`（有代码证据）

- **Maven**：`https://maven.impactdev.net/repository/development/`
  - `com.cobblemon:mod`（通用/API，编译用）、`com.cobblemon:fabric`（开发运行用）
  - 版本 `1.8.1+1.21.1`；loader 依赖 fabricloader ≥0.17.2、fabric-api ≥0.116.6+1.21.1、MC 1.21.1、Java 21
  - Cobblemon 自身是 **Kotlin 2.2.20** 写的 → 我们的 addon 也用 Kotlin
- **种族值 = JSON**：`data/cobblemon/species/generationN/<name>.json`
  - 覆写已有：同 namespace + 同文件名；新增：换 namespace
  - **改已有精灵的官方安全姿势**：`data/<ns>/species_additions/*.json`（`{"target":"cobblemon:pikachu", ...}` 补丁）
- **招式 / 特性 = 不是 JSON**：来自内置的 **Pokémon Showdown 引擎（JavaScript, GraalVM）**
  - 招式：`Moves.register()` 是 **private** → 只能"写 JS 文件 + 重载"
  - 特性：`Abilities.register()` 是 **public** → 可运行时注册
- **⚠ 关键限制**：`PokemonSpecies / Moves / Abilities` 均登记为 **`reloadable = false`**
  - 源码注释：*"species are not safe to reload"*
  - **`/reload` 改不动它们**；只有**进入世界/重启**才重新加载
  - 只有招式**动画**层（`ActionEffects`）能 `/reload`
- **Fabric 1.21.1 GUI/网络**：`KeyBindingHelper`、`ClientTickEvents`、`ClientPlayNetworking` / `ServerPlayNetworking` / `PayloadTypeRegistry`、`CommandRegistrationCallback` 均已核实

## 4. 架构（分层）

```
[ 客户端 GUI（Screen/Widget）]
        │  C2S：编辑后的数据模型（不是文件路径！）
        ▼
[ 服务端校验层 ]  ← 权限/范围/命名唯一性/冲突检测
        │
        ▼
[ 持久化写入层 ]  写入世界 datapack：<world>/datapacks/<addon_ns>/
        ├─ 新增精灵      → data/<ns>/species/<id>.json
        ├─ 修改已有精灵  → data/<ns>/species_additions/<id>.json   （补丁，保留原文件）
        ├─ 特性          → data/<ns>/abilities/<id>.js  （Showdown JS）+ 可选 Abilities.register
        └─ 招式          → data/<ns>/moves/<id>.js      （Showdown JS）
        │
        ▼
[ 应用层 ]  v1：提示「退出重进世界生效」；v2：可选受保护的强制重载
        │
        ▼
[ 同步层 ]  Cobblemon 在玩家加入时自动 sync 注册表（*RegistrySyncPacket）
```

### 4.1 组件
1. **编辑器数据模型（client）**：镜像物种 JSON 的 Kotlin 模型（baseStats/types/abilities/learnset/species_additions）+ 招式模型（Showdown 字段）
2. **GUI（client）**：
   - 精灵列表 → 精灵编辑页（种族值/属性/特性/招式表）
   - 招式编辑页（威力/命中/属性/PP/优先级 + **效果积木拼装**）
   - 特性编辑页
3. **网络层**：C2S 提交编辑；S2C 返回结果 / 服务端主动打开编辑器
4. **校验层（server）**：数值范围、名称正则、唯一性、冲突
5. **写入层（server）**：把校验过的模型序列化成 JSON/JS 写进世界 datapack（**绝不信任客户端给的文件路径或原始 JSON**）
6. **效果积木库（server）**：一套**版本锁定**的 Showdown JS 片段（如 `secondary.burn 10%`、`drain 50%`、`multihit 2-5`、`recoil`、`stat-change`、`priority`…）+ 组合器 + 生成前校验
7. **备份层**：每次写入前对整个 addon datapack 做快照，保留最近 N 份

## 5. 典型数据流（例：改皮卡丘种族值）

1. 打开 GUI → 选 `pikachu`
2. 改 `speed` 90 → 120
3. 客户端发模型到服务端
4. 服务端校验（0–255 整数等）→ 写 `species_additions/pikachu.json`
5. GUI 提示：**「已保存，退出并重进世界后生效」**
6. 重进世界 → Cobblemon 加载补丁 → 自动同步给客户端 → 生效

## 6. 应用（生效）方式

- **v1（默认，稳）**：保存后提示玩家**退出并重进世界**。零风险。
- **v2（实验性，可选开关）**：加「一键强制重载」按钮，mod 自行调用各注册表 `reload` 并重新 sync。
  - 保护：**战斗进行中禁止**；重载失败自动回滚；明确标注"实验性"。

## 7. 错误处理与安全

- 输入校验：种族值 0–255 整数；名称 `[a-z0-9_]`；ID 唯一
- 写入前备份；写入失败保留旧文件并报错
- 冲突提示：检测是否有其它数据包改了同一个 identifier（last pack wins）
- 权限：仅 OP / 单人可编辑（v1 单机为主）
- 不信任客户端路径 / 原始 JSON

## 8. 测试策略

- **阶段循环测试**：开发客户端里跑起来 → 编辑 → 重进世界 → 游戏内确认数值变化
- **单元测试**：模型 → JSON 序列化的 golden-file 测试；JS 积木组合器输出测试
- **每阶段手测清单**

## 9. 分期计划

| 阶段 | 内容 | 验收 |
|---|---|---|
| **0** | 空壳 Fabric+Kotlin addon，接上 Cobblemon，能启动进游戏 | 能进游戏、日志无错 |
| **1** | GUI 骨架 + **种族值编辑**（写 `species_additions`）→ 重进世界生效 | 改皮卡丘速度可见 |
| **2** | 新建精灵 + 精灵列表管理 | 造一只新精灵并存档 |
| **3** | 特性 增改（`register` + Showdown JS 行为） | 新特性可生效 |
| **4** | 招式 + **效果积木库**（JS 组合器）| 拼一个新招式并生效 |
| **5** | 打磨：备份/校验/帮助/可选一键重载 | 可用性验收 |

## 10. 风险与对策

| 风险 | 对策 |
|---|---|
| `/reload` 不生效（reloadable=false） | v1 用"重进世界"；v2 受保护强制重载 |
| 招式效果=Showdown JS，无运行时注册接口 | 预置**版本锁定**积木库；生成前校验；不做任意 JS |
| 覆写+同步+战斗中缓存 Species 引用 → 可能崩 | 优先 `species_additions` 补丁；改基础数据时要求无战斗 |
| Kotlin `internal`/`object` API 摩擦 | 用 Kotlin 写 addon；必要时反射 |
| GraalVM/Showdown 版本漂移 | 积木库与内置 Showdown 版本绑定，升级时同步 |

## 11. 待定问题（实现前需验证）

- Fabric Loom 精确版本 + 1.21.1 的 Yarn mappings build 号
- datapack `moves/*.js` / `abilities/*.js` 的确切期望格式（裸 Showdown 对象 vs 包装）
- `PokemonSpecies.reload` 在活跃服务器上到底安不安全
- 独立 module 能否直接调用 Kotlin `internal` 的 `SpeciesAdditions`（否则用反射）

## 12. 项目结构（计划）

```
projects/cobblemon-editor/
├─ docs/superpowers/specs/2026-10-11-cobblemon-data-editor-design.md
├─ research/                      # 调研产物（temp/ 不纳入 git）
├─ .gitignore
├─ build.gradle / settings.gradle / gradle.properties
├─ gradle/wrapper/
└─ src/main/kotlin/...            # addon 源码
    src/main/resources/fabric.mod.json
```

---

_本文档由头脑风暴流程产出；经用户评审后进入实现计划（writing-plans）。_
