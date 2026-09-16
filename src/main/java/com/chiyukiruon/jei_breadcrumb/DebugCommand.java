package com.chiyukiruon.jei_breadcrumb;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * {@code /jeib debug enable|disable} -- turns this mod's debug logging on or off for the current run.
 * <p>
 * The setting lives in the log4j configuration rather than on disk, so it resets when the game
 * restarts. Both changes are echoed at INFO, which means the log file itself records the point where
 * debug output starts, so a report can be read back without guessing.
 * <p>
 * A client command: this mod is client-only, so there is nothing to ask a server for.
 */
public final class DebugCommand {
	/** The package every logger in this mod lives under. */
	private static final String LOGGER_NAME = "com.chiyukiruon.jei_breadcrumb";

	private static boolean enabled = false;

	private DebugCommand() {

	}

	public static void register(RegisterClientCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		dispatcher.register(
			Commands.literal("jeib")
				.then(Commands.literal("debug")
					.executes(DebugCommand::report)
					.then(Commands.literal("enable")
						.executes(context -> set(context, true)))
					.then(Commands.literal("disable")
						.executes(context -> set(context, false)))
				)
		);
		JEIBreadcrumb.LOGGER.debug("Registered the /jeib debug command");
	}

	private static int set(CommandContext<CommandSourceStack> context, boolean value) {
		enabled = value;
		Configurator.setLevel(LOGGER_NAME, value ? Level.DEBUG : Level.INFO);
		// Always INFO, so that turning it on is visible in the log it turns on.
		JEIBreadcrumb.LOGGER.info("Debug logging {}", value ? "enabled" : "disabled");
		context.getSource().sendSuccess(() -> message(value), false);
		return 1;
	}

	private static int report(CommandContext<CommandSourceStack> context) {
		context.getSource().sendSuccess(() -> message(enabled), false);
		return 1;
	}

	private static Component message(boolean value) {
		return Component.translatable(value
			? "jei_breadcrumb.command.debug.enabled"
			: "jei_breadcrumb.command.debug.disabled");
	}
}
