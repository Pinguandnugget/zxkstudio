package zxk.leviathan.arena;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** One transient visual per wave. Collision and damage remain in RadialWave. */
public final class WaveVisualEntity extends Entity implements GeoEntity {
    private static final TrackedData<Integer> CLOCK=DataTracker.registerData(WaveVisualEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public WaveVisualEntity(EntityType<? extends WaveVisualEntity> type,World world){super(type,world);setNoGravity(true);noClip=true;setInvulnerable(true);}
    @Override protected void initDataTracker(DataTracker.Builder b){b.add(CLOCK,0);}
    public int waveTick(){return getDataTracker().get(CLOCK);}
    @Override public void tick(){super.tick();if(!getWorld().isClient){getDataTracker().set(CLOCK,age);if(age>=296)discard();}}
    @Override protected void readCustomDataFromNbt(NbtCompound n){}
    @Override protected void writeCustomDataToNbt(NbtCompound n){}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar r){
        r.add(new AnimationController<WaveVisualEntity>(this,"wave",0,s->s.setAndContinue(RawAnimation.begin().thenPlay("animation.tsunami_abisal.full_attack"))));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
