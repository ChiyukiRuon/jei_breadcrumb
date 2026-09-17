package com.chiyukiruon.jei_breadcrumb.breadcrumb;

import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.drawable.IScalableDrawable;
import mezz.jei.common.Internal;
import mezz.jei.common.gui.JeiGuiColors;
import mezz.jei.common.gui.JeiGuiColors.GuiColor;
import mezz.jei.common.gui.textures.Textures;
import mezz.jei.common.util.ImmutableRect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Draws and handles the breadcrumb bar that {@code RecipesGuiMixin} reserves at the top of the recipe
 * screen.
 * <p>
 * Geometry is derived from {@code RecipesGui.area} on every frame, so it stays correct across resizes
 * without any state of its own beyond the currently pressed element.
 */
public final class BreadcrumbBar {
	/**
	 * Height of the bar itself, in pixels. Odd, so that the 13px buttons land on an exact vertical
	 * center.
	 */
	private static final int BAR_HEIGHT = 21;

	/**
	 * Gap between the top of the recipe panel and the bar. JEI's category tabs hang over the panel's
	 * top {@code TAB_GUI_OVERLAP} pixels, and without this the bar would sit right against them.
	 */
	private static final int BAR_TOP_INSET = 4;

	/** Total height taken away from the recipe layouts area, in pixels. */
	public static final int RESERVED_HEIGHT = BAR_TOP_INSET + BAR_HEIGHT;

	private static final int CONTENT_HEIGHT = BreadcrumbEntry.ICON_SIZE;

	private static final int BORDER_PADDING = 6;
	private static final int BUTTON_SIZE = 13;
	private static final int BUTTON_GAP = 2;
	private static final int BUTTON_INSET = 2;
	private static final int TEXT_GAP = 3;
	private static final int CONTENT_RIGHT_PADDING = 2;

	private static final int BACK = 0;
	private static final int FORWARD = 1;
	private static final int ROOT = 2;
	private static final int STEP_BASE = 10;
	private static final int NONE = -1;

	private static final String SEPARATOR = ">";
	private static final String ELLIPSIS = "...";

	private static final int TINT_NORMAL = 0xFFE0E0E0;
	private static final int TINT_HOVERED = 0xFFFFFFFF;
	private static final int TINT_INACTIVE = 0xFFA0A0A0;
	private static final int SEPARATOR_COLOR = 0xFF909090;
	private static final int CURRENT_STEP_COLOR = 0x25FFFFFF;
	private static final int HOVERED_STEP_COLOR = 0x45FFFFFF;

	private static int pressed = NONE;

	private BreadcrumbBar() {

	}

	public static void draw(GuiGraphics guiGraphics, ImmutableRect2i area, int mouseX, int mouseY) {
		if (area.isEmpty()) {
			return;
		}

		Font font = Minecraft.getInstance().font;
		ImmutableRect2i bar = barArea(area);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		// Items are rendered at a high Z and JEI draws its category tabs with the depth test off, so
		// the bar turns it off too rather than fight them for depth.
		RenderSystem.disableDepthTest();

		List<Component> tooltip = null;
		try {
			guiGraphics.fill(
				RenderType.gui(),
				bar.getX(),
				bar.getY(),
				bar.getX() + bar.getWidth(),
				bar.getY() + bar.getHeight(),
				JeiGuiColors.getColor(GuiColor.PAGE_NAVIGATION_BACKGROUND)
			);

			// Every element must be drawn: only the tooltip is picked. Argument order is the draw order.
			tooltip = firstNonNull(
				drawButton(guiGraphics, buttonArea(bar, BACK), mouseX, mouseY, pressed == BACK,
					BreadcrumbHistory.canGoBack(), Icon.PREVIOUS, "jei_breadcrumb.button.back"),
				drawButton(guiGraphics, buttonArea(bar, FORWARD), mouseX, mouseY, pressed == FORWARD,
					BreadcrumbHistory.canGoForward(), Icon.NEXT, "jei_breadcrumb.button.forward"),
				drawButton(guiGraphics, buttonArea(bar, ROOT), mouseX, mouseY, pressed == ROOT,
					BreadcrumbHistory.canGoToRoot(), Icon.ROOT, "jei_breadcrumb.button.root"),
				drawSteps(guiGraphics, font, bar, mouseX, mouseY)
			);
		} finally {
			RenderSystem.enableDepthTest();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		}

		if (tooltip != null) {
			guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
		}
	}

