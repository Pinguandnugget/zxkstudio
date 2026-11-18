package com.zxkstudio.sirenia.effect;

import com.zxkstudio.sirenia.registry.ModEffects;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.FluidTags;

/**
 * Eventos del lado del servidor ligados a Sirenomorphosis. Sin mixins: solo eventos de Fabric API.
 */
public final class SirenomorphosisEvents {
	private SirenomorphosisEvents() {
	}

	public static void register() {
		// Equivalente a HgjhgjProcedure del original: cancelar el daño por ahogamiento bajo el agua.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				!(source.isIn(DamageTypeTags.IS_DROWNING)
						&& entity.hasStatusEffect(ModEffects.SIRENOMORPHOSIS)
						&& entity.isSubmergedIn(FluidTags.WATER)));

		// Limpia los datos temporales del jugador al salir.
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				SirenomorphosisEffect.forget(handler.getPlayer().getUuid()));
	}
}
