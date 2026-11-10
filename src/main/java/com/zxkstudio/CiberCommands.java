package com.zxkstudio;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * /ciberpsicosis (alias /cp): control de Cyberpsychosis. Requiere operador (nivel 2).
 * /inmunosupresores: cualquier jugador puede ver su estado.
 * No hay comando para activar o desactivar el evento: siempre está activo.
 */
public final class CiberCommands {
	private CiberCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, entorno) -> {
			dispatcher.register(CommandManager.literal("inmunosupresores").executes(ctx -> {
				ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
				CiberPsicosis.consultar(p);
				return 1;
			}));

			LiteralArgumentBuilder<ServerCommandSource> raiz = CommandManager.literal("ciberpsicosis")
					.requires(fuente -> fuente.hasPermissionLevel(2));

			raiz.then(CommandManager.literal("estado").executes(ctx -> {
				mostrarEstado(ctx.getSource());
				return 1;
			}));

			// --- acciones sobre jugadores
			raiz.then(CommandManager.literal("entrar")
					.then(CommandManager.argument("jugadores", EntityArgumentType.players())
							.executes(ctx -> {
								Collection<ServerPlayerEntity> lista = EntityArgumentType.getPlayers(ctx, "jugadores");
								for (ServerPlayerEntity p : lista) {
									CiberPsicosis.forzarPsicosis(p);
								}
								ctx.getSource().sendFeedback(() -> Text.literal(
										"☠ En cyberpsychosis: " + lista.size()), true);
								return lista.size();
							})));

			raiz.then(CommandManager.literal("curar")
					.then(CommandManager.argument("jugadores", EntityArgumentType.players())
							.executes(ctx -> {
								Collection<ServerPlayerEntity> lista = EntityArgumentType.getPlayers(ctx, "jugadores");
								for (ServerPlayerEntity p : lista) {
									CiberPsicosis.curarForzado(p);
								}
								ctx.getSource().sendFeedback(() -> Text.literal("✔ Curados: " + lista.size()), true);
								return lista.size();
							})));

			raiz.then(CommandManager.literal("reiniciar")
					.then(CommandManager.argument("jugadores", EntityArgumentType.players())
							.executes(ctx -> {
								Collection<ServerPlayerEntity> lista = EntityArgumentType.getPlayers(ctx, "jugadores");
								for (ServerPlayerEntity p : lista) {
									CiberPsicosis.reiniciar(p);
								}
								ctx.getSource().sendFeedback(() -> Text.literal(
										"Reloj reiniciado: " + lista.size()), true);
								return lista.size();
							})));

			raiz.then(CommandManager.literal("reloj")
					.then(CommandManager.argument("jugadores", EntityArgumentType.players())
							.then(CommandManager.argument("segundos", IntegerArgumentType.integer(1, 86400))
									.executes(ctx -> {
										Collection<ServerPlayerEntity> lista = EntityArgumentType.getPlayers(ctx, "jugadores");
										int s = IntegerArgumentType.getInteger(ctx, "segundos");
										for (ServerPlayerEntity p : lista) {
											CiberPsicosis.fijarReloj(p, s);
										}
										ctx.getSource().sendFeedback(() -> Text.literal(
												"Reloj fijado a " + s + " s para " + lista.size() + " jugador(es)"), true);
										return lista.size();
									}))));

			raiz.then(CommandManager.literal("dar")
					.then(CommandManager.literal("inmunosupresor")
							.then(darItem(true)))
					.then(CommandManager.literal("bloqueador")
							.then(darItem(false))));

			// --- modo
			raiz.then(CommandManager.literal("modo")
					.executes(ctx -> {
						boolean solo = CiberConfig.get().modoSoloBajas;
						ctx.getSource().sendFeedback(() -> Text.literal("Modo actual: " + nombreModo(solo)), false);
						return solo ? 1 : 0;
					})
					.then(CommandManager.literal("normal").executes(ctx -> {
						cambiarModo(ctx.getSource(), false);
						return 0;
					}))
					.then(CommandManager.literal("solo_bajas").executes(ctx -> {
						cambiarModo(ctx.getSource(), true);
						return 1;
					})));

			// --- valores
			entero(raiz, "tiempo", "Segundos entre inmunosupresores y cyberpsychosis", 30, 86400,
					() -> CiberConfig.get().tiempoSegundos, v -> CiberConfig.get().tiempoSegundos = v);
			entero(raiz, "bajas", "Bajas necesarias para estabilizarse", 1, 100,
					() -> CiberConfig.get().bajasNecesarias, v -> CiberConfig.get().bajasNecesarias = v);
			decimal(raiz, "dano", "Daño de cada pop (2.0 = 1 corazón, 1000 = muerte como el datapack)", 0.0f, 1000.0f,
					() -> CiberConfig.get().danoPop, v -> CiberConfig.get().danoPop = v);
			entero(raiz, "popmin", "Segundos mínimos entre pops", 1, 600,
					() -> CiberConfig.get().popMinSegundos, v -> CiberConfig.get().popMinSegundos = v);
			entero(raiz, "popmax", "Segundos máximos entre pops", 1, 600,
					() -> CiberConfig.get().popMaxSegundos, v -> CiberConfig.get().popMaxSegundos = v);
			entero(raiz, "bloqueador", "Segundos de inmunidad del Bloqueador Neural", 10, 86400,
					() -> CiberConfig.get().bloqueadorSegundos, v -> CiberConfig.get().bloqueadorSegundos = v);

			// --- efectos visuales
			interruptor(raiz, "overlays", "Pantallazos de glitch",
					() -> CiberConfig.get().overlays, v -> CiberConfig.get().overlays = v);
			interruptor(raiz, "figuras", "Figuras alucinadas",
					() -> CiberConfig.get().figuras, v -> CiberConfig.get().figuras = v);
			interruptor(raiz, "chatfalso", "Mensajes falsos en el chat",
					() -> CiberConfig.get().chatFalso, v -> CiberConfig.get().chatFalso = v);
			interruptor(raiz, "sacudidas", "Sacudidas de cámara",
					() -> CiberConfig.get().sacudidas, v -> CiberConfig.get().sacudidas = v);

			// --- configuración
			raiz.then(CommandManager.literal("recargar").executes(ctx -> {
				CiberConfig.cargar();
				ctx.getSource().sendFeedback(() -> Text.literal("Configuración recargada desde el archivo"), true);
				return 1;
			}));
			raiz.then(CommandManager.literal("restablecer").executes(ctx -> {
				CiberConfig.restablecer();
				ctx.getSource().sendFeedback(() -> Text.literal("Valores por defecto restablecidos"), true);
				return 1;
			}));

			LiteralCommandNode<ServerCommandSource> nodo = dispatcher.register(raiz);
			dispatcher.register(CommandManager.literal("cp")
					.requires(fuente -> fuente.hasPermissionLevel(2))
					.redirect(nodo));
		});
	}

	// ------------------------------------------------------------------ ayudantes

	private static com.mojang.brigadier.builder.RequiredArgumentBuilder<ServerCommandSource, ?> darItem(
			boolean inmunosupresor) {
		return CommandManager.argument("jugadores", EntityArgumentType.players())
				.executes(ctx -> dar(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "jugadores"), 1,
						inmunosupresor))
				.then(CommandManager.argument("cantidad", IntegerArgumentType.integer(1, 64))
						.executes(ctx -> dar(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "jugadores"),
								IntegerArgumentType.getInteger(ctx, "cantidad"), inmunosupresor)));
	}

	private static int dar(ServerCommandSource fuente, Collection<ServerPlayerEntity> lista, int cantidad,
			boolean inmunosupresor) {
		Item item = inmunosupresor ? ModContent.INMUNOSUPRESOR : ModContent.BLOQUEADOR_NEURAL;
		for (ServerPlayerEntity p : lista) {
			ItemStack pila = new ItemStack(item, cantidad);
			p.getInventory().insertStack(pila);
			if (!pila.isEmpty()) {
				p.dropItem(pila, false);
			}
		}
		fuente.sendFeedback(() -> Text.literal(
				(inmunosupresor ? "Inmunosupresor" : "Bloqueador Neural") + " x" + cantidad
						+ " entregado a " + lista.size() + " jugador(es)"), true);
		return lista.size();
	}

	private static String nombreModo(boolean soloBajas) {
		return soloBajas
				? "solo_bajas (los inmunosupresores NO curan la cyberpsychosis; solo las bajas)"
				: "normal (los inmunosupresores curan)";
	}

	private static void cambiarModo(ServerCommandSource fuente, boolean soloBajas) {
		CiberConfig.get().modoSoloBajas = soloBajas;
		CiberConfig.guardar();
		fuente.sendFeedback(() -> Text.literal("✔ Modo: " + nombreModo(soloBajas)), true);
	}

	private static void entero(LiteralArgumentBuilder<ServerCommandSource> raiz, String nombre, String etiqueta,
			int min, int max, IntSupplier lector, IntConsumer escritor) {
		raiz.then(CommandManager.literal(nombre)
				.executes(ctx -> {
					int actual = lector.getAsInt();
					ctx.getSource().sendFeedback(() -> Text.literal(etiqueta + ": " + actual), false);
					return actual;
				})
				.then(CommandManager.argument("valor", IntegerArgumentType.integer(min, max))
						.executes(ctx -> {
							int valor = IntegerArgumentType.getInteger(ctx, "valor");
							escritor.accept(valor);
							CiberConfig.guardar();
							ctx.getSource().sendFeedback(() -> Text.literal("✔ " + etiqueta + " = " + valor), true);
							return valor;
						})));
	}

	private static void decimal(LiteralArgumentBuilder<ServerCommandSource> raiz, String nombre, String etiqueta,
			float min, float max, Supplier<Float> lector, Consumer<Float> escritor) {
		raiz.then(CommandManager.literal(nombre)
				.executes(ctx -> {
					float actual = lector.get();
					ctx.getSource().sendFeedback(() -> Text.literal(etiqueta + ": " + actual), false);
					return Math.round(actual);
				})
				.then(CommandManager.argument("valor", FloatArgumentType.floatArg(min, max))
						.executes(ctx -> {
							float valor = FloatArgumentType.getFloat(ctx, "valor");
							escritor.accept(valor);
							CiberConfig.guardar();
							ctx.getSource().sendFeedback(() -> Text.literal("✔ " + etiqueta + " = " + valor), true);
							return Math.round(valor);
						})));
	}

	private static void interruptor(LiteralArgumentBuilder<ServerCommandSource> raiz, String nombre, String etiqueta,
			BooleanSupplier lector, Consumer<Boolean> escritor) {
		raiz.then(CommandManager.literal(nombre)
				.executes(ctx -> {
					boolean actual = lector.getAsBoolean();
					ctx.getSource().sendFeedback(() -> Text.literal(etiqueta + ": " + (actual ? "sí" : "no")), false);
					return actual ? 1 : 0;
				})
				.then(CommandManager.argument("valor", BoolArgumentType.bool())
						.executes(ctx -> {
							boolean valor = BoolArgumentType.getBool(ctx, "valor");
							escritor.accept(valor);
							CiberConfig.guardar();
							ctx.getSource().sendFeedback(() -> Text.literal(
									"✔ " + etiqueta + ": " + (valor ? "sí" : "no")), true);
							return valor ? 1 : 0;
						})));
	}

	private static void mostrarEstado(ServerCommandSource fuente) {
		CiberConfig c = CiberConfig.get();
		String texto = "§4§lCyberpsychosis§r\n"
				+ "§7modo: §f" + (c.modoSoloBajas ? "solo_bajas" : "normal")
				+ "\n§7reloj: §f" + c.tiempoSegundos + " s §7bajas: §f" + c.bajasNecesarias
				+ "\n§7daño pop: §f" + c.danoPop + " cada " + c.popMinSegundos + "-" + c.popMaxSegundos + " s"
				+ "\n§7bloqueador: §f" + c.bloqueadorSegundos + " s"
				+ "\n§7overlays: §f" + c.overlays + " §7figuras: §f" + c.figuras
				+ " §7chat falso: §f" + c.chatFalso + " §7sacudidas: §f" + c.sacudidas;
		fuente.sendFeedback(() -> Text.literal(texto), false);
	}
}
