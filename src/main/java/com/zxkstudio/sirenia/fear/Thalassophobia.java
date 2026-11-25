package com.zxkstudio.sirenia.fear;

import com.mojang.serialization.Codec;
import com.zxkstudio.ZxkStudio;
import com.zxkstudio.sirenia.config.SireniaConfig;
import com.zxkstudio.sirenia.registry.ModEffects;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Talasofobia: miedo al agua profunda.
 *
 * Flujo (todo en el servidor, una comprobación por segundo y solo por jugador):
 *  1. El jugador pasa N segundos con la cabeza bajo el agua y agua profunda bajo sus pies -> aparece el miedo.
 *  2. Tiene "countdown" segundos para reaccionar. En los últimos segundos empiezan las alucinaciones.
 *  3. Si llega a 0, empieza el pánico: cada pocos segundos recibe un golpe letal que gasta un tótem (o lo mata).
 *  Cura: salir del agua, o comer un Corazón de Sirenia maduro o marchito. Con Sirenomorphosis activa es inmune.
 *
 * Estado guardado en la ficha del jugador (segundos restantes; 0 = pánico). El efecto visible solo es un indicador
 * que se vuelve a poner si algo lo quita (leche, tótem), así que ninguna de esas cosas cura el miedo.
 * Morir lo cura, como cualquier efecto.
 */
public final class Thalassophobia {
	private Thalassophobia() {
	}

	/** Segundos restantes de la cuenta atrás; 0 = pánico; sin dato = sin miedo. */
	private static final AttachmentType<Integer> FEAR = AttachmentRegistry.createPersistent(
			Identifier.of(ZxkStudio.MOD_ID, "thalassophobia_fear"), Codec.INT);

	private static final Map<UUID, Integer> EXPOSURE = new HashMap<>();
	private static final Map<UUID, Integer> PANIC_TIMER = new HashMap<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Hallucinations.tick(server);
			if (server.getTicks() % 20 != 0) {
				return;
			}
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				secondTick(player);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.getPlayer()));
	}

	private static void forget(ServerPlayerEntity player) {
		EXPOSURE.remove(player.getUuid());
		PANIC_TIMER.remove(player.getUuid());
		Hallucinations.stop(player);
	}

	public static boolean isAfraid(ServerPlayerEntity player) {
		return player.getAttached(FEAR) != null;
	}

	private static void secondTick(ServerPlayerEntity player) {
		SireniaConfig cfg = SireniaConfig.get();
		Integer fear = player.getAttached(FEAR);
		boolean immune = cfg.thalassophobia_immune_with_sirenomorphosis && player.hasStatusEffect(ModEffects.SIRENOMORPHOSIS);

		if (!cfg.thalassophobia_enabled || immune || !player.isAlive() || player.isCreative() || player.isSpectator()) {
			if (fear != null) {
				cure(player);
			}
			EXPOSURE.remove(player.getUuid());
			return;
		}

		if (fear == null) {
			if (isInAbyss(player, cfg)) {
				int seconds = EXPOSURE.merge(player.getUuid(), 1, Integer::sum);
				if (seconds >= cfg.thalassophobia_expose_seconds) {
					start(player, cfg);
				}
			} else {
				EXPOSURE.computeIfPresent(player.getUuid(), (id, v) -> v <= 1 ? null : v - 1);
			}
			return;
		}

		// Con miedo: salir del agua lo cura.
		if (!player.isTouchingWater()) {
			cure(player);
			return;
		}

		if (fear > 0) {
			fear--;
			player.setAttached(FEAR, fear);
			if (fear == 0) {
				PANIC_TIMER.put(player.getUuid(), 0);
				Hallucinations.scare(player);
			}
		}
		syncIndicator(player, fear);

		if (fear == 3 && cfg.thalassophobia_hallucination_seconds >= 3) {
			Hallucinations.scare(player); // susto 3 s antes de que acabe la cuenta atrás
		}
		if (fear > 0 && fear <= cfg.thalassophobia_hallucination_seconds) {
			Hallucinations.hallucinate(player, false);
		} else if (fear == 0) {
			Hallucinations.hallucinate(player, true);
			int timer = PANIC_TIMER.merge(player.getUuid(), 1, Integer::sum);
			if (timer >= cfg.thalassophobia_panic_interval_seconds) {
				PANIC_TIMER.put(player.getUuid(), 0);
				pop(player);
			}
		}
	}

	/** Golpe letal: si lleva tótem se gasta (y sobrevive); si no, muere. No salta el tótem con /kill ni con el vacío. */
	private static void pop(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		player.damage(world.getDamageSources().magic(), 1000.0f);
	}

	private static void start(ServerPlayerEntity player, SireniaConfig cfg) {
		EXPOSURE.remove(player.getUuid());
		player.setAttached(FEAR, cfg.thalassophobia_countdown_seconds);
		syncIndicator(player, cfg.thalassophobia_countdown_seconds);
	}

	/** Mantiene el indicador del HUD (tiempo restante, o infinito en pánico) y lo repone si algo lo quitó. */
	private static void syncIndicator(ServerPlayerEntity player, int fear) {
		StatusEffectInstance current = player.getStatusEffect(ModEffects.THALASSOPHOBIA);
		int wanted = fear <= 0 ? StatusEffectInstance.INFINITE : fear * 20;
		boolean ok = current != null && (fear <= 0 ? current.isInfinite()
				: !current.isInfinite() && Math.abs(current.getDuration() - wanted) <= 60);
		if (!ok) {
			player.addStatusEffect(new StatusEffectInstance(ModEffects.THALASSOPHOBIA, wanted, 0, false, true, true));
		}
	}

	/** Cabeza bajo el agua y agua seguida durante "depth" bloques por debajo de los pies. */
	private static boolean isInAbyss(ServerPlayerEntity player, SireniaConfig cfg) {
		if (!player.isSubmergedIn(FluidTags.WATER)) {
			return false;
		}
		ServerWorld world = player.getServerWorld();
		BlockPos feet = player.getBlockPos();
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int i = 1; i <= cfg.thalassophobia_depth_blocks; i++) {
			int y = feet.getY() - i;
			if (y < world.getBottomY()) {
				return false;
			}
			pos.set(feet.getX(), y, feet.getZ());
			if (!world.getFluidState(pos).isIn(FluidTags.WATER)) {
				return false;
			}
		}
		return true;
	}

	public static void cure(LivingEntity entity) {
		if (!(entity instanceof ServerPlayerEntity player)) {
			return;
		}
		if (player.getAttached(FEAR) != null) {
			player.removeAttached(FEAR);
		}
		player.removeStatusEffect(ModEffects.THALASSOPHOBIA);
		EXPOSURE.remove(player.getUuid());
		PANIC_TIMER.remove(player.getUuid());
		Hallucinations.stop(player);
	}
}
