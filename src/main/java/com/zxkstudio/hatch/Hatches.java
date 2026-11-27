package com.zxkstudio.hatch;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import static net.minecraft.server.command.CommandManager.*;

public final class Hatches implements ModInitializer {
    public static final Identifier ID=Identifier.of("zxkstudio","escotilla_abisal");
    public static final HatchBlock BLOCK=new HatchBlock(AbstractBlock.Settings.create().strength(-1,3600000).nonOpaque().dropsNothing().pistonBehavior(PistonBehavior.BLOCK));
    public static BlockEntityType<HatchEntity> TYPE;
    public static final Item ITEM=new Item(new Item.Settings().maxCount(1)) {
        @Override public ActionResult useOnBlock(ItemUsageContext ctx) {
            if(ctx.getPlayer()==null || !ctx.getPlayer().hasPermissionLevel(2)) return ActionResult.FAIL;
            if(ctx.getWorld() instanceof ServerWorld world) {
                BlockPos clicked=ctx.getBlockPos();
                BlockState state=world.getBlockState(clicked);
                BlockPos bottom=state.isAir() || state.isOf(Blocks.WATER) ? clicked : clicked.offset(ctx.getSide());
                Direction facing=ctx.getHorizontalPlayerFacing().getOpposite();
                if(!HatchBlock.place(world,bottom.up(),facing)) {
                    ctx.getPlayer().sendMessage(Text.literal("Escotilla: despeja un hueco vertical de 3x3 y aléjate del plano."),true);
                    return ActionResult.FAIL;
                }
                if(!ctx.getPlayer().isCreative()) ctx.getStack().decrement(1);
            }
            return ActionResult.SUCCESS;
        }
    };
    @Override public void onInitialize() {
        Registry.register(Registries.BLOCK,ID,BLOCK);
        TYPE=Registry.register(Registries.BLOCK_ENTITY_TYPE,ID,FabricBlockEntityTypeBuilder.create(HatchEntity::new,BLOCK).build());
        Registry.register(Registries.ITEM,ID,ITEM);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries->entries.add(ITEM));
        CommandRegistrationCallback.EVENT.register((dispatcher,registryAccess,environment)-> {
            var root=literal("escotilla").requires(source->source.hasPermissionLevel(2));
            root.then(literal("crear").then(argument("pos",BlockPosArgumentType.blockPos())
                .then(argument("exterior",StringArgumentType.word())
                    .suggests((ctx,builder)->{for(String d:new String[]{"north","south","east","west"}) if(d.startsWith(builder.getRemainingLowerCase())) builder.suggest(d); return builder.buildFuture();})
                    .executes(Hatches::create))));
            for(String operation:new String[]{"manual","grupo","estado","eliminar"}) {
                root.then(literal(operation).then(argument("pos",BlockPosArgumentType.blockPos()).executes(ctx->operate(ctx,operation))));
            }
            dispatcher.register(root);
        });
    }
    private static int create(CommandContext<ServerCommandSource> ctx) {
        var src=ctx.getSource();
        Direction facing=Direction.byName(StringArgumentType.getString(ctx,"exterior"));
        if(facing==null || !facing.getAxis().isHorizontal()) {
            src.sendError(Text.literal("Dirección exterior: north, south, east o west.")); return 0;
        }
        BlockPos pos=BlockPosArgumentType.getBlockPos(ctx,"pos");
        if(!HatchBlock.place(src.getWorld(),pos,facing)) {
            src.sendError(Text.literal("No se pudo colocar: hueco 3x3 ocupado, fuera del mundo, entidades en el plano o chunks descargados.")); return 0;
        }
        src.sendFeedback(()->Text.literal("Escotilla abisal creada en modo grupal. Exterior: "+facing.getName()),true); return 1;
    }
    private static int operate(CommandContext<ServerCommandSource> ctx,String op) {
        var src=ctx.getSource();
        BlockPos pos=BlockPosArgumentType.getBlockPos(ctx,"pos");
        if(!src.getWorld().isChunkLoaded(pos)) {src.sendError(Text.literal("Chunk descargado."));return 0;}
        HatchEntity hatch=HatchBlock.at(src.getWorld(),pos);
        if(hatch==null) {src.sendError(Text.literal("No hay escotilla en esa posición."));return 0;}
        switch(op) {
            case "manual" -> hatch.setManual();
            case "grupo" -> hatch.resetGroup();
            case "eliminar" -> HatchBlock.removeAssembly(src.getWorld(),hatch.getPos(),hatch.getCachedState().get(HatchBlock.FACING));
            default -> {}
        }
        src.sendFeedback(()->Text.literal(op.equals("eliminar")?"Escotilla eliminada.":hatch.status()),!op.equals("estado"));
        return 1;
    }
}
