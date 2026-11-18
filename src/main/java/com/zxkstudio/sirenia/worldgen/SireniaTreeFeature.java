package com.zxkstudio.sirenia.worldgen;

import com.mojang.serialization.Codec;
import com.zxkstudio.sirenia.config.SireniaConfig;
import com.zxkstudio.sirenia.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Árbol de Sirenia generado de forma natural. Una instancia por zona (manglar, costa, cueva) para poder
 * leer una probabilidad distinta de la configuración.
 *
 * Reglas de seguridad:
 *  - solo se lee y escribe a menos de 9 bloques del origen (dentro de la región de generación),
 *  - el tronco, las raíces y las hojas solo ocupan aire o plantas reemplazables (nunca estructuras),
 *  - el único bloque "ajeno" que se cambia es el suelo natural bajo el tronco, que pasa a barro,
 *  - si la configuración lo exige, solo aparece con suficiente agua cerca de la base (la misma regla del cultivo).
 */
public class SireniaTreeFeature extends Feature<DefaultFeatureConfig> {
	public enum Kind {
		MANGROVE, COAST, CAVE
	}

	private static final int MIN_HEIGHT = 9;
	private static final int MAX_HEIGHT = 15;
	private static final int[] CROWN_RADIUS = {2, 3, 3, 3, 2, 1}; // desde 3 bloques bajo la punta hasta 2 por encima

	private final Kind kind;

