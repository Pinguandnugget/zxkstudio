package com.zxkstudio;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Alucinaciones de la cyberpsychosis: el jugador "viaja" a una playa o a una montaña nevada
 * (bloques, hora, clima, sonidos y figuras falsas que SOLO ve él), pasa un susto y vuelve
 * a la realidad todavía en cyberpsychosis. Todo se envía como paquetes: el mundo real no cambia.
 *
 * El paisaje falso se envía como chunks modificados (bloques y bioma) en toda la distancia
 * de render cercana, así cambian también el cielo, la niebla, el color de la hierba y la nieve.
 * Los bloques falsos tienen siempre la misma colisión que los reales (cubo por cubo),
 * así el jugador no se atasca ni rebota.
 */
public final class Alucinacion {
	private Alucinacion() {
	}

	public enum Tipo {
		PLAYA, NIEVE
	}

	/** Radio máximo, en chunks, del paisaje falso (se limita a la distancia de render del servidor). */
	private static final int RADIO_CHUNKS = 7;
	private static final int CHUNKS_POR_TICK = 6;

	public enum Bicho {
		AHOGADO, WARDEN, SOMBRA
	}

	private static final Set<Block> TIERRA = Set.of(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT,
			Blocks.PODZOL, Blocks.MYCELIUM, Blocks.ROOTED_DIRT, Blocks.MOSS_BLOCK, Blocks.CLAY, Blocks.GRAVEL,
			Blocks.MUD, Blocks.SNOW_BLOCK);
	private static final Set<Block> PIEDRA = Set.of(Blocks.STONE, Blocks.ANDESITE, Blocks.DIORITE,
			Blocks.GRANITE, Blocks.TUFF);
	private static final Set<Block> ARENA = Set.of(Blocks.SAND, Blocks.RED_SAND, Blocks.SANDSTONE);
	private static final Random RNG = new Random();

	private static final int F_ENTRADA = 0;
	private static final int F_ESCENA = 1;
	private static final int F_SUSTO = 2;
	private static final int F_SALIDA = 3;

	private static final Map<UUID, Escena> ESCENAS = new HashMap<>();
	private static final Map<UUID, Integer> ESPERA = new HashMap<>();

	private static final class Escena {
		final Tipo tipo;
		final ServerWorld mundo;
		int fase = F_ENTRADA;
		int t = 0;
		int tf = 0;
		int centroX;
		int centroZ;
		final Set<Long> hechas = new HashSet<>();
		final ArrayDeque<ChunkPos> cola = new ArrayDeque<>();
		final List<ChunkPos> enviados = new ArrayList<>();
		Entity figura;
		Bicho bicho;
		boolean restaurado = false;

		Escena(Tipo tipo, ServerWorld mundo) {
			this.tipo = tipo;
			this.mundo = mundo;
		}
	}

	public static void register() {
		// durante la alucinación no se rompe ni se coloca nada (los bloques que ve no son los reales)
		AttackBlockCallback.EVENT.register((jugador, mundo, mano, pos, dir) ->
				ESCENAS.containsKey(jugador.getUuid()) ? ActionResult.FAIL : ActionResult.PASS);
		UseBlockCallback.EVENT.register((jugador, mundo, mano, hit) ->
				ESCENAS.containsKey(jugador.getUuid()) ? ActionResult.FAIL : ActionResult.PASS);
		ServerPlayConnectionEvents.DISCONNECT.register((manejador, servidor) ->
				olvidar(manejador.getPlayer().getUuid()));
	}

	public static boolean enEscena(ServerPlayerEntity p) {
		return ESCENAS.containsKey(p.getUuid());
	}

	/** Olvida todo (al morir, desconectarse o cambiar de dimensión el cliente ya recargó el mundo). */
	public static void olvidar(UUID id) {
		ESCENAS.remove(id);
		ESPERA.remove(id);
	}

