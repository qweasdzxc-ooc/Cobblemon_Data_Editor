package com.xiaoyu.cobblemoneditor

import com.xiaoyu.cobblemoneditor.server.EditorCommands
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object CobblemonEditor : ModInitializer {
	const val MOD_ID = "cobblemoneditor"
	private val log = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		log.info("[cobblemoneditor] server init")
		EditorCommands.register()
	}
}
