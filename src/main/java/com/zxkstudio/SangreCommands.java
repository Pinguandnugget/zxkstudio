package com.zxkstudio;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * /sangrejurasica (alias /sj): control total de Sangre Jurásica desde el juego.
 * Requiere permisos de operador (nivel 2).
 */
public final class SangreCommands {
	private SangreCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, entorno) -> {
			LiteralArgumentBuilder<ServerCommandSource> raiz = CommandManager.literal("sangrejurasica")
					.requires(fuente -> fuente.hasPermissionLevel(2));

			raiz.then(CommandManager.literal("estado").executes(ctx -> {
				mostrarEstado(ctx.getSource());
				return 1;
			}));

			// --- valores numéricos
			entero(raiz, "tiempo", "Segundos hasta la fase terminal", 10, 86400,
					() -> SangreConfig.get().tiempoSegundos, v -> SangreConfig.get().tiempoSegundos = v);
			decimal(raiz, "dano", "Daño de la fase terminal (2.0 = 1 corazón)", 0.0f, 1000.0f,
					() -> SangreConfig.get().danoTerminal, v -> SangreConfig.get().danoTerminal = v);
			entero(raiz, "intervalo", "Segundos entre golpes de daño en fase terminal", 1, 3600,
					() -> SangreConfig.get().intervaloDanoSegundos, v -> SangreConfig.get().intervaloDanoSegundos = v);
			entero(raiz, "veneno", "Nivel de Veneno en fase terminal (0 = sin veneno)", 0, 10,
					() -> SangreConfig.get().nivelVeneno, v -> SangreConfig.get().nivelVeneno = v);
			entero(raiz, "debilidad", "Nivel de Debilidad (0 = apagada)", 0, 10,
					() -> SangreConfig.get().nivelDebilidad, v -> SangreConfig.get().nivelDebilidad = v);
			entero(raiz, "lentitud", "Nivel de Lentitud (0 = apagada)", 0, 10,
					() -> SangreConfig.get().nivelLentitud, v -> SangreConfig.get().nivelLentitud = v);
			entero(raiz, "radio", "Radio en bloques para provocar dinosaurios", 1, 128,
					() -> SangreConfig.get().radioProvocar, v -> SangreConfig.get().radioProvocar = v);

			// --- interruptores
			interruptor(raiz, "nausea", "Náuseas",
					() -> SangreConfig.get().nausea, v -> SangreConfig.get().nausea = v);
			interruptor(raiz, "oscuridad", "Oscuridad",
					() -> SangreConfig.get().oscuridad, v -> SangreConfig.get().oscuridad = v);
			interruptor(raiz, "latido", "Latido del corazón",
					() -> SangreConfig.get().latido, v -> SangreConfig.get().latido = v);
			interruptor(raiz, "particulas", "Partículas",
					() -> SangreConfig.get().particulas, v -> SangreConfig.get().particulas = v);
			interruptor(raiz, "mensajes", "Mensajes en el chat",
					() -> SangreConfig.get().mensajes, v -> SangreConfig.get().mensajes = v);
			interruptor(raiz, "provocar", "Dinosaurios hostiles atacan de lejos",
					() -> SangreConfig.get().provocarDinos, v -> SangreConfig.get().provocarDinos = v);

			// --- acciones sobre jugadores
			raiz.then(CommandManager.literal("infectar")
					.then(CommandManager.argument("jugadores", EntityArgumentType.players())
							.executes(ctx -> {
								Collection<ServerPlayerEntity> lista = EntityArgumentType.getPlayers(ctx, "jugadores");
								for (ServerPlayerEntity p : lista) {
									SangreJurasica.infectar(p);
								}
								ctx.getSource().sendFeedback(() -> Text.literal(
										"☠ Infectados: " + lista.size()), true);
								return lista.size();
							})));

			raiz.then(CommandManager.literal("curar")
					.then(CommandManager.argument("jugadores", EntityArgumentType.players())
							.executes(ctx -> {
								Collection<ServerPlayerEntity> lista = EntityArgumentType.getPlayers(ctx, "jugadores");
								int curados = 0;
								for (ServerPlayerEntity p : lista) {
									if (SangreJurasica.curar(p)) {
										curados++;
									}
								}
								int total = curados;
								ctx.getSource().sendFeedback(() -> Text.literal(
										"✔ Curados: " + total), true);
								return total;
							})));

			raiz.then(CommandManager.literal("antidoto")
					.then(CommandManager.argument("jugadores", EntityArgumentType.players())
							.executes(ctx -> {
								Collection<ServerPlayerEntity> lista = EntityArgumentType.getPlayers(ctx, "jugadores");
								for (ServerPlayerEntity p : lista) {
									ItemStack pocion = new ItemStack(ModContent.ANTIDOTO_AMBAR);
									p.getInventory().insertStack(pocion);
									if (!pocion.isEmpty()) {
										p.dropItem(pocion, false);
									}
								}
								ctx.getSource().sendFeedback(() -> Text.literal(
										"Antídoto de Ámbar entregado a " + lista.size() + " jugador(es)"), true);
								return lista.size();
							})));

			// --- configuración
			raiz.then(CommandManager.literal("recargar").executes(ctx -> {
				SangreConfig.cargar();
				ctx.getSource().sendFeedback(() -> Text.literal("Configuración recargada desde el archivo"), true);
				return 1;
			}));
			raiz.then(CommandManager.literal("restablecer").executes(ctx -> {
				SangreConfig.restablecer();
				ctx.getSource().sendFeedback(() -> Text.literal("Valores por defecto restablecidos"), true);
				return 1;
			}));

			LiteralCommandNode<ServerCommandSource> nodo = dispatcher.register(raiz);
			dispatcher.register(CommandManager.literal("sj")
					.requires(fuente -> fuente.hasPermissionLevel(2))
					.redirect(nodo));
		});
	}

	// ------------------------------------------------------------------ ayudantes

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
							SangreConfig.guardar();
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
							SangreConfig.guardar();
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
							SangreConfig.guardar();
							ctx.getSource().sendFeedback(() -> Text.literal(
									"✔ " + etiqueta + ": " + (valor ? "sí" : "no")), true);
							return valor ? 1 : 0;
						})));
	}

	private static void mostrarEstado(ServerCommandSource fuente) {
		SangreConfig c = SangreConfig.get();
		String texto = "§4§lSangre Jurásica§r\n"
				+ "§7tiempo hasta fase terminal: §f" + c.tiempoSegundos + " s"
				+ "\n§7daño terminal: §f" + c.danoTerminal + " cada " + c.intervaloDanoSegundos + " s"
				+ "\n§7veneno: §f" + c.nivelVeneno + " §7debilidad: §f" + c.nivelDebilidad
				+ " §7lentitud: §f" + c.nivelLentitud
				+ "\n§7náuseas: §f" + c.nausea + " §7oscuridad: §f" + c.oscuridad
				+ " §7latido: §f" + c.latido
				+ "\n§7partículas: §f" + c.particulas + " §7mensajes: §f" + c.mensajes
				+ "\n§7provocar dinos: §f" + c.provocarDinos + " §7radio: §f" + c.radioProvocar;
		fuente.sendFeedback(() -> Text.literal(texto), false);
	}
}
