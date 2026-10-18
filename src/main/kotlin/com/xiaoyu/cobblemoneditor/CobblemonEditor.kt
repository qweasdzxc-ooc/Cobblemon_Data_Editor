package com.xiaoyu.cobblemoneditor

import com.xiaoyu.cobblemoneditor.server.EditorCommands
import com.xiaoyu.cobblemoneditor.server.LangPackWriter
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.world.level.storage.LevelResource
import org.slf4j.LoggerFactory

/**
 * Fabric 入口类。必须用 class（而不是 object）：Fabric 通过公开无参构造创建入口，
 * Kotlin object 的构造是 private，会导致 IllegalAccessException 崩溃。
 */
class CobblemonEditor : ModInitializer {
	override fun onInitialize() {
		log.info("[cobblemoneditor] server init")
		EditorCommands.register()
		// 世界加载完成后，把数据包里的自制招式/特性/精灵显示名同步进世界资源包
		// （resources.zip，进入世界时自动加载），让旧版本保存的数据也能显示中文名。
		// 内容没有变化时不写文件，避免资源管理器检测到包变化触发第二次全量加载。
		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			val worldDir = server.getWorldPath(LevelResource.ROOT)
			try {
				LangPackWriter.syncAll(worldDir, worldDir.resolve("datapacks").resolve(EditorCommands.PACK_ID))
			} catch (e: Exception) {
				log.warn("[cobblemoneditor] lang sync failed", e)
			}
		}
		// 世界退出时客户端才释放 resources.zip 的文件锁：后台线程趁隙补写，
		// 保证"退出世界 → 下次进入"时新保存特性的中文名已经就绪。
		ServerLifecycleEvents.SERVER_STOPPED.register { server ->
			val worldDir = server.getWorldPath(LevelResource.ROOT)
			try {
				LangPackWriter.syncInBackground(worldDir, worldDir.resolve("datapacks").resolve(EditorCommands.PACK_ID))
			} catch (e: Exception) {
				log.warn("[cobblemoneditor] lang sync failed", e)
			}
		}
	}

	companion object {
		const val MOD_ID = "cobblemoneditor"
		private val log = LoggerFactory.getLogger(MOD_ID)
	}
}
