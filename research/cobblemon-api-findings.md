# Cobblemon 1.8.x Addon API — Research Findings

- **Target:** Minecraft 1.21.1, Fabric; Cobblemon **1.8.1** (Modrinth `cobblemon`, project id `MdwFAVRL`).
- **Product goal (context):** in-game GUI editor so a non-programmer can add/edit Cobblemon species base stats, abilities and moves (composing move effects from a predefined library), persisted as JSON and applied via `/reload`.
- **Method:** downloaded `Cobblemon-fabric-1.8.1+1.21.1.jar` from Modrinth, inspected with `jar tf`, extracted JSON/JS, `javap`'d key classes, and cross-checked Kotlin sources from the GitLab repo (`https://gitlab.com/cable-mc/cobblemon`, default branch `main`, project id `31496946`). Fabric API classes verified from `fabric-api-0.116.17+1.21.1.jar`.
- **Evidence is cited inline as `path` / class / source-file + quoted line.** Anything not proven is marked **UNKNOWN**.

Local artifacts (not part of any build):
- Jar: `projects/cobblemon-editor/research/temp/cobblemon-1.8.1.jar` (137 MB; jar entry count 41,482).
- `temp/jar-list.txt` (full entry list), `temp/versions.json`, `temp/x/` (extracted data), `temp/c/` (extracted classes), `temp/fabric-api.jar`.

---

## 1. Maven repository, coordinates, Gradle snippet

### 1.1 Repository (CONFIRMED)
- URL: **`https://maven.impactdev.net/repository/development/`** — a Sonatype Nexus (`OSS 3.69.0-02`), repo name "development".
  - Evidence: Nexus search API `GET https://maven.impactdev.net/service/rest/v1/search?repository=development&group=com.cobblemon` returned items with `group=com.cobblemon`.
  - NOTE: `maven.cobblemon.com` / `maven.cable-mc.com` do **not** resolve (DNS FAIL). Use `maven.impactdev.net`.

### 1.2 Coordinates (CONFIRMED)
`com/cobblemon/fabric/maven-metadata.xml`:
```xml
<groupId>com.cobblemon</groupId><artifactId>fabric</artifactId>
<release>1.8.2+1.21.1</release>
<latest>1.8.3+1.21.1-SNAPSHOT</latest>
<versions> ... 1.8.0+1.21.1, 1.8.1+1.21.1, 1.8.2+1.21.1, 1.8.3+1.21.1-SNAPSHOT ... </versions>
```
Artifacts published (Nexus search): `com.cobblemon:mod`, `com.cobblemon:fabric`, `com.cobblemon:neoforge`, `com.cobblemon:forge`.
- `com.cobblemon:mod` = the **common / loader-agnostic** code (its POM has no dependencies). ~115 MB jar.
- `com.cobblemon:fabric` = Fabric layer, POM depends on `net.fabricmc:fabric-language-kotlin:1.13.6+kotlin.2.2.20`, `org.graalvm.{js,sdk,regex,truffle-api}:22.3.0`, `com.ibm.icu:icu4j:71.1`, `org.mongodb:mongodb-driver-*:4.10.2`. ~148 MB jar.
- Both jars contain the full data/resources (`data/`, `assets/`, `datapacks/`).
- The Modrinth `1.8.1+1.21.1` jar corresponds to **gitCommit `0bc5bb5caaca5622c9f1856cadd866ad85d21287`** (from the POMs).

### 1.3 Loader/runtime deps of Cobblemon (CONFIRMED — from jar `fabric.mod.json`)
```json
"depends": {
  "fabricloader": ">=0.17.2",
  "fabric-api": ">=0.116.6+1.21.1",
  "minecraft": "1.21.1",
  "java": "21"
}
```
It jars-in-jar (JiJ) bundles fabric-language-kotlin `1.13.6+kotlin.2.2.20`, GraalVM `22.3.0`, icu4j `71.1`, mongodb `4.10.2`.
- Fabric API latest for 1.21.1 (from `maven.fabricmc.net` metadata): **`0.116.17+1.21.1`** (37 versions `*+1.21.1`).

