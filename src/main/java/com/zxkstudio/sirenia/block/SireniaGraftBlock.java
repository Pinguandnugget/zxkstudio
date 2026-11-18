package com.zxkstudio.sirenia.block;

import com.mojang.serialization.MapCodec;
import com.zxkstudio.sirenia.blockentity.SireniaGraftBlockEntity;
import com.zxkstudio.sirenia.config.SireniaConfig;
import com.zxkstudio.sirenia.registry.ModBlocks;
import com.zxkstudio.sirenia.registry.ModItems;
import com.zxkstudio.sirenia.registry.ModTags;
import com.zxkstudio.sirenia.util.FruitAging;
import com.zxkstudio.sirenia.util.GraftRules;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Injerto y fruto del Corazón de Sirenia. Es un bloque pegado al costado de un tronco pelado.
 *
 * stage 0 = semilla injertada, 1 = fruto pequeño, 2 = maduro, 3 = marchito, 4 = podrido.
 * FACING = hacia dónde sobresale (el tronco está en la dirección contraria).
 */
public class SireniaGraftBlock extends Block implements BlockEntityProvider, Fertilizable {
	public static final MapCodec<SireniaGraftBlock> CODEC = createCodec(SireniaGraftBlock::new);
	public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
	public static final IntProperty STAGE = IntProperty.of("stage", 0, 4);

	/** Cada cuántos ticks se revisa un injerto (5 s). Es un tick programado, no un tick por bloque. */
	public static final int TICK_INTERVAL = 100;

	private static final VoxelShape[][] SHAPES = buildShapes();

