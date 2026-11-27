package com.zxkstudio.hatch;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.*;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import java.util.*;

public final class HatchEntity extends BlockEntity {
    public static final int DURATION=32;
    public static final double GROUP_RADIUS=10.0;
    private enum Mode { WAITING, ACTIVE, LOCKED, MANUAL }
    private Mode mode=Mode.WAITING;
    private final Set<UUID> group=new HashSet<>();
    private final Set<UUID> entered=new HashSet<>();
    private final Map<UUID,Vec3d> previous=new HashMap<>();
    private int progress=0, lastProgress=0;
    private boolean motionOpen=false, manualOpen=false, openedOnce=false;

    public HatchEntity(BlockPos pos, BlockState state) { super(Hatches.TYPE,pos,state); }
    // Render-time animation is independent of the arrival rate of block-entity NBT packets.
    // Only the synced block-state direction drives it; the server still owns collision and sealing.
    private long visualTime;
    private float visualProgress;
    public float animation(float tickDelta) {
        long now=System.nanoTime();
        BlockState state=getCachedState();
        boolean target=state.get(HatchBlock.OPENING);
        if(visualTime==0L) {
            visualTime=now;
            visualProgress=state.get(HatchBlock.PASSABLE)?DURATION:0;
            return visualProgress/DURATION;
        }
        float elapsed=Math.min(4f,Math.max(0f,(now-visualTime)/50_000_000f));
        visualTime=now;
        visualProgress=MathHelper.clamp(visualProgress+(target?elapsed:-elapsed),0f,(float)DURATION);
        return visualProgress/DURATION;
    }
    private Direction facing() { return getCachedState().get(HatchBlock.FACING); }
    private Vec3d relative(ServerPlayerEntity p) { return p.getBoundingBox().getCenter().subtract(Vec3d.ofCenter(pos)); }
    private double depth(Vec3d p) { Direction d=facing(); return p.x*d.getOffsetX()+p.z*d.getOffsetZ(); }
    private double lateral(Vec3d p) { Direction d=facing().rotateYCounterclockwise(); return p.x*d.getOffsetX()+p.z*d.getOffsetZ(); }
    private static boolean survival(ServerPlayerEntity p) { return p.interactionManager.getGameMode()==GameMode.SURVIVAL; }

    public static void tick(World world, BlockPos pos, BlockState state, HatchEntity hatch) {
        hatch.lastProgress=hatch.progress;
        if(world instanceof ServerWorld server) hatch.serverTick(server);
        // Clients interpolate the authoritative position; they never decide opening, locking or collision.
    }

    private void serverTick(ServerWorld world) {
        Direction facing=facing();
        if(!HatchBlock.loaded(world,pos,facing)) {
            if(motionOpen && progress<DURATION) {motionOpen=false;sync(world);}
            return;
        }
        if(world.getTime()%20==0) for(int i=0;i<9;i++) {
            if(!HatchBlock.matches(world.getBlockState(HatchBlock.cell(pos,facing,i)),facing,i)) {
                HatchBlock.removeAssembly(world,pos,facing); return;
            }
        }

        Mode oldMode=mode;
        int oldProgress=progress;
        boolean oldMotion=motionOpen;
        boolean desired=false;
        if(mode==Mode.ACTIVE) {
            if(progress==DURATION) openedOnce=true;
            boolean everybodyInside=checkCrossings(world);
            desired=!openedOnce || !everybodyInside;
        } else if(mode==Mode.MANUAL) desired=manualOpen;

        // Never begin or finish a close while a living entity occupies the opening.
        boolean blocked=!desired && progress>0 && playerObstructed(world,pos,facing);
        motionOpen=desired || blocked;
        // A normal block-state update is reliably broadcast even if intermediate BE packets are coalesced.
        BlockState centerState=world.getBlockState(pos);
        if(centerState.get(HatchBlock.OPENING)!=motionOpen) {
            world.setBlockState(pos,centerState.with(HatchBlock.OPENING,motionOpen),Block.NOTIFY_ALL);
        }
        if(motionOpen) progress=Math.min(DURATION,progress+1);
        else progress=Math.max(0,progress-1);
        if(oldMotion!=motionOpen && oldProgress!=progress) {
            world.playSound(null,pos,motionOpen?SoundEvents.BLOCK_IRON_DOOR_OPEN:SoundEvents.BLOCK_IRON_DOOR_CLOSE,SoundCategory.BLOCKS,1.0f,0.65f);
        }

        // Collision is released only when fully open, restored only after the last closing tick.
        // During a reversal, preserve the prior collision state until an endpoint is reached.
        if(progress==0 || progress==DURATION) {
            boolean passable=progress==DURATION;
            if(getCachedState().get(HatchBlock.PASSABLE)!=passable) {
                for(int i=0;i<9;i++) {
                    BlockPos p=HatchBlock.cell(pos,facing,i);
                    BlockState s=world.getBlockState(p);
                    if(HatchBlock.matches(s,facing,i)) world.setBlockState(p,s.with(HatchBlock.PASSABLE,passable),Block.NOTIFY_ALL);
                }
            }
        }
        if(mode==Mode.ACTIVE && openedOnce && !desired && progress==0) {
            mode=Mode.LOCKED;
            previous.clear();
            world.playSound(null,pos,SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE,SoundCategory.BLOCKS,1.0f,0.5f);
            for(UUID id:group) {
                ServerPlayerEntity p=world.getServer().getPlayerManager().getPlayer(id);
                if(p!=null) p.sendMessage(Text.literal("§7[Escotilla] §cAcceso sellado."),true);
            }
        }
        if(oldProgress!=progress || oldMode!=mode || oldMotion!=motionOpen) {
            markDirty();
            if(oldMode!=mode || oldMotion!=motionOpen || progress==0 || progress==DURATION) sync(world);
        }
    }