### 1.4 Gradle snippet for a Fabric 1.21.1 (Loom) addon
```groovy
// build.gradle
plugins {
    id 'fabric-loom' version '1.7-SNAPSHOT'          // pick a Loom release that supports 1.21.1 — see UNKNOWN
    id 'org.jetbrains.kotlin.jvm' version '2.2.20'   // match Cobblemon's Kotlin 2.2.20
    id 'org.jetbrains.kotlin.plugin.serialization' version '2.2.20'
}

repositories {
    mavenCentral()
    maven { url = 'https://maven.fabricmc.net/' }
    maven { url = 'https://maven.impactdev.net/repository/development/' } // Cobblemon
}

dependencies {
    minecraft 'com.mojang:minecraft:1.21.1'
    mappings 'net.fabricmc:yarn:1.21.1+build.3:v2'   // exact build = UNKNOWN, verify
    modImplementation 'net.fabricmc:fabric-loader:0.17.2'
    modImplementation 'net.fabricmc.fabric-api:fabric-api:0.116.17+1.21.1'
    modImplementation 'net.fabricmc:fabric-language-kotlin:1.13.6+kotlin.2.2.20'

    // Cobblemon API (common). Compile-only; never bundle — the player installs Cobblemon.
    modCompileOnly 'com.cobblemon:mod:1.8.1+1.21.1'
    // For a dev run, add the Fabric artifact so the game actually launches:
    modLocalRuntime 'com.cobblemon:fabric:1.8.1+1.21.1'
}
```
Notes:
- Use `modCompileOnly` (or `modImplementation` if you intentionally rely on Cobblemon's runtime Kotlin classes) — do **not** ship/`include` Cobblemon.
- Because Cobblemon is Kotlin, writing the addon in Kotlin is strongly advised (accessing `object` members, lambdas, `KFunction` builders from Java is painful).
- Loom version + Yarn build number are **UNKNOWN** (not verified against a 1.21.1 template); confirm before building.

---

## 2. Data JSON: locations, namespaces, schema

### 2.1 Species — JSON (CONFIRMED)
- Path inside jar: **`data/cobblemon/species/generationN/<name>.json`**, N=1..9 — **1025 files** (e.g. `data/cobblemon/species/generation1/pikachu.json`, `.../generation1/charizard.json`).
- Registry class: **`com.cobblemon.mod.common.api.pokemon.PokemonSpecies`** — `object PokemonSpecies : JsonDataRegistry<Species>`, `id = cobblemonResource("species")`, `type = PackType.SERVER_DATA`, `resourcePath = "species"`.
  - `JsonDataRegistry.reload(manager)` reads `manager.listResources("species") { it.path.endsWith(".json") }` and keys each entry by `ResourceLocation(namespace, filenameWithoutExtension)` (source: `common/.../api/data/JsonDataRegistry.kt`).
- JSON schema shipped in the jar: **`assets/cobblemon/species.schema.json`** (JSON Schema draft 2020-12). It documents `baseScale, baseStats{attack,defence,hp,specialAttack,specialDefence,speed}, battleTheme, catchRate, evolutions[], hitbox{fixed,width,height}, maleRatio, name, nationalPokedexNumber, primaryType, secondaryType`.

### 2.2 Species JSON fields (CONFIRMED from `pikachu.json` + `Species.kt`)
Top-level keys observed in real files and in `com.cobblemon.mod.common.pokemon.Species`:
```
name, nationalPokedexNumber, primaryType, secondaryType,
baseStats { hp, attack, defence, special_attack, special_defence, speed },   // JSON uses snake_case keys
evYield   { hp, attack, defence, special_attack, special_defence, speed },
abilities [ "static", "h:lightningrod" ],      // "h:" prefix = hidden ability (also "a:"/"n:" markers exist; see AbilityPool/HiddenAbility)
aspects, baseExperienceYield, baseFriendship, baseScale, catchRate,
drops { amount, entries[ {item, percentage} ] },
eggCycles, eggGroups, experienceGroup,
evolutions [ { id, variant, result, requirements[], learnableMoves[], consumeHeldItem, requiredContext } ],
forms [ { name, aspects, primaryType, secondaryType, baseStats, height, weight, maleRatio, labels, pokedex, evolutions, battleOnly, ... } ],
features, height, weight, hitbox { fixed, width, height }, implemented, labels, maleRatio,
moves [ "<method>:<movename>", ... ],
pokedex [ "cobblemon.species.pikachu.desc" ], preEvolution, shoulderMountable,
behaviour { combat{...}, herd{...}, moving{walk,swim}, resting{...} },
battleTheme, ai[], baseAI[], riding{...}, lightningData{...}, signatureMoves[], defaultWildMovesetBuilder
```
- **Learnset (`moves`) format:** `"<method>:<movename>"`. Methods seen: numeric level `"1:thundershock"`, `"egg:*"`, `"tm:*"`, `"tutor:*"`, `"legacy:*"`, `"special:*"`. Move names are lowercased Showdown IDs and are resolved via `Moves.getByName`.
- `PokemonSpecies` runtime fields (from `Species.kt`/`javap`): `baseStats: HashMap<Stat,Int>`, `evYield`, `abilities: AbilityPool`, `moves: Learnset`, `forms: List<FormData>`, `primaryType/secondaryType: ElementalType`, etc. Mutable + has setters for many fields (used by SpeciesAdditions, §3.2).

### 2.3 Moves — **NOT JSON** (CONFIRMED)
There is **no** `data/cobblemon/moves/*.json`. Move *definitions* come from the bundled **Pokémon Showdown** engine:
- `data/cobblemon/showdown.zip` (11.6 MB; 931 entries) contains the full Showdown server, incl. `data/moves.js`, `data/abilities.js`, and the Cobblemon mod `data/mods/cobblemon/{moves,abilities,scripts,conditions,pokedex,rulesets}.js`. (`data/mods/cobblemon/moves.js` is effectively empty: `const Moves = {};`.)
- Source `common/.../api/moves/Moves.kt`: `reload()` resets Showdown registry data, streams datapack `*.js` to Showdown, then reads back `getRegistryData("move")` and builds **`MoveTemplate`** objects.
- `com.cobblemon.mod.common.api.moves.MoveTemplate` ctor: `(name, num, elementalType, damageCategory, power, target: MoveTarget, accuracy, pp, priority, critRatio, effectChances: Double[], weight)`.
  - `Move` (instance) = `template + currentPp + raisedPpStages`. `MoveSet` = up to 4 `Move`, with `add/setMove/copyFrom/swapMove/heal/saveToJSON`.
- Related datapack JSON that **is** loaded from data:
  - `data/cobblemon/move_weights.json` — AI move-selection weights, content: `{"focusblast":50,"terablast":0}`.
  - `data/cobblemon/action_effects/moves/<name>.json` + `.../moves/generic/*.json` — **138** client-side **animation timelines** (registry `ActionEffects`, reloadable). Example `protect.json`: `{"timeline":[{"type":"add_holds","holds":["effects"]},{"type":"animation","animation":["protect","status"]}, ...]}`
  - `data/cobblemon/moveset_builders/{wild,alpha}.json`.

### 2.4 Abilities — **NOT JSON** (CONFIRMED)
- From Showdown `data/abilities.js`. Source `common/.../api/abilities/Abilities.kt` reads `getRegistryData("ability")` and registers **only** `AbilityTemplate(id)` per id (name-only template).
- `com.cobblemon.mod.common.api.abilities.{Ability, AbilityTemplate, AbilityPool, Abilities, CommonAbility, HiddenAbility...}`.
  - `AbilityTemplate(name, builder: Function3<AbilityTemplate,Boolean,Priority,Ability>, displayName, description)`; `Ability` holds `template, forced, index, priority`.
  - Species reference abilities by name string; `"h:<name>"` = hidden.

### 2.5 Species additions (patch existing species) — JSON (CONFIRMED)
- `com.cobblemon.mod.common.pokemon.SpeciesAdditions` : `JsonDataRegistry<AdditionParameter>`, `id = cobblemonResource("species_additions")`, resourcePath `species_additions`.
- Datapack path: **`data/<ns>/species_additions/*.json`**.
- Format (`AdditionParameterAdapter`): `{ "target": "<species id>", "<Species property name>": <value>, ... }`; each property is set via Kotlin reflection on the `Species` object (only mutable `KotlinProperty`s). `forms` and `evolutions` **append** rather than replace.
- This is the *official supported way* to modify existing Cobblemon species (base stats, types, abilities, moves, etc.) without shipping a full replacement file.

---

## 3. Datapack override/extend semantics and `/reload`

### 3.1 Override / extend (CONFIRMED)
`JsonDataRegistry.reload(manager)` (source `common/.../api/data/JsonDataRegistry.kt`):
```kotlin
manager.listResources(resourcePath) { it.path.endsWith(".json") }.forEach { (identifier, resource) ->
    val resolvedIdentifier = ResourceLocation.fromNamespaceAndPath(identifier.namespace, File(identifier.path).nameWithoutExtension)
    data[resolvedIdentifier] = parse(reader, resolvedIdentifier)
}
reload(data)
```
- The map is keyed by **`namespace` + filename-without-extension**. `listResources` yields one resource per identifier with the normal Minecraft resource-pack precedence (world datapack > mod resources). Therefore:
  - **Override existing species** → must use the **same namespace and filename**, e.g. `data/cobblemon/species/generation1/pikachu.json` (folder name = namespace).
  - **Add a new species** → use a different namespace, e.g. `data/mypack/species/newmon.json` → identifier `mypack:newmon` (the mod's `Species.showdownId()` prefixes non-cobblemon namespaces, e.g. `mypacknewmon`).
  - **Patch a Cobblemon species** → prefer `data/<ns>/species_additions/*.json` with `{"target":"cobblemon:pikachu", ...}`.
- Species namespace/path mapping confirmed by `PokemonSpecies.reload(data)`: `species.resourceIdentifier = identifier; speciesByIdentifier[identifier]=species; speciesByDex[namespace][dexNo]=species`.

### 3.2 `/reload` will **NOT** apply species/move/ability changes (CONFIRMED — critical)
Source `common/.../data/CobblemonDataProvider.kt`:
- Registries are registered with a `reloadable` flag; **`PokemonSpecies`, `SpeciesAdditions`, `Moves`, `Abilities` are registered `reloadable = false`** (also `SpeciesFeatures`, `Dexes`, `TechnicalMachines`, `BagItems`, `PokeBalls`, ...).
- Log comment in code: *"Note: Cobblemon data registries are only loaded once per server instance as Pokémon species are not safe to reload."*
- The reload listener:
```kotlin
val reloadAllowed = server()?.isReady != true                     // true only during world create/load
registries.filter { it.type == SERVER_DATA && (reloadAllowed || it in reloadableRegistries) }
          .forEach { it.reload(manager) }
```
  So on a **running** server, only `reloadableRegistries` reload.
- **Reloadable set (reloadable = true):** `CobblemonScripts, ActionEffects, SpawnDetailPresets, CobblemonSpawnRules, CobblemonMechanics, Dialogues, NaturalMaterials, Fossils, CobblemonCosmeticItems, CobblemonCallbacks, CobblemonUnlockableWallpapers, StarterDataLoader, HabitatPools, CobblemonRideSettings`.
- **Consequence for the product:** editing species/abilities/moves JSON (or JS) takes effect on **server restart / world load**, *not* on `/reload`. The `/reload`-only assumption must be revised (see §6). `ActionEffects` (move *visual* timelines) DO reload on `/reload`.
- Additional caveat **UNKNOWN**: whether a *newly added* datapack folder under `<world>/datapacks/` is discovered by `/reload` (assumed yes in modern MC; unverified here) — irrelevant for species since a restart is needed anyway.

---

## 4. Java/Kotlin API for registering/mutating species, abilities, moves + battle effects

### 4.1 Registries and their mutability (CONFIRMED via `javap` + sources)
| Concept | Class (package) | Public runtime mutation? |
|---|---|---|
| Species | `com.cobblemon.mod.common.api.pokemon.PokemonSpecies` | **No `register`**. Reads `getByName/getByIdentifier/getByPokedexNumber/species/implemented/count/random/getNamespaces`, `sync(player)`, `reload(ResourceManager)`. |
| Species patch | `com.cobblemon.mod.common.pokemon.SpeciesAdditions` | Kotlin `internal object` → not callable from another module's Kotlin; bytecode-public so reachable from Java/reflection (unsupported). |
| Abilities | `com.cobblemon.mod.common.api.abilities.Abilities` | **Yes** — `@JvmStatic fun register(AbilityTemplate): AbilityTemplate` is public; also `get/all/count/DUMMY`. |
| Moves | `com.cobblemon.mod.common.api.moves.Moves` | **No** — `register()` is private; only `reload(ResourceManager)` and internal `receiveSyncPacket`. Custom moves must go through the Showdown JS path + reload. |
| Moves (client sync) | `...net.messages.client.data.MovesRegistrySyncPacket` / `SpeciesRegistrySyncPacket` / `AbilityRegistrySyncPacket` | Server→client sync of registries. |

- `AbilityTemplate` builder is a Kotlin `Function3<AbilityTemplate, Boolean /*hidden*/, Priority, Ability>`; creating one from Java requires implementing a Kotlin function type — practical only in Kotlin.
- Server-side data sync: `CobblemonDataProvider.sync(player)` sends every registry's sync packet when a player joins (skipped only for in-memory connection). So the server is authoritative for species/moves/abilities.

### 4.2 The battle-effect system (CONFIRMED)
- **Move effects are derived from the bundled Pokémon Showdown engine (GraalVM JS).**
  - `com.cobblemon.mod.common.battles.runner.ShowdownService` (impls `...runner.graal.GraalShowdownService`, `...runner.socket.SocketShowdownService`) exposes:
    `startBattle(...)`, `send(...)`, `getRegistryData(String): JsonArray`, `sendRegistryData(Map<String,String>, String)`, `sendRegistryEntry(String,String)`, `resetRegistryData(String)`, `resetAllRegistries()`, `indicateSpeciesInitialized()`.
  - JS bridge `cobbled-exports/cobbled-index.js` (inside `showdown.zip`) exposes:
    `receiveMoveData(moveId, move) -> Cobblemon.moveRegistry.register(move, toID(moveId))`,
    `receiveAbilityData(abilityId, ability)`, `receiveSpeciesData(...)`, `getCobbledMoves()`, `getTypeChart()`. So a datapack-provided `moves/<id>.js` / `abilities/<id>.js` (raw JS, Showdown format) is streamed into the engine.
  - Showdown move objects carry the effect logic: `basePower, accuracy, category, type, priority, target, pp, secondaries/secondary (chance), onHit/onTryHit/onBasePower/... conditions`. This is where "move effects" are actually authored.
- `com.cobblemon.mod.common.api.battles.interpreter.Effect` is a **battle-log effect descriptor**, not an authoring API: `getId()`, `getType(): Effect.Type`, `getRawData()`. `Effect.Type` = `ABILITY, ITEM, MOVE, BAGITEM, PURE` (each with a prefix). Impl: `com.cobblemon.mod.common.battles.interpreter.CobblemonEffect(id, type, rawData)`.
- Kotlin-side battle engine: `com.cobblemon.mod.common.battles.*` (`InterpreterInstruction`, `InstructionSet`, `ShowdownInterpreter`; instructions `DamageInstruction`, `BoostInstruction`, `StatusInstruction`, `MoveInstruction`, `ActivateInstruction`, ...). These translate Showdown battle-log lines; **not** a move-authoring API.
- Client move/animation effects: `com.cobblemon.mod.common.api.moves.animations/{ActionEffects, ActionEffectTimeline, *Keyframe}` + datapack `data/cobblemon/action_effects/moves/*.json` (reloadable). This is the visual layer only.

---

## 5. Fabric 1.21.1 client GUI + networking essentials

All class names below verified against `fabric-api-0.116.17+1.21.1.jar` (`javap`), matching API version in §1.3 (MC classes are obfuscated `class_XXXX` in the signatures because mappings aren't applied here).

### 5.1 Keybind → Screen (CONFIRMED classes)
- Register key: `net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(KeyMapping)`.
- Poll in tick: `net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> { while (key.consumeClick()) client.setScreen(new EditorScreen()); })`.
- Open screen: `net.minecraft.client.gui.screens.Screen` via `Minecraft.getInstance().setScreen(...)`.

### 5.2 Command → Screen / server action (CONFIRMED classes)
- Server & client command registration: `net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT` (client variant: `net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback`).
- Recommended: a server command `/cobbleeditor` sends an S2C "open editor" payload; the client opens the `Screen` (keeps the open action server-decided).

### 5.3 Networking with CustomPayload (1.21.1) (CONFIRMED classes/signatures)
- Payload: a record implementing `net.minecraft.network.packet.CustomPayload`, with a `CustomPayload.Id<T>` + a `StreamCodec`.
- Register: `net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(id, codec)` and `playS2C().register(id, codec)`.
- Client→server: `net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload)`.
- Server receive: `net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(id, (payload, context) -> { ... context.player().server.execute(serverWork); })`.
- Server→client: `net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload)`.
- Client receive: `net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(id, (payload, context) -> client.execute(clientWork))`.
- Buffers: `net.fabricmc.fabric.api.networking.v1.PacketByteBufs`.

### 5.4 Recommended minimal pattern (server-authoritative)
1. Client: user edits a **draft model** in a custom `Screen`/`AbstractContainerScreen` (no world writes client-side).
2. Client → server C2S payload with the edit (species/move/ability + fields).
3. Server: validate (permission/op + value ranges + name uniqueness), serialize, **write JSON/JS** into the world datapack dir, then apply (see §6.3), then sync.
4. Server → client: S2C ack/err payload; optionally re-open/refresh the GUI showing the authoritative result.
5. Never trust client-supplied file paths or raw JSON — build JSON server-side from a validated model.

Container-based screens need `AbstractContainerMenu` + `MenuType` + `net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory` (module `fabric-screen-handler-api-v1`); otherwise a plain `Screen` + custom widgets is enough for a pure settings editor.

---

## 6. Recommended addon architecture, risks, unknowns

### 6.1 Layered architecture
1. **Editor model + GUI (client):** a validated Kotlin data model mirroring the species JSON (`baseStats`, `types`, `abilities`, learnset, `species_additions` patches) and a move model (Showdown fields). Screens for species list/edit, move editor, effect-template composer.
2. **Custom-effect registry (server-side library):** a curated catalog of predefined Showdown effect fragments (e.g. `secondary.burn 10%`, `drain 50%`, `multihit 2-5`, `priority`, `recoil`, `stat-change`) that the GUI composes into a Showdown move object, then serializes to a `moves/<id>.js`. Effects are *not* JSON in Cobblemon. (`ActionEffects` JSON is purely visual and separate.)
3. **Datapack-write layer (server):**
   - New species → `data/<addon_ns>/species/<id>.json`.
   - Existing-species edits → `data/<addon_ns>/species_additions/<id>.json` (patch semantics; preserves the base file).
   - Moves/abilities → `data/<addon_ns>/moves/<id>.js`, `.../abilities/<id>.js` (Showdown format).
   - Written under the save's `datapacks/<pack>/...` so the addon doesn't fight the mod jar.
4. **Apply/resync layer (server):** see §6.3.
5. **Network:** C2S edits, S2C results/open-editor; server authoritative; re-sync registries to clients after changes.

### 6.2 Why not just "write JSON + `/reload`"
- §3.2: species/moves/abilities are `reloadable = false`; `/reload` won't pick them up in a live world. The product's stated apply-mechanism must change.

### 6.3 Apply options (pick one; each has trade-offs)
- **A. Restart-to-apply (safest):** write files, tell user to restart. Cobblemon loads all `SERVER_DATA` registries on world load.
- **B. Forced reload (fast, risky):** after writing, the addon itself calls `PokemonSpecies.reload(server.getResourceManager())` + `SpeciesAdditions` (reflection, internal) + `Moves.reload` / `Abilities.reload`, then re-syncs (`PokemonSpecies.sync(player)` or `CobblemonDataProvider.sync`). Cobblemon explicitly warns species reload is "not safe" — battles may hold stale `Species` references. Guard: only allow when no battle is active.
- **C. Reloadable-only edits:** for the *visual* layer (`ActionEffects`) `/reload` is fine; not sufficient for stats/moves.

### 6.4 Top risks / unknowns
**Top 3 implementation risks**
1. **`/reload` does not apply species/move/ability data** (`reloadable = false`, and Cobblemon says species reload is unsafe). The product must either require a restart, or addon-force a registry reload with strict guards. This invalidates the simplest version of the product goal.
2. **Move "effects" are Pokémon Showdown JavaScript** executed by GraalVM — there is no Cobblemon JSON schema or library for them. Composing effects means generating/validating Showdown JS move objects; third-party runtime registration of moves is impossible (`Moves.register` is private). Risk: JS-authoring complexity, validation, and engine-version drift.
3. **Override + sync semantics:** to override a Cobblemon species you must use namespace `cobblemon` + exact filename; the base dex is shared and synced to clients (`SpeciesRegistrySyncPacket`), and battle objects cache `Species`. Live edits can desync clients or crash mid-battle; also writing into `<world>/datapacks` requires pack discovery + (for species) a restart.

**Additional risks**
- Kotlin `internal`/`object` API friction (`SpeciesAdditions` is internal; `AbilityTemplate` needs Kotlin function types).
- Editing base data may break other mods/datapacks that also target `cobblemon:` identifiers (last pack wins).
- GraalVM/Showdown coupling: custom JS must match the bundled Showdown version bundled in `showdown.zip`.

### 6.5 Risk-mitigating recommendation
- Prefer `species_additions` patches over full species overrides (smaller blast radius, preserves upstream updates).
- Keep all custom content in the addon's own namespace (`data/<addon_ns>/...`), reserving `cobblemon:` overrides for deliberate base changes.
- Implement apply as **explicit "Restart to apply"** (Option A) for v1; add guarded forced-reload (Option B) later.
- For move effects, ship a **fixed, version-pinned library of Showdown fragments** and validate any generated JS against the bundled Showdown registry before saving.

---

## Summary

**CONFIRMED**
- Maven: `https://maven.impactdev.net/repository/development/`; group `com.cobblemon`, artifacts `mod` (common) / `fabric`; released version `1.8.1+1.21.1` (also `1.8.2+1.21.1`, `1.8.3+1.21.1-SNAPSHOT`). Loader deps: fabricloader ≥0.17.2, fabric-api ≥0.116.6+1.21.1, MC 1.21.1, Java 21; Fabric API latest `0.116.17+1.21.1`.
- Species JSON at `data/cobblemon/species/generationN/<name>.json` (1025 files); registry `PokemonSpecies : JsonDataRegistry<Species>`; schema `assets/cobblemon/species.schema.json`; fields documented (§2.2).
- **Moves and abilities are NOT JSON** — they come from the bundled Pokémon Showdown engine (`data/cobblemon/showdown.zip` → `data/moves.js`, `data/abilities.js`); `Moves.reload` builds `MoveTemplate`s from Showdown registry data.
- Datapack override = same namespace+filename; new content = new namespace; `species_additions` JSON patches existing species.
- **`/reload` does NOT reload species/moves/abilities** (`reloadable=false`); only `ActionEffects` and the other listed registries reload.
- Runtime API: `Abilities.register(AbilityTemplate)` is public; species/moves have no public register (use JSON/JS + reload); `Abilities`/`PokemonSpecies`/`Moves` are Kotlin `object`s; sync via `*RegistrySyncPacket`.
- Battle effects = Showdown JS via `ShowdownService.sendRegistryData(...)`; `api.battles.interpreter.Effect` is only a log-effect descriptor.
- Fabric 1.21.1 GUI/networking classes verified (KeyBindingHelper, ClientTickEvents, ClientPlayNetworking/ServerPlayNetworking/PayloadTypeRegistry, CommandRegistrationCallback).

**UNKNOWN (needs verification before building)**
- Exact Fabric Loom version and Yarn mappings build for 1.21.1.
- Exact expected content/format of datapack `moves/*.js` / `abilities/*.js` (raw Showdown move object vs wrapped) and the full evaluation path in `GraalShowdownService` (only the JS bridge `receiveMoveData` was confirmed).
- Whether `PokemonSpecies.reload` on a live server is actually safe in practice (Cobblemon's own comment says it is not).
- Whether `/reload` re-scans newly added `<world>/datapacks` folders (assumed yes; unverified).
- Whether a separate module can call Kotlin `internal` (`SpeciesAdditions`) without reflection.
- Yarn-mapped names for MC classes shown as obfuscated `class_XXXX` in the javap output.

**Top 3 risks:** (1) `/reload` won't apply species/moves/abilities → needs restart or guarded forced reload; (2) move effects require authoring/validating Pokémon Showdown JS, with no runtime move-registration API; (3) override + client-sync + battle-cached-species hazards make live edits fragile.
