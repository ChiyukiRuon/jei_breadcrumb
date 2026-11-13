package com.chiyukiruon.jei_breadcrumb.breadcrumb;

import com.chiyukiruon.jei_breadcrumb.Config;
import mezz.jei.gui.recipes.RecipesGui;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Handles breadcrumb clicks before JEI's own screen input listeners. */
public final class BreadcrumbMouse {
    private BreadcrumbMouse() {
    }

    public static void onPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!Config.enabled) {
            BreadcrumbBar.reset();
            return;
        }
        if (event.getScreen() instanceof RecipesGui gui &&
            BreadcrumbBar.onPress(gui.getArea(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    public static void onReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!Config.enabled) {
            BreadcrumbBar.reset();
            return;
        }
        if (event.getScreen() instanceof RecipesGui gui &&
            BreadcrumbBar.onRelease(gui.getArea(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    public static void onClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof RecipesGui) {
            BreadcrumbBar.reset();
        }
    }
}
