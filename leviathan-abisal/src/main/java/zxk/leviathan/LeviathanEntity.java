package zxk.leviathan;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
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
 private boolean dying=false; private ServerPlayerEntity victim;
 private static final String PREFIX="animation.leviathan_abisal.";
 private static final String[] ATTACKS={"attack_bite","attack_suction","attack_head_slam","attack_sweep","attack_pressure_roar","attack_corrosive_spit","attack_abyssal_gaze"};
 private static final int[] LENGTHS={270,160,80,70,120,100,140};
 public LeviathanEntity(EntityType<? extends HostileEntity> type,World world){super(type,world);setNoGravity(true);setPersistent();}
 public static DefaultAttributeContainer.Builder attributes(){return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,100).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,0).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,1);}
 @Override public boolean damage(DamageSource source,float amount){if(getWorld().isClient||dying||amount<=0||!Float.isFinite(amount)||isInvulnerableTo(source))return false; hp=Math.max(0,hp-amount);bar.setPercent((float)(hp/LeviathanMod.CONFIG.maxHealth));if(hp<=0){dying=true;victim=null;start("death",360);}return true;}
 private int duration;
 private void start(String name,int ticks){action=name;time=0;duration=ticks;triggerAnim("main",name);}
 @Override public void tick(){super.tick();if(getWorld().isClient)return;setVelocity(0,0,0);if(!action.isEmpty()){time++;if(action.equals("attack_bite")){if(time==30 && victim!=null && (!victim.isAlive()||squaredDistanceTo(victim)>400))victim=null;if(time>=30&&time<230&&victim!=null&&victim.isAlive()&&victim.getWorld()==getWorld()){if(time>=60&&(time-60)%30==0){victim.damage(getDamageSources().mobAttack(this),Float.MAX_VALUE);}}}if(time>=duration){if(dying){bar.clearPlayers();discard();return;}action="";victim=null;cooldown=phase==3?20:50;}return;}int next=hp/LeviathanMod.CONFIG.maxHealth<=LeviathanMod.CONFIG.phase3Threshold?3:hp/LeviathanMod.CONFIG.maxHealth<=LeviathanMod.CONFIG.phase2Threshold?2:1;if(next>phase){phase=next;start("phase_transition",160);return;}if(cooldown-->0)return;PlayerEntity target=getWorld().getClosestPlayer(this,30);if(target instanceof ServerPlayerEntity player&&!player.isCreative()&&!player.isSpectator()){int i=random.nextInt(phase==1?3:7);int idx=phase==1?new int[]{0,2,3}[i]:i;victim=player;start(ATTACKS[idx],LENGTHS[idx]);}}
 @Override public void onStartedTrackingBy(ServerPlayerEntity p){super.onStartedTrackingBy(p);bar.addPlayer(p);bar.setPercent((float)(hp/LeviathanMod.CONFIG.maxHealth));}
 @Override public void onStoppedTrackingBy(ServerPlayerEntity p){super.onStoppedTrackingBy(p);bar.removePlayer(p);}
 @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putDouble("AbyssalHealth",hp);}
 @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);if(n.contains("AbyssalHealth"))hp=Math.max(0,Math.min(LeviathanMod.CONFIG.maxHealth,n.getDouble("AbyssalHealth")));if(hp==0){hp=1;}}
 @Override public void registerControllers(AnimatableManager.ControllerRegistrar r){var c=new AnimationController<LeviathanEntity>(this,"main",0,s->s.setAndContinue(RawAnimation.begin().thenLoop(PREFIX+"idle")));for(String a:ATTACKS)c.triggerableAnim(a,RawAnimation.begin().thenPlay(PREFIX+a));for(String a:new String[]{"phase_transition","critical_hit","stun","death","breach_roar"})c.triggerableAnim(a,RawAnimation.begin().thenPlay(PREFIX+a));r.add(c);}
 @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
