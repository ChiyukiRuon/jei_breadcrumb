package com.chiyukiruon.jei_breadcrumb;

import com.chiyukiruon.jei_breadcrumb.breadcrumb.BreadcrumbKeys;
import com.chiyukiruon.jei_breadcrumb.breadcrumb.BreadcrumbMouse;
import com.chiyukiruon.jei_breadcrumb.breadcrumb.BreadcrumbRenderer;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * Client-only JEI addon: the mixins in {@code jei_breadcrumb.mixins.json} target JEI's client GUI
 * classes, so none of this may run on a dedicated server.
 * <p>
 * The constructor takes no arguments because that is what Forge 1.20.1 offers mods -- unlike NeoForge
 * it does not inject {@code IEventBus}/{@code ModContainer}/{@code Dist}, and its {@code @Mod} has no
 * {@code dist} attribute, so the mod loads everywhere and guards itself instead. The mixins are
 * declared in the {@code client} section of the mixin config, so they never apply on a server either.
 */
@Mod(JEIBreadcrumb.MODID)
public class JEIBreadcrumb {
    public static final String MODID = "jei_breadcrumb";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JEIBreadcrumb() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        LOGGER.info("JEI Breadcrumb {} loading", modVersion());
        logJeiVersion();

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(Config::onLoad);
        modEventBus.addListener(BreadcrumbKeys::register);

        MinecraftForge.EVENT_BUS.addListener(BreadcrumbKeys::onKeyPressed);
        MinecraftForge.EVENT_BUS.addListener(BreadcrumbKeys::onKeyReleased);
        MinecraftForge.EVENT_BUS.addListener(BreadcrumbKeys::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(DebugCommand::register);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, BreadcrumbMouse::onPressed);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, BreadcrumbMouse::onReleased);
        MinecraftForge.EVENT_BUS.addListener(BreadcrumbMouse::onClosing);
        MinecraftForge.EVENT_BUS.addListener(BreadcrumbRenderer::onRender);
    }

    private static String modVersion() {
        ModList modList = ModList.get();
        if (modList == null) {
            return "unknown";
        }
        return modList.getModContainerById(MODID)
            .map(container -> container.getModInfo().getVersion().toString())
            .orElse("unknown");
    }

    /**
     * The mixins reach into JEI internals, so the JEI version is the first thing to check when
     * something breaks.
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
