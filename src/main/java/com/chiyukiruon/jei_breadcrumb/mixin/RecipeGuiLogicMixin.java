package com.chiyukiruon.jei_breadcrumb.mixin;

import com.chiyukiruon.jei_breadcrumb.JEIBreadcrumb;
import com.chiyukiruon.jei_breadcrumb.breadcrumb.BreadcrumbBar;
import com.chiyukiruon.jei_breadcrumb.breadcrumb.BreadcrumbEntry;
import com.chiyukiruon.jei_breadcrumb.breadcrumb.BreadcrumbHistory;
import com.mojang.blaze3d.platform.Window;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.gui.recipes.IRecipeGuiLogic;
import mezz.jei.gui.recipes.RecipeGuiLogic;
import mezz.jei.gui.recipes.RecipesGui;
import mezz.jei.gui.recipes.lookups.ILookupState;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeGuiLogic.class)
public abstract class RecipeGuiLogicMixin {
    @Shadow(remap = false)
    @Final
    private IIngredientManager ingredientManager;

    @Shadow(remap = false)
    private ILookupState state;

    /**
     * {@code setState} is the single choke point every display change goes through.
     */
    @Inject(method = "setState", remap = false, at = @At("RETURN"))
    private void jei_breadcrumb$onSetState(ILookupState state, boolean saveHistory, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }

        // R8 can reuse the constructor's this slot. Bind only once a lookup succeeds,
        // before recording any steps that can invoke back/forward navigation.
        BreadcrumbHistory.setLogic((IRecipeGuiLogic) (Object) this);

        if (saveHistory) {
            BreadcrumbHistory.record(BreadcrumbEntry.snapshot(ingredientManager, state), isClickInsideRecipeGui());
            return;
        }

        BreadcrumbEntry snapshot = BreadcrumbEntry.snapshot(ingredientManager, state);
        int step = BreadcrumbHistory.indexOf(state);
        if (step < 0) {
            step = BreadcrumbHistory.indexOfLookup(snapshot.lookupKey());
            if (step >= 0) {
                JEIBreadcrumb.LOGGER.debug(
                    "JEI restored a state that is not on the path by identity, matched step {} by lookup instead", step + 1);
            }
        }

        if (step >= 0) {
            BreadcrumbHistory.moveTo(step);
            BreadcrumbHistory.replace(step, snapshot);
        } else {
            BreadcrumbHistory.record(snapshot, false);
        }
    }

    /**
     * Whether the lookup was started by clicking inside the recipe screen, as opposed to the ingredient
     * list sidebar next to it -- which starts a fresh path.
     * <p>
     * The screen alone cannot tell these apart: JEI keeps the ingredient list visible while the recipe
     * screen is open, so {@code minecraft.screen} is the recipe screen either way. Two things count as
     * "inside":
     * <ul>
     *     <li>the pointer is over the recipe panel itself ({@code isMouseOver} ignores the overlay),</li>
     *     <li>or the recipe screen has an ingredient under the pointer -- which covers the catalyst
     *         column of applicable blocks, drawn outside the panel.</li>
     * </ul>
     */
    private static boolean isClickInsideRecipeGui() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof RecipesGui recipesGui)) {
            return false;
        }

        // The pointer in GUI coordinates. This is JEI's MouseUtil#getX/#getY inlined: that class only
        // exists from 19.56 on, and the math is the same one vanilla uses for screen coordinates.
        Window window = minecraft.getWindow();
        double scale = window.getGuiScaledWidth() / (double) window.getScreenWidth();
        double mouseX = minecraft.mouseHandler.xpos() * scale;
        double mouseY = minecraft.mouseHandler.ypos() * scale;

        return recipesGui.isMouseOver(mouseX, mouseY) ||
            recipesGui.getIngredientUnderMouse(mouseX, mouseY).findAny().isPresent();
    }

    /**
     * Switching the displayed recipe category rewrites the current lookup state in place, so it never
     * reaches {@code setState}. Without this the current step would keep showing the category it was
     * first recorded with instead of the one on screen.
     */
    @Inject(method = "setRecipeCategory", remap = false, at = @At("RETURN"))
    private void jei_breadcrumb$onSetRecipeCategory(IRecipeCategory<?> recipeCategory, CallbackInfo ci) {
        jei_breadcrumb$refreshCurrentStep();
    }

    @Inject(method = "nextRecipeCategory", remap = false, at = @At("RETURN"))
    private void jei_breadcrumb$onNextRecipeCategory(CallbackInfoReturnable<Boolean> cir) {
        jei_breadcrumb$refreshCurrentStep();
    }

    @Inject(method = "previousRecipeCategory", remap = false, at = @At("RETURN"))
    private void jei_breadcrumb$onPreviousRecipeCategory(CallbackInfoReturnable<Boolean> cir) {
        jei_breadcrumb$refreshCurrentStep();
    }

    private void jei_breadcrumb$refreshCurrentStep() {
        if (this.state == null) {
            return;
        }
        BreadcrumbHistory.refreshCurrent(BreadcrumbEntry.snapshot(ingredientManager, this.state));
    }

    @Inject(method = "clearHistory", remap = false, at = @At("HEAD"))
    private void jei_breadcrumb$onClearHistory(CallbackInfo ci) {
        BreadcrumbHistory.clear();
        BreadcrumbBar.reset();
    }
}
