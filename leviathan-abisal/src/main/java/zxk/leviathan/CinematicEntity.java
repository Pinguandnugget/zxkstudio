package zxk.leviathan;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class CinematicEntity extends Entity implements GeoEntity {
    private static final TrackedData<String> STAGE=DataTracker.registerData(CinematicEntity.class,TrackedDataHandlerRegistry.STRING);
    private static final String PREFIX="animation.leviathan_abisal.";
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private Vec3d destination;
    private float facing;
    public CinematicEntity(EntityType<? extends CinematicEntity> type,World world){super(type,world);setNoGravity(true);noClip=true;setInvulnerable(true);}
    public void configure(Vec3d destination,float yaw){this.destination=destination;this.facing=yaw;}
    @Override protected void initDataTracker(DataTracker.Builder b){b.add(STAGE,"sleep_coiled");}
    private void stage(String name){getDataTracker().set(STAGE,name);}
    private void sound(net.minecraft.sound.SoundEvent event,float volume){getWorld().playSound(null,getBlockPos(),event,SoundCategory.HOSTILE,volume,1f);}
    @Override public void tick(){
        super.tick();
        if(!(getWorld() instanceof ServerWorld world))return;
        if(destination==null){discard();return;}
        if(age==1)sound(LeviathanSounds.AMBIENT,.75f);
        if(age==200){stage("eye_open");sound(LeviathanSounds.DISTANT_ROAR,.9f);}
        if(age==420){stage("swim_zigzag");sound(LeviathanSounds.ALTERNATE,.85f);}
        if(age==800){
            Vec3d swimOffset=new Vec3d(0,0,-855.0/16).rotateY((float)Math.toRadians(-facing));
            Vec3d p=getPos().add(swimOffset);
            refreshPositionAndAngles(p.x,p.y,p.z,facing,0);
            stage("ascend_to_arena");sound(LeviathanSounds.DISTANT_ROAR,.9f);
        }
        if(age>=1000){
            for(var player:world.getPlayers())if(player.getPos().squaredDistanceTo(getPos())<128*128)
                player.sendMessage(net.minecraft.text.Text.literal("Cinemática finalizada."),true);
            discard();
        }
    }
    @Override protected void readCustomDataFromNbt(NbtCompound n){}
    @Override protected void writeCustomDataToNbt(NbtCompound n){}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar r){
        r.add(new AnimationController<CinematicEntity>(this,"cinematic",0,s->{
            String stage=getDataTracker().get(STAGE);
            return s.setAndContinue(stage.equals("sleep_coiled")?RawAnimation.begin().thenLoop(PREFIX+stage):RawAnimation.begin().then(PREFIX+stage,Animation.LoopType.HOLD_ON_LAST_FRAME));
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
