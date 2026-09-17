package com.chiyukiruon.jei_breadcrumb.breadcrumb;

import com.chiyukiruon.jei_breadcrumb.JEIBreadcrumb;
import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.gui.recipes.RecipesGui;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Keyboard navigation for the bar: back, forward and back-to-top.
 * <p>
 * Unbound by default -- the player assigns them in the controls screen.
 * <p>
 * These have to be read from the screen's own key events rather than through
 * {@link KeyMapping#consumeClick()}: {@code KeyboardHandler} only calls {@code KeyMapping.click} while
 * no screen is open, so the click queue never advances on the recipe screen.
 * <p>
 * Holding back or forward repeats, speeding up the longer it is held. GLFW's own key repeat is a fixed
 * rate, so it is ignored and the timing is driven from the client tick instead.
 */
public final class BreadcrumbKeys {
	private static final Logger LOGGER = JEIBreadcrumb.LOGGER;

	public static final String CATEGORY = "key.categories.jei_breadcrumb";

	private static final int UNBOUND = InputConstants.UNKNOWN.getValue();

	public static final KeyMapping BACK = new KeyMapping(
		"key.jei_breadcrumb.back", KeyConflictContext.GUI, InputConstants.Type.KEYSYM, UNBOUND, CATEGORY);
	public static final KeyMapping FORWARD = new KeyMapping(
		"key.jei_breadcrumb.forward", KeyConflictContext.GUI, InputConstants.Type.KEYSYM, UNBOUND, CATEGORY);
	public static final KeyMapping ROOT = new KeyMapping(
		"key.jei_breadcrumb.root", KeyConflictContext.GUI, InputConstants.Type.KEYSYM, UNBOUND, CATEGORY);

	/** How long the key must be held before it starts repeating. */
	private static final long REPEAT_DELAY_MS = 300L;
	/** Gap between the first two repeats. */
	private static final float REPEAT_INTERVAL_START_MS = 150.0F;
	/** The gap shrinks by this factor after every repeat, down to {@link #REPEAT_INTERVAL_MIN_MS}. */
	private static final float REPEAT_ACCELERATION = 0.75F;
	private static final float REPEAT_INTERVAL_MIN_MS = 30.0F;

	private static int heldKey = InputConstants.UNKNOWN.getValue();
	private static @Nullable Action heldAction;
	private static long nextRepeatAt;
	private static float repeatInterval = REPEAT_INTERVAL_START_MS;

	private BreadcrumbKeys() {

	}

	public static void register(RegisterKeyMappingsEvent event) {
		event.register(BACK);
		event.register(FORWARD);
		event.register(ROOT);
	}

	public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
		if (!(event.getScreen() instanceof RecipesGui)) {
			return;
		}

		int keyCode = event.getKeyCode();
		Action action = actionFor(keyCode, event.getScanCode());
		if (action == null) {
			return;
		}

		// Swallow the key so JEI does not also act on it if the player binds one of JEI's own keys.
		event.setCanceled(true);

		if (heldKey == keyCode) {
			// GLFW's key repeat; the tick handler owns the repeat timing instead.
			return;
		}

		heldKey = keyCode;
		heldAction = action;
		repeatInterval = REPEAT_INTERVAL_START_MS;
		nextRepeatAt = Util.getMillis() + REPEAT_DELAY_MS;
		LOGGER.debug("Key bound to {} pressed (key {})", action, keyCode);
		action.run();
	}

	/** Not filtered by screen: a key released after the recipe screen closed must still be forgotten. */
	public static void onKeyReleased(ScreenEvent.KeyReleased.Pre event) {
		if (event.getKeyCode() == heldKey) {
			heldKey = InputConstants.UNKNOWN.getValue();
			heldAction = null;
		}
	}

	public static void onClientTick(TickEvent.ClientTickEvent event) {
		// Forge fires this for both the start and the end of the tick; only the end matches what the
		// NeoForge build used to get from ClientTickEvent.Post.
		if (event.phase != TickEvent.Phase.END) {
			return;
		}

		Action action = heldAction;
		if (action == null || !action.REPEATABLE) {
			return;
		}
		long now = Util.getMillis();
		if (now < nextRepeatAt) {
			return;
		}
		if (!(Minecraft.getInstance().screen instanceof RecipesGui)) {
			return;
		}

		nextRepeatAt = now + (long) repeatInterval;
		repeatInterval = Math.max(REPEAT_INTERVAL_MIN_MS, repeatInterval * REPEAT_ACCELERATION);
		action.run();
	}

	private static @Nullable Action actionFor(int keyCode, int scanCode) {
		if (BACK.matches(keyCode, scanCode)) {
			return Action.BACK;
		}
		if (FORWARD.matches(keyCode, scanCode)) {
			return Action.FORWARD;
		}
		if (ROOT.matches(keyCode, scanCode)) {
			return Action.ROOT;
		}
		return null;
	}

	private enum Action {
		/** Going back or forward is worth holding down; jumping to the first step is not. */
		BACK(true),
		FORWARD(true),
		ROOT(false);

		private final boolean REPEATABLE;

		Action(boolean repeatable) {
			this.REPEATABLE = repeatable;
		}

		void run() {
			switch (this) {
				case BACK -> BreadcrumbHistory.back();
				case FORWARD -> BreadcrumbHistory.forward();
				case ROOT -> BreadcrumbHistory.goToRoot();
			}
		}
	}
}
