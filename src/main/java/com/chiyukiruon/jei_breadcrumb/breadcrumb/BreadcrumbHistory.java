package com.chiyukiruon.jei_breadcrumb.breadcrumb;

import com.chiyukiruon.jei_breadcrumb.JEIBreadcrumb;
import mezz.jei.gui.recipes.IRecipeGuiLogic;
import mezz.jei.gui.recipes.lookups.ILookupState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The navigation path shown by the breadcrumb bar.
 * <p>
 * JEI keeps its own back/forward stacks ({@code NavigationHistory}) but does not expose them, so this
 * mirrors the same model with random access, which is what a breadcrumb needs. The two stay in sync
 * because both are driven by {@code RecipeGuiLogic.setState}:
 * <ul>
 *     <li>{@code saveHistory == true} (a real lookup) appends a step,</li>
 *     <li>{@code saveHistory == false} (JEI's own back/forward) is matched back onto the path.</li>
 * </ul>
 * Navigation therefore just delegates to {@code IRecipeGuiLogic#back()/forward()} and lets the hook
 * report where we ended up -- no private JEI methods are touched.
 */
public final class BreadcrumbHistory {
	private static final Logger LOGGER = JEIBreadcrumb.LOGGER;

	private static final List<BreadcrumbEntry> path = new ArrayList<>();
	private static int index = -1;
	private static @Nullable IRecipeGuiLogic logic;

	private BreadcrumbHistory() {

	}

	public static void setLogic(IRecipeGuiLogic recipeGuiLogic) {
		logic = recipeGuiLogic;
	}

	/**
	 * @param fromWithinRecipeGui true when the lookup was started by clicking inside the recipe screen,
	 *                            false when it came from anywhere else (the ingredient list sidebar,
	 *                            another screen) which starts a fresh path.
	 */
	public static void record(BreadcrumbEntry entry, boolean fromWithinRecipeGui) {
		if (fromWithinRecipeGui && index >= 0 && index < path.size()) {
			if (path.get(index).lookupKey().equals(entry.lookupKey())) {
				// Looking the same thing up again is not a step forward; just re-describe where we are.
				path.set(index, entry);
				LOGGER.debug("Same lookup as the current step, refreshed it instead of adding one: {} ({}/{})",
					entry.describe(), index + 1, path.size());
				return;
			}
			path.subList(index + 1, path.size()).clear();
		} else {
			path.clear();
		}
		path.add(entry);
		index = path.size() - 1;
		LOGGER.debug("Step {} of {} recorded, started from {}: {}",
			index + 1, path.size(),
			fromWithinRecipeGui ? "inside the recipe screen" : "outside, so the path restarted",
			entry.describe());
	}

	/**
	 * Finds the step a state belongs to when identity comparison fails -- JEI still has the state it
	 * recorded for a lookup we folded into the current step, so walking back onto it must land here
	 * rather than start a new path.
	 */
	public static int indexOfLookup(String lookupKey) {
		if (index >= 0 && index < path.size() && path.get(index).lookupKey().equals(lookupKey)) {
			return index;
		}
		for (int i = 0; i < path.size(); i++) {
			if (path.get(i).lookupKey().equals(lookupKey)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Finds a state JEI restored without recording it (its own back/forward) on the path.
	 * Identity comparison is deliberate: the entries hold the exact objects JEI recorded.
	 *
	 * @return the step index, or -1 when the state is not on the path at all -- which happens when
	 *         JEI's history still holds a state from a previous session, and the caller should then
	 *         start a fresh path.
	 */
	public static int indexOf(ILookupState state) {
		for (int i = 0; i < path.size(); i++) {
			if (path.get(i).state() == state) {
				return i;
			}
		}
		return -1;
	}

	public static void moveTo(int newIndex) {
		if (newIndex >= 0 && newIndex < path.size()) {
			index = newIndex;
		}
	}

	/**
	 * Refreshes a step's snapshot. The lookup state mutates in place as the user switches category or
	 * page, so a step revisited later needs to describe what it looks like now.
	 */
	public static void replace(int stepIndex, BreadcrumbEntry entry) {
		if (stepIndex >= 0 && stepIndex < path.size()) {
			path.set(stepIndex, entry);
		}
	}

	/**
	 * Re-describes the step the user is on. Used when the displayed recipe category changes, which
	 * happens without a history entry and would otherwise leave a stale category badge.
	 */
	public static void refreshCurrent(BreadcrumbEntry entry) {
		replace(index, entry);
	}

	public static void clear() {
		if (!path.isEmpty()) {
			LOGGER.debug("Path cleared, dropped {} step(s)", path.size());
		}
		path.clear();
		index = -1;
	}

	public static List<BreadcrumbEntry> entries() {
		return Collections.unmodifiableList(path);
	}

	public static int index() {
		return index;
	}

	public static boolean canGoBack() {
		return index > 0;
	}

	public static boolean canGoForward() {
		return index >= 0 && index < path.size() - 1;
	}

	public static boolean canGoToRoot() {
		return index > 0;
	}

	public static boolean back() {
		if (logic == null || !canGoBack() || !logic.back()) {
			return false;
		}
		LOGGER.debug("Went back to step {} of {}", index + 1, path.size());
		return true;
	}

	public static boolean forward() {
		if (logic == null || !canGoForward() || !logic.forward()) {
			return false;
		}
		LOGGER.debug("Went forward to step {} of {}", index + 1, path.size());
		return true;
	}

	public static boolean goToRoot() {
		return jumpTo(0);
	}

	/**
	 * Walks the path one step at a time using JEI's own navigation, so that the history JEI keeps
	 * stays consistent with what the bar shows.
	 */
	public static boolean jumpTo(int target) {
		if (logic == null || target < 0 || target >= path.size() || target == index) {
			return false;
		}
		int guard = path.size() + 1;
		while (index != target && guard-- > 0) {
			boolean moved = index > target ? logic.back() : logic.forward();
			if (!moved) {
				LOGGER.debug("Stuck walking from step {} to step {}, giving up", index + 1, target + 1);
				return false;
			}
		}
		LOGGER.debug("Jumped to step {} of {}", index + 1, path.size());
		return index == target;
	}
}
