package zxk.leviathan;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import com.mojang.brigadier.arguments.StringArgumentType;
import static net.minecraft.server.command.CommandManager.*;
public final class LeviathanCommands {
 public static void init(){
 CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(literal("leviathan").requires(s->s.hasPermissionLevel(2))
 .then(literal("spawn").executes(c->spawn(c.getSource(),true)))
 .then(literal("start").executes(c->spawn(c.getSource(),true)))
 .then(literal("testattack").then(argument("attack",StringArgumentType.word()).suggests((c,b)->{for(String a:new String[]{"attack_bite","attack_suction","attack_head_slam","attack_sweep","attack_pressure_roar","attack_corrosive_spit","attack_abyssal_gaze","attack_tsunami"})b.suggest(a);return b.buildFuture();}).executes(c->{var s=c.getSource();var boss=s.getWorld().getEntitiesByClass(LeviathanEntity.class,new net.minecraft.util.math.Box(s.getPosition(),s.getPosition()).expand(100),e->e.isAlive()).stream().min(java.util.Comparator.comparingDouble(e->e.getPos().squaredDistanceTo(s.getPosition()))).orElse(null);if(boss==null||!boss.testAttack(StringArgumentType.getString(c,"attack"))){s.sendError(Text.literal("Boss no encontrado, ocupado o ataque inválido."));return 0;}return 1;}))));
 }
 private static int spawn(ServerCommandSource s,boolean appearance){var boss=LeviathanMod.BOSS.create(s.getWorld());if(boss==null)return 0;Vec3d p=s.getPosition();boss.refreshPositionAndAngles(p.x,p.y,p.z,s.getRotation().y,0);if(!s.getWorld().spawnEntity(boss))return 0;if(appearance)boss.beginAppearance();return 1;}
}
