package com.chiyukiruon.jei_breadcrumb.breadcrumb;

import com.chiyukiruon.jei_breadcrumb.Config;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.gui.recipes.lookups.ILookupState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One step of the navigation path, drawn as the looked-up ingredient with the recipe category that
 * produced it as a small badge in its bottom-right corner.
 * <p>
 * {@link ILookupState} is mutable and shared by reference: switching category or turning a page
 * rewrites the instance in place, and back/forward restore that exact instance. So everything needed
 * to draw is copied out at record time, and refreshed whenever the step's category changes.
 */
public final class BreadcrumbEntry {
	/** Side of the ingredient icon, and the height the bar lays its contents out in. */
	public static final int ICON_SIZE = 16;
	/** Half of {@link #ICON_SIZE}; the category badge drawn over the ingredient icon. */
	public static final int BADGE_SIZE = 8;
	private static final int BADGE_SCALE = 2;
	/**
	 * Items are rendered at z=100 ({@code GuiGraphics.renderItem} translates by 100 to sit above the
	 * GUI), so anything drawn over them has to go higher still.
	 */
	private static final float OVERLAY_Z = 200.0F;

	private final ILookupState state;
	/** Identifies what was looked up, so that looking the same thing up twice is not a new step. */
	private final String lookupKey;
	private final Role role;
	private final @Nullable ITypedIngredient<?> ingredient;
	private final @Nullable IIngredientRenderer<?> ingredientRenderer;
	private final @Nullable IDrawable categoryIcon;
	private final String ingredientName;
	private final String categoryName;

	private BreadcrumbEntry(
		ILookupState state,
		String lookupKey,
		Role role,
		@Nullable ITypedIngredient<?> ingredient,
		@Nullable IIngredientRenderer<?> ingredientRenderer,
		@Nullable IDrawable categoryIcon,
		String ingredientName,
		String categoryName
	) {
		this.state = state;
		this.lookupKey = lookupKey;
		this.role = role;
		this.ingredient = ingredient;
		this.ingredientRenderer = ingredientRenderer;
		this.categoryIcon = categoryIcon;
		this.ingredientName = ingredientName;
		this.categoryName = categoryName;
	}

	public static BreadcrumbEntry snapshot(IIngredientManager ingredientManager, ILookupState state) {
		IRecipeCategory<?> category = state.getFocusedRecipes().getRecipeCategory();
		IDrawable categoryIcon = category.getIcon();
		String categoryName = category.getTitle().getString();

		IFocusGroup focuses = state.getFocuses();
		String lookupKey = lookupKey(ingredientManager, focuses, categoryName);
		Role role = roleOf(focuses);

		// Normalized so the icon does not show a stack size: clicking an ingredient of a recipe that
		// outputs several of them would otherwise draw the count over the category badge.
		ITypedIngredient<?> ingredient = focuses.getAllFocuses().stream()
			.map(IFocus::getTypedValue)
			.map(typed -> normalize(ingredientManager, typed))
			.findFirst()
			.orElse(null);

		if (ingredient == null) {
			return new BreadcrumbEntry(state, lookupKey, role, null, null, categoryIcon, "", categoryName);
		}

		return new BreadcrumbEntry(
			state,
			lookupKey,
			role,
			ingredient,
			ingredientRenderer(ingredientManager, ingredient),
			categoryIcon,
			displayName(ingredientManager, ingredient),
			categoryName
		);
	}

	/**
	 * Which way round the lookup is. JEI looks up recipes for an item with an {@code OUTPUT} focus and
	 * uses of it with {@code INPUT}/{@code CATALYST} focuses; browsing a whole category has neither.
	 */
	private static Role roleOf(IFocusGroup focuses) {
		Role role = Role.NONE;
		for (IFocus<?> focus : focuses.getAllFocuses()) {
			if (focus.getRole() == RecipeIngredientRole.OUTPUT) {
				return Role.OUTPUT;
			}
			role = Role.USES;
		}
		return role;
	}

	/**
	 * What this step looks up. Uses the ingredient's JEI uid rather than the focus roles, so that
	 * clicking the same item with the other mouse button still counts as the same step.
	 * <p>
	 * A lookup with no focus at all (browsing whole recipe categories) falls back to the category, so
	 * different categories stay distinct.
	 */
	private static String lookupKey(IIngredientManager ingredientManager, IFocusGroup focuses, String categoryName) {
		List<String> parts = new ArrayList<>();
		for (IFocus<?> focus : focuses.getAllFocuses()) {
			parts.add(uniqueId(ingredientManager, focus.getTypedValue()));
		}
		if (parts.isEmpty()) {
			return "category|" + categoryName;
		}
		Collections.sort(parts);
		return String.join("|", parts);
	}

