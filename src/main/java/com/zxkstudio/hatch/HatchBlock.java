package com.zxkstudio.hatch;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.Hand;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.*;
import net.minecraft.world.*;

public final class HatchBlock extends BlockWithEntity {
    public static final MapCodec<HatchBlock> CODEC = createCodec(HatchBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final IntProperty PART = IntProperty.of("part", 0, 8);
    public static final BooleanProperty PASSABLE = BooleanProperty.of("passable");
    public static final int CENTER = 4;
    private static final VoxelShape[][][] SHAPES = new VoxelShape[4][9][2];
    private static final ThreadLocal<Boolean> REMOVING = ThreadLocal.withInitial(() -> false);
    static {
        for (Direction facing : Direction.Type.HORIZONTAL) {
            boolean z = facing.getAxis() == Direction.Axis.Z;
            int low=(facing==Direction.NORTH || facing==Direction.WEST)?5:7, high=low+4;
            for (int i=0; i<9; i++) {
                SHAPES[facing.getHorizontal()][i][0] = z
                    ? createCuboidShape(0,0,low,16,16,high) : createCuboidShape(low,0,0,high,16,16);
                VoxelShape shape = VoxelShapes.empty();
                for (int u=0; u<4; u++) for (int v=0; v<4; v++) {
                    double x = (i%3-1)*16+u*4+2-8;
                    double y = (i/3-1)*16+v*4+2-8;
                    if (x*x+y*y < 20*20) continue;
                    int a = u*4;
                    // Local positive x = facing rotated counterclockwise.
                    Direction right = facing.rotateYCounterclockwise();
                    if (right==Direction.WEST || right==Direction.NORTH) a=12-a;
                    shape = VoxelShapes.union(shape,z
                        ? createCuboidShape(a,v*4,low,a+4,v*4+4,high)
                        : createCuboidShape(low,v*4,a,high,v*4+4,a+4));
                }
                SHAPES[facing.getHorizontal()][i][1] = shape.simplify();
            }
        }
    }

    public HatchBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.SOUTH).with(PART,CENTER).with(PASSABLE,false));
    }
    @Override public MapCodec<HatchBlock> getCodec() { return CODEC; }
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder) { builder.add(FACING,PART,PASSABLE); }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.INVISIBLE; }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(PART)==CENTER ? new HatchEntity(pos,state) : null;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return state.get(PART)==CENTER ? validateTicker(type,Hatches.TYPE,HatchEntity::tick) : null;
    }
    private static VoxelShape shape(BlockState state) {
        return SHAPES[state.get(FACING).getHorizontal()][state.get(PART)][state.get(PASSABLE)?1:0];
    }
    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) { return shape(state); }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) { return shape(state); }
    public static BlockPos cell(BlockPos center, Direction facing, int index) {
        return center.offset(facing.rotateYCounterclockwise(),index%3-1).up(index/3-1);
    }
    public static BlockPos center(BlockPos cell, BlockState state) {
        int i=state.get(PART);
        return cell.offset(state.get(FACING).rotateYCounterclockwise(),1-i%3).up(1-i/3);
    }
    public static boolean matches(BlockState state, Direction facing, int index) {
        return state.isOf(Hatches.BLOCK) && state.get(FACING)==facing && state.get(PART)==index;
    }
    public static boolean loaded(ServerWorld world, BlockPos center, Direction facing) {
        for(int i=0;i<9;i++) if(!world.isChunkLoaded(cell(center,facing,i))) return false;
        return true;
    }
    public static boolean place(ServerWorld world, BlockPos center, Direction facing) {
        if(!loaded(world,center,facing)) return false;
        for(int i=0;i<9;i++) {
            BlockPos p=cell(center,facing,i);
            if(world.isOutOfHeightLimit(p) || !world.getWorldBorder().contains(p)) return false;
            BlockState s=world.getBlockState(p);
            if(!s.isAir() && !s.isOf(Blocks.WATER)) return false;
        }
        if(HatchEntity.obstructed(world,center,facing)) return false;
        BlockState base=Hatches.BLOCK.getDefaultState().with(FACING,facing);
        for(int i=0;i<9;i++) {
            if(!world.setBlockState(cell(center,facing,i),base.with(PART,i),Block.NOTIFY_ALL)) {
                removeAssembly(world,center,facing);
                return false;
            }
        }
        return true;
    }
    public static HatchEntity at(World world, BlockPos pos) {
        BlockState state=world.getBlockState(pos);
        if(!state.isOf(Hatches.BLOCK)) return null;
        BlockEntity entity=world.getBlockEntity(center(pos,state));
        return entity instanceof HatchEntity hatch ? hatch : null;
    }
    public static void removeAssembly(ServerWorld world,BlockPos center,Direction facing) {
        if(REMOVING.get()) return;
        REMOVING.set(true);
        try {
            for(int i=0;i<9;i++) {
                BlockPos p=cell(center,facing,i);
                if(matches(world.getBlockState(p),facing,i)) world.removeBlock(p,false);
            }
        } finally { REMOVING.remove(); }
    }
    @Override protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState replacement, boolean moved) {
        if(world instanceof ServerWorld server && !replacement.isOf(this)) removeAssembly(server,center(pos,state),state.get(FACING));
        super.onStateReplaced(state,world,pos,replacement,moved);
    }
    @Override protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(hand==Hand.MAIN_HAND) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return ItemActionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if(!world.isClient && !player.isSpectator()) {
            HatchEntity hatch=at(world,pos);
            if(hatch!=null) hatch.interact(player);
        }
        return ActionResult.SUCCESS;
    }
}