    private void tryStart(ServerWorld world, ServerPlayerEntity initiator) {
        List<ServerPlayerEntity> players=world.getServer().getPlayerManager().getPlayerList().stream().filter(HatchEntity::survival).toList();
        if(progress!=0 || !survival(initiator) || !initiator.isAlive()) {
            initiator.sendMessage(Text.literal("§7Escotilla: solo un jugador en supervivencia puede iniciar el acceso cuando está cerrada."),true);
            return;
        }
        if(!HatchBlock.loaded(world,pos,facing())) return;
        long nearby=players.stream().filter(p->p.getWorld()==world && p.isAlive() && p.getPos().squaredDistanceTo(Vec3d.ofCenter(pos))<=GROUP_RADIUS*GROUP_RADIUS).count();
        if(players.isEmpty() || nearby!=players.size()) {
            initiator.sendMessage(Text.literal("§7[Escotilla] §eGrupo incompleto: "+nearby+"/"+players.size()+" supervivientes a 10 bloques."),true);
            return;
        }
        group.clear(); entered.clear(); previous.clear();
        for(ServerPlayerEntity p:players) {
            UUID id=p.getUuid();
            group.add(id);
            Vec3d r=relative(p);
            previous.put(id,r);
            p.sendMessage(Text.literal("§7[Escotilla] §aGrupo reunido. Acceso en apertura."),true);
        }
        openedOnce=false;
        mode=Mode.ACTIVE;
        markDirty();
    }

    private boolean checkCrossings(ServerWorld world) {
        boolean all=!group.isEmpty();
        for(UUID id:group) {
            ServerPlayerEntity p=world.getServer().getPlayerManager().getPlayer(id);
            if(p==null || p.getWorld()!=world || !p.isAlive() || !survival(p)) {
                // The original cohort stays fixed: disconnecting or changing mode cannot cause an early seal.
                previous.remove(id);
                all=false;
                continue;
            }
            Vec3d now=relative(p), old=previous.put(id,now);
            double n=depth(now);
            if(n>=0.35 && entered.remove(id)) markDirty();
            if(getCachedState().get(HatchBlock.PASSABLE) && old!=null && depth(old)>=0 && n<0 && old.squaredDistanceTo(now)<=16.0) {
                double t=depth(old)/(depth(old)-n);
                Vec3d crossing=old.lerp(now,t);
                double x=lateral(crossing), y=crossing.y;
                if(x*x+y*y<=1.30*1.30 && entered.add(id)) {
                    markDirty();
                    p.sendMessage(Text.literal("§7[Escotilla] §aEntrada registrada ("+entered.size()+"/"+group.size()+")."),true);
                }
            }
            if(!entered.contains(id) || n>-0.85) all=false;
        }
        return all;
    }

    public static boolean obstructed(ServerWorld world,BlockPos pos,Direction facing) {
        Vec3d c=Vec3d.ofCenter(pos);
        boolean z=facing.getAxis()==Direction.Axis.Z;
        double dx=z?1.55:0.62, dz=z?0.62:1.55;
        Box area=new Box(c.x-dx,c.y-1.55,c.z-dz,c.x+dx,c.y+1.55,c.z+dz);
        return !world.getOtherEntities(null,area,e->e instanceof LivingEntity && e.isAlive() && !e.isSpectator()).isEmpty();
    }