	/** The state this step restores. Only use it to jump; it mutates as the user browses. */
	public ILookupState state() {
		return state;
	}

	public String lookupKey() {
		return lookupKey;
	}

	/** One-line description for debug logs, e.g. {@code OUTPUT [Crafting]Iron Ingot}. */
	public String describe() {
		return role + " [" + categoryName + "]" + ingredientName;
	}

	public List<Component> tooltipLines() {
		Component title = Component.literal("[" + categoryName + "]" + ingredientName);
		if (role == Role.NONE) {
			return List.of(title);
		}
		Style style = Style.EMPTY
			.withColor(TextColor.fromRgb(role.color() & 0xFFFFFF))
			.withItalic(true);
		return List.of(title, Component.translatable(role.translationKey()).withStyle(style));
	}

	public void draw(GuiGraphics guiGraphics, int x, int y) {
		drawIngredient(guiGraphics, x, y);
		if (categoryIcon != null) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(x + BADGE_SIZE, y + BADGE_SIZE, OVERLAY_Z);
			guiGraphics.pose().scale(1.0F / BADGE_SCALE, 1.0F / BADGE_SCALE, 1.0F);
			categoryIcon.draw(guiGraphics, 0, 0);
			guiGraphics.pose().popPose();
		}
		drawOutline(guiGraphics, x, y);
	}

	/**
	 * Traces the icon so the role is visible at a glance. Drawn outside the 16x16 icon, so it never
	 * covers the ingredient or the category badge.
	 */
	private void drawOutline(GuiGraphics guiGraphics, int x, int y) {
		if (role == Role.NONE) {
			return;
		}
		int color = role.color();
		int end = x + ICON_SIZE;
		int bottom = y + ICON_SIZE;

		guiGraphics.fill(x - 1, y - 1, end + 1, y, color);
		guiGraphics.fill(x - 1, bottom, end + 1, bottom + 1, color);
		guiGraphics.fill(x - 1, y, x, bottom, color);
		guiGraphics.fill(end, y, end + 1, bottom, color);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void drawIngredient(GuiGraphics guiGraphics, int x, int y) {
		if (ingredient == null || ingredientRenderer == null) {
			return;
		}
		((IIngredientRenderer) ingredientRenderer).render(guiGraphics, ingredient.getIngredient(), x, y);
	}

	/** Drops the stack size and anything else that only matters for a specific lookup. */
	private static ITypedIngredient<?> normalize(IIngredientManager ingredientManager, ITypedIngredient<?> typedIngredient) {
		return ingredientManager.normalizeTypedIngredient(typedIngredient);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static String uniqueId(IIngredientManager ingredientManager, ITypedIngredient<?> typedIngredient) {
		IIngredientHelper helper = ingredientManager.getIngredientHelper((IIngredientType) typedIngredient.getType());
		Object uid = helper.getUid(typedIngredient.getIngredient(), UidContext.Ingredient);
		return uid != null ? uid.toString() : displayName(ingredientManager, typedIngredient);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static String displayName(IIngredientManager ingredientManager, ITypedIngredient<?> typedIngredient) {
		IIngredientHelper helper = ingredientManager.getIngredientHelper((IIngredientType) typedIngredient.getType());
		return helper.getDisplayName(typedIngredient.getIngredient());
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static IIngredientRenderer<?> ingredientRenderer(IIngredientManager ingredientManager, ITypedIngredient<?> typedIngredient) {
		return ingredientManager.getIngredientRenderer((IIngredientType) typedIngredient.getType());
	}

	/** Which way round the lookup is, which decides the outline color and the tooltip label. */
	private enum Role {
		OUTPUT("jei_breadcrumb.role.output") {
			@Override
			int color() {
				return Config.recipeColor;
			}
		},
		USES("jei_breadcrumb.role.uses") {
			@Override
			int color() {
				return Config.usesColor;
			}
		},
		/** Browsing a whole category: no role to show. */
		NONE("") {
			@Override
			int color() {
				return 0;
			}
		};

		private final String translationKey;

		Role(String translationKey) {
			this.translationKey = translationKey;
		}

		String translationKey() {
			return translationKey;
		}

		abstract int color();
	}
}
