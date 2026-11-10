package com.zxkstudio;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Lógica de Cyberpsychosis. Reemplaza las funciones del datapack ciberpsicosis.
 *
 * Cada jugador tiene un reloj de inmunosupresores. Si se agota entra en cyberpsychosis y
 * se estabiliza matando entidades (bajas) o, en modo normal, con un Inmunosupresor.
 * Siempre está activa; los jugadores en creativo o espectador no se ven afectados.
 */
public final class CiberPsicosis {
	private CiberPsicosis() {
	}

	private static final Random RNG = new Random();

	/** Figuras alucinadas que ve cada jugador (solo él las ve). */
	private static final Map<UUID, List<Figura>> FIGURAS = new HashMap<>();

	private static final class Figura {
		final double x;
		final double y;
		final double z;
		final int tipo;
		int restante = 45;

		Figura(double x, double y, double z, int tipo) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.tipo = tipo;
		}
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(CiberPsicosis::alTick);
		ServerLivingEntityEvents.AFTER_DEATH.register(CiberPsicosis::alMorirEntidad);
		ServerPlayerEvents.COPY_FROM.register(CiberPsicosis::alCopiarJugador);
	}

	// ------------------------------------------------------------------ estado

	public static CiberEstado estado(ServerPlayerEntity jugador) {
		CiberEstado e = jugador.getAttached(ModContent.CIBER_ESTADO);
		if (e == null) {
			e = new CiberEstado(CiberConfig.get().tiempoTicks(), 0, 0, 0, 0, 0);
			jugador.setAttached(ModContent.CIBER_ESTADO, e);
		}
		return e;
	}

	// ------------------------------------------------------------------ eventos

	private static void alTick(MinecraftServer servidor) {
		CiberConfig cfg = CiberConfig.get();
		for (ServerPlayerEntity jugador : new ArrayList<>(servidor.getPlayerManager().getPlayerList())) {
			boolean enPsicosis = false;
			if (!jugador.isCreative() && !jugador.isSpectator()
					&& !jugador.getCommandTags().contains("cp_inmune")) {
				CiberEstado e = estado(jugador);
				if (e.inm > 0) {
					inmuneTick(jugador, e, cfg);
				} else if (e.est == 0) {
					normal(jugador, e, cfg);
				} else {
					psicosis(jugador, e, cfg);
				}
				jugador.setAttached(ModContent.CIBER_ESTADO, e);
				enPsicosis = e.est == 1;
			}
			Alucinacion.tick(jugador, enPsicosis);
			dibujarFiguras(jugador);
		}
	}

	/** Cada baja cuenta para quien está en cyberpsychosis. */
	private static void alMorirEntidad(LivingEntity entidad, DamageSource fuente) {
		Entity atacante = fuente.getAttacker();
		if (!(atacante instanceof ServerPlayerEntity jugador) || atacante == entidad) {
			return;
		}
		CiberEstado e = jugador.getAttached(ModContent.CIBER_ESTADO);
		if (e == null || e.est != 1) {
			return;
		}
		e.kills++;
		sonido(jugador, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 0.6f);
		jugador.sendMessage(barraBajas(e), true);
	}

	/** Al morir: pierde la cyberpsychosis y el reloj vuelve a empezar con 30 s de regalo. */
	private static void alCopiarJugador(ServerPlayerEntity viejo, ServerPlayerEntity nuevo, boolean vivo) {
		Alucinacion.olvidar(viejo.getUuid());
		CiberEstado anterior = viejo.getAttached(ModContent.CIBER_ESTADO);
		if (anterior == null) {
			return;
		}
		if (vivo) {
			if (nuevo.getAttached(ModContent.CIBER_ESTADO) == null) {
				nuevo.setAttached(ModContent.CIBER_ESTADO, anterior);
			}
			return;
		}
		if (anterior.est == 1 && nuevo.getServer() != null) {
			nuevo.getServer().getPlayerManager().broadcast(Text.empty()
					.append(Text.literal("☠ ").formatted(Formatting.DARK_RED))
					.append(nuevo.getDisplayName().copy().formatted(Formatting.RED))
					.append(Text.literal(" sucumbió a la cyberpsychosis.")
							.formatted(Formatting.DARK_RED, Formatting.ITALIC)), false);
		}
		FIGURAS.remove(nuevo.getUuid());
		nuevo.setAttached(ModContent.CIBER_ESTADO,
				new CiberEstado(CiberConfig.get().tiempoTicks() + 600, 0, 0, 0, 0, anterior.inm));
	}

	// ------------------------------------------------------------------ fases

	private static void normal(ServerPlayerEntity p, CiberEstado e, CiberConfig cfg) {
		e.t--;
		if (e.t > 0 && e.t % 20 == 0 && !p.hasStatusEffect(ModContent.RELOJ_EFECTO)) {
			relojEfecto(p, e);
		}
		if (e.t == 300) {
			avisoFinal(p);
		}
		if (e.t >= 1 && e.t <= 300) {
			alucinar(p, e, cfg);
		}
		if (e.t <= 0) {
			entrarPsicosis(p, e, cfg);
		}
	}

	private static void inmuneTick(ServerPlayerEntity p, CiberEstado e, CiberConfig cfg) {
		e.inm--;
		e.t = cfg.tiempoTicks();
		if (e.inm == 0) {
			p.sendMessage(Text.empty()
					.append(prefijo())
					.append(Text.literal("Tu Bloqueador Neural se agotó. El reloj de inmunosupresores vuelve a correr.")
							.formatted(Formatting.GRAY)));
			sonido(p, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.MASTER, 1.0f, 1.0f);
			relojEfecto(p, e);
		}
	}

	private static void avisoFinal(ServerPlayerEntity p) {
		p.sendMessage(Text.empty()
				.append(Text.literal("[Neural] ").formatted(Formatting.DARK_RED, Formatting.BOLD))
				.append(Text.literal("Los inmunosupresores se agotan... el cableado te quema por dentro.")
						.formatted(Formatting.RED, Formatting.ITALIC)));
		sonido(p, SoundEvents.AMBIENT_CAVE, SoundCategory.MASTER, 2.0f, 0.6f);
		efecto(p, StatusEffects.NAUSEA, 400, 0);
		efecto(p, StatusEffects.DARKNESS, 240, 0);
	}

	/** Últimos 15 segundos: errores, sonidos y sustos. */
	private static void alucinar(ServerPlayerEntity p, CiberEstado e, CiberConfig cfg) {
		int m20 = e.t % 20;
		if (m20 == 0) {
			sonido(p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.MASTER, 2.0f, 0.9f);
			p.sendMessage(Text.empty()
					.append(Text.literal("▌ERROR ").formatted(Formatting.RED, Formatting.OBFUSCATED))
					.append(Text.literal("INMUNOSUPRESORES AGOTADOS").formatted(Formatting.DARK_RED, Formatting.BOLD))
					.append(Text.literal(" ERROR▐").formatted(Formatting.RED, Formatting.OBFUSCATED)), true);
		}
		if (m20 == 5) {
			evento(p, cfg);
		}
		if (m20 == 10 && e.t <= 180) {
			sonido(p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.MASTER, 2.0f, 1.15f);
		}
		if (m20 == 15 && e.t <= 100) {
			evento(p, cfg);
		}
		if (e.t % 30 == 0 || (e.t <= 100 && e.t % 30 == 15)) {
			overlay(p, 0, cfg);
		}
		if (e.t <= 120 && m20 == 12) {
			sacudida(p, cfg);
		}
	}

	public static void entrarPsicosis(ServerPlayerEntity p, CiberEstado e, CiberConfig cfg) {
		e.est = 1;
		e.kills = 0;
		e.pt = 0;
		e.t = 0;
		Text subtitulo = cfg.modoSoloBajas
				? Text.literal("Mata " + cfg.bajasNecesarias + " para estabilizarte").formatted(Formatting.RED)
				: Text.literal("Mata o toma un Inmunosupresor").formatted(Formatting.RED);
		titulo(p, 5, 50, 15,
				Text.literal("CYBERPSYCHOSIS").formatted(Formatting.DARK_RED, Formatting.BOLD), subtitulo);
		sonido(p, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.MASTER, 0.8f, 1.5f);
		if (p.getServer() != null) {
			p.getServer().getPlayerManager().broadcast(Text.empty()
					.append(Text.literal("☠ ").formatted(Formatting.DARK_RED))
					.append(p.getDisplayName().copy().formatted(Formatting.RED))
					.append(Text.literal(" ha entrado en cyberpsychosis.").formatted(Formatting.DARK_RED)), false);
		}
		pop(p, e, cfg);
		p.removeStatusEffect(ModContent.RELOJ_EFECTO);
	}

	private static void psicosis(ServerPlayerEntity p, CiberEstado e, CiberConfig cfg) {
		e.pt++;
		if (e.kills >= cfg.bajasNecesarias) {
			curarPorBajas(p, e, cfg);
		}
		if (e.est == 1) {
			psicosisFx(p, e, cfg);
		}
	}

	private static void psicosisFx(ServerPlayerEntity p, CiberEstado e, CiberConfig cfg) {
		e.pop--;
		if (e.pop <= 0) {
			pop(p, e, cfg);
		}
		if (Alucinacion.enEscena(p)) {
			return; // durante la alucinación mandan sus propios sonidos y sustos
		}
		int m20 = e.pt % 20;
		if (m20 == 0) {
			sonido(p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.MASTER, 2.0f, 1.3f);
			p.sendMessage(barraBajas(e), true);
		}
		if (m20 == 4 || m20 == 12) {
			evento(p, cfg);
		}
		if (e.pt % 15 == 0) {
			overlay(p, 4, cfg);
		}
		if (e.pt % 50 == 25) {
			sacudida(p, cfg);
		}
		if (e.pt % 100 == 50) {
			aplicarEfectos(p);
		}
	}

	/** Descarga de la cyberpsychosis: daño y un golpe de efectos; la siguiente llega al azar. */
	private static void pop(ServerPlayerEntity p, CiberEstado e, CiberConfig cfg) {
		if (cfg.danoPop > 0.0f) {
			p.damage(p.getServerWorld().getDamageSources().magic(), cfg.danoPop);
		}
		aplicarEfectos(p);
		int min = cfg.popMinSegundos * 20;
		int max = cfg.popMaxSegundos * 20;
		e.pop = min + RNG.nextInt(max - min + 1);
	}

	static void aplicarEfectos(ServerPlayerEntity p) {
		efecto(p, StatusEffects.NAUSEA, 240, 0);
		efecto(p, StatusEffects.STRENGTH, 240, 0);
		efecto(p, StatusEffects.SPEED, 240, 0);
		efecto(p, StatusEffects.DARKNESS, 120, 0);
	}

	// ------------------------------------------------------------------ curación

	/** Reinicia el reloj y quita la cyberpsychosis. */
	public static void curar(ServerPlayerEntity p, CiberEstado e, CiberConfig cfg) {
		e.t = cfg.tiempoTicks();
		e.est = 0;
		e.kills = 0;
		e.pt = 0;
		p.removeStatusEffect(StatusEffects.NAUSEA);
		p.removeStatusEffect(StatusEffects.DARKNESS);
		p.removeStatusEffect(StatusEffects.STRENGTH);
		p.removeStatusEffect(StatusEffects.SPEED);
		p.networkHandler.sendPacket(new ClearTitleS2CPacket(false));
		FIGURAS.remove(p.getUuid());
		relojEfecto(p, e);
	}

	private static void curarPorBajas(ServerPlayerEntity p, CiberEstado e, CiberConfig cfg) {
		curar(p, e, cfg);
		titulo(p, 5, 40, 15,
				Text.literal("ESTABILIZADO").formatted(Formatting.GREEN, Formatting.BOLD),
				Text.literal("Sangre fría. Tu sistema se estabiliza.").formatted(Formatting.GREEN));
		sonido(p, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 0.7f);
		sonido(p, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 1.0f, 1.2f);
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 160, 1, false, false, false));
		if (p.getServer() != null) {
			p.getServer().getPlayerManager().broadcast(Text.empty()
					.append(p.getDisplayName().copy().formatted(Formatting.GOLD))
					.append(Text.literal(" retomó el control tras la cyberpsychosis.").formatted(Formatting.YELLOW)),
					false);
		}
	}

	/** En el modo "solo_bajas" los ítems no curan a quien está en cyberpsychosis. */
	public static boolean bloqueaItems(ServerPlayerEntity p) {
		return CiberConfig.get().modoSoloBajas && estado(p).est == 1;
	}

	public static void rechazarItem(ServerPlayerEntity p) {
		CiberEstado e = estado(p);
		p.sendMessage(Text.empty()
				.append(prefijo())
				.append(Text.literal("Tu cableado rechaza el inmunosupresor. Solo las bajas te estabilizarán: ")
						.formatted(Formatting.RED))
				.append(Text.literal(e.kills + "/" + CiberConfig.get().bajasNecesarias).formatted(Formatting.WHITE)));
		sonido(p, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.MASTER, 1.0f, 0.7f);
	}

	public static void usarInmunosupresor(ServerPlayerEntity p) {
		curar(p, estado(p), CiberConfig.get());
		p.sendMessage(Text.empty()
				.append(prefijo())
				.append(Text.literal("Inmunosupresor inyectado. Tu reloj se reinició.").formatted(Formatting.AQUA)));
		sonido(p, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 1.0f, 1.3f);
	}

	public static void usarBloqueador(ServerPlayerEntity p) {
		CiberConfig cfg = CiberConfig.get();
		CiberEstado e = estado(p);
		curar(p, e, cfg);
		e.inm = cfg.bloqueadorSegundos * 20;
		p.removeStatusEffect(ModContent.RELOJ_EFECTO);
		p.sendMessage(Text.empty()
				.append(prefijo())
				.append(Text.literal("Bloqueador Neural activo: " + SangreJurasica.formatoTiempo(cfg.bloqueadorSegundos)
						+ " de inmunidad.").formatted(Formatting.LIGHT_PURPLE)));
		sonido(p, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 1.0f, 0.8f);
	}

	// ------------------------------------------------------------------ acciones de comandos

	public static void forzarPsicosis(ServerPlayerEntity p) {
		CiberEstado e = estado(p);
		e.inm = 0;
		entrarPsicosis(p, e, CiberConfig.get());
	}

	public static void curarForzado(ServerPlayerEntity p) {
		curar(p, estado(p), CiberConfig.get());
	}

	public static void reiniciar(ServerPlayerEntity p) {
		CiberEstado e = estado(p);
		e.inm = 0;
		e.pop = 0;
		curar(p, e, CiberConfig.get());
	}

	public static void fijarReloj(ServerPlayerEntity p, int segundos) {
		CiberEstado e = estado(p);
		e.est = 0;
		e.kills = 0;
		e.pt = 0;
		e.inm = 0;
		e.t = Math.max(1, segundos * 20);
		relojEfecto(p, e);
	}

	/** Respuesta de /inmunosupresores (la puede usar cualquiera). */
	public static void consultar(ServerPlayerEntity p) {
		CiberEstado e = estado(p);
		CiberConfig cfg = CiberConfig.get();
		MutableText msg = Text.empty().append(prefijo());
		if (e.est == 1) {
			msg.append(Text.literal("Estás en cyberpsychosis. Bajas: ").formatted(Formatting.RED))
					.append(Text.literal(e.kills + "/" + cfg.bajasNecesarias).formatted(Formatting.WHITE))
					.append(Text.literal(cfg.modoSoloBajas
							? ". Solo matando te estabilizarás."
							: ". Mata o usa un Inmunosupresor.").formatted(Formatting.RED));
		} else if (e.inm > 0) {
			msg.append(Text.literal("Bloqueador Neural activo. Estás protegido.").formatted(Formatting.LIGHT_PURPLE));
		} else {
			int s = Math.max(0, e.t / 20);
			msg.append(Text.literal("Te quedan ").formatted(Formatting.GRAY))
					.append(Text.literal(String.format("%d:%02d", s / 60, s % 60)).formatted(Formatting.WHITE))
					.append(Text.literal(" de inmunosupresores.").formatted(Formatting.GRAY));
		}
		p.sendMessage(msg);
	}

	// ------------------------------------------------------------------ animación de inyección

	/**
	 * Secuencia de la inyección (el ítem dura ~32 ticks): clic del inyector, pistón, pinchazo,
	 * burbujeo del líquido, latido. Todos los cercanos oyen los sonidos; las partículas salen del brazo.
	 */
	public static void animacionInyeccion(ServerPlayerEntity p, int restante) {
		net.minecraft.server.world.ServerWorld mundo = p.getServerWorld();
		Vec3d brazo = local(p, -0.35, p.getStandingEyeHeight() - 0.35, 0.45);
		switch (restante) {
			case 31 -> {
				fx(mundo, p, SoundEvents.ITEM_CROSSBOW_LOADING_END, 0.9f, 1.5f);
				fx(mundo, p, SoundEvents.ITEM_ARMOR_EQUIP_IRON, 0.6f, 1.7f);
			}
			case 26 -> fx(mundo, p, SoundEvents.BLOCK_PISTON_EXTEND, 0.25f, 2.0f);
			case 20 -> {
				fx(mundo, p, SoundEvents.ENTITY_BEE_STING, 0.6f, 1.8f);
				fx(mundo, p, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 1.6f);
				mundo.spawnParticles(polvo(0.8f, 0.0f, 0.05f, 0.7f), brazo.x, brazo.y, brazo.z, 4, 0.04, 0.04, 0.04, 0.0);
				p.networkHandler.requestTeleport(p.getX(), p.getY(), p.getZ(), p.getYaw() - 2.5f,
						MathHelper.clamp(p.getPitch() + 3.0f, -90.0f, 90.0f));
				overlay(p, 0, CiberConfig.get());
			}
			case 18, 15, 12, 9 -> {
				fx(mundo, p, SoundEvents.BLOCK_BREWING_STAND_BREW, 0.4f, 1.6f);
				fx(mundo, p, SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.12f, 2.0f);
				mundo.spawnParticles(polvo(0.3f, 0.9f, 1.0f, 0.8f), brazo.x, brazo.y, brazo.z, 3, 0.05, 0.05, 0.05, 0.0);
			}
			case 8 -> {
				fx(mundo, p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.3f);
				mundo.spawnParticles(ParticleTypes.ELECTRIC_SPARK, brazo.x, brazo.y, brazo.z, 6, 0.1, 0.1, 0.1, 0.05);
			}
			case 4 -> fx(mundo, p, SoundEvents.BLOCK_BEACON_AMBIENT, 0.6f, 2.0f);
			default -> {
			}
		}
	}

	private static void fx(net.minecraft.server.world.ServerWorld mundo, ServerPlayerEntity p,
			RegistryEntry<SoundEvent> s, float volumen, float tono) {
		fx(mundo, p, s.value(), volumen, tono);
	}

	private static void fx(net.minecraft.server.world.ServerWorld mundo, ServerPlayerEntity p, SoundEvent s,
			float volumen, float tono) {
		mundo.playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundCategory.PLAYERS, volumen, tono);
	}

	// ------------------------------------------------------------------ sustos

	/** Un susto al azar, siempre solo para quien lo sufre. */
	private static void evento(ServerPlayerEntity p, CiberConfig cfg) {
		switch (RNG.nextInt(10)) {
			case 0 -> sonido(p, SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.HOSTILE, local(p, 0, 0, -4), 1.3f, 1.0f);
			case 1 -> sonido(p, SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, local(p, -5, 0, 2), 1.3f, 0.8f);
			case 2 -> sonido(p, SoundEvents.ENTITY_SKELETON_AMBIENT, SoundCategory.HOSTILE, local(p, 5, 0, -3), 1.3f, 0.9f);
			case 3 -> sonido(p, SoundEvents.ENTITY_ENDERMAN_STARE, SoundCategory.HOSTILE, local(p, 0, 0, 6), 1.2f, 1.0f);
			case 4, 5, 6 -> {
				if (cfg.figuras) {
					figuraNueva(p);
				}
			}
			case 7 -> {
				if (cfg.chatFalso) {
					chatFalso(p);
				}
			}
			case 8 -> sonido(p, SoundEvents.ITEM_TOTEM_USE, SoundCategory.MASTER, 0.6f, 1.0f);
			default -> sonido(p, SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
		}
	}

	private static void chatFalso(ServerPlayerEntity p) {
		if (p.getServer() == null) {
			return;
		}
		List<ServerPlayerEntity> otros = new ArrayList<>(p.getServer().getPlayerManager().getPlayerList());
		otros.remove(p);
		if (otros.isEmpty()) {
			return;
		}
		String[] frases = {"corre", "no mires atrás", "ya vienen por ti", "mira arriba", "no es real... ¿o sí?"};
		ServerPlayerEntity emisor = otros.get(RNG.nextInt(otros.size()));
		p.sendMessage(Text.translatable("commands.message.display.incoming",
				emisor.getDisplayName(), frases[RNG.nextInt(frases.length)])
				.formatted(Formatting.GRAY, Formatting.ITALIC));
	}

	/** Pantallazo de glitch (una de 4 imágenes); base 0 = leves, 4 = fuertes. */
	static void overlay(ServerPlayerEntity p, int base, CiberConfig cfg) {
		if (!cfg.overlays) {
			return;
		}
		String glifo = String.valueOf((char) (0xE000 + base + RNG.nextInt(4)));
		Text texto = Text.literal(glifo).setStyle(Style.EMPTY.withFont(Identifier.of(ZxkStudio.MOD_ID, "overlay")));
		p.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 6, 10));
		p.networkHandler.sendPacket(new TitleS2CPacket(texto));
	}

	/** Sacude la cámara un momento. */
	private static void sacudida(ServerPlayerEntity p, CiberConfig cfg) {
		if (!cfg.sacudidas) {
			return;
		}
		float[][] giros = {{7, 2}, {-7, -2}, {4, -6}, {-5, 5}};
		float[] g = giros[RNG.nextInt(giros.length)];
		p.networkHandler.requestTeleport(p.getX(), p.getY(), p.getZ(),
				p.getYaw() + g[0], MathHelper.clamp(p.getPitch() + g[1], -90.0f, 90.0f));
	}

	// ------------------------------------------------------------------ figuras alucinadas

	private static void figuraNueva(ServerPlayerEntity p) {
		double[][] sitios = {{-6, 8}, {6, 8}, {0, 10}, {-8, -3}, {8, -3}};
		double[] s = sitios[RNG.nextInt(sitios.length)];
		Vec3d pos = local(p, s[0], 0, s[1]);
		FIGURAS.computeIfAbsent(p.getUuid(), k -> new ArrayList<>())
				.add(new Figura(pos.x, pos.y, pos.z, RNG.nextInt(4)));
	}

	private static void dibujarFiguras(ServerPlayerEntity p) {
		List<Figura> lista = FIGURAS.get(p.getUuid());
		if (lista == null) {
			return;
		}
		lista.removeIf(f -> {
			f.restante--;
			if (f.restante >= 1) {
				dibujar(p, f);
				return false;
			}
			return true;
		});
		if (lista.isEmpty()) {
			FIGURAS.remove(p.getUuid());
		}
	}

	private static void dibujar(ServerPlayerEntity p, Figura f) {
		switch (f.tipo) {
			case 0 -> { // sombra
				part(p, polvo(0.03f, 0.03f, 0.06f, 1.5f), f.x, f.y + 0.9, f.z, 0.16, 0.8, 0.16, 0, 16);
				part(p, polvo(1.0f, 0.05f, 0.1f, 0.6f), f.x, f.y + 1.65, f.z, 0.05, 0.02, 0.05, 0, 3);
			}
			case 1 -> { // creeper
				part(p, polvo(0.15f, 0.85f, 0.2f, 1.4f), f.x, f.y + 0.7, f.z, 0.22, 0.65, 0.22, 0, 18);
				part(p, polvo(0.0f, 0.0f, 0.0f, 0.9f), f.x, f.y + 1.1, f.z, 0.1, 0.05, 0.03, 0, 4);
			}
			case 2 -> { // enderman
				part(p, polvo(0.04f, 0.0f, 0.08f, 1.5f), f.x, f.y + 1.5, f.z, 0.1, 1.45, 0.1, 0, 20);
				part(p, polvo(0.85f, 0.2f, 1.0f, 0.8f), f.x, f.y + 2.45, f.z, 0.07, 0.02, 0.07, 0, 3);
				part(p, ParticleTypes.PORTAL, f.x, f.y + 1.5, f.z, 0.3, 1.2, 0.3, 0.1, 6);
			}
			default -> { // otro jugador
				part(p, polvo(0.8f, 0.92f, 1.0f, 1.3f), f.x, f.y + 0.9, f.z, 0.17, 0.8, 0.17, 0, 14);
				part(p, ParticleTypes.END_ROD, f.x, f.y + 1.0, f.z, 0.15, 0.7, 0.15, 0, 1);
			}
		}
	}

	private static DustParticleEffect polvo(float r, float g, float b, float escala) {
		return new DustParticleEffect(new Vector3f(r, g, b), escala);
	}

	/** Partículas que solo ve el jugador indicado. */
	static void part(ServerPlayerEntity p, ParticleEffect efecto, double x, double y, double z,
			double dx, double dy, double dz, double velocidad, int cantidad) {
		p.networkHandler.sendPacket(new ParticleS2CPacket(efecto, true, x, y, z,
				(float) dx, (float) dy, (float) dz, (float) velocidad, cantidad));
	}

	// ------------------------------------------------------------------ utilidades

	/** Como las coordenadas locales ^izquierda ^arriba ^adelante de los comandos (sin cabeceo). */
	static Vec3d local(ServerPlayerEntity p, double izquierda, double arriba, double adelante) {
		double yaw = Math.toRadians(p.getYaw());
		double fx = -Math.sin(yaw);
		double fz = Math.cos(yaw);
		double lx = Math.cos(yaw);
		double lz = Math.sin(yaw);
		return new Vec3d(p.getX() + lx * izquierda + fx * adelante, p.getY() + arriba,
				p.getZ() + lz * izquierda + fz * adelante);
	}

	/** Reloj en pantalla: la duración del efecto es el tiempo que queda. */
	private static void relojEfecto(ServerPlayerEntity p, CiberEstado e) {
		p.removeStatusEffect(ModContent.RELOJ_EFECTO);
		if (e.t <= 0) {
			return;
		}
		int duracion = e.t > 30000 ? 1000000 : e.t;
		p.addStatusEffect(new StatusEffectInstance(ModContent.RELOJ_EFECTO, duracion, 0, false, false, true));
	}

	static void efecto(ServerPlayerEntity p, RegistryEntry<StatusEffect> tipo, int ticks, int nivel) {
		p.addStatusEffect(new StatusEffectInstance(tipo, ticks, nivel, false, false, false));
	}

	private static Text prefijo() {
		return Text.literal("[Inmunosupresores] ").formatted(Formatting.AQUA, Formatting.BOLD);
	}

	private static Text barraBajas(CiberEstado e) {
		return Text.empty()
				.append(Text.literal("☠ CYBERPSYCHOSIS ☠ ").formatted(Formatting.DARK_RED, Formatting.BOLD))
				.append(Text.literal("Bajas: ").formatted(Formatting.RED))
				.append(Text.literal(String.valueOf(e.kills)).formatted(Formatting.WHITE))
				.append(Text.literal("/").formatted(Formatting.GRAY))
				.append(Text.literal(String.valueOf(CiberConfig.get().bajasNecesarias)).formatted(Formatting.WHITE));
	}

	private static void titulo(ServerPlayerEntity p, int entrada, int quieto, int salida, Text titulo, Text subtitulo) {
		p.networkHandler.sendPacket(new TitleFadeS2CPacket(entrada, quieto, salida));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(subtitulo));
		p.networkHandler.sendPacket(new TitleS2CPacket(titulo));
	}

	// sonidos que solo oye el jugador (como "playsound ... @s")

	static void sonido(ServerPlayerEntity p, SoundEvent s, SoundCategory c, float volumen, float tono) {
		sonido(p, Registries.SOUND_EVENT.getEntry(s), c, p.getPos(), volumen, tono);
	}

	static void sonido(ServerPlayerEntity p, RegistryEntry<SoundEvent> s, SoundCategory c, float volumen,
			float tono) {
		sonido(p, s, c, p.getPos(), volumen, tono);
	}

	static void sonido(ServerPlayerEntity p, SoundEvent s, SoundCategory c, Vec3d pos, float volumen,
			float tono) {
		sonido(p, Registries.SOUND_EVENT.getEntry(s), c, pos, volumen, tono);
	}

	static void sonido(ServerPlayerEntity p, RegistryEntry<SoundEvent> s, SoundCategory c, Vec3d pos,
			float volumen, float tono) {
		p.networkHandler.sendPacket(new PlaySoundS2CPacket(s, c, pos.x, pos.y, pos.z, volumen, tono, RNG.nextLong()));
	}
}
