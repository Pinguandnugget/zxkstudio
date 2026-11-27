package zxk.leviathan;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.server.world.ServerWorld;
import java.util.UUID;
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
 private boolean dying=false; private UUID victim;
 private static final TrackedData<String> ANIMATION = DataTracker.registerData(LeviathanEntity.class, TrackedDataHandlerRegistry.STRING);
 private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.leviathan_abisal.idle");
 private static final String PREFIX="animation.leviathan_abisal.";
 private static final String[] ATTACKS={"attack_bite","attack_suction","attack_head_slam","attack_sweep","attack_pressure_roar","attack_corrosive_spit","attack_abyssal_gaze"};
 private static final int[] LENGTHS={270,160,80,70,120,100,140};
 public LeviathanEntity(EntityType<? extends HostileEntity> type,World world){super(type,world);setNoGravity(true);setPersistent();}
 @Override protected void initDataTracker(DataTracker.Builder builder){super.initDataTracker(builder);builder.add(ANIMATION,"idle");}
 public static DefaultAttributeContainer.Builder attributes(){return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,100).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,0).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,1);}
 @Override public boolean damage(DamageSource source,float amount){if(getWorld().isClient||dying||amount<=0||!Float.isFinite(amount)||isInvulnerableTo(source))return false; hp=Math.max(0,hp-amount);bar.setPercent((float)(hp/LeviathanMod.CONFIG.maxHealth));if(hp<=0){dying=true;victim=null;start("death",360);}return true;}
 private int duration;
 private void start(String name,int ticks){action=name;time=0;duration=ticks;getDataTracker().set(ANIMATION,name);}
 private void resetAction(){action="";victim=null;getDataTracker().set(ANIMATION,"idle");cooldown=phase==3?40:80;}
 private ServerPlayerEntity resolveVictim(){if(victim==null||!(getWorld() instanceof ServerWorld sw))return null;var p=sw.getServer().getPlayerManager().getPlayer(victim);return p!=null&&p.isAlive()&&!p.isRemoved()&&p.getWorld()==sw&&!p.isCreative()&&!p.isSpectator()&&squaredDistanceTo(p)<=900?p:null;}
 private void biteTick(){if(time<30||time>=230)return;var p=resolveVictim();if(p==null){victim=null;return;}if(time>=60&&time<=210&&(time-60)%30==0){float amount=Math.min(10000f,Math.max(40f,(p.getHealth()+p.getAbsorptionAmount())*4f));p.damage(getDamageSources().mobAttack(this),amount);if(!p.isAlive()||p.isRemoved())victim=null;}}
 @Override public void tick(){super.tick();if(getWorld().isClient)return;setVelocity(0,0,0);if(!action.isEmpty()){time++;if(action.equals("attack_bite"))biteTick();if(time>=duration){if(dying){bar.clearPlayers();discard();return;}resetAction();}return;}int next=hp/LeviathanMod.CONFIG.maxHealth<=LeviathanMod.CONFIG.phase3Threshold?3:hp/LeviathanMod.CONFIG.maxHealth<=LeviathanMod.CONFIG.phase2Threshold?2:1;if(next>phase){phase=next;start("phase_transition",160);return;}if(cooldown-->0)return;PlayerEntity target=getWorld().getClosestPlayer(this,30);if(target instanceof ServerPlayerEntity player&&!player.isCreative()&&!player.isSpectator()){int i=random.nextInt(phase==1?3:7);int idx=phase==1?new int[]{0,2,3}[i]:i;victim=player.getUuid();start(ATTACKS[idx],LENGTHS[idx]);}}
 @Override public void onStartedTrackingBy(ServerPlayerEntity p){super.onStartedTrackingBy(p);bar.addPlayer(p);bar.setPercent((float)(hp/LeviathanMod.CONFIG.maxHealth));}
 @Override public void onStoppedTrackingBy(ServerPlayerEntity p){super.onStoppedTrackingBy(p);bar.removePlayer(p);}
 @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putDouble("AbyssalHealth",hp);}
 @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);if(n.contains("AbyssalHealth"))hp=Math.max(0,Math.min(LeviathanMod.CONFIG.maxHealth,n.getDouble("AbyssalHealth")));if(hp==0){hp=1;}}
 @Override public void registerControllers(AnimatableManager.ControllerRegistrar r){
  r.add(new AnimationController<LeviathanEntity>(this,"main",5,state->{String current=getDataTracker().get(ANIMATION);if(current.equals("idle"))return state.setAndContinue(IDLE);return state.setAndContinue(RawAnimation.begin().then(PREFIX+current,Animation.LoopType.PLAY_ONCE).thenLoop(PREFIX+"idle"));}));
 }
 @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