    private static boolean playerObstructed(ServerWorld world,BlockPos pos,Direction facing) {
        Vec3d c=Vec3d.ofCenter(pos);
        boolean z=facing.getAxis()==Direction.Axis.Z;
        double dx=z?1.55:0.62, dz=z?0.62:1.55;
        Box area=new Box(c.x-dx,c.y-1.55,c.z-dz,c.x+dx,c.y+1.55,c.z+dz);
        return !world.getOtherEntities(null,area,entity->entity instanceof PlayerEntity && entity.isAlive() && !entity.isSpectator()).isEmpty();
    }

    public void interact(PlayerEntity p) {
        if(mode==Mode.MANUAL) {
            manualOpen=!manualOpen;
            markDirty();
            p.sendMessage(Text.literal(manualOpen?"§7Escotilla: abriendo.":"§7Escotilla: cerrando."),true);
        } else if(mode==Mode.WAITING && p instanceof ServerPlayerEntity initiator && world instanceof ServerWorld server) {
            tryStart(server,initiator);
        } else p.sendMessage(Text.literal(status()),true);
    }
    public void setManual() {
        manualOpen=progress>0 || motionOpen;
        mode=Mode.MANUAL;
        group.clear();entered.clear();previous.clear();openedOnce=false;
        if(world instanceof ServerWorld server) sync(server);
    }
    public void resetGroup() {
        mode=Mode.WAITING;
        group.clear();entered.clear();previous.clear();openedOnce=false;manualOpen=false;
        if(world instanceof ServerWorld server) sync(server);
    }
    public String status() {
        return switch(mode) {
            case WAITING -> "Escotilla: esperando a todos los jugadores en supervivencia (10 bloques) y clic derecho.";
            case ACTIVE -> "Escotilla: acceso grupal. Entradas registradas "+entered.size()+"/"+group.size()+".";
            case LOCKED -> "Escotilla: sellada definitivamente. Un administrador puede activar el modo manual.";
            case MANUAL -> "Escotilla: modo manual. Clic derecho para abrir/cerrar.";
        };
    }
    private void sync(ServerWorld world) {
        markDirty();
        world.getChunkManager().markForUpdate(pos);
    }
    private static NbtList ids(Set<UUID> set) {
        NbtList list=new NbtList();
        for(UUID id:set) list.add(NbtString.of(id.toString()));
        return list;
    }
    private static void loadIds(NbtCompound nbt,String key,Set<UUID> set) {
        set.clear();
        for(NbtElement e:nbt.getList(key,NbtElement.STRING_TYPE)) {
            try {set.add(UUID.fromString(e.asString()));} catch(IllegalArgumentException ignored) {}
        }
    }
    @Override protected void writeNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt,lookup);
        nbt.putString("Mode",mode.name());
        nbt.putInt("Progress",progress);
        nbt.putBoolean("MotionOpen",motionOpen);
        nbt.putBoolean("ManualOpen",manualOpen);
        nbt.putBoolean("OpenedOnce",openedOnce);
        nbt.put("Group",ids(group));nbt.put("Entered",ids(entered));
    }
    @Override protected void readNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt,lookup);
        int next=MathHelper.clamp(nbt.getInt("Progress"),0,DURATION);
        lastProgress=world!=null && world.isClient?progress:next;
        progress=next;
        motionOpen=nbt.getBoolean("MotionOpen");
        if(nbt.contains("Mode")) {
            try { mode=Mode.valueOf(nbt.getString("Mode")); } catch(IllegalArgumentException ignored) {mode=Mode.LOCKED;}
            manualOpen=nbt.getBoolean("ManualOpen");openedOnce=nbt.getBoolean("OpenedOnce");
            loadIds(nbt,"Group",group);loadIds(nbt,"Entered",entered);entered.retainAll(group);
            previous.clear();
        }
    }
    @Override public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup lookup) {
        NbtCompound nbt=new NbtCompound();
        nbt.putInt("Progress",progress);nbt.putBoolean("MotionOpen",motionOpen);
        return nbt;
    }
    @Override public BlockEntityUpdateS2CPacket toUpdatePacket() {return BlockEntityUpdateS2CPacket.create(this);}
}
