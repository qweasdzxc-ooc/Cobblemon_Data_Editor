package com.xiaoyu.cobblemoneditor.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Base64

/**
 * 纯逻辑层单元测试：只测 parse/render 等数据函数，不启动 Minecraft。
 * （LearnsetMerger/AbilityPoolMerger 依赖 Cobblemon 运行时类型，无法在纯 JVM 下跑，不在覆盖范围。）
 */
class SpecParsingTest {

    // ---------- SpeciesFactory ----------

    @Test
    fun `species minimal spec fills defaults`() {
        val s = SpeciesFactory.parse("testmon", "dex=9001,type1=electric,abilities=static")
        assertNotNull(s)
        s!!
        assertEquals("testmon", s.name)
        assertEquals(9001, s.dex)
        assertEquals("electric", s.primaryType)
        assertNull(s.secondaryType)
        assertEquals(listOf("1:tackle"), s.moves)
        assertEquals(listOf("static"), s.abilities)
        val json = SpeciesFactory.buildJson(s)
        assertTrue(json.contains("\"implemented\": true"))
        assertTrue(json.contains("\"hp\": 50"))
        assertTrue(json.contains("\"primaryType\": \"electric\""))
    }

    @Test
    fun `species rejects missing required keys and bad values`() {
        assertNull(SpeciesFactory.parse("a", "dex=9001,abilities=static"))          // 缺 type1
        assertNull(SpeciesFactory.parse("a", "type1=electric,abilities=static"))    // 缺 dex
        assertNull(SpeciesFactory.parse("a", "dex=9001,type1=electric"))            // 缺 abilities
        assertNull(SpeciesFactory.parse("Bad-Id!", "dex=9001,type1=electric,abilities=static"))
        assertNull(SpeciesFactory.parse("a", "dex=9001,type1=notatype,abilities=static"))
        assertNull(SpeciesFactory.parse("a", "dex=9001,type1=electric,abilities=static,moves=1:"))
        assertNull(SpeciesFactory.parse("a", "dex=9001,type1=electric,abilities=static,hp=300"))
    }

    @Test
    fun `species namezh accepts Chinese and rejects spec-breaking chars`() {
        val ok = SpeciesFactory.parse("m", "dex=9002,type1=fire,abilities=blaze,namezh=小火龙")
        assertEquals("小火龙", ok?.nameZh)
        assertNull(SpeciesFactory.parse("m", "dex=9002,type1=fire,abilities=blaze,namezh=含,逗号"))
        assertNull(SpeciesFactory.parse("m", "dex=9002,type1=fire,abilities=blaze,namezh=" + "长".repeat(33)))
    }

    // ---------- SpeciesPatch ----------

    @Test
    fun `species patch parses stats and ability lists`() {
        val p = SpeciesPatch.parseSpec("speed=120,primaryType=electric,abilities=static|h:lightningrod")
        assertNotNull(p)
        p!!
        assertEquals(120, p.stats["speed"])
        assertEquals(listOf("static", "h:lightningrod"), p.mergeAbilities)
        val json = SpeciesPatch.buildJson("pikachu", p)
        assertTrue(json.contains("\"speed\": 120"))
        assertTrue(json.contains("h:lightningrod"))
    }

    @Test
    fun `species patch moves merge vs replace`() {
        val merge = SpeciesPatch.parseSpec("moves=1:tackle|egg:growl")!!
        assertEquals(listOf("1:tackle", "egg:growl"), merge.mergeMoves)
        assertNull(merge.replaceMoves)
        val replace = SpeciesPatch.parseSpec("movesReplace=1:tackle")!!
        assertEquals(listOf("1:tackle"), replace.replaceMoves)
        assertNull(replace.mergeMoves)
    }

    // ---------- MoveSpecFactory ----------

    @Test
    fun `status move always emits basePower 0 (readback requirement)`() {
        val m = MoveSpecFactory.parse("id=foo&name=测试&type=normal&category=status&pp=10")
        assertNotNull(m)
        val js = MoveSpecFactory.renderJs(m!!)
        assertTrue(js.contains("basePower: 0"))
        assertTrue(js.contains("category: \"Status\""))
        assertTrue(js.contains("shortDesc: \"测试\""))
    }

    @Test
    fun `multiple secondary bricks merge into one secondaries array`() {
        val m = MoveSpecFactory.parse(
            "id=foo&type=normal&category=physical&power=50&bricks=secondary_status:30:brn|secondary_flinch:10"
        )
        assertNotNull(m)
        val js = MoveSpecFactory.renderJs(m!!)
        assertTrue(js.contains("secondaries: ["))
        assertFalse(js.contains("secondary: "))
    }

    @Test
    fun `raw handler params are base64 decoded in parse and rendered verbatim`() {
        val rawJs = "onTryHit(target, source, move) { return false; }"
        val b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(rawJs.toByteArray(Charsets.UTF_8))
        val m = MoveSpecFactory.parse("id=foo&type=ground&category=physical&power=80&bricks=raw_handler:$b64")
        assertNotNull(m)
        val js = MoveSpecFactory.renderJs(m!!)
        assertTrue(js.contains(rawJs))
    }

    // ---------- AbilitySpecFactory ----------

    @Test
    fun `raw effect is output at object level (not nested in handler)`() {
        val rawJs = "onTryHit(target, source, move) { return false; }"
        val b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(rawJs.toByteArray(Charsets.UTF_8))
        val a = AbilitySpecFactory.parse("id=test&name=测试&desc=描述&effects=raw_effect:$b64")
        assertNotNull(a)
        val js = AbilitySpecFactory.renderJs(a!!)
        assertTrue(js.contains(rawJs))
        // 对象级输出意味着不会出现 "onStart(target, source, move) { ... rawJs ... }" 式嵌套
        assertFalse(js.contains("handler(params)"))
    }

    // ---------- HeldItemSpecFactory ----------

    @Test
    fun `held item residual heal renders onResidual handler`() {
        val item = HeldItemSpecFactory.parse("berry", "name=恢复果&desc=测试&bricks=residual_heal:16")
        assertNotNull(item)
        val js = HeldItemSpecFactory.renderJs(item!!)
        assertTrue(js.contains("name: \"berry\""))
        assertTrue(js.contains("onResidual"))
    }

    @Test
    fun `held item rejects unknown brick and bad params`() {
        assertNull(HeldItemSpecFactory.parse("b", "name=x&bricks=no_such_brick:1"))
        assertNull(HeldItemSpecFactory.parse("b", "name=x&bricks=residual_heal:abc"))
    }

    // ---------- 事件处理器参数防呆（GraalVM strict mode 拒绝重复形参） ----------

    @Test
    fun `ability triggers have unique parameter names`() {
        for (t in AbilityTrigger.entries) {
            val names = t.params.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            assertEquals(names.size, names.toSet().size, "触发时机 ${t.handler} 的参数列表有重复形参: ${t.params}")
        }
    }
}
