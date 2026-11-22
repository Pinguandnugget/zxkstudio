package com.zxkstudio.sirenia.fear;

import com.zxkstudio.sirenia.config.SireniaConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Alucinaciones de la Talasofobia. Todo es falso y solo lo percibe el jugador afectado:
 *  - Sonidos del fondo del mar (paquetes de sonido solo para él, desde posiciones al azar).
 *  - Un tiburón fantasma: una entidad que NO existe en el servidor (solo se le envían paquetes de aparición y
 *    movimiento a ese jugador), que se acerca, "muerde" y se va. Las mordidas son el golpe de cámara y el sonido
 *    de daño, pero la vida no cambia.
 *  - El susto del Guardián Anciano.
 * Como no hay entidades reales, no hay IA, ni carga de chunks, ni conflictos con otros mods.
 * Minecraft no trae tiburón: el fantasma usa el modelo del delfín.
 */
public final class Hallucinations {
	private Hallucinations() {
	}

	private static final Random RNG = new Random();
	private static final Vector3f BLOOD = new Vector3f(0.55f, 0.0f, 0.05f);

	private static final SoundEvent[] CREEPY = {
			SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT,
			SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE,
			SoundEvents.ENTITY_GUARDIAN_ATTACK,
			SoundEvents.ENTITY_WARDEN_AMBIENT,
			SoundEvents.ENTITY_WARDEN_HEARTBEAT,
			SoundEvents.ENTITY_WARDEN_ROAR,
			SoundEvents.ENTITY_WARDEN_NEARBY_CLOSER,
			SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
			SoundEvents.ENTITY_GHAST_SCREAM,
			SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE,
	};

	private static final class Ghost {
		final DolphinEntity entity;
		final ServerWorld world;
		Vec3d pos;
		int age;
		int bites;
		int nextBite = 20;
		boolean fleeing;
		Vec3d fleeDir = Vec3d.ZERO;

		Ghost(DolphinEntity entity, ServerWorld world, Vec3d pos) {
			this.entity = entity;
			this.world = world;
			this.pos = pos;
		}
	}

	private static final Map<UUID, Ghost> GHOSTS = new HashMap<>();
	/** Segundos que faltan para que vuelva a aparecer el tiburón tras irse. */
	private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();

	/** Una vez por segundo mientras el jugador alucina. */
	public static void hallucinate(ServerPlayerEntity player, boolean panic) {
		if (RNG.nextFloat() < (panic ? 1.0f : 0.7f)) {
			playCreepy(player);
		}
		if (!GHOSTS.containsKey(player.getUuid())) {
			int wait = COOLDOWN.getOrDefault(player.getUuid(), 0);
			if (wait > 0) {
				COOLDOWN.put(player.getUuid(), wait - 1);
			} else {
				spawnGhost(player);
			}
		}
	}

