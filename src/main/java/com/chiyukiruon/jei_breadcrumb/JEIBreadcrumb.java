package com.chiyukiruon.jei_breadcrumb;

import com.chiyukiruon.jei_breadcrumb.breadcrumb.BreadcrumbKeys;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Client-only JEI addon: the mixins in {@code jei_breadcrumb.mixins.json} target JEI's client GUI
 * classes, so the mod must not even be constructed on a dedicated server.
 */
@Mod(value = JEIBreadcrumb.MODID, dist = Dist.CLIENT)
public class JEIBreadcrumb {
	public static final String MODID = "jei_breadcrumb";
	public static final Logger LOGGER = LogUtils.getLogger();

	public JEIBreadcrumb(IEventBus modEventBus, ModContainer modContainer) {
		LOGGER.info("JEI Breadcrumb {} loading", modContainer.getModInfo().getVersion());
		logJeiVersion();

		modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
		modEventBus.addListener(Config::onLoad);
		modEventBus.addListener(BreadcrumbKeys::register);
		NeoForge.EVENT_BUS.addListener(BreadcrumbKeys::onKeyPressed);
		NeoForge.EVENT_BUS.addListener(BreadcrumbKeys::onKeyReleased);
		NeoForge.EVENT_BUS.addListener(BreadcrumbKeys::onClientTick);
		NeoForge.EVENT_BUS.addListener(DebugCommand::register);
	}

	/**
	 * The mixins reach into JEI internals, so the JEI version is the first thing to check when
	 * something breaks. Logged separately because it is only known once the mod list is up.
	 */
	private static void logJeiVersion() {
		ModList modList = ModList.get();
		if (modList == null) {
			return;
		}
		modList.getModContainerById("jei")
			.ifPresent(jei -> LOGGER.info("JEI Breadcrumb found JEI {}", jei.getModInfo().getVersion()));
	}
}
