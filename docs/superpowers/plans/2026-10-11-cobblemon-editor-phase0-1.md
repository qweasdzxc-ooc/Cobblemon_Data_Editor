# Cobblemon 数据编辑器 — 阶段 0/1 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 Fabric+Kotlin addon 能构建、能在带 Cobblemon 的开发客户端启动（阶段 0），并做出"改种族值"的最小闭环：GUI 改 → 写 `species_additions` → 重进世界生效（阶段 1）。

**Architecture:** Fabric 1.21.1 多源集（main/client）+ Kotlin；客户端 `Screen` 编辑 → C2S payload → 服务端校验并写入世界 datapack（`datapacks/cobblemoneditor/`）→ 提示"重进世界生效" → Cobblemon 重载并自动同步。

**Tech Stack:** Gradle(wrapper) · Fabric Loom `1.18-SNAPSHOT`(Mojang 官方映射) · Kotlin `2.2.20` · fabric-loader `0.19.5` · fabric-api `0.116.17+1.21.1` · Cobblemon `1.8.1+1.21.1`（Maven `https://maven.impactdev.net/repository/development/`）· Java 21。

**确定性事实（已核实）**：Loom 用 `net.fabricmc.fabric-loom-remap` 插件、`loom.officialMojangMappings()`；`splitEnvironmentSourceSets()`；`Cobblemon` 用 `modCompileOnly com.cobblemon:mod` + `modLocalRuntime com.cobblemon:fabric`。

## 文件结构

```
projects/cobblemon-editor/
├─ settings.gradle                       # 仓库 + rootProject.name='cobblemoneditor'
├─ build.gradle                          # Loom + Kotlin + 依赖
├─ gradle.properties                     # 版本变量
├─ gradlew / gradlew.bat / gradle/wrapper/*   # 从官方示例 clone 得到
├─ src/main/resources/fabric.mod.json
├─ src/main/kotlin/com/xiaoyu/cobblemoneditor/
│   ├─ CobblemonEditor.kt                # ModInitializer（服务端入口）
│   └─ data/SpeciesStatPatch.kt          # 数据模型 + 序列化（纯逻辑，可单测）
│   └─ data/DatapackWriter.kt            # 写 species_additions JSON + 备份
│   └─ net/EditorPayloads.kt             # C2S/S2C payload 定义 + 编解码
│   └─ server/EditorServer.kt            # 命令 + 接收器 + 校验
├─ src/client/kotlin/com/xiaoyu/cobblemoneditor/client/
│   ├─ CobblemonEditorClient.kt          # ClientModInitializer + 键位
│   └─ EditorScreen.kt                   # 种族值编辑界面
├─ src/test/kotlin/com/xiaoyu/cobblemoneditor/
│   ├─ SpeciesStatPatchTest.kt
│   └─ DatapackWriterTest.kt
└─ src/main/resources/assets/cobblemoneditor/  # 图标等（可空）
```

modid: `cobblemoneditor`　group: `com.xiaoyu.cobblemoneditor`

---

## 阶段 0：能构建、能启动

### Task 0.1：引导 Gradle Wrapper

**Files:**
- Create: `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`

- [ ] **Step 1: clone 官方示例作为骨架来源**
```bash
git clone --depth 1 -b 1.21.1 https://github.com/FabricMC/fabric-example-mod.git _example
```
- [ ] **Step 2: 拷贝 wrapper 与 gradlew**
```bash
cp -r _example/gradle . && cp _example/gradlew _example/gradlew.bat .
```
- [ ] **Step 3: 校验**
Run: `./gradlew --version` 　Expected: 打印 Gradle 版本、JVM 21

### Task 0.2：gradle.properties

**Files:** Create `gradle.properties`
```properties
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true

# Fabric
minecraft_version=1.21.1
loader_version=0.19.5
loom_version=1.18-SNAPSHOT
fabric_api_version=0.116.17+1.21.1

# Mod
mod_version=0.1.0
maven_group=com.xiaoyu
archives_base_name=cobblemoneditor

# Cobblemon
cobblemon_version=1.8.1+1.21.1
```
- [ ] Step: 写入文件

### Task 0.3：settings.gradle

**Files:** Create `settings.gradle`
```groovy
pluginManagement {
	repositories {
		maven { name = 'Fabric'; url = 'https://maven.fabricmc.net/' }
		mavenCentral()
		gradlePluginPortal()
	}
}
rootProject.name = 'cobblemoneditor'
```
- [ ] Step: 写入文件

### Task 0.4：build.gradle（Loom + Kotlin + Cobblemon）