	public SireniaTreeFeature(Codec<DefaultFeatureConfig> codec, Kind kind) {
		super(codec);
		this.kind = kind;
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		SireniaConfig cfg = SireniaConfig.get();
		if (!cfg.generation_enabled) {
			return false;
		}
		Random random = context.getRandom();
		double chance = switch (kind) {
			case MANGROVE -> cfg.mangrove_chance;
			case COAST -> cfg.coast_chance;
			case CAVE -> cfg.lush_cave_chance;
		};
		if (random.nextDouble() >= chance) {
			return false;
		}
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		for (int attempt = 0; attempt < 6; attempt++) {
			BlockPos column = origin.add(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
			BlockPos ground = kind == Kind.CAVE ? findCaveFloor(world, column) : findSurfaceGround(world, column);
			if (ground != null && grow(world, random, ground, cfg)) {
				return true;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------- búsqueda de suelo

	private static boolean isNaturalGround(BlockState state) {
		return state.isIn(BlockTags.DIRT) || state.isIn(BlockTags.SAND) || state.isIn(BlockTags.BASE_STONE_OVERWORLD)
				|| state.isOf(Blocks.CLAY) || state.isOf(Blocks.GRAVEL) || state.isOf(Blocks.MUDDY_MANGROVE_ROOTS)
				|| state.isOf(Blocks.MOSS_BLOCK);
	}

	/** Un bloque por el que el árbol puede "atravesar": aire, plantas, o hojas (nunca agua ni sólidos). */
	private static boolean isFree(BlockState state) {
		return state.isAir() || (state.isReplaceable() && state.getFluidState().isEmpty());
	}

	private static BlockPos findSurfaceGround(StructureWorldAccess world, BlockPos column) {
		int top = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, column.getX(), column.getZ());
		BlockPos.Mutable p = new BlockPos.Mutable(column.getX(), top - 1, column.getZ());
		for (int i = 0; i < 8; i++) {
			if (world.isOutOfHeightLimit(p.getY())) {
				return null;
			}
			BlockState state = world.getBlockState(p);
			if (isNaturalGround(state)) {
				return isFree(world.getBlockState(p.up())) ? p.toImmutable() : null; // nada de suelo bajo el agua
			}
			if (!state.isAir() && state.getFluidState().isEmpty() && !state.isReplaceable()) {
				return null; // un bloque que no es suelo natural (estructura, tronco de otro árbol...)
			}
			p.move(Direction.DOWN);
		}
		return null;
	}

	private static BlockPos findCaveFloor(StructureWorldAccess world, BlockPos start) {
		BlockPos.Mutable p = start.mutableCopy();
		// Si empezamos dentro de roca, subimos hasta encontrar aire (máx. 8 bloques).
		for (int i = 0; i < 8 && !world.isOutOfHeightLimit(p.getY()) && !isFree(world.getBlockState(p)); i++) {
			p.move(Direction.UP);
		}
		if (!isFree(world.getBlockState(p))) {
			return null;
		}
		for (int i = 0; i < 24; i++) {
			p.move(Direction.DOWN);
			if (world.isOutOfHeightLimit(p.getY())) {
				return null;
			}
			BlockState state = world.getBlockState(p);
			if (isNaturalGround(state)) {
				return p.toImmutable();
			}
			if (!isFree(state)) {
				return null;
			}
		}
		return null;
	}

	// ---------------------------------------------------------------- agua

	private static boolean hasWater(StructureWorldAccess world, BlockPos base, SireniaConfig cfg) {
		int needed = cfg.required_water_blocks;
		if (needed <= 0) {
			return true;
		}
		int r = cfg.water_detection_radius;
		int found = 0;
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				for (int dy = -1; dy <= 1; dy++) {
					p.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
					FluidState fluid = world.getFluidState(p);
					if (fluid.isStill() && fluid.isIn(FluidTags.WATER) && ++found >= needed + 2) {
						return true; // margen de 2 por si las raíces tapan algún bloque
					}
				}
			}
		}
		return false;
	}

	// ---------------------------------------------------------------- construcción

	private boolean grow(StructureWorldAccess world, Random random, BlockPos ground, SireniaConfig cfg) {
		BlockPos base = ground.up();
		int height = MIN_HEIGHT + random.nextInt(MAX_HEIGHT - MIN_HEIGHT + 1);

		// Hace falta la columna libre del tronco (y la de la punta) antes de tocar nada.
		for (int i = 0; i < height + 2; i++) {
			BlockPos p = base.up(i);
			if (world.isOutOfHeightLimit(p.getY())) {
				return false;
			}
			BlockState s = world.getBlockState(p);
			if (!isFree(s) && !(i >= height - 1 && s.getBlock() instanceof LeavesBlock)) {
				if (kind == Kind.CAVE || i < height) {
					return false;
				}
			}
		}
		if (cfg.require_water_for_generation && !hasWater(world, base, cfg)) {
			return false;
		}

		Set<BlockPos> logs = new HashSet<>();
		Set<BlockPos> leaves = new HashSet<>();

		// Base de barro bajo el tronco (necesaria para el sistema de cultivo).
		world.setBlockState(ground, Blocks.MUD.getDefaultState(), Block.NOTIFY_LISTENERS);

		// Tronco recto, para que se puedan injertar sus cuatro caras.
		BlockState trunk = ModBlocks.SIRENIA_LOG.getDefaultState();
		for (int i = 0; i < height; i++) {
			BlockPos p = base.up(i);
			world.setBlockState(p, trunk, Block.NOTIFY_LISTENERS);
			logs.add(p);
		}

		placeRoots(world, random, base, logs);

		BlockPos top = base.up(height - 1);
		crown(world, random, top, logs, leaves);

		// Dos ramas laterales cortas con su propia mata de hojas.
		int branches = 1 + random.nextInt(2);
		for (int b = 0; b < branches; b++) {
			Direction dir = Direction.Type.HORIZONTAL.random(random);
			int y = height - 5 - random.nextInt(2);
			if (y < 3) {
				continue;
			}
			BlockPos a = base.up(y).offset(dir);
			BlockPos c = a.offset(dir);
			if (isFree(world.getBlockState(a)) && isFree(world.getBlockState(c))) {
				BlockState branch = ModBlocks.SIRENIA_LOG.getDefaultState().with(PillarBlock.AXIS, dir.getAxis());
				world.setBlockState(a, branch, Block.NOTIFY_LISTENERS);
				world.setBlockState(c, branch, Block.NOTIFY_LISTENERS);
				logs.add(a);
				logs.add(c);
				blob(world, random, c.up(), 2, logs, leaves);
			}
		}

		placeLeaves(world, logs, leaves);
		return true;
	}

	/** Raíces tipo manglar: 3 o 4 brazos que salen del pie del tronco y bajan hacia el suelo. */
	private static void placeRoots(StructureWorldAccess world, Random random, BlockPos base, Set<BlockPos> logs) {
		BlockState wood = ModBlocks.SIRENIA_WOOD.getDefaultState();
		for (Direction dir : Direction.Type.HORIZONTAL) {
			if (random.nextInt(4) == 0) {
				continue;
			}
			BlockPos first = base.offset(dir);
			BlockPos second = first.offset(dir);
			if (!isFree(world.getBlockState(first)) || !isFree(world.getBlockState(second))) {
				continue;
			}
			BlockState horizontal = wood.with(PillarBlock.AXIS, dir.getAxis());
			world.setBlockState(first, horizontal, Block.NOTIFY_LISTENERS);
			world.setBlockState(second, horizontal, Block.NOTIFY_LISTENERS);
			logs.add(first);
			logs.add(second);
			// Si el suelo cae junto a la punta de la raíz, esta sigue hacia abajo (máx. 3 bloques).
			BlockPos.Mutable down = second.down().mutableCopy();
			for (int i = 0; i < 3 && isFree(world.getBlockState(down)); i++) {
				world.setBlockState(down, wood.with(PillarBlock.AXIS, Direction.Axis.Y), Block.NOTIFY_LISTENERS);
				logs.add(down.toImmutable());
				down.move(Direction.DOWN);
			}
		}
	}

	/** Copa irregular: capas de radio variable alrededor de la punta del tronco. */
	private static void crown(StructureWorldAccess world, Random random, BlockPos top, Set<BlockPos> logs, Set<BlockPos> leaves) {
		for (int layer = 0; layer < CROWN_RADIUS.length; layer++) {
			int r = CROWN_RADIUS[layer];
			int y = top.getY() - 3 + layer;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					int d2 = dx * dx + dz * dz;
					if (d2 > r * r + 1) {
						continue;
					}
					if (d2 >= r * r - 1 && random.nextInt(4) == 0) {
						continue; // bordes irregulares
					}
					putLeaf(world, new BlockPos(top.getX() + dx, y, top.getZ() + dz), logs, leaves);
				}
			}
		}
	}

	private static void blob(StructureWorldAccess world, Random random, BlockPos center, int radius, Set<BlockPos> logs, Set<BlockPos> leaves) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					int d2 = dx * dx + dy * dy * 2 + dz * dz;
					if (d2 > radius * radius + 1 || (d2 >= radius * radius && random.nextInt(3) == 0)) {
						continue;
					}
					putLeaf(world, center.add(dx, dy, dz), logs, leaves);
				}
			}
		}
	}

	private static void putLeaf(StructureWorldAccess world, BlockPos pos, Set<BlockPos> logs, Set<BlockPos> leaves) {
		if (world.isOutOfHeightLimit(pos.getY()) || logs.contains(pos) || leaves.contains(pos)) {
			return;
		}
		if (isFree(world.getBlockState(pos))) {
			leaves.add(pos);
		}
	}

	/** Coloca las hojas con la distancia al tronco ya calculada para que no se caigan al generarse. */
	private static void placeLeaves(StructureWorldAccess world, Set<BlockPos> logs, Set<BlockPos> leaves) {
		Map<BlockPos, Integer> distance = new HashMap<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (BlockPos leaf : leaves) {
			for (Direction d : Direction.values()) {
				if (logs.contains(leaf.offset(d))) {
					distance.put(leaf, 1);
					queue.add(leaf);
					break;
				}
			}
		}
		while (!queue.isEmpty()) {
			BlockPos cur = queue.poll();
			int next = distance.get(cur) + 1;
			if (next > 6) {
				continue;
			}
			for (Direction d : Direction.values()) {
				BlockPos n = cur.offset(d);
				if (leaves.contains(n) && !distance.containsKey(n)) {
					distance.put(n, next);
					queue.add(n);
				}
			}
		}
		BlockState base = ModBlocks.SIRENIA_LEAVES.getDefaultState();
		for (BlockPos leaf : leaves) {
			int dist = distance.getOrDefault(leaf, 7);
			if (dist >= 7) {
				continue; // hoja suelta: no se coloca
			}
			world.setBlockState(leaf, base.with(LeavesBlock.DISTANCE, dist), Block.NOTIFY_LISTENERS);
		}
	}
}
