package com.zxkstudio.sirenia.block;

import com.mojang.serialization.MapCodec;
import com.zxkstudio.sirenia.config.SireniaConfig;
import com.zxkstudio.sirenia.util.GraftRules;
import com.zxkstudio.sirenia.worldgen.SireniaTreeFeature;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.World;

/**
 * Brote del Árbol de Sirenia.
 *
 * Requisitos para plantarlo y para que crezca:
 *  - el bloque de abajo debe ser BARRO,
 *  - el brote no puede estar dentro del agua (necesita aire a su alrededor),
 *  - debe haber suficiente agua cerca (misma regla que el cultivo del fruto, configurable),
 *  - al convertirse en árbol hace falta la columna libre de unos 9 a 15 bloques de alto.
 *
 * Si el sitio deja de cumplir los requisitos, el brote solo deja de crecer (no se rompe).
 */
public class SireniaSaplingBlock extends Block implements Fertilizable {
	public static final MapCodec<SireniaSaplingBlock> CODEC = createCodec(SireniaSaplingBlock::new);
	public static final IntProperty STAGE = IntProperty.of("stage", 0, 2);

	private static final VoxelShape[] SHAPES = {
			Block.createCuboidShape(4, 0, 4, 12, 6, 12),
			Block.createCuboidShape(3, 0, 3, 13, 10, 13),
			Block.createCuboidShape(2, 0, 2, 14, 14, 14)
	};

	public SireniaSaplingBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(STAGE, 0));
	}

	@Override
	public MapCodec<SireniaSaplingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(STAGE);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES[state.get(STAGE)];
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.empty();
	}

	/** Barro debajo y sin fluido en su propia posición. */
	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return world.getBlockState(pos.down()).isOf(Blocks.MUD) && world.getFluidState(pos).isEmpty();
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
			WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		if (direction == Direction.DOWN && !neighborState.isOf(Blocks.MUD)) {
			return Blocks.AIR.getDefaultState();
		}
		return state;
	}

	/** Motivo del fallo (clave de mensaje) o null si el sitio es bueno. Sirve también para el ítem. */
	public static String problemAt(World world, BlockPos pos) {
		if (!world.getBlockState(pos.down()).isOf(Blocks.MUD)) {
			return "no_mud";
		}
		if (!world.getFluidState(pos).isEmpty()) {
			return "in_water";
		}
		if (!GraftRules.hasEnoughWater(world, pos)) {
			return "no_water";
		}
		return null;
	}

	// ---------------------------------------------------------------- crecimiento

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (random.nextInt(SireniaConfig.get().sapling_growth_chance) != 0) {
			return;
		}
		if (problemAt(world, pos) == null) {
			advance(world, random, pos, state);
		}
	}

	private static void advance(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		int stage = state.get(STAGE);
		if (stage < 2) {
			world.setBlockState(pos, state.with(STAGE, stage + 1), Block.NOTIFY_ALL);
			return;
		}
		growTree(world, random, pos, state);
	}

	private static void growTree(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		// Solo si todo el área del árbol ya está cargada: así no se fuerza carga de chunks.
		for (int dx = -7; dx <= 7; dx += 7) {
			for (int dz = -7; dz <= 7; dz += 7) {
				if (!world.getChunkManager().isChunkLoaded((pos.getX() + dx) >> 4, (pos.getZ() + dz) >> 4)) {
					return;
				}
			}
		}
		// Se quita el brote para que su propio bloque no estorbe; si el árbol no cabe, se devuelve.
		world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
		if (SireniaTreeFeature.growFromSapling(world, random, pos.down())) {
			world.playSound(null, pos, SoundEvents.BLOCK_MANGROVE_ROOTS_PLACE, SoundCategory.BLOCKS, 1.2f, 0.7f);
			world.updateNeighborsAlways(pos, Blocks.AIR);
		} else {
			world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
		}
	}

	// ---------------------------------------------------------------- polvo de hueso

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		return world instanceof World w && problemAt(w, pos) == null; // si el sitio no sirve, no se gasta
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return random.nextFloat() < 0.45f;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		advance(world, random, pos, state);
	}
}