**Files:** Create `build.gradle`
```groovy
plugins {
	id 'net.fabricmc.fabric-loom-remap' version "${loom_version}"
	id 'org.jetbrains.kotlin.jvm' version '2.2.20'
}

repositories {
	mavenCentral()
	maven { url = 'https://maven.impactdev.net/repository/development/' } // Cobblemon
}

loom {
	splitEnvironmentSourceSets()
	mods {
		"cobblemoneditor" {
			sourceSet sourceSets.main
			sourceSet sourceSets.client
		}
	}
}

dependencies {
	minecraft "com.mojang:minecraft:${project.minecraft_version}"
	mappings loom.officialMojangMappings()
	modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
	modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
	modImplementation "net.fabricmc:fabric-language-kotlin:1.13.6+kotlin.2.2.20"

	// Cobblemon API：只编译期依赖，绝不打包（玩家自己装 Cobblemon）
	modCompileOnly "com.cobblemon:mod:${project.cobblemon_version}"
	modLocalRuntime "com.cobblemon:fabric:${project.cobblemon_version}"
}

processResources {
	inputs.property "version", project.mod_version
	filesMatching("fabric.mod.json") { expand "version": project.mod_version }
}

tasks.withType(JavaCompile).configureEach { it.options.release = 21 }

kotlin { jvmToolchain(21) }

java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}
```

- [ ] **Step 1: 写入文件**
- [ ] **Step 2: 解析依赖**
Run: `./gradlew dependencies --configuration modCompileClasspath`
Expected: 出现 `com.cobblemon:mod:1.8.1+1.21.1`，无解析错误
（若 `fabric-loom-remap` 插件 ID 报错，回退为 `id 'fabric-loom'`）

### Task 0.5：fabric.mod.json + 主入口

**Files:** Create `src/main/resources/fabric.mod.json`
```json
{
	"schemaVersion": 1,
	"id": "cobblemoneditor",
	"version": "${version}",
	"name": "Cobblemon Data Editor",
	"description": "In-game GUI editor for Cobblemon species stats, abilities and moves.",
	"license": "MIT",
	"environment": "*",
	"entrypoints": {
		"main": ["com.xiaoyu.cobblemoneditor.CobblemonEditor"],
		"client": ["com.xiaoyu.cobblemoneditor.client.CobblemonEditorClient"]
	},
	"depends": {
		"fabricloader": ">=0.19.5",
		"fabric-api": "*",
		"fabric-language-kotlin": "*",
		"minecraft": "1.21.1",
		"cobblemon": ">=1.8.1"
	}
}
```

**Files:** Create `src/main/kotlin/com/xiaoyu/cobblemoneditor/CobblemonEditor.kt`
```kotlin
package com.xiaoyu.cobblemoneditor

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object CobblemonEditor : ModInitializer {
	const val MOD_ID = "cobblemoneditor"
	private val log = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		log.info("[cobblemoneditor] server init")
		// 阶段1：EditorServer.register()
	}
}
```

**Files:** Create `src/client/kotlin/com/xiaoyu/cobblemoneditor/client/CobblemonEditorClient.kt`
```kotlin
package com.xiaoyu.cobblemoneditor.client

import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

object CobblemonEditorClient : ClientModInitializer {
	private val log = LoggerFactory.getLogger("cobblemoneditor-client")
	override fun onInitializeClient() {
		log.info("[cobblemoneditor] client init")
		// 阶段1：键位 -> EditorScreen
	}
}
```

- [ ] **Step 1: 写入三个文件**
- [ ] **Step 2: 构建**
Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`，产出 `build/libs/cobblemoneditor-0.1.0.jar`
- [ ] **Step 3: 提交**
```bash
git add -A && git commit -m "chore: scaffold Fabric+Kotlin addon for Cobblemon (phase 0)"
```

### Task 0.6：启动开发客户端（验收）

- [ ] **Step 1: 启动**
Run: `./gradlew runClient`（首次会下载 MC 客户端 + 资源，较久）
Expected: 游戏启动，主菜单可见
- [ ] **Step 2: 确认 Cobblemon 已加载**
Expected: 模组列表里有 **Cobblemon 1.8.1** 和 **Cobblemon Data Editor**；日志出现 `[cobblemoneditor] client init`
- [ ] **Step 3: 记录并提交任何修正**

---

## 阶段 1：种族值编辑最小闭环

> 目标：进世界后按键打开界面 → 选精灵 → 改 `speed` → 提交 → 文件出现在 `<world>/datapacks/cobblemoneditor/data/cobblemoneditor/species_additions/<id>.json` → 退出重进世界 → 数值生效。

### Task 1.1：数据模型 + 序列化（纯逻辑，TDD）

**Files:** Create `src/main/kotlin/com/xiaoyu/cobblemoneditor/data/SpeciesStatPatch.kt`
```kotlin
package com.xiaoyu.cobblemoneditor.data

import kotlinx.serialization.Serializable