	/** Lo llama CiberPsicosis cada tick por jugador. */
	public static void tick(ServerPlayerEntity p, boolean enPsicosis) {
		UUID id = p.getUuid();
		CiberConfig cfg = CiberConfig.get();
		Escena es = ESCENAS.get(id);
		if (es != null) {
			if (p.isRemoved() || !p.isAlive() || p.getServerWorld() != es.mundo) {
				ESCENAS.remove(id);
				return;
			}
			if (!enPsicosis || p.isCreative() || p.isSpectator()) {
				terminarYa(p, es);
				return;
			}
			avanzar(p, es, cfg);
			return;
		}
		if (!enPsicosis || !cfg.alucinaciones) {
			ESPERA.remove(id);
			return;
		}
		Integer espera = ESPERA.get(id);
		if (espera == null) {
			ESPERA.put(id, esperaAleatoria(cfg));
			return;
		}
		if (espera > 0) {
			ESPERA.put(id, espera - 1);
			return;
		}
		if (puedeAlucinar(p)) {
			iniciar(p, RNG.nextBoolean() ? Tipo.PLAYA : Tipo.NIEVE);
		} else {
			ESPERA.put(id, 100);
		}
	}

	private static int esperaAleatoria(CiberConfig cfg) {
		int min = cfg.alucinacionEsperaMin * 20;
		int max = cfg.alucinacionEsperaMax * 20;
		return min + RNG.nextInt(max - min + 1);
	}

	private static boolean puedeAlucinar(ServerPlayerEntity p) {
		return p.getServerWorld().getRegistryKey() == World.OVERWORLD
				&& !p.isTouchingWater() && !p.hasVehicle() && !p.isFallFlying() && !p.isSleeping();
	}

	/** Empieza una alucinación (también la usa /cp alucinar). */
	public static boolean iniciar(ServerPlayerEntity p, Tipo tipo) {
		if (ESCENAS.containsKey(p.getUuid())) {
			return false;
		}
		ESCENAS.put(p.getUuid(), new Escena(tipo, p.getServerWorld()));
		ESPERA.remove(p.getUuid());
		return true;
	}

	// ------------------------------------------------------------------ desarrollo

	private static void avanzar(ServerPlayerEntity p, Escena es, CiberConfig cfg) {
		es.t++;
		es.tf++;
		switch (es.fase) {
			case F_ENTRADA -> entrada(p, es);
			case F_ESCENA -> escena(p, es, cfg);
			case F_SUSTO -> susto(p, es, cfg);
			default -> salida(p, es);
		}
	}

	private static void entrada(ServerPlayerEntity p, Escena es) {
		if (es.tf == 1) {
			// se van los efectos de la cyberpsychosis mientras dura la alucinación (siguen en cyberpsychosis)
			p.removeStatusEffect(StatusEffects.NAUSEA);
			p.removeStatusEffect(StatusEffects.DARKNESS);
			p.removeStatusEffect(StatusEffects.STRENGTH);
			p.removeStatusEffect(StatusEffects.SPEED);
			CiberPsicosis.efecto(p, StatusEffects.BLINDNESS, 40, 0);
			CiberPsicosis.sonido(p, SoundEvents.AMBIENT_CAVE, SoundCategory.MASTER, 1.5f, 0.5f);
			CiberPsicosis.sonido(p, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.MASTER, 1.0f, 0.5f);
			es.centroX = p.getChunkPos().x;
			es.centroZ = p.getChunkPos().z;
			encolar(p, es);
		}
		procesarCola(p, es);
		if (es.tf == 5) {
			enviarClima(p, es);
			enviarTiempo(p, es);
		}
		if (es.tf >= 10) {
			es.fase = F_ESCENA;
			es.tf = 0;
		}
	}

	private static void escena(ServerPlayerEntity p, Escena es, CiberConfig cfg) {
		procesarCola(p, es);
		if (es.tf % 10 == 0) {
			enviarTiempo(p, es);
			if (p.getChunkPos().x != es.centroX || p.getChunkPos().z != es.centroZ) {
				es.centroX = p.getChunkPos().x;
				es.centroZ = p.getChunkPos().z;
				encolar(p, es);
			}
		}
		sonidos(p, es);

		if (es.figura == null && es.tf >= 120) {
			crearFigura(p, es);
		}
		if (es.figura != null && es.tf % 6 == 0) {
			acercarFigura(p, es);
		}
		boolean cerca = es.figura != null && distancia(p, es.figura) <= 4.5;
		boolean tiempo = es.tf >= cfg.alucinacionSegundos * 20;
		if (cerca || tiempo) {
			if (es.figura != null) {
				es.fase = F_SUSTO;
			} else {
				es.fase = F_SALIDA;
			}
			es.tf = 0;
		}
	}

