package com.xiaoyu.cobblemoneditor.client

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory

/**
 * 客户端入口。同样必须是 class（见 CobblemonEditor 的说明）。
 */
class CobblemonEditorClient : ClientModInitializer {
	override fun onInitializeClient() {
		log.info("[cobblemoneditor] client init")
		val openKey = KeyBindingHelper.registerKeyBinding(
			KeyMapping("key.cobblemoneditor.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.cobblemoneditor")
		)
		ClientTickEvents.END_CLIENT_TICK.register { client ->
			while (openKey.consumeClick()) {
				client.setScreen(EditorScreen())
			}
		}
	}

	companion object {
		private val log = LoggerFactory.getLogger("cobblemoneditor-client")
	}
}