/** 六项种族值；null = 不改该项。 */
@Serializable
data class StatPatch(
	val hp: Int? = null,
	val attack: Int? = null,
	val defence: Int? = null,
	val special_attack: Int? = null,
	val special_defence: Int? = null,
	val speed: Int? = null,
)

@Serializable
data class SpeciesAddition(val target: String, val baseStats: StatPatch? = null)

object SpeciesStatPatch {
	const val MIN = 0
	const val MAX = 255

	/** 校验并生成 species_additions JSON。返回 null 表示校验失败。 */
	fun build(speciesId: String, patch: StatPatch): String? {
		if (!speciesId.matches(Regex("[a-z0-9_]+"))) return null
		val vals = listOfNotNull(patch.hp, patch.attack, patch.defence,
			patch.special_attack, patch.special_defence, patch.speed)
		if (vals.any { it < MIN || it > MAX }) return null
		if (vals.isEmpty()) return null
		return SpeciesAdditionsJson.encodePretty(
			SpeciesAddition(target = "cobblemon:$speciesId", baseStats = patch)
		)
	}
}
```
（`SpeciesAdditionsJson` 用 kotlinx.serialization 的 `Json { prettyPrint = true; encodeDefaults = false }`，放同文件或 `Json.kt`。）

**Files:** Create `src/test/kotlin/com/xiaoyu/cobblemoneditor/SpeciesStatPatchTest.kt`
```kotlin
package com.xiaoyu.cobblemoneditor

import com.xiaoyu.cobblemoneditor.data.SpeciesStatPatch
import com.xiaoyu.cobblemoneditor.data.StatPatch
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpeciesStatPatchTest {
	@Test fun rejectsOutOfRange() {
		assertNull(SpeciesStatPatch.build("pikachu", StatPatch(speed = 999)))
	}
	@Test fun rejectsBadId() {
		assertNull(SpeciesStatPatch.build("Pikachu!", StatPatch(speed = 100)))
	}
	@Test fun rejectsEmpty() {
		assertNull(SpeciesStatPatch.build("pikachu", StatPatch()))
	}
	@Test fun emitsPatch() {
		val json = SpeciesStatPatch.build("pikachu", StatPatch(speed = 120))!!
		assertTrue(json.contains("\"target\": \"cobblemon:pikachu\""))
		assertTrue(json.contains("\"speed\": 120"))
	}
}
```
需在 `build.gradle` 加测试依赖：`testImplementation "org.jetbrains.kotlin:kotlin-test"` 与 `testImplementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3"`，并加 Kotlin serialization 插件 `org.jetbrains.kotlin.plugin.serialization`。

- [ ] Step 1: 写失败测试
- [ ] Step 2: 运行 `./gradlew test --tests "*SpeciesStatPatchTest*"` → 编译失败/未通过
- [ ] Step 3: 实现 `SpeciesStatPatch` + 序列化插件与依赖
- [ ] Step 4: 运行 → 通过
- [ ] Step 5: 提交 `feat: species stat patch model with validation`

### Task 1.2：datapack 写入器 + 备份（TDD）

**Files:** Create `src/main/kotlin/com/xiaoyu/cobblemoneditor/data/DatapackWriter.kt`
- 职责：给定 world 的 `datapacks/` 根目录、namespace、文件相对路径、内容 → 先备份旧文件（`.bak-<epochMillis>`），再写新文件；返回结果枚举。
```kotlin
package com.xiaoyu.cobblemoneditor.data

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object DatapackWriter {
	sealed interface Result {
		data class Written(val path: Path) : Result
		data class Failed(val message: String) : Result
	}

	fun writeSpeciesAddition(packRoot: Path, namespace: String, speciesId: String, json: String): Result {
		return try {
			val dir = packRoot.resolve("data/$namespace/species_additions")
			Files.createDirectories(dir)
			val file = dir.resolve("$speciesId.json")
			if (Files.exists(file)) {
				Files.copy(file, dir.resolve("$speciesId.json.bak-${System.currentTimeMillis()}"),
					StandardCopyOption.REPLACE_EXISTING)
			}
			Files.writeString(file, json)
			Result.Written(file)
		} catch (e: Exception) {
			Result.Failed(e.message ?: "write failed")
		}
	}
}
```
**Files:** Create `src/test/kotlin/com/xiaoyu/cobblemoneditor/DatapackWriterTest.kt`
```kotlin
class DatapackWriterTest {
	@Test fun writesFileAndBacksup() {
		val tmp = Files.createTempDirectory("dpe")
		val ok = DatapackWriter.writeSpeciesAddition(tmp, "cobblemoneditor", "pikachu", "{}")
		assertTrue(ok is DatapackWriter.Result.Written)
		assertTrue(Files.exists(tmp.resolve("data/cobblemoneditor/species_additions/pikachu.json")))
		DatapackWriter.writeSpeciesAddition(tmp, "cobblemoneditor", "pikachu", "{\"a\":1}")
		val baks = Files.list(tmp.resolve("data/cobblemoneditor/species_additions")).filter { it.fileName.toString().contains(".bak-") }.count()
		assertTrue(baks >= 1)
	}
}
```
- [ ] Step1 写失败测试 → Step2 运行 → Step3 实现 → Step4 通过 → Step5 提交

