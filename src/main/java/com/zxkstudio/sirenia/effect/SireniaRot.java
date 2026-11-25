package com.zxkstudio.sirenia.effect;

import com.mojang.serialization.Codec;
import com.zxkstudio.ZxkStudio;
import com.zxkstudio.sirenia.config.SireniaConfig;
import com.zxkstudio.sirenia.registry.ModEffects;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Contagio y cura de la Podredumbre de Sirenia.
 *
 * Mientras dure "hasta curarse" (duración 0 en la configuración) el jugador lleva además una marca guardada
 * en su ficha. Si algo le quita el efecto que no sea el fruto bueno (la leche, por ejemplo), un chequeo
 * barato (una vez por segundo, solo jugadores marcados) lo vuelve a poner. Morir lo cura, como cualquier efecto.
 */
public final class SireniaRot {
	private SireniaRot() {
	}

	private static final AttachmentType<Boolean> INFECTED = AttachmentRegistry.createPersistent(
			Identifier.of(ZxkStudio.MOD_ID, "sirenia_rot_infected"), Codec.BOOL);

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 20 != 0) {
				return;
			}
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				if (Boolean.TRUE.equals(player.getAttached(INFECTED)) && !player.hasStatusEffect(ModEffects.SIRENIA_ROT)) {
					player.addStatusEffect(instance(SireniaConfig.get()));
				}
			}
		});
	}

	private static StatusEffectInstance instance(SireniaConfig cfg) {
		int ticks = cfg.rot_effect_duration_seconds <= 0 ? StatusEffectInstance.INFINITE : cfg.rot_effect_duration_seconds * 20;
		return new StatusEffectInstance(ModEffects.SIRENIA_ROT, ticks, 0, false, true, true);
	}

	public static void infect(LivingEntity user) {
		SireniaConfig cfg = SireniaConfig.get();
		user.addStatusEffect(instance(cfg));
		if (user instanceof PlayerEntity player) {
			if (cfg.rot_effect_duration_seconds <= 0) {
				player.setAttached(INFECTED, Boolean.TRUE);
			}
		}
	}

	public static void cure(LivingEntity user) {
		user.removeStatusEffect(ModEffects.SIRENIA_ROT);
		if (user instanceof PlayerEntity player) {
			player.removeAttached(INFECTED);
		}
	}
}
