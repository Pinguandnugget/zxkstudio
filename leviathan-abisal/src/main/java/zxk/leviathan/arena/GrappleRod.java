package zxk.leviathan.arena;

import java.util.HashMap;
import java.util.UUID;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class GrappleRod extends Item {
    private record Session(ServerWorld world, BlockPos anchor, Vec3d target, int age, int held){}
    private record Landing(ServerWorld world,int ticks){}
    private static final HashMap<UUID,Landing> LANDING=new HashMap<>();
    private static final HashMap<UUID,Session> ACTIVE = new HashMap<>();
    public GrappleRod(Settings settings){super(settings);}
    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand){
        ItemStack stack=player.getStackInHand(hand);
        if(world.isClient)return TypedActionResult.success(stack);
        if(!(player instanceof ServerPlayerEntity p)||!(world instanceof ServerWorld sw))return TypedActionResult.fail(stack);
        if(ACTIVE.remove(p.getUuid())!=null){release(p);return TypedActionResult.success(stack);}
        if(p.getItemCooldownManager().isCoolingDown(this)||p.isSpectator()||p.hasVehicle())return TypedActionResult.fail(stack);
        HitResult result=p.raycast(GrappleMotion.REACH,1,false);
        if(!(result instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK||!world.getBlockState(hit.getBlockPos()).isOf(GrappleItems.ANCHOR)){
            p.sendMessage(Text.literal("Apunta a un ancla de emergencia."),true);return TypedActionResult.fail(stack);
        }
        Vec3d target=GrappleMotion.target(p.getPos(),Vec3d.ofCenter(hit.getBlockPos()));
        if(target==null||!world.isSpaceEmpty(p,p.getBoundingBox().offset(target.subtract(p.getPos())))){
            p.sendMessage(Text.literal("Ancla fuera de alcance o ascenso obstruido."),true);return TypedActionResult.fail(stack);
        }
        LANDING.remove(p.getUuid());
        ACTIVE.put(p.getUuid(),new Session(sw,hit.getBlockPos(),target,0,0));
        p.sendMessage(Text.literal("Anclaje activado"),true);
        return TypedActionResult.success(stack);
    }
    private static void release(ServerPlayerEntity p){
        p.getItemCooldownManager().set(GrappleItems.ROD,GrappleMotion.COOLDOWN_TICKS);
        p.fallDistance=0;
        LANDING.put(p.getUuid(),new Landing(p.getServerWorld(),100));
        p.setVelocity(Vec3d.ZERO);p.velocityModified=true;
    }
    public static void tick(MinecraftServer server){
        var land=LANDING.entrySet().iterator();
        while(land.hasNext()){
            var e=land.next();var p=server.getPlayerManager().getPlayer(e.getKey());var l=e.getValue();
            if(p==null||!p.isAlive()||p.getWorld()!=l.world()||l.ticks()<=0){land.remove();continue;}
            p.fallDistance=0;
            if(p.isOnGround()){land.remove();continue;}
            e.setValue(new Landing(l.world(),l.ticks()-1));
        }
        var it=ACTIVE.entrySet().iterator();
        while(it.hasNext()){
            var entry=it.next();var s=entry.getValue();
            var p=server.getPlayerManager().getPlayer(entry.getKey());
            if(p==null){it.remove();continue;}
            if(!p.isAlive()||p.isSpectator()||p.hasVehicle()||p.getWorld()!=s.world()
                ||!s.world().isChunkLoaded(s.anchor())||!s.world().getBlockState(s.anchor()).isOf(GrappleItems.ANCHOR)
                ||(!p.getMainHandStack().isOf(GrappleItems.ROD)&&!p.getOffHandStack().isOf(GrappleItems.ROD))
                ||s.age()>=180||s.held()>=GrappleMotion.HOLD_TICKS){
                it.remove();release(p);continue;
            }
            Vec3d delta=s.target().subtract(p.getPos());
            Vec3d motion=GrappleMotion.velocity(p.getPos(),s.target());
            if(!s.world().isSpaceEmpty(p,p.getBoundingBox().offset(motion))){
                it.remove();release(p);continue;
            }
            p.setVelocity(motion);p.velocityModified=true;p.fallDistance=0;
            if(s.age()%4==0){
                Vec3d start=p.getEyePos(),end=Vec3d.ofCenter(s.anchor());
                int count=Math.min(24,Math.max(1,(int)start.distanceTo(end)));
                for(int j=1;j<=count;j++){
                    Vec3d q=start.lerp(end,j/(double)count);
                    s.world().spawnParticles(net.minecraft.particle.ParticleTypes.END_ROD,q.x,q.y,q.z,1,0,0,0,0);
                }
            }
            int held=s.held()+(delta.length()<0.7?1:0);
            entry.setValue(new Session(s.world(),s.anchor(),s.target(),s.age()+1,held));
        }
    }
}