	public SireniaGraftBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(STAGE, 0));
	}

	@Override
	public MapCodec<SireniaGraftBlock> getCodec() {
		return CODEC;
	}

	public static int lightFor(int stage) {
		return switch (stage) {
			case 0 -> 2;
			case 1 -> 3;
			case 2 -> 6;
			case 3 -> 3;
			default -> 0;
		};
	}

	public static boolean isGraftSupport(BlockState state) {
		return state.isIn(ModTags.STRIPPED_SIRENIA_LOGS);
	}

	/** Coloca un injerto nuevo en {@code target}, sobresaliendo hacia {@code side}. */
	public static boolean plant(World world, BlockPos target, Direction side) {
		BlockState current = world.getBlockState(target);
		boolean free = current.isAir() || (current.isReplaceable() && current.getFluidState().isEmpty());
		if (!free) {
			return false;
		}
		BlockState graft = ModBlocks.SIRENIA_GRAFT.getDefaultState().with(FACING, side).with(STAGE, 0);
		if (!world.setBlockState(target, graft, Block.NOTIFY_ALL)) {
			return false;
		}
		world.playSound(null, target, SoundEvents.BLOCK_MANGROVE_ROOTS_PLACE, SoundCategory.BLOCKS, 1.0f, 0.9f);
		world.emitGameEvent(GameEvent.BLOCK_PLACE, target, GameEvent.Emitter.of(graft));
		return true;
	}

	public static int stageFor(long growthTicks) {
		SireniaConfig c = SireniaConfig.get();
		if (growthTicks >= c.rotTicks()) {
			return 4;
		}
		if (growthTicks >= c.wiltTicks()) {
			return 3;
		}
		if (growthTicks >= c.matureTicks()) {
			return 2;
		}
		if (growthTicks >= c.smallTicks()) {
			return 1;
		}
		return 0;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, STAGE);
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new SireniaGraftBlockEntity(pos, state);
	}

	// ---------------------------------------------------------------- forma y soporte

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		int stage = state.get(STAGE);
		int shapeStage = stage == 0 ? 0 : stage == 1 ? 1 : 2;
		return SHAPES[shapeStage][state.get(FACING).getHorizontal()];
	}

	/**
	 * Sin colisión, pero SIN usar noCollision(): así el bloque cuenta como "que bloquea el paso" para los
	 * fluidos y el agua que rodea al árbol no lo arrastra ni lo rompe, aunque los jugadores lo atraviesen.
	 */
	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.empty();
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return isGraftSupport(world.getBlockState(pos.offset(state.get(FACING).getOpposite())));
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
			WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		// Si desaparece (o se despela) el tronco que lo sostiene, el injerto desaparece con él.
		if (direction == state.get(FACING).getOpposite() && !isGraftSupport(neighborState)) {
			return Blocks.AIR.getDefaultState();
		}
		return state;
	}

	// ---------------------------------------------------------------- crecimiento

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		if (!world.isClient && !oldState.isOf(this)) {
			world.scheduleBlockTick(pos, this, TICK_INTERVAL);
		}
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		BlockPos support = pos.offset(state.get(FACING).getOpposite());
		if (!isGraftSupport(world.getBlockState(support))) {
			world.removeBlock(pos, false);
			return;
		}
		if (!(world.getBlockEntity(pos) instanceof SireniaGraftBlockEntity graft)) {
			return;
		}
		// Si el barro o el agua faltan, el crecimiento se pausa pero el reloj sigue comprobando.
		if (GraftRules.checkSite(world, support) == GraftRules.Result.OK) {
			graft.growthTicks += TICK_INTERVAL;
			graft.markDirty();
		}
		int stage = stageFor(graft.growthTicks);
		if (stage != state.get(STAGE)) {
			world.setBlockState(pos, state.with(STAGE, stage), Block.NOTIFY_ALL);
		}
		if (stage < 4) {
			world.scheduleBlockTick(pos, this, TICK_INTERVAL);
		}
	}

	// ---------------------------------------------------------------- polvo de hueso

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		if (state.get(STAGE) >= 2 || !(world instanceof World realWorld)) {
			return false; // solo mientras crece; no rejuvenece frutos maduros, marchitos ni podridos
		}
		BlockPos support = pos.offset(state.get(FACING).getOpposite());
		return GraftRules.checkSite(realWorld, support) == GraftRules.Result.OK; // si no, no se gasta
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		if (!(world.getBlockEntity(pos) instanceof SireniaGraftBlockEntity graft)) {
			return;
		}
		SireniaConfig c = SireniaConfig.get();
		int small = c.boneMealForSmall();
		if (graft.boneMeal < small && state.get(STAGE) >= 1) {
			graft.boneMeal = small;
		}
		graft.boneMeal++;
		if (graft.boneMeal >= small) {
			graft.growthTicks = Math.max(graft.growthTicks, c.smallTicks());
		}
		if (graft.boneMeal >= c.bone_meal_required) {
			graft.growthTicks = Math.max(graft.growthTicks, c.matureTicks());
		}
		graft.markDirty();
		int stage = stageFor(graft.growthTicks);
		if (stage != state.get(STAGE)) {
			world.setBlockState(pos, state.with(STAGE, stage), Block.NOTIFY_ALL);
		}
	}

	// ---------------------------------------------------------------- cosecha

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
			PlayerEntity player, net.minecraft.util.Hand hand, BlockHitResult hit) {
		// Mientras crece, el clic con polvo de hueso lo gestiona el propio polvo de hueso.
		// Si el fruto ya está maduro, marchito o podrido, el clic cosecha (no se gasta polvo).
		if (stack.isOf(Items.BONE_MEAL) && state.get(STAGE) < 2) {
			return ItemActionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		}
		return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		int stage = state.get(STAGE);
		if (stage < 2) {
			return ActionResult.PASS;
		}
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		// Se vuelve a leer el bloque real: si otro jugador lo cosechó en este mismo tick, no hay nada.
		BlockState current = world.getBlockState(pos);
		if (!current.isOf(this) || current.get(STAGE) != stage
				|| !(world.getBlockEntity(pos) instanceof SireniaGraftBlockEntity graft)) {
			return ActionResult.CONSUME;
		}
		ItemStack fruit = FruitAging.harvested(stage, graft.growthTicks);
		world.removeBlock(pos, false); // primero se quita el bloque: así no puede cosecharse dos veces
		player.getInventory().offerOrDrop(fruit);
		if (stage == FruitAging.MATURE && world.random.nextDouble() < SireniaConfig.get().seed_bonus_chance) {
			player.getInventory().offerOrDrop(new ItemStack(ModItems.SIRENIA_SEED));
		}
		world.playSound(null, pos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.BLOCKS, 1.0f, 1.0f);
		return ActionResult.CONSUME;
	}

	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!world.isClient && !player.isCreative() && state.get(STAGE) >= 2
				&& world.getBlockEntity(pos) instanceof SireniaGraftBlockEntity graft) {
			Block.dropStack(world, pos, FruitAging.harvested(state.get(STAGE), graft.growthTicks));
		}
		return super.onBreak(world, pos, state, player);
	}

	// ---------------------------------------------------------------- formas

	private static VoxelShape[][] buildShapes() {
		// Cajas pensadas para FACING = north (el tronco queda al sur, z = 16).
		double[][] boxes = {
				{2, 2, 15, 14, 14, 16},
				{5.5, 5, 11, 10.5, 10, 16},
				{4, 3, 9, 12, 12, 16}
		};
		VoxelShape[][] shapes = new VoxelShape[3][4];
		for (int s = 0; s < 3; s++) {
			double[] b = boxes[s];
			// Índices de Direction#getHorizontal: SOUTH=0, WEST=1, NORTH=2, EAST=3
			shapes[s][Direction.NORTH.getHorizontal()] = Block.createCuboidShape(b[0], b[1], b[2], b[3], b[4], b[5]);
			shapes[s][Direction.EAST.getHorizontal()] = Block.createCuboidShape(16 - b[5], b[1], b[0], 16 - b[2], b[4], b[3]);
			shapes[s][Direction.SOUTH.getHorizontal()] = Block.createCuboidShape(16 - b[3], b[1], 16 - b[5], 16 - b[0], b[4], 16 - b[2]);
			shapes[s][Direction.WEST.getHorizontal()] = Block.createCuboidShape(b[2], b[1], 16 - b[3], b[5], b[4], 16 - b[0]);
		}
		return shapes;
	}
}
