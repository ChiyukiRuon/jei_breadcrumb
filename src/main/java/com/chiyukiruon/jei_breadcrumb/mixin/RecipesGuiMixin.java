package com.chiyukiruon.jei_breadcrumb.mixin;

import com.chiyukiruon.jei_breadcrumb.Config;
import com.chiyukiruon.jei_breadcrumb.JEIBreadcrumb;
import com.chiyukiruon.jei_breadcrumb.breadcrumb.BreadcrumbBar;
import mezz.jei.common.util.ImmutableRect2i;
import mezz.jei.gui.elements.GuiIconButton;
import mezz.jei.gui.recipes.RecipesGui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipesGui.class)
public abstract class RecipesGuiMixin {
	@Shadow(remap = false)
	@Final
	private GuiIconButton nextRecipeCategory;
	@Shadow(remap = false)
	@Final
	private GuiIconButton previousRecipeCategory;
	@Shadow(remap = false)
	@Final
	private GuiIconButton nextPage;
	@Shadow(remap = false)
	@Final
	private GuiIconButton previousPage;
	@Shadow(remap = false)
	private int headerHeight;
	@Shadow(remap = false)
	private ImmutableRect2i area;

	/**
	 * Makes room for the breadcrumb bar by pushing JEI's own header down.
	 * <p>
	 * Injected before the {@code updateLayout()} at the end of {@code init()}, so the recipe layout
	 * area and the category title are recomputed with the new {@code headerHeight} in one pass.
	 * {@code area} itself is untouched, which is what keeps the panel frame exactly where JEI put it.
	 */
	@Inject(
		method = "init",
		at = @At(value = "INVOKE", target = "Lmezz/jei/gui/recipes/RecipesGui;updateLayout()V", remap = false)
	)
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

	private static void shiftDown(GuiIconButton button, int amount) {
		ImmutableRect2i buttonArea = button.getArea();
		button.updateBounds(buttonArea.setPosition(buttonArea.getX(), buttonArea.getY() + amount));
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void jei_breadcrumb$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
		if (Config.enabled) {
			BreadcrumbBar.draw(guiGraphics, this.area, mouseX, mouseY);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void jei_breadcrumb$mouseClicked(double mouseX, double mouseY, int mouseButton, CallbackInfoReturnable<Boolean> cir) {
		if (Config.enabled && BreadcrumbBar.onPress(this.area, mouseX, mouseY, mouseButton)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
	private void jei_breadcrumb$mouseReleased(double mouseX, double mouseY, int mouseButton, CallbackInfoReturnable<Boolean> cir) {
		if (Config.enabled && BreadcrumbBar.onRelease(this.area, mouseX, mouseY, mouseButton)) {
			cir.setReturnValue(true);
		}
	}
}
