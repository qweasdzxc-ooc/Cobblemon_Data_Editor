package com.xiaoyu.cobblemoneditor

import com.xiaoyu.cobblemoneditor.server.EditorCommands
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

/**
 * Fabric 入口类。必须用 class（而不是 object）：Fabric 通过公开无参构造创建入口，
 * Kotlin object 的构造是 private，会导致 IllegalAccessException 崩溃。
 */
class CobblemonEditor : ModInitializer {
	override fun onInitialize() {
		log.info("[cobblemoneditor] server init")
		EditorCommands.register()
	}

	companion object {
		const val MOD_ID = "cobblemoneditor"
		private val log = LoggerFactory.getLogger(MOD_ID)
	}
}
