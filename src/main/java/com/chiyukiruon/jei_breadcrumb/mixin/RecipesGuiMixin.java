package com.chiyukiruon.jei_breadcrumb.mixin;

import com.chiyukiruon.jei_breadcrumb.Config;
import com.chiyukiruon.jei_breadcrumb.JEIBreadcrumb;
import com.chiyukiruon.jei_breadcrumb.breadcrumb.BreadcrumbBar;
import mezz.jei.common.util.ImmutableRect2i;
import mezz.jei.gui.elements.IconButton;
import mezz.jei.gui.recipes.RecipesGui;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipesGui.class)
public abstract class RecipesGuiMixin {
    @Shadow
    @Final
    private IconButton nextRecipeCategory;
    @Shadow
    @Final
    private IconButton previousRecipeCategory;
    @Shadow
    @Final
    private IconButton nextPage;
    @Shadow
    @Final
    private IconButton previousPage;
    @Shadow
    private int headerHeight;

    /**
     * JEI resets navigation positions and header height on each layout update.
     * Reserve space afterwards, including during resizing and category changes.
     */
    @Inject(method = "updateNavigation", at = @At("TAIL"))
    private void jei_breadcrumb$reserveSpace(CallbackInfo ci) {
        if (!Config.enabled) {
            return;
        }

        int shift = BreadcrumbBar.RESERVED_HEIGHT;
        shiftDown(this.previousRecipeCategory, shift);
        shiftDown(this.nextRecipeCategory, shift);
        shiftDown(this.previousPage, shift);
        shiftDown(this.nextPage, shift);
        this.headerHeight += shift;
        JEIBreadcrumb.LOGGER.debug("Reserved {}px above the recipe layouts for the breadcrumb bar", shift);
    }

    private static void shiftDown(IconButton button, int amount) {
        ImmutableRect2i buttonArea = button.getArea();
        button.updateBounds(buttonArea.setPosition(buttonArea.getX(), buttonArea.getY() + amount));
    }
}
