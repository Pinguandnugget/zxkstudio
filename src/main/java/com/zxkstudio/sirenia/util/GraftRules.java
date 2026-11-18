package com.zxkstudio.sirenia.util;

import com.zxkstudio.sirenia.config.SireniaConfig;
import com.zxkstudio.sirenia.registry.ModTags;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Reglas de cultivo: barro bajo la base del árbol, agua cercana, madera pelada conectada y cara libre. */
public final class GraftRules {
	private static final int MAX_TRUNK_SCAN = 48;

	private GraftRules() {
	}

	public enum Result {
		OK("ok"),
		NOT_STRIPPED("not_stripped"),
		BLOCKED("blocked"),
		NO_MUD("no_mud"),
		NO_WATER("no_water");

		public final String key;

		Result(String key) {
			this.key = key;
		}
	}

	/** Comprueba todo para colocar un injerto en la cara lateral {@code side} del tronco en {@code logPos}. */
	public static Result checkPlacement(World world, BlockPos logPos, Direction side) {
		if (side.getAxis().isVertical() || !world.getBlockState(logPos).isIn(ModTags.STRIPPED_SIRENIA_LOGS)) {
			return Result.NOT_STRIPPED;
		}
		BlockPos target = logPos.offset(side);
		BlockState targetState = world.getBlockState(target);
		boolean free = targetState.isAir() || (targetState.isReplaceable() && targetState.getFluidState().isEmpty());
		if (!free) {
			return Result.BLOCKED;
		}
		return checkSite(world, logPos);
	}

	/** Comprueba solo el emplazamiento (barro + agua), usado también para pausar o reanudar el crecimiento. */
	public static Result checkSite(World world, BlockPos logPos) {
		BlockPos base = findBase(world, logPos);
		if (!world.getBlockState(base.down()).isOf(Blocks.MUD)) {
			return Result.NO_MUD;
		}
		if (!hasEnoughWater(world, base)) {
			return Result.NO_WATER;
		}
		return Result.OK;
	}

	/** Baja por la columna de troncos de Sirenia hasta el más bajo: esa es la base del árbol. */
	public static BlockPos findBase(World world, BlockPos from) {
		BlockPos.Mutable cursor = from.mutableCopy();
		for (int i = 0; i < MAX_TRUNK_SCAN; i++) {
			if (world.getBlockState(cursor.down()).isIn(ModTags.SIRENIA_LOGS)) {
				cursor.move(Direction.DOWN);
			} else {
				break;
			}
		}
		return cursor.toImmutable();
	}

	/** Cuenta fuentes de agua en un cuadrado horizontal alrededor de la base (sin cargar chunks). */
	public static boolean hasEnoughWater(World world, BlockPos base) {
		SireniaConfig cfg = SireniaConfig.get();
		int needed = cfg.required_water_blocks;
		if (needed <= 0) {
			return true;
		}
		int r = cfg.water_detection_radius;
		int found = 0;
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				int x = base.getX() + dx;
				int z = base.getZ() + dz;
				if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
					continue;
				}
				for (int dy = -1; dy <= 1; dy++) {
					p.set(x, base.getY() + dy, z);
					FluidState fluid = world.getFluidState(p);
					if (fluid.isStill() && fluid.isIn(FluidTags.WATER)) {
						found++;
						if (found >= needed) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}
