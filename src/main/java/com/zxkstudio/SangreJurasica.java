package com.zxkstudio;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import org.joml.Vector3f;

import java.util.ArrayList;

/** Lógica de la infección. Reemplaza las funciones del datapack sangre_jurasica. */
public final class SangreJurasica {
	private SangreJurasica() {
	}

	private static int ticks = 0;
	private static long segundos = 0;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(SangreJurasica::alTick);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(SangreJurasica::alRecibirDano);
	}

	// ------------------------------------------------------------------ eventos

	private static void alTick(MinecraftServer servidor) {
		if (++ticks < 20) {
			return;
		}
		ticks = 0;
		segundos++;

		SangreConfig cfg = SangreConfig.get();
		if (!cfg.activo) {
			return;
		}
		for (ServerPlayerEntity jugador : new ArrayList<>(servidor.getPlayerManager().getPlayerList())) {
			if (jugador.isCreative() || jugador.isSpectator()) {
				continue;
			}
			if (cfg.provocarDinos) {
				provocar(jugador, cfg);
			}
			if (estaInfectado(jugador)) {
				sintomas(jugador, cfg);
			}
		}
	}

	private static void alRecibirDano(LivingEntity entidad, DamageSource fuente,
			float danoBase, float danoRecibido, boolean bloqueado) {
		if (bloqueado || !(entidad instanceof ServerPlayerEntity jugador)) {
			return;
		}
		if (!SangreConfig.get().activo || jugador.isCreative() || jugador.isSpectator()) {
			return;
		}
		Entity atacante = fuente.getAttacker();
		if (atacante == null || !atacante.getType().isIn(ModContent.DINOS)) {
			return;
		}
		if (!estaInfectado(jugador)) {
			infectar(jugador);
		}
	}

	// ------------------------------------------------------------------ estado

	public static boolean estaInfectado(ServerPlayerEntity jugador) {
		return jugador.hasAttached(ModContent.SANGRE_TIEMPO);
	}

	public static void infectar(ServerPlayerEntity jugador) {
		SangreConfig cfg = SangreConfig.get();
		jugador.setAttached(ModContent.SANGRE_TIEMPO, cfg.tiempoSegundos);
		efecto(jugador, StatusEffects.NAUSEA, 120, 0);
		sonido(jugador, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0.8f);
		jugador.sendMessage(Text.empty()
				.append(Text.literal("☠ Sangre Jurásica ").formatted(Formatting.DARK_RED, Formatting.BOLD))
				.append(Text.literal("corre por tus venas. Tienes " + formatoTiempo(cfg.tiempoSegundos)
						+ " para beber un antídoto o tu cuerpo se consumirá.").formatted(Formatting.RED)));
	}

	/** Devuelve true si el jugador estaba infectado y se curó. */
	public static boolean curar(ServerPlayerEntity jugador) {
		if (!estaInfectado(jugador)) {
			return false;
		}
		jugador.removeAttached(ModContent.SANGRE_TIEMPO);
		jugador.removeStatusEffect(ModContent.SANGRE_EFECTO);

		ServerWorld mundo = jugador.getServerWorld();
		mundo.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
				jugador.getX(), jugador.getY() + 1, jugador.getZ(), 25, 0.4, 0.6, 0.4, 0.0);
		sonido(jugador, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f);
		jugador.sendMessage(Text.literal("✔ Te has curado de la Sangre Jurásica")
				.formatted(Formatting.GREEN), true);
		jugador.sendMessage(Text.empty()
				.append(Text.literal("✔ ").formatted(Formatting.GREEN))
				.append(Text.literal("El antídoto limpió tu sangre. Otro golpe jurásico volverá a infectarte.")
						.formatted(Formatting.GREEN)));
		return true;
	}

	// ------------------------------------------------------------------ síntomas (cada segundo)

	private static void sintomas(ServerPlayerEntity jugador, SangreConfig cfg) {
		// icono y nombre del efecto en pantalla
		jugador.addStatusEffect(new StatusEffectInstance(ModContent.SANGRE_EFECTO, 60, 0, false, false, true));
		if (cfg.particulas) {
			jugador.getServerWorld().spawnParticles(
					new DustParticleEffect(new Vector3f(0.55f, 0.0f, 0.0f), 1.0f),
					jugador.getX(), jugador.getY() + 1, jugador.getZ(), 8, 0.3, 0.5, 0.3, 0.0);
		}

		int t = jugador.getAttachedOrElse(ModContent.SANGRE_TIEMPO, 0);
		if (t > 0) {
			fase1(jugador, cfg, t);
			t = jugador.getAttachedOrElse(ModContent.SANGRE_TIEMPO, 0);
		}
		if (t <= 0) {
			fase2(jugador, cfg);
		}
	}

	private static void fase1(ServerPlayerEntity jugador, SangreConfig cfg, int tiempo) {
		int restante = tiempo - 1;
		jugador.setAttached(ModContent.SANGRE_TIEMPO, restante);

		if (cfg.nivelDebilidad > 0) {
			efecto(jugador, StatusEffects.WEAKNESS, 60, cfg.nivelDebilidad - 1);
		}
		if (cfg.nivelLentitud > 0) {
			efecto(jugador, StatusEffects.SLOWNESS, 60, cfg.nivelLentitud - 1);
		}

		Text barra = Text.empty()
				.append(Text.literal("☠ Sangre Jurásica ").formatted(Formatting.DARK_RED, Formatting.BOLD))
				.append(Text.literal("— antídoto en ").formatted(Formatting.RED))
				.append(Text.literal((restante / 60) + ":" + String.format("%02d", restante % 60))
						.formatted(Formatting.GOLD));
		jugador.sendMessage(barra, true);

		if (cfg.nausea && restante % 20 == 0) {
			efecto(jugador, StatusEffects.NAUSEA, 160, 0);
		}
		if (cfg.oscuridad && restante % 30 == 0) {
			efecto(jugador, StatusEffects.DARKNESS, 120, 0);
		}
		if (cfg.latido && restante <= 60) {
			sonido(jugador, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.0f);
		}

		if (cfg.mensajes) {
			switch (restante) {
				case 120 -> aviso(jugador, "Quedan 2 minutos para el antídoto.");
				case 60 -> aviso(jugador, "Queda 1 minuto. Tu corazón late cada vez más fuerte.");
				case 10 -> aviso(jugador, "10 segundos...");
				case 0 -> jugador.sendMessage(Text.empty()
						.append(Text.literal("☠ Sangre Jurásica ").formatted(Formatting.DARK_RED, Formatting.BOLD))
						.append(Text.literal("ha llegado a su fase terminal. Solo un antídoto puede salvarte.")
								.formatted(Formatting.RED)));
				default -> {
				}
			}
		}
	}

	private static void fase2(ServerPlayerEntity jugador, SangreConfig cfg) {
		jugador.setAttached(ModContent.SANGRE_TIEMPO, 0);

		if (cfg.nausea) {
			efecto(jugador, StatusEffects.NAUSEA, 120, 0);
		}
		if (cfg.oscuridad) {
			efecto(jugador, StatusEffects.DARKNESS, 60, 0);
		}
		if (cfg.nivelLentitud > 0) {
			efecto(jugador, StatusEffects.SLOWNESS, 60, cfg.nivelLentitud);
		}
		if (cfg.nivelDebilidad > 0) {
			efecto(jugador, StatusEffects.WEAKNESS, 60, cfg.nivelDebilidad);
		}
		if (cfg.nivelVeneno > 0) {
			efecto(jugador, StatusEffects.POISON, 80, cfg.nivelVeneno - 1);
		}
		if (cfg.particulas) {
			jugador.getServerWorld().spawnParticles(ParticleTypes.CRIMSON_SPORE,
					jugador.getX(), jugador.getY() + 1, jugador.getZ(), 12, 0.4, 0.6, 0.4, 0.0);
		}
		if (cfg.latido) {
			sonido(jugador, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.2f);
		}
		jugador.sendMessage(Text.empty()
				.append(Text.literal("☠ Sangre Jurásica ").formatted(Formatting.DARK_RED, Formatting.BOLD))
				.append(Text.literal("— fase terminal. Solo un antídoto puede salvarte").formatted(Formatting.RED)),
				true);

		if (cfg.danoTerminal > 0.0f && segundos % cfg.intervaloDanoSegundos == 0) {
			jugador.damage(jugador.getServerWorld().getDamageSources().magic(), cfg.danoTerminal);
		}
	}

	// ------------------------------------------------------------------ dinos

	/** Los dinosaurios hostiles cercanos fijan al jugador como objetivo. */
	private static void provocar(ServerPlayerEntity jugador, SangreConfig cfg) {
		ServerWorld mundo = jugador.getServerWorld();
		double radio = cfg.radioProvocar;
		Box zona = jugador.getBoundingBox().expand(radio);
		for (MobEntity mob : mundo.getEntitiesByClass(MobEntity.class, zona,
				m -> m.isAlive()
						&& m.getTarget() == null
						&& m.getType().isIn(ModContent.HOSTILES)
						&& m.squaredDistanceTo(jugador) <= radio * radio)) {
			mob.setTarget(jugador);
		}
	}

	// ------------------------------------------------------------------ utilidades

	private static void efecto(ServerPlayerEntity jugador, RegistryEntry<StatusEffect> tipo, int duracion, int nivel) {
		jugador.addStatusEffect(new StatusEffectInstance(tipo, duracion, nivel, false, false, true));
	}

	private static void sonido(ServerPlayerEntity jugador, SoundEvent sonido, float tono) {
		jugador.getServerWorld().playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
				sonido, SoundCategory.PLAYERS, 1.0f, tono);
	}

	private static void aviso(ServerPlayerEntity jugador, String texto) {
		jugador.sendMessage(Text.empty()
				.append(Text.literal("☠ ").formatted(Formatting.DARK_RED))
				.append(Text.literal(texto).formatted(Formatting.RED)));
	}

	public static String formatoTiempo(int s) {
		if (s % 60 == 0) {
			int m = s / 60;
			return m + (m == 1 ? " minuto" : " minutos");
		}
		return s + " segundos";
	}
}
