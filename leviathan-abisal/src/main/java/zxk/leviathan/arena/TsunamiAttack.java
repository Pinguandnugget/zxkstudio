package zxk.leviathan.arena;

import java.util.ArrayList;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import static net.minecraft.server.command.CommandManager.literal;

public final class TsunamiAttack {
    private record Active(ServerWorld world,RadialWave wave,WaveVisualEntity visual){}
    private record Pulse(ServerWorld world,java.util.UUID player,long tick){}
    private static final ArrayList<Active> WAVES=new ArrayList<>();
    private static final ArrayList<Pulse> PULSES=new ArrayList<>();
    private static long clock;
    public static boolean start(ServerWorld world,Vec3d floor){
        if(WAVES.stream().anyMatch(a->a.world()==world&&a.wave().origin().distanceTo(floor)<200))return false;
        WaveVisualEntity visual=null;
        if(TsunamiAttack.class.getResource("/assets/leviathan_abisal/geo/tsunami.geo.json")!=null && TsunamiAttack.class.getResource("/assets/leviathan_abisal/textures/entity/tsunami.png")!=null && TsunamiAttack.class.getResource("/assets/leviathan_abisal/animations/tsunami.animation.json")!=null){
            visual=zxk.leviathan.LeviathanMod.WAVE.create(world);
            if(visual!=null){visual.refreshPositionAndAngles(floor.x,floor.y,floor.z,0,0);if(!world.spawnEntity(visual))visual=null;}
        }
        WAVES.add(new Active(world,new RadialWave(floor,100,8,.5),visual));
        world.playSound(null,net.minecraft.util.math.BlockPos.ofFloored(floor),zxk.leviathan.LeviathanSounds.roar(),net.minecraft.sound.SoundCategory.HOSTILE,5f,.9f);
        for(var p:world.getPlayers())if(p.getPos().squaredDistanceTo(floor)<110*110)p.sendMessage(Text.literal("¡TSUNAMI ABISAL! Busca un ancla elevada."),true);
        return true;
    }
    public static void init(){
        ServerTickEvents.END_SERVER_TICK.register(TsunamiAttack::tick);
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(literal("leviathanwave").requires(s->s.hasPermissionLevel(2)).executes(c->start(c.getSource().getWorld(),c.getSource().getPosition())?1:0)));
    }
    private static void impact(ServerPlayerEntity p,ServerWorld world){
        // Normal lethal damage permits vanilla totem handling. Never remove items directly.
        p.damage(world.getDamageSources().generic(),10000);
        if(p.isAlive()&&!p.isCreative()&&!p.isSpectator())PULSES.add(new Pulse(world,p.getUuid(),clock+30));
    }
    private static void tick(MinecraftServer server){
        clock++;
        var pending=PULSES.iterator();
        while(pending.hasNext()){
            var pulse=pending.next();if(pulse.tick()>clock)continue;
            var p=server.getPlayerManager().getPlayer(pulse.player());
            if(p!=null&&p.isAlive()&&p.getWorld()==pulse.world()&&!p.isCreative()&&!p.isSpectator())p.damage(pulse.world().getDamageSources().generic(),10000);
            pending.remove();
        }
        var it=WAVES.iterator();
        while(it.hasNext()){
            var a=it.next();var w=a.wave();
            boolean running=w.tick(a.world(),p->impact(p,a.world()));
            // Lightweight diagnostic effect; the approved V5 renderer replaces this later.
            if((a.visual()==null||a.visual().isRemoved())&&w.age()%4==0){
                double r=w.age()<=80?3:w.front();
                for(var p:a.world().getPlayers()){
                    double dx=p.getX()-w.origin().x,dz=p.getZ()-w.origin().z;
                    if(Math.abs(Math.hypot(dx,dz)-r)>32)continue;
                    double center=Math.atan2(dz,dx);
                    for(int i=-8;i<=8;i++){
                        double angle=center+i*2.0/Math.max(3,r);
                        double x=w.origin().x+r*Math.cos(angle),z=w.origin().z+r*Math.sin(angle);
                        a.world().spawnParticles(p,ParticleTypes.SPLASH,false,x,w.origin().y+(w.age()<=80?.25:7.5),z,2,.3,.2,.3,0);
                    }
                }
            }
            if(!running)it.remove();
        }
    }
}