	/** El susto del Guardián Anciano (imagen y sonido propios del juego, solo para este jugador). */
	public static void scare(ServerPlayerEntity player) {
		player.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.ELDER_GUARDIAN_EFFECT, 0.0f));
	}

	public static void stop(ServerPlayerEntity player) {
		Ghost ghost = GHOSTS.remove(player.getUuid());
		COOLDOWN.remove(player.getUuid());
		if (ghost != null && player.networkHandler != null) {
			player.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(ghost.entity.getId()));
		}
	}

	/** Cada tick: mueve a los tiburones fantasma (solo hay uno por jugador que alucina). */
	public static void tick(MinecraftServer server) {
		if (GHOSTS.isEmpty()) {
			return;
		}
		for (UUID id : new ArrayList<>(GHOSTS.keySet())) {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
			Ghost ghost = GHOSTS.get(id);
			if (player == null) {
				GHOSTS.remove(id);
				COOLDOWN.remove(id);
				continue;
			}
			if (ghost == null) {
				continue;
			}
			if (!Thalassophobia.isAfraid(player) || player.getServerWorld() != ghost.world || !tickGhost(player, ghost)) {
				GHOSTS.remove(id);
				COOLDOWN.put(id, 4);
				player.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(ghost.entity.getId()));
			}
		}
	}

	private static void spawnGhost(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		double angle = RNG.nextDouble() * Math.PI * 2.0;
		Vec3d start = player.getPos().add(Math.cos(angle) * 16.0, RNG.nextDouble() * 4.0 - 2.0, Math.sin(angle) * 16.0);
		DolphinEntity entity = new DolphinEntity(EntityType.DOLPHIN, world); // nunca se añade al mundo
		entity.setPosition(start);
		player.networkHandler.sendPacket(new EntitySpawnS2CPacket(entity.getId(), entity.getUuid(),
				start.x, start.y, start.z, 0.0f, 0.0f, EntityType.DOLPHIN, 0, Vec3d.ZERO, 0.0));
		GHOSTS.put(player.getUuid(), new Ghost(entity, world, start));
	}

	/** Devuelve false cuando el tiburón ya terminó y hay que retirarlo. */
	private static boolean tickGhost(ServerPlayerEntity player, Ghost ghost) {
		Vec3d target = player.getEyePos();
		Vec3d toPlayer = target.subtract(ghost.pos);
		double dist = toPlayer.length();
		Vec3d move;
		if (!ghost.fleeing) {
			if (dist > 2.0) {
				move = toPlayer.normalize().multiply(Math.min(0.5, dist - 1.8));
			} else {
				Vec3d side = new Vec3d(-toPlayer.z, 0.0, toPlayer.x);
				move = side.lengthSquared() > 1.0E-4 ? side.normalize().multiply(0.15) : Vec3d.ZERO;
				if (ghost.age >= ghost.nextBite) {
					bite(player, ghost);
					ghost.bites++;
					ghost.nextBite = ghost.age + 12;
					if (ghost.bites >= 3) {
						ghost.fleeing = true;
						ghost.fleeDir = toPlayer.lengthSquared() > 1.0E-4 ? toPlayer.normalize().multiply(-1.0) : new Vec3d(1, 0, 0);
					}
				}
			}
		} else {
			move = ghost.fleeDir.multiply(0.7);
			if (dist > 26.0) {
				return false;
			}
		}
		ghost.pos = ghost.pos.add(move);
		ghost.age++;
		if (ghost.age > 300) {
			return false;
		}

		Vec3d look = ghost.fleeing ? ghost.fleeDir : toPlayer;
		double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
		float yaw = (float) (MathHelper.atan2(-look.x, look.z) * 180.0 / Math.PI);
		float pitch = (float) (-MathHelper.atan2(look.y, horizontal) * 180.0 / Math.PI);
		ghost.entity.setPosition(ghost.pos);
		ghost.entity.setYaw(yaw);
		ghost.entity.setPitch(pitch);
		player.networkHandler.sendPacket(new EntityPositionS2CPacket(ghost.entity));
		player.networkHandler.sendPacket(new EntitySetHeadYawS2CPacket(ghost.entity, (byte) MathHelper.floor(yaw * 256.0f / 360.0f)));
		return true;
	}

	/** Mordida falsa: golpe de cámara y sonido de daño, sin quitar vida. */
	private static void bite(ServerPlayerEntity player, Ghost ghost) {
		player.networkHandler.sendPacket(new EntityStatusS2CPacket(player, (byte) 2));
		float volume = SireniaConfig.get().thalassophobia_sound_volume;
		sendSound(player, SoundEvents.ENTITY_GUARDIAN_ATTACK, ghost.pos, volume, 0.6f);
		sendSound(player, SoundEvents.ENTITY_DOLPHIN_ATTACK, ghost.pos, volume, 0.5f);
		player.getServerWorld().spawnParticles(player, new DustParticleEffect(BLOOD, 1.3f), true,
				player.getX(), player.getEyeY() - 0.3, player.getZ(), 8, 0.35, 0.35, 0.35, 0.0);
	}

	private static void playCreepy(ServerPlayerEntity player) {
		SireniaConfig cfg = SireniaConfig.get();
		double angle = RNG.nextDouble() * Math.PI * 2.0;
		double distance = 6.0 + RNG.nextDouble() * 10.0;
		Vec3d at = player.getPos().add(Math.cos(angle) * distance, -2.0 - RNG.nextDouble() * 8.0, Math.sin(angle) * distance);
		SoundEvent sound = CREEPY[RNG.nextInt(CREEPY.length)];
		sendSound(player, sound, at, cfg.thalassophobia_sound_volume * (0.8f + RNG.nextFloat() * 0.6f), 0.5f + RNG.nextFloat() * 0.4f);
	}

	private static void sendSound(ServerPlayerEntity player, SoundEvent sound, Vec3d at, float volume, float pitch) {
		player.networkHandler.sendPacket(new PlaySoundS2CPacket(RegistryEntry.of(sound), SoundCategory.MASTER,
				at.x, at.y, at.z, volume, pitch, RNG.nextLong()));
	}
}