	@SafeVarargs
	private static @Nullable List<Component> firstNonNull(@Nullable List<Component>... tooltips) {
		for (List<Component> tooltip : tooltips) {
			if (tooltip != null) {
				return tooltip;
			}
		}
		return null;
	}

	private static @Nullable List<Component> drawButton(
		GuiGraphics guiGraphics,
		ImmutableRect2i button,
		int mouseX,
		int mouseY,
		boolean isPressed,
		boolean active,
		Icon icon,
		String tooltipKey
	) {
		Textures textures = Internal.getTextures();
		boolean hovered = active && button.contains(mouseX, mouseY);

		// The API interface: JEI renamed the backing class between versions (DrawableNineSliceTexture
		// in 19.27, ScalableDrawable from 19.44) but IScalableDrawable is unchanged across all of them.
		IScalableDrawable background = textures.getButtonForState(isPressed, active, hovered);
		background.draw(guiGraphics, button.getX(), button.getY(), button.getWidth(), button.getHeight());

		int x = button.getX() + (button.getWidth() - icon.width()) / 2;
		int y = button.getY() + (button.getHeight() - icon.height()) / 2;
		if (isPressed && hovered) {
			x += 1;
			y += 1;
		}

		int tint = !active ? TINT_INACTIVE : (hovered ? TINT_HOVERED : TINT_NORMAL);
		icon.draw(guiGraphics, x, y, tint);

		return hovered ? List.of(Component.translatable(tooltipKey)) : null;
	}

	private enum Icon {
		PREVIOUS,
		NEXT,
		/** JEI has no "back to top" sprite, so this one is drawn from two rectangles instead. */
		ROOT;

		private static final int ROOT_WIDTH = 5;
		private static final int ROOT_HEIGHT = 7;

		/** JEI's arrow sprite, or null for {@link #ROOT}. */
		private @Nullable IDrawableStatic sprite() {
			return switch (this) {
				case PREVIOUS -> Internal.getTextures().getArrowPrevious();
				case NEXT -> Internal.getTextures().getArrowNext();
				case ROOT -> null;
			};
		}

		/**
		 * The sprite's own size, queried rather than assumed: JEI resized its arrow icons between
		 * versions (9x7 in 19.x, 9x9 in 15.x), and a wrong height is what pushes the arrow off center.
		 */
		int width() {
			IDrawableStatic drawable = sprite();
			return drawable != null ? drawable.getWidth() : ROOT_WIDTH;
		}

		int height() {
			IDrawableStatic drawable = sprite();
			return drawable != null ? drawable.getHeight() : ROOT_HEIGHT;
		}

		void draw(GuiGraphics guiGraphics, int x, int y, int tint) {
			IDrawableStatic drawable = sprite();
			if (drawable != null) {
				blit(guiGraphics, drawable, x, y, tint);
			} else {
				drawChevrons(guiGraphics, x, y, tint);
			}
		}

		private static void blit(GuiGraphics guiGraphics, IDrawableStatic drawable, int x, int y, int tint) {
			RenderSystem.setShaderColor(
				((tint >> 16) & 0xFF) / 255.0F,
				((tint >> 8) & 0xFF) / 255.0F,
				(tint & 0xFF) / 255.0F,
				((tint >> 24) & 0xFF) / 255.0F
			);
			drawable.draw(guiGraphics, x, y);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		}

		/**
		 * JEI has no "back to top" sprite, so two thin up-chevrons are drawn from solid rectangles.
		 * Each chevron is an outline 5 wide and 3 tall, matching the weight of the arrow sprites.
		 */
		private static void drawChevrons(GuiGraphics guiGraphics, int x, int y, int tint) {
			int apex = x + 2;
			for (int chevron = 0; chevron < 2; chevron++) {
				int top = y + chevron * 4;
				for (int row = 0; row < 3; row++) {
					int left = apex - row;
					int right = apex + row;
					guiGraphics.fill(left, top + row, left + 1, top + row + 1, tint);
					if (right != left) {
						guiGraphics.fill(right, top + row, right + 1, top + row + 1, tint);
					}
				}
			}
		}
	}

