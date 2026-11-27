package zxk.leviathan;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.server.world.ServerWorld;
import java.util.UUID;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import zxk.leviathan.arena.TsunamiAttack;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.boss.*;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
public class LeviathanEntity extends HostileEntity implements GeoEntity {
 private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
 private final ServerBossBar bar=new ServerBossBar(Text.translatable("boss.leviathan_abisal.leviathan"),BossBar.Color.BLUE,BossBar.Style.PROGRESS);
 private double hp=LeviathanMod.CONFIG.maxHealth;
 private int phase=1, time=0, cooldown=20; private String action="";
 private boolean dying=false; private UUID victim; private boolean captured;
 private static final TrackedData<String> ANIMATION = DataTracker.registerData(LeviathanEntity.class, TrackedDataHandlerRegistry.STRING);
 private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.leviathan_abisal.idle");
 private static final String PREFIX="animation.leviathan_abisal.";
 private static final String[] ATTACKS={"attack_bite","attack_suction","attack_head_slam","attack_sweep","attack_pressure_roar","attack_corrosive_spit","attack_abyssal_gaze"};
 private static final int[] LENGTHS={270,160,80,70,120,100,140};
 public LeviathanEntity(EntityType<? extends HostileEntity> type,World world){super(type,world);setNoGravity(true);setPersistent();}
 @Override protected void initDataTracker(DataTracker.Builder builder){super.initDataTracker(builder);builder.add(ANIMATION,"idle");}
 public static DefaultAttributeContainer.Builder attributes(){return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,100).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,0).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,1);}
 @Override public boolean damage(DamageSource source,float amount){if(getWorld().isClient||dying||action.equals("breach_roar")||amount<=0||!Float.isFinite(amount)||isInvulnerableTo(source))return false; hp=Math.max(0,hp-amount);bar.setPercent((float)(hp/LeviathanMod.CONFIG.maxHealth));if(hp<=0){dying=true;victim=null;start("death",360);}return true;}
 private int duration;
 public void beginAppearance(){if(!getWorld().isClient&&!dying)start("breach_roar",320);}
 public boolean testAttack(String name){if(dying||!action.isEmpty())return false;for(int i=0;i<ATTACKS.length;i++)if(ATTACKS[i].equals(name)){var p=getWorld().getClosestPlayer(this,100);victim=p instanceof ServerPlayerEntity sp?sp.getUuid():null;start(name,LENGTHS[i]);return true;}if(name.equals("attack_tsunami")&&getWorld() instanceof ServerWorld sw){start("attack_pressure_roar",120);return TsunamiAttack.start(sw,getPos());}return false;}
 private void lethal(ServerPlayerEntity p){if(p.isAlive())p.damage(getDamageSources().mobAttack(this),10000f);}
 private void attackEffects(){
  if(time==20&&(action.equals("attack_suction")||action.equals("attack_pressure_roar")||action.equals("breach_roar")||action.equals("phase_transition")))getWorld().playSound(null,getBlockPos(),LeviathanSounds.roar(),net.minecraft.sound.SoundCategory.HOSTILE,5f,1f);
  if(!(getWorld() instanceof ServerWorld sw))return;
  for(var p:sw.getPlayers()){
   if(!p.isAlive()||p.isCreative()||p.isSpectator()||p.getPos().squaredDistanceTo(getPos())>10000)continue;
   Vec3d delta=getPos().add(0,3,0).subtract(p.getPos());double d=delta.length();
   switch(action){
    case "attack_suction" -> {if(time>=35&&time<=130&&d<50&&d>1){p.addVelocity(delta.normalize().multiply(.16));p.velocityModified=true;}if(time==140&&d<12)lethal(p);}
    case "attack_head_slam" -> {if(time==46&&d<100){lethal(p);if(p.isAlive()){p.addVelocity(0,.8,0);p.velocityModified=true;}}}
    case "attack_sweep" -> {if(time==38&&d<35){p.damage(getDamageSources().mobAttack(this),24);if(d>0){p.addVelocity(delta.normalize().multiply(-1.1));p.velocityModified=true;}}}
    case "attack_pressure_roar" -> {if(time==40||time==75||time==100){p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS,80,0));p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,60,1));if(d>1){p.addVelocity(delta.normalize().multiply(-.6));p.velocityModified=true;}p.damage(getDamageSources().mobAttack(this),12);}}
    case "attack_corrosive_spit" -> {if(time>=55&&time<=95&&time%20==15&&d<30){p.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,100,1));p.damage(getDamageSources().mobAttack(this),8);}}
    case "attack_abyssal_gaze" -> {if(time==100&&p.getUuid().equals(victim)&&d<20)lethal(p);}
   }
  }
 }
 private void start(String name,int ticks){captured=false;action=name;time=0;duration=ticks;getDataTracker().set(ANIMATION,name);}
 private void resetAction(){captured=false;action="";victim=null;getDataTracker().set(ANIMATION,"idle");cooldown=phase==3?40:80;}
 private ServerPlayerEntity resolveVictim(){if(victim==null||!(getWorld() instanceof ServerWorld sw))return null;var p=sw.getServer().getPlayerManager().getPlayer(victim);return p!=null&&p.isAlive()&&!p.isRemoved()&&p.getWorld()==sw&&!p.isCreative()&&!p.isSpectator()&&squaredDistanceTo(p)<=900?p:null;}
 private void biteTick(){
  if(time<30||time>=230){captured=false;return;}
  var p=resolveVictim();if(p==null){victim=null;captured=false;return;}
  Vec3d mouth=BiteSocket.position(this,time);
  if(time==30){captured=p.getPos().add(0,0.9,0).distanceTo(mouth)<7;}
  if(!captured)return;
  Vec3d target=mouth.add(0,-0.9,0);
  if(!getWorld().isSpaceEmpty(p,p.getBoundingBox().offset(target.subtract(p.getPos())))){captured=false;victim=null;return;}
  p.networkHandler.requestTeleport(target.x,target.y,target.z,p.getYaw(),p.getPitch());
  p.setVelocity(Vec3d.ZERO);p.velocityModified=true;p.fallDistance=0;
  if(time>=60&&time<=210&&(time-60)%30==0){float amount=Math.min(10000f,Math.max(40f,(p.getHealth()+p.getAbsorptionAmount())*4f));p.damage(getDamageSources().mobAttack(this),amount);if(!p.isAlive()||p.isRemoved()){victim=null;captured=false;}}
 }
 @Override public void tick(){super.tick();if(getWorld().isClient)return;setVelocity(0,0,0);if(!action.isEmpty()){time++;if(action.equals("attack_bite"))biteTick();else attackEffects();if(time>=duration){if(dying){bar.clearPlayers();discard();return;}resetAction();}return;}int next=hp/LeviathanMod.CONFIG.maxHealth<=LeviathanMod.CONFIG.phase3Threshold?3:hp/LeviathanMod.CONFIG.maxHealth<=LeviathanMod.CONFIG.phase2Threshold?2:1;if(next>phase){phase=next;start("phase_transition",160);return;}if(cooldown-->0)return;PlayerEntity target=getWorld().getClosestPlayer(this,30);if(target instanceof ServerPlayerEntity player&&!player.isCreative()&&!player.isSpectator()){int i=random.nextInt(phase==1?3:7);int idx=phase==1?new int[]{0,2,3}[i]:i;if(phase>=2&&random.nextInt(5)==0&&getWorld() instanceof ServerWorld sw&&TsunamiAttack.start(sw,getPos())){start("attack_pressure_roar",120);return;}victim=player.getUuid();start(ATTACKS[idx],LENGTHS[idx]);}}
 @Override public void onStartedTrackingBy(ServerPlayerEntity p){super.onStartedTrackingBy(p);bar.addPlayer(p);bar.setPercent((float)(hp/LeviathanMod.CONFIG.maxHealth));}
 @Override public void onStoppedTrackingBy(ServerPlayerEntity p){super.onStoppedTrackingBy(p);bar.removePlayer(p);}
 @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putDouble("AbyssalHealth",hp);}
 @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);if(n.contains("AbyssalHealth"))hp=Math.max(0,Math.min(LeviathanMod.CONFIG.maxHealth,n.getDouble("AbyssalHealth")));if(hp==0){hp=1;}}
 @Override public void registerControllers(AnimatableManager.ControllerRegistrar r){
  r.add(new AnimationController<LeviathanEntity>(this,"main",5,state->{String current=getDataTracker().get(ANIMATION);if(current.equals("idle"))return state.setAndContinue(IDLE);return state.setAndContinue(RawAnimation.begin().then(PREFIX+current,Animation.LoopType.PLAY_ONCE).thenLoop(PREFIX+"idle"));}));
 }
 @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
