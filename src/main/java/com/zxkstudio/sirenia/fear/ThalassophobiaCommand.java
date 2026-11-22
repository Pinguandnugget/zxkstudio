package com.zxkstudio.sirenia.fear;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zxkstudio.sirenia.config.SireniaConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

/** /talasofobia on | off | estado | curar <jugadores>  (solo operadores). */
public final class ThalassophobiaCommand {
	private ThalassophobiaCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("talasofobia")
					.requires(source -> source.hasPermissionLevel(2));

			root.then(CommandManager.literal("on").executes(ctx -> toggle(ctx.getSource(), true)));
			root.then(CommandManager.literal("off").executes(ctx -> toggle(ctx.getSource(), false)));
			root.then(CommandManager.literal("estado").executes(ctx -> {
				SireniaConfig cfg = SireniaConfig.get();
				ctx.getSource().sendFeedback(() -> Text.literal("Talasofobia: " + (cfg.thalassophobia_enabled ? "ACTIVADA" : "desactivada")
						+ " (abismo " + cfg.thalassophobia_depth_blocks + " bloques, " + cfg.thalassophobia_expose_seconds
						+ " s para que aparezca, " + cfg.thalassophobia_countdown_seconds + " s para reaccionar)."), false);
				return 1;
			}));
			root.then(CommandManager.literal("curar")
					.then(CommandManager.argument("jugadores", EntityArgumentType.players()).executes(ctx -> {
						Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "jugadores");
						for (ServerPlayerEntity player : players) {
							Thalassophobia.cure(player);
						}
						ctx.getSource().sendFeedback(() -> Text.literal("Talasofobia curada en " + players.size() + " jugador(es)."), true);
						return players.size();
					})));
			dispatcher.register(root);
		});
	}

	private static int toggle(ServerCommandSource source, boolean enabled) {
		SireniaConfig.get().thalassophobia_enabled = enabled;
		SireniaConfig.save();
		source.sendFeedback(() -> Text.literal(enabled
				? "Talasofobia ACTIVADA: el agua profunda empieza a dar miedo."
				: "Talasofobia desactivada (se cura a quien la tenga en el siguiente segundo)."), true);
		return 1;
	}
}