### Task 1.3：网络 payload

**Files:** Create `src/main/kotlin/com/xiaoyu/cobblemoneditor/net/EditorPayloads.kt`
- `EditSpeciesStatsPayload(speciesId: String, patch: StatPatch)` 实现 `CustomPayload`，含 `ID` + `StreamCodec`；`ResultPayload(ok: Boolean, message: String)`。
- 注册：`PayloadTypeRegistry.playC2S().register(...)` / `playS2C().register(...)`（在 main init）。

- [ ] Step1 定义 payload + codec
- [ ] Step2 在 `CobblemonEditor.onInitialize()` 注册
- [ ] Step3 构建通过 → 提交

### Task 1.4：服务端接收 + 命令 + 校验

**Files:** Create `src/main/kotlin/com/xiaoyu/cobblemoneditor/server/EditorServer.kt`
- `CommandRegistrationCallback`：`/cobbleeditor`（仅 OP）→ 给发起玩家发 S2C「打开编辑器」payload。
- C2S 接收器：`ServerPlayNetworking.registerGlobalReceiver(EditSpeciesStatsPayload.ID) { p, ctx -> ... }`
  - 校验：`ctx.player().hasPermissionLevel(2)`；`SpeciesStatPatch.build(...)` 非 null
  - 定位 world datapack 根：`ctx.player().server.getSavePath(WorldSavePath.DATAPACKS).resolve("cobblemoneditor")`，并确保存在 `pack.mcmeta`
  - 写入 → 回 `ResultPayload(true, "已保存；退出并重进世界后生效")`
- 首次写入需创建 `<world>/datapacks/cobblemoneditor/pack.mcmeta`：
```json
{ "pack": { "pack_format": 48, "description": "Cobblemon Data Editor" } }
```
（`pack_format` 对 1.21.1 = 48；如不符以游戏报错提示为准调整。）

- [ ] Step1 实现命令 + 接收器 + pack.mcmeta 生成
- [ ] Step2 构建通过 → 提交

### Task 1.5：客户端 GUI

**Files:** Create `src/client/kotlin/com/xiaoyu/cobblemoneditor/client/EditorScreen.kt`
- `Screen` 子类：文本输入框输入精灵 id（如 `pikachu`）+ 6 个数字输入（六项种族值，留空=不改）+ 「保存」按钮 → 发送 `EditSpeciesStatsPayload`。
- 位置：`src/client/.../EditorScreen.kt`。
- 键位：在 `CobblemonEditorClient` 用 `KeyBindingHelper` 注册（默认 `K`）+ `ClientTickEvents.END_CLIENT_TICK` 打开；或仅由服务端命令触发打开（更简单，先做命令）。
- 收到 `ResultPayload` → 用 `client.player.displayClientMessage(...)` 显示结果。

- [ ] Step1 写 Screen
- [ ] Step2 接键位/命令打开
- [ ] Step3 构建 → 提交

### Task 1.6：端到端手测（验收）

- [ ] Step1 `./gradlew runClient`，新建世界，装 Cobblemon
- [ ] Step2 `/cobbleeditor` → 打开界面 → 输入 `pikachu`，speed=120 → 保存
- [ ] Step3 确认文件 `run/saves/<world>/datapacks/cobblemoneditor/data/cobblemoneditor/species_additions/pikachu.json` 内容正确
- [ ] Step4 退出世界再进入 → 用 Cobblemon 指令/查看确认速度变为 120
- [ ] Step5 提交并更新本计划勾选状态

---

## Self-Review 记录
- **Spec 覆盖**：阶段0=骨架/构建/启动；阶段1=种族值闭环（对应 spec §4/§5/§9 阶段0-1）。特性(阶段3)、招式+积木(阶段4) 后续各自单独出计划 ✅
- **占位符**：无 TODO/TBD；每个代码步骤给了实际代码 ✅
- **类型一致**：`StatPatch`/`SpeciesAddition`/`SpeciesStatPatch.build`/`DatapackWriter.Result` 前后一致 ✅
- **已知需现场确认**：`fabric-loom-remap` 插件 ID 与 Gradle wrapper 版本（Task 0.1/0.4 有回退方案）；`pack_format=48` 以游戏报错为准。