	private static void sonidos(ServerPlayerEntity p, Escena es) {
		int t = es.tf;
		// ambiente de fondo: tranquilo al principio, cada vez más raro
		if (t % 100 == 20) {
			if (es.tipo == Tipo.PLAYA) {
				CiberPsicosis.sonido(p, SoundEvents.BLOCK_WATER_AMBIENT, SoundCategory.MASTER, 1.3f, 0.8f);
			} else {
				CiberPsicosis.sonido(p, SoundEvents.ENTITY_PHANTOM_AMBIENT, SoundCategory.MASTER, 0.6f, 0.5f);
			}
		}
		if (t < 100) {
			return;
		}
		if (t % 40 == 0 && t > 200) {
			CiberPsicosis.sonido(p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.MASTER, 1.5f, 1.0f);
		}
		if (t % 90 == 45) {
			Vec3d pos = CiberPsicosis.local(p, RNG.nextInt(17) - 8, 0, RNG.nextInt(17) - 8);
			switch (RNG.nextInt(6)) {
				case 0 -> CiberPsicosis.sonido(p, SoundEvents.ENTITY_WARDEN_NEARBY_CLOSE, SoundCategory.HOSTILE, pos, 1.0f, 0.9f);
				case 1 -> CiberPsicosis.sonido(p, SoundEvents.ENTITY_ENDERMAN_STARE, SoundCategory.HOSTILE, pos, 1.0f, 0.7f);
				case 2 -> CiberPsicosis.sonido(p, SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.HOSTILE, pos, 0.6f, 0.6f);
				case 3 -> CiberPsicosis.sonido(p, SoundEvents.ENTITY_ZOMBIE_VILLAGER_AMBIENT, SoundCategory.HOSTILE, pos, 1.0f, 0.5f);
				case 4 -> CiberPsicosis.sonido(p, SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, pos, 0.6f, 0.6f);
				default -> CiberPsicosis.sonido(p, SoundEvents.ENTITY_PLAYER_BREATH, SoundCategory.PLAYERS,
						CiberPsicosis.local(p, 0, 0, -1), 1.2f, 0.5f);
			}
		}
		if (es.tipo == Tipo.PLAYA && t > 150 && t % 60 == 30) {
			// la playa "se equivoca": sonidos del fondo del mar
			CiberPsicosis.sonido(p, SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT, SoundCategory.HOSTILE, 0.8f, 0.5f);
		}
	}

	private static void susto(ServerPlayerEntity p, Escena es, CiberConfig cfg) {
		if (es.tf == 1) {
			// la figura aparece pegada a su cara, en silencio roto por el ruido
			Vec3d dir = p.getRotationVec(1.0f);
			double len = Math.max(0.0001, Math.hypot(dir.x, dir.z));
			double fx = p.getX() + dir.x / len * 2.2;
			double fz = p.getZ() + dir.z / len * 2.2;
			Double suelo = alturaSuelo(es.mundo, fx, fz);
			double fy = suelo != null ? suelo : p.getY();
			moverFigura(p, es, fx, fy, fz);
			CiberPsicosis.sonido(p, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.MASTER, 1.0f, 0.7f);
			CiberPsicosis.sonido(p, SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.MASTER, 1.5f, 0.8f);
			CiberPsicosis.sonido(p, SoundEvents.ENTITY_ENDERMAN_SCREAM, SoundCategory.MASTER, 1.2f, 0.6f);
			p.networkHandler.requestTeleport(p.getX(), p.getY(), p.getZ(), p.getYaw(),
					MathHelper.clamp(p.getPitch() + 8.0f, -90.0f, 90.0f));
		}
		if (es.tf == 1 || es.tf == 4 || es.tf == 8) {
			CiberPsicosis.overlay(p, 4, cfg);
		}
		if (es.tf >= 14) {
			es.fase = F_SALIDA;
			es.tf = 0;
		}
	}

	/** Silencio brusco, ceguera, vuelve la realidad y el latido. La cyberpsychosis sigue. */
	private static void salida(ServerPlayerEntity p, Escena es) {
		if (es.tf == 1) {
			p.networkHandler.sendPacket(new StopSoundS2CPacket(null, null));
			CiberPsicosis.efecto(p, StatusEffects.BLINDNESS, 40, 0);
			destruirFigura(p, es);
		}
		if (es.tf == 8) {
			restaurar(p, es);
		}
		if (es.tf == 16) {
			CiberPsicosis.sonido(p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.MASTER, 2.0f, 1.2f);
			CiberPsicosis.aplicarEfectos(p);
		}
		if (es.tf >= 40) {
			ESCENAS.remove(p.getUuid());
			ESPERA.put(p.getUuid(), esperaAleatoria(CiberConfig.get()));
		}
	}