	private static @Nullable List<Component> drawSteps(GuiGraphics guiGraphics, Font font, ImmutableRect2i bar, int mouseX, int mouseY) {
		List<BreadcrumbEntry> entries = visibleSteps();
		if (entries.isEmpty()) {
			return null;
		}

		Layout layout = Layout.compute(font, contentArea(bar), entries);
		int iconY = contentY(bar);
		int textY = iconY + (CONTENT_HEIGHT - font.lineHeight) / 2;
		int separatorWidth = separatorWidth(font);

		if (layout.from() > 0) {
			guiGraphics.drawString(font, ELLIPSIS, layout.contentX(), textY, SEPARATOR_COLOR, false);
			int separatorX = layout.contentX() + font.width(ELLIPSIS) + 2;
			guiGraphics.drawString(font, SEPARATOR, separatorX, textY, SEPARATOR_COLOR, false);
		}

		int hoveredStep = NONE;
		for (int i = layout.from(); i < entries.size(); i++) {
			int x = layout.x(i);
			boolean hovered = contains(mouseX, mouseY, x, bar);

			if (i == entries.size() - 1) {
				fillStep(guiGraphics, x, bar, CURRENT_STEP_COLOR);
			} else if (hovered) {
				fillStep(guiGraphics, x, bar, HOVERED_STEP_COLOR);
			}

			if (i > layout.from()) {
				guiGraphics.drawString(font, SEPARATOR, x - separatorWidth + 2, textY, SEPARATOR_COLOR, false);
			}

			entries.get(i).draw(guiGraphics, x, iconY);

			if (hovered) {
				hoveredStep = i;
			}
		}

		if (hoveredStep != NONE) {
			return entries.get(hoveredStep).tooltipLines();
		}
		return null;
	}

	private static void fillStep(GuiGraphics guiGraphics, int x, ImmutableRect2i bar, int color) {
		int top = contentY(bar);
		guiGraphics.fill(x - 1, top - 1, x + BreadcrumbEntry.ICON_SIZE + 1, top + CONTENT_HEIGHT + 1, color);
	}

	private static boolean contains(double mouseX, double mouseY, int x, ImmutableRect2i bar) {
		return mouseX >= x - 1 && mouseX < x + BreadcrumbEntry.ICON_SIZE + 1 &&
			mouseY >= bar.getY() && mouseY < bar.getY() + bar.getHeight();
	}

	/** @return true when the press was consumed by the bar. */
	public static boolean onPress(ImmutableRect2i area, double mouseX, double mouseY, int mouseButton) {
		if (area.isEmpty() || mouseButton != 0) {
			return false;
		}
		ImmutableRect2i bar = barArea(area);
		pressed = hitTest(bar, mouseX, mouseY);
		return pressed != NONE;
	}

	/** @return true when the release should be swallowed, even if it did not complete a click. */
	public static boolean onRelease(ImmutableRect2i area, double mouseX, double mouseY, int mouseButton) {
		if (area.isEmpty() || mouseButton != 0 || pressed == NONE) {
			return false;
		}
		int target = pressed;
		pressed = NONE;

		ImmutableRect2i bar = barArea(area);
		if (hitTest(bar, mouseX, mouseY) == target) {
			activate(target);
		}
		return true;
	}

	public static void reset() {
		pressed = NONE;
	}

	private static int hitTest(ImmutableRect2i bar, double mouseX, double mouseY) {
		if (!bar.contains(mouseX, mouseY)) {
			return NONE;
		}

		for (int i = 0; i < 3; i++) {
			if (buttonArea(bar, i).contains(mouseX, mouseY)) {
				return i;
			}
		}

		List<BreadcrumbEntry> entries = visibleSteps();
		Layout layout = Layout.compute(Minecraft.getInstance().font, contentArea(bar), entries);
		for (int i = layout.from(); i < entries.size(); i++) {
			if (contains(mouseX, mouseY, layout.x(i), bar)) {
				return STEP_BASE + i;
			}
		}
		return NONE;
	}

