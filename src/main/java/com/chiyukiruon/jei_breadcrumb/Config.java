package com.chiyukiruon.jei_breadcrumb;

import net.neoforged.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Locale;
import java.util.regex.Pattern;

public final class Config {
	private static final Logger LOGGER = JEIBreadcrumb.LOGGER;

	private static final Pattern HEX_COLOR = Pattern.compile("#?[0-9a-fA-F]{6}");

	private static final int DEFAULT_RECIPE_COLOR = 0xFF4CAF50;
	private static final int DEFAULT_USES_COLOR = 0xFFE08A2E;

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	private static final ModConfigSpec.BooleanValue ENABLED = BUILDER
		.comment("Show the breadcrumb navigation bar at the top of the JEI recipe screen.")
		.define("enabled", true);

	private static final ModConfigSpec.ConfigValue<String> RECIPE_COLOR = BUILDER
		.comment(
			"Color of the \"recipe\" marker: a step looked up to see how to make that ingredient.",
			"Format: #RRGGBB. Used for both the icon outline and the tooltip label."
		)
		.define("recipeColor", "#4CAF50", Config::isHexColor);

	private static final ModConfigSpec.ConfigValue<String> USES_COLOR = BUILDER
		.comment(
			"Color of the \"uses\" marker: a step looked up to see what that ingredient is used for.",
			"Format: #RRGGBB. Used for both the icon outline and the tooltip label."
		)
		.define("usesColor", "#E08A2E", Config::isHexColor);

	public static final ModConfigSpec SPEC = BUILDER.build();

	public static boolean enabled = true;
	public static int recipeColor = DEFAULT_RECIPE_COLOR;
	public static int usesColor = DEFAULT_USES_COLOR;

	private Config() {

	}

	/** Registered on the mod event bus by {@link JEIBreadcrumb}; called again whenever the file changes. */
	static void onLoad(final ModConfigEvent event) {
		if (event.getConfig().getSpec() != SPEC) {
			return;
		}
		enabled = ENABLED.get();
		recipeColor = parseColor(RECIPE_COLOR.get(), DEFAULT_RECIPE_COLOR);
		usesColor = parseColor(USES_COLOR.get(), DEFAULT_USES_COLOR);

		String summary = "enabled=" + enabled + ", recipeColor=" + hex(recipeColor) + ", usesColor=" + hex(usesColor);
		if (event instanceof ModConfigEvent.Loading) {
			LOGGER.info("Config loaded: {}", summary);
		} else {
			LOGGER.debug("Config reloaded: {}", summary);
		}
	}

	private static boolean isHexColor(Object value) {
		return value instanceof String color && HEX_COLOR.matcher(color).matches();
	}

	/** @return the color as ARGB, fully opaque. */
	private static int parseColor(String value, int fallback) {
		String hex = value == null ? null : (value.startsWith("#") ? value.substring(1) : value);
		if (hex == null || hex.length() != 6) {
			LOGGER.warn("Color '{}' is not in #RRGGBB format, falling back to {}", value, hex(fallback));
			return fallback;
		}
		try {
			return 0xFF000000 | Integer.parseInt(hex, 16);
		} catch (NumberFormatException e) {
			LOGGER.warn("Color '{}' is not valid hex, falling back to {}", value, hex(fallback));
			return fallback;
		}
	}

	private static String hex(int argb) {
		return String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF);
	}
}