	private static void terminarYa(ServerPlayerEntity p, Escena es) {
		destruirFigura(p, es);
		restaurar(p, es);
		ESCENAS.remove(p.getUuid());
		ESPERA.put(p.getUuid(), esperaAleatoria(CiberConfig.get()));
	}

	// ------------------------------------------------------------------ mundo falso

	/** Pone en cola los chunks cercanos que todavía no se han falseado. */
	private static void encolar(ServerPlayerEntity p, Escena es) {
		int vista = p.getServer() != null ? p.getServer().getPlayerManager().getViewDistance() : RADIO_CHUNKS;
		int r = Math.max(2, Math.min(RADIO_CHUNKS, vista));
		List<int[]> nuevas = new ArrayList<>();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				int d2 = dx * dx + dz * dz;
				if (d2 > r * r) {
					continue;
				}
				ChunkPos cp = new ChunkPos(es.centroX + dx, es.centroZ + dz);
				if (es.hechas.add(cp.toLong())) {
					nuevas.add(new int[]{cp.x, cp.z, d2});
				}
			}
		}
		nuevas.sort(Comparator.comparingInt(a -> a[2]));
		for (int[] c : nuevas) {
			es.cola.add(new ChunkPos(c[0], c[1]));
		}
	}

	private static void procesarCola(ServerPlayerEntity p, Escena es) {
		try {
			for (int i = 0; i < CHUNKS_POR_TICK && !es.cola.isEmpty(); i++) {
				ChunkPos cp = es.cola.poll();
				WorldChunk real = es.mundo.getChunkManager().getWorldChunk(cp.x, cp.z);
				if (real == null) {
					es.hechas.remove(cp.toLong()); // aún no cargado: se reintenta luego
					continue;
				}
				enviarChunkFalso(p, es, real);
				es.enviados.add(cp);
			}
		} catch (Exception e) {
			ZxkStudio.LOGGER.error("Falló el paisaje falso de la alucinación; se cancela", e);
			es.cola.clear();
			es.fase = F_SALIDA;
			es.tf = 0;
		}
	}

	/**
	 * Copia el chunk real, cambia la superficie (y los cantos de los acantilados) por bloques del
	 * mismo tamaño y le pone el bioma de playa o de montaña helada. Solo se envía a este jugador.
	 */
	private static void enviarChunkFalso(ServerPlayerEntity p, Escena es, WorldChunk real) {
		ServerWorld w = es.mundo;
		Registry<Biome> registro = w.getRegistryManager().get(RegistryKeys.BIOME);
		RegistryEntry<Biome> bioma = registro.entryOf(es.tipo == Tipo.PLAYA ? BiomeKeys.BEACH : BiomeKeys.FROZEN_PEAKS);
		boolean nieve = es.tipo == Tipo.NIEVE;

		WorldChunk falso = new WorldChunk(w, real.getPos());
		ChunkSection[] origen = real.getSectionArray();
		ChunkSection[] destino = falso.getSectionArray();
		int secciones = Math.min(origen.length, destino.length);

		int[][] cima = new int[16][16];
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				cima[x][z] = real.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
			}
		}
		int[][] bajo = new int[16][16];
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int m = cima[x][z];
				if (x > 0) {
					m = Math.min(m, cima[x - 1][z]);
				}
				if (x < 15) {
					m = Math.min(m, cima[x + 1][z]);
				}
				if (z > 0) {
					m = Math.min(m, cima[x][z - 1]);
				}
				if (z < 15) {
					m = Math.min(m, cima[x][z + 1]);
				}
				bajo[x][z] = Math.min(m - 1, cima[x][z] - 3);
			}
		}
		boolean[][] cambiado = new boolean[16][16];
		int base = w.getBottomY();
		for (int i = 0; i < secciones; i++) {
			ChunkSection s = origen[i];
			ChunkSection d = new ChunkSection(s.getBlockStateContainer().copy(),
					new PalettedContainer<>(registro.getIndexedEntries(), bioma,
							PalettedContainer.PaletteProvider.BIOME));
			destino[i] = d;
			int y0 = base + i * 16;
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					int ymin = Math.max(bajo[x][z], y0);
					int ymax = Math.min(cima[x][z] + 1, y0 + 15);
					for (int y = ymin; y <= ymax; y++) {
						BlockState viejo = s.getBlockState(x, y - y0, z);
						BlockState nuevo;
						if (y == cima[x][z] + 1) {
							nuevo = cambiado[x][z] ? encima(viejo, nieve) : null;
						} else {
							nuevo = suelo(viejo, nieve, y, cima[x][z]);
							if (y == cima[x][z] && nuevo != null) {
								cambiado[x][z] = true;
							}
						}
						if (nuevo != null) {
							d.setBlockState(x, y - y0, z, nuevo, false);
						}
					}
				}
			}
		}
		p.networkHandler.sendPacket(new ChunkDataS2CPacket(falso, w.getLightingProvider(), null, null));
	}

	/** Bloque del suelo falso (mismo tamaño que el real) o null si se queda igual. */
	private static BlockState suelo(BlockState s, boolean nieve, int y, int cima) {
		if (!s.getFluidState().isEmpty()) {
			return null;
		}
		Block b = s.getBlock();
		if (nieve) {
			if (TIERRA.contains(b) || ARENA.contains(b)) {
				return Blocks.SNOW_BLOCK.getDefaultState();
			}
			if (PIEDRA.contains(b) && y >= cima - 1) {
				return Blocks.SNOW_BLOCK.getDefaultState();
			}
			return null;
		}
		if (TIERRA.contains(b)) {
			return Blocks.SAND.getDefaultState();
		}
		if (PIEDRA.contains(b)) {
			return Blocks.SANDSTONE.getDefaultState();
		}
		return null;
	}

	/** Lo que hay justo encima del suelo: hierbas fuera en la playa, una capa fina de nieve en la nieve. */
	private static BlockState encima(BlockState s, boolean nieve) {
		if (s.isAir()) {
			return nieve ? Blocks.SNOW.getDefaultState() : null; // capa fina: sin colisión
		}
		if (s.isReplaceable() && s.getFluidState().isEmpty()) {
			return nieve ? Blocks.SNOW.getDefaultState() : Blocks.AIR.getDefaultState();
		}
		return null;
	}

	/** Reenvía el mundo real, la hora y el clima. */
	private static void restaurar(ServerPlayerEntity p, Escena es) {
		if (es.restaurado) {
			return;
		}
		es.restaurado = true;
		ServerWorld w = es.mundo;
		for (ChunkPos cp : es.enviados) {
			WorldChunk real = w.getChunkManager().getWorldChunk(cp.x, cp.z);
			if (real != null) {
				p.networkHandler.sendPacket(new ChunkDataS2CPacket(real, w.getLightingProvider(), null, null));
			}
		}
		es.enviados.clear();
		es.cola.clear();
		p.networkHandler.sendPacket(new WorldTimeUpdateS2CPacket(w.getTime(), w.getTimeOfDay(),
				w.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)));
		p.networkHandler.sendPacket(new GameStateChangeS2CPacket(
				GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, w.getRainGradient(1.0f)));
		p.networkHandler.sendPacket(new GameStateChangeS2CPacket(
				GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, w.getThunderGradient(1.0f)));
	}

	private static void enviarTiempo(ServerPlayerEntity p, Escena es) {
		long hora = es.tipo == Tipo.PLAYA ? 6000L : 15500L;
		// el servidor reenvía la hora real cada segundo, por eso se repite cada 10 ticks
		p.networkHandler.sendPacket(new WorldTimeUpdateS2CPacket(es.mundo.getTime(), hora, false));
	}

	private static void enviarClima(ServerPlayerEntity p, Escena es) {
		// en el bioma helado falso, la "lluvia" se ve como una ventisca de nieve
		float lluvia = es.tipo == Tipo.NIEVE ? 1.0f : 0.0f;
		p.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, lluvia));
		p.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, 0.0f));
	}

	// ------------------------------------------------------------------ figura falsa

	private static Double alturaSuelo(ServerWorld w, double x, double z) {
		int bx = MathHelper.floor(x);
		int bz = MathHelper.floor(z);
		if (!w.isChunkLoaded(bx >> 4, bz >> 4)) {
			return null;
		}
		return (double) w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, bx, bz);
	}

	private static double distancia(ServerPlayerEntity p, Entity figura) {
		return Math.hypot(figura.getX() - p.getX(), figura.getZ() - p.getZ());
	}

	/** Una figura que solo ve este jugador: Ahogado, Warden o la Sombra (negra y alta). */
	private static void crearFigura(ServerPlayerEntity p, Escena es) {
		double ang = Math.toRadians(p.getYaw() + RNG.nextInt(101) - 50);
		double fx = p.getX() - Math.sin(ang) * 22.0;
		double fz = p.getZ() + Math.cos(ang) * 22.0;
		Double fy = alturaSuelo(es.mundo, fx, fz);
		if (fy == null) {
			return;
		}
		Bicho bicho = es.tipo == Tipo.NIEVE
				? (RNG.nextBoolean() ? Bicho.WARDEN : Bicho.SOMBRA)
				: (RNG.nextBoolean() ? Bicho.AHOGADO : Bicho.SOMBRA);
		Entity figura = switch (bicho) {
			case AHOGADO -> EntityType.DROWNED.create(es.mundo);
			case WARDEN -> EntityType.WARDEN.create(es.mundo);
			// la "Sombra" es un Illusioner (no sale en el juego normal) con piel negra propia del mod
			default -> EntityType.ILLUSIONER.create(es.mundo);
		};
		if (figura == null) {
			return;
		}
		es.bicho = bicho;
		es.figura = figura; // no se añade al mundo: solo se usa para tener un id y mandar paquetes
		figura.refreshPositionAndAngles(fx, fy, fz, miradaHacia(p, fx, fz), 0.0f);
		p.networkHandler.sendPacket(new EntitySpawnS2CPacket(figura.getId(), figura.getUuid(), fx, fy, fz,
				0.0f, figura.getYaw(), figura.getType(), 0, Vec3d.ZERO, figura.getYaw()));
		if (bicho == Bicho.SOMBRA) {
			// más alto de lo normal
			EntityAttributeInstance escala = new EntityAttributeInstance(EntityAttributes.GENERIC_SCALE, inst -> {
			});
			escala.setBaseValue(1.7);
			p.networkHandler.sendPacket(new EntityAttributesS2CPacket(figura.getId(), List.of(escala)));
		}
		enviarPose(p, figura);
	}

	/** Se acerca solo cuando el jugador no la está mirando. */
	private static void acercarFigura(ServerPlayerEntity p, Escena es) {
		Entity f = es.figura;
		double dx = p.getX() - f.getX();
		double dz = p.getZ() - f.getZ();
		double d = Math.hypot(dx, dz);
		if (d < 0.001) {
			return;
		}
		Vec3d mira = p.getRotationVec(1.0f);
		double dot = (mira.x * -dx + mira.z * -dz) / (Math.max(0.0001, Math.hypot(mira.x, mira.z)) * d);
		if (dot > 0.55) {
			// la está mirando: solo gira la cabeza hacia él
			moverFigura(p, es, f.getX(), f.getY(), f.getZ());
			return;
		}
		double paso = 1.3;
		double nx = f.getX() + dx / d * paso;
		double nz = f.getZ() + dz / d * paso;
		Double ny = alturaSuelo(es.mundo, nx, nz);
		moverFigura(p, es, nx, ny != null ? ny : f.getY(), nz);
	}

	private static void moverFigura(ServerPlayerEntity p, Escena es, double x, double y, double z) {
		Entity f = es.figura;
		if (f == null) {
			return;
		}
		f.refreshPositionAndAngles(x, y, z, miradaHacia(p, x, z), 0.0f);
		enviarPose(p, f);
	}

	private static void enviarPose(ServerPlayerEntity p, Entity f) {
		p.networkHandler.sendPacket(new EntityPositionS2CPacket(f));
		p.networkHandler.sendPacket(new EntitySetHeadYawS2CPacket(f, (byte) MathHelper.floor(f.getYaw() * 256.0f / 360.0f)));
	}

	private static float miradaHacia(ServerPlayerEntity p, double x, double z) {
		return (float) (Math.toDegrees(Math.atan2(p.getZ() - z, p.getX() - x))) - 90.0f;
	}

	private static void destruirFigura(ServerPlayerEntity p, Escena es) {
		if (es.figura != null) {
			p.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(es.figura.getId()));
			es.figura = null;
		}
	}
}
