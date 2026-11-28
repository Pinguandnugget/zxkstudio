package zxk.leviathan;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.Comparator;
import static net.minecraft.server.command.CommandManager.*;

public final class LeviathanCommands {
    private static final String[] ATTACKS = {
        "attack_bite", "attack_suction", "attack_head_slam", "attack_sweep",
        "attack_pressure_roar", "attack_corrosive_spit", "attack_abyssal_gaze", "attack_tsunami"
    };

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            var root = literal("leviathan").requires(s -> s.hasPermissionLevel(2));
            root.then(literal("spawn").executes(c -> spawn(c.getSource())));
            root.then(literal("start").executes(c -> cinematic(c.getSource())));
            root.then(literal("cinematic").executes(c -> cinematic(c.getSource())));
            var attack = argument("attack", StringArgumentType.word())
                .suggests((c, b) -> {
                    for (String name : ATTACKS) b.suggest(name);
                    return b.buildFuture();
                })
                .executes(LeviathanCommands::testAttack);
            root.then(literal("testattack").then(attack));
            dispatcher.register(root);
        });
    }

    private static int testAttack(CommandContext<ServerCommandSource> c) {
        var source = c.getSource();
        Vec3d pos = source.getPosition();
        var boss = source.getWorld()
            .getEntitiesByClass(LeviathanEntity.class, new Box(pos, pos).expand(100), e -> e.isAlive())
            .stream().min(Comparator.comparingDouble(e -> e.getPos().squaredDistanceTo(pos)))
            .orElse(null);
        if (boss == null || !boss.testAttack(StringArgumentType.getString(c, "attack"))) {
            source.sendError(Text.literal("Boss no encontrado, ocupado o ataque inválido."));
            return 0;
        }
        return 1;
    }

    private static int cinematic(ServerCommandSource source) {
        var world=source.getWorld();
        Vec3d origin=source.getPosition();
        float yaw=source.getRotation().y;
        Vec3d travel=new Vec3d(0,850.0/16,-1045.0/16).rotateY((float)Math.toRadians(-yaw));
        Vec3d destination=origin.add(travel);
        if(!world.isChunkLoaded(net.minecraft.util.math.BlockPos.ofFloored(origin))){
            source.sendError(Text.literal("Carga primero los chunks de la fosa cinematográfica."));
            return 0;
        }
        var full=LeviathanMod.FULL.create(world);
        if(full==null)return 0;
        full.refreshPositionAndAngles(origin.x,origin.y,origin.z,yaw,0);
        full.configure(destination,yaw);
        if(!world.spawnEntity(full))return 0;
        source.sendFeedback(()->Text.literal("Cinemática iniciada (50 segundos). El boss se invoca por separado con /leviathan spawn."),false);
        return 1;
    }

    private static int spawn(ServerCommandSource source) {
        var boss = LeviathanMod.BOSS.create(source.getWorld());
        if (boss == null) return 0;
        Vec3d pos = source.getPosition();
        boss.refreshPositionAndAngles(pos.x, pos.y, pos.z, source.getRotation().y, 0);
        if (!source.getWorld().spawnEntity(boss)) return 0;
        boss.beginAppearance();
        return 1;
    }
}
