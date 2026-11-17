package com.zxkstudio.sirenia.effect;

import com.zxkstudio.ZxkStudio;
import com.zxkstudio.sirenia.config.SireniaConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sirenomorphosis: recreación independiente (Fabric 1.21.1) del efecto de Dedsafio 3.
 *
 * Comportamiento heredado del original (verificado en el bytecode de referencia):
 *  - bajo el agua recupera aire (el original suma 1 por tick y otro procedimiento +7; aquí +7),
 *  - fuera del agua pierde aire y, si se agota, recibe daño periódico (opcional, desactivado por defecto).
 *
 * Diferencias a propósito:
 *  - El contador de daño y el "estaba bajo el agua" se guardan POR JUGADOR (el original los guardaba
 *    dentro de la instancia única del efecto, compartida entre todos los jugadores).
 *  - Minería y velocidad de nado usan atributos de vanilla: Minecraft los añade al empezar el efecto y los
 *    quita al terminar, sin tocar nada de otras fuentes y sin coste por tick.
 *  - La visión submarina es solo visual y vive en el cliente (mixin de niebla).
 */
public class SirenomorphosisEffect extends StatusEffect {
	/** Aire que se recupera por tick bajo el agua (el original: +1 en el efecto y +7 en OxProcedure). */
	private static final int AIR_GAIN_PER_TICK = 7;
	/** Si pasan más ticks que esto sin ejecutarse el efecto, el estado guardado se considera caducado. */
	private static final long STALE_AFTER_TICKS = 5L;

	private static final Vector3f TURQUOISE = new Vector3f(0.10f, 0.85f, 0.80f);

	/** Estado del castigo en tierra por jugador. Se vacía al desconectarse y se reinicia si queda obsoleto. */
	private static final Map<UUID, LandState> LAND_STATES = new ConcurrentHashMap<>();

	private static final class LandState {
		int damageTimer;
		boolean wasUnderwater;
		long lastTick;
	}

	public SirenomorphosisEffect() {
		super(StatusEffectCategory.NEUTRAL, 0x00CCCC);
		SireniaConfig cfg = SireniaConfig.get();
		if (cfg.underwater_speed_enabled && cfg.underwater_swim_efficiency > 0.0) {
			addAttributeModifier(EntityAttributes.GENERIC_WATER_MOVEMENT_EFFICIENCY,
					Identifier.of(ZxkStudio.MOD_ID, "effect.sirenomorphosis.swim"),
					cfg.underwater_swim_efficiency,
					EntityAttributeModifier.Operation.ADD_VALUE);
		}
		if (cfg.underwater_mining_enabled) {
			// La penalización de vanilla bajo el agua es x0,2. Sumando 0,8 queda en x1 (velocidad normal).
			addAttributeModifier(EntityAttributes.PLAYER_SUBMERGED_MINING_SPEED,
					Identifier.of(ZxkStudio.MOD_ID, "effect.sirenomorphosis.mining"),
					0.8,
					EntityAttributeModifier.Operation.ADD_VALUE);
		}
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
		if (!(entity.getWorld() instanceof ServerWorld world)) {
			return true;
		}
		SireniaConfig cfg = SireniaConfig.get();
		boolean submerged = entity.isSubmergedIn(FluidTags.WATER);

		if (submerged) {
			int max = entity.getMaxAir();
			if (entity.getAir() < max) {
				entity.setAir(Math.min(max, entity.getAir() + AIR_GAIN_PER_TICK));
			}
			if (cfg.underwater_particles_enabled && entity.age % cfg.underwater_particle_interval_ticks == 0) {
				spawnParticles(world, entity);
			}
		}

		if (cfg.land_penalty_enabled && entity instanceof PlayerEntity player) {
			landPenalty(world, player, submerged, cfg);
		}
		return true;
	}

	private static void spawnParticles(ServerWorld world, LivingEntity entity) {
		double x = entity.getX();
		double y = entity.getBodyY(0.5);
		double z = entity.getZ();
		world.spawnParticles(new DustParticleEffect(TURQUOISE, 1.0f), x, y, z, 2, 0.35, 0.5, 0.35, 0.0);
		if (entity.getRandom().nextInt(3) == 0) {
			world.spawnParticles(ParticleTypes.BUBBLE, x, y + 0.4, z, 1, 0.2, 0.2, 0.2, 0.02);
		}
	}

	/** Mecánica original: sin agua se pierde aire; con el aire agotado y pasado el margen, daño periódico. */
	private static void landPenalty(ServerWorld world, PlayerEntity player, boolean submerged, SireniaConfig cfg) {
		long now = world.getTime();
		LandState st = LAND_STATES.computeIfAbsent(player.getUuid(), id -> new LandState());
		if (now - st.lastTick > STALE_AFTER_TICKS) {
			st.damageTimer = 0;
			st.wasUnderwater = false;
		}
		st.lastTick = now;

		if (submerged) {
			st.damageTimer = 0;
			st.wasUnderwater = true;
			return;
		}
		player.setAir(Math.max(player.getAir() - cfg.land_air_loss_per_tick, 0));
		if (player.getAir() > 0) {
			st.damageTimer = cfg.land_grace_ticks;
			return;
		}
		if (st.wasUnderwater) {
			st.damageTimer = cfg.land_grace_ticks; // margen tras salir del agua
			st.wasUnderwater = false;
		}
		if (st.damageTimer <= 0) {
			player.damage(world.getDamageSources().dryOut(), cfg.land_damage);
			st.damageTimer = cfg.land_damage_interval_ticks;
		} else {
			st.damageTimer--;
		}
	}

	/** Libera el estado de un jugador (se llama al desconectarse). */
	public static void forget(UUID playerId) {
		LAND_STATES.remove(playerId);
	}
}