	private static void activate(int target) {
		boolean moved = switch (target) {
			case BACK -> BreadcrumbHistory.back();
			case FORWARD -> BreadcrumbHistory.forward();
			case ROOT -> BreadcrumbHistory.goToRoot();
			default -> BreadcrumbHistory.jumpTo(target - STEP_BASE);
		};
		if (moved) {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		}
	}

	/**
	 * The steps up to and including the current one. Going back -- or clicking an earlier step -- hides
	 * the steps ahead of the cursor without dropping them, so the forward button can still walk back out
	 * again; a new lookup is what finally truncates the path.
	 * <p>
	 * Indexes here are identical to path indexes, since this is always a prefix of the path.
	 */
	private static List<BreadcrumbEntry> visibleSteps() {
		List<BreadcrumbEntry> entries = BreadcrumbHistory.entries();
		int index = BreadcrumbHistory.index();
		if (index < 0) {
			return List.of();
		}
		if (index >= entries.size() - 1) {
			return entries;
		}
		return entries.subList(0, index + 1);
	}

	private static ImmutableRect2i barArea(ImmutableRect2i area) {
		return new ImmutableRect2i(
			area.getX() + BORDER_PADDING,
			area.getY() + BAR_TOP_INSET,
			area.getWidth() - 2 * BORDER_PADDING,
			BAR_HEIGHT
		);
	}

	private static ImmutableRect2i buttonArea(ImmutableRect2i bar, int index) {
		return new ImmutableRect2i(
			bar.getX() + BUTTON_INSET + index * (BUTTON_SIZE + BUTTON_GAP),
			bar.getY() + (BAR_HEIGHT - BUTTON_SIZE) / 2,
			BUTTON_SIZE,
			BUTTON_SIZE
		);
	}

	/** Top edge of the ingredient icons; centered vertically in the bar. */
	private static int contentY(ImmutableRect2i bar) {
		return bar.getY() + (BAR_HEIGHT - CONTENT_HEIGHT) / 2;
	}

	private static ImmutableRect2i contentArea(ImmutableRect2i bar) {
		int left = BUTTON_INSET + 3 * BUTTON_SIZE + 2 * BUTTON_GAP + TEXT_GAP;
		return new ImmutableRect2i(
			bar.getX() + left,
			bar.getY(),
			bar.getWidth() - left - CONTENT_RIGHT_PADDING,
			BAR_HEIGHT
		);
	}

	private static int separatorWidth(Font font) {
		return font.width(SEPARATOR) + 4;
	}

	/**
	 * Where each step lands. Every step is the same width, so when the path outgrows the bar the oldest
	 * steps are dropped behind an ellipsis.
	 */
	private record Layout(int from, int contentX, int[] xs) {
		static Layout compute(Font font, ImmutableRect2i content, List<BreadcrumbEntry> entries) {
			int count = entries.size();
			if (count == 0) {
				return new Layout(0, content.getX(), new int[0]);
			}

			int separator = separatorWidth(font);
			int step = BreadcrumbEntry.ICON_SIZE + separator;
			int available = content.getWidth();

			int from = 0;
			if (visibleWidth(count, from, step, separator) > available) {
				// Dropping the oldest steps frees up room, at the cost of an ellipsis prefix.
				int ellipsis = font.width(ELLIPSIS) + separator;
				from = 1;
				while (from < count - 1 && ellipsis + visibleWidth(count, from, step, separator) > available) {
					from++;
				}
			}

			int[] xs = new int[count];
			int x = content.getX() + (from > 0 ? font.width(ELLIPSIS) + separator : 0);
			for (int i = from; i < count; i++) {
				if (i > from) {
					x += separator;
				}
				xs[i] = x;
				x += BreadcrumbEntry.ICON_SIZE;
			}
			return new Layout(from, content.getX(), xs);
		}

		/** Width of {@code entries[from..count)}, separators included and no leading one. */
		private static int visibleWidth(int count, int from, int step, int separator) {
			return (count - from) * step - separator;
		}

		int x(int index) {
			return xs[index];
		}
	}
}
