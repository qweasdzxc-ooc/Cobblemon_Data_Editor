package com.xiaoyu.cobblemoneditor.client

import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

object CobblemonEditorClient : ClientModInitializer {
	private val log = LoggerFactory.getLogger("cobblemoneditor-client")

	override fun onInitializeClient() {
		log.info("[cobblemoneditor] client init")
		// Phase 1: keybind -> EditorScreen
	}
}
