package com.chiyukiruon.jei_breadcrumb.breadcrumb;

import com.chiyukiruon.jei_breadcrumb.Config;
import mezz.jei.gui.recipes.RecipesGui;
import net.minecraftforge.client.event.ScreenEvent;

/** Draws after the screen without depending on JEI's optimized render bytecode. */
public final class BreadcrumbRenderer {
    private BreadcrumbRenderer() {
    }

    public static void onRender(ScreenEvent.Render.Post event) {
        if (Config.enabled && event.getScreen() instanceof RecipesGui gui) {
            BreadcrumbBar.draw(event.getGuiGraphics(), gui.getArea(), event.getMouseX(), event.getMouseY());
        }
    }
}
