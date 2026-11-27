package zxk.leviathan.arena;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Server-authoritative wave, no fluid updates, no helper entities or chunk loading. */
public final class RadialWave {
    public static final int WARNING_TICKS = 80;
    public static final double DEFAULT_RADIUS = 100, DEFAULT_HEIGHT = 8, DEFAULT_SPEED = 0.5;
    private final Vec3d origin;
    private final double radius, height, speed;
    private final Set<UUID> hit = new HashSet<>();
    private int age;
    public RadialWave(Vec3d origin, double radius, double height, double speed) {
        if (!Double.isFinite(radius) || !Double.isFinite(height) || !Double.isFinite(speed)
            || radius <= 0 || radius > 256 || height <= 0 || speed <= 0 || speed > 10)
            throw new IllegalArgumentException("Invalid wave dimensions");
        this.origin=origin; this.radius=radius; this.height=height; this.speed=speed;
    }
    public Vec3d origin(){return origin;}
    public int age(){return age;}
    public double front(){return Math.min(radius, Math.max(0,age-WARNING_TICKS)*speed);}
    public boolean tick(ServerWorld world, java.util.function.Consumer<ServerPlayerEntity> onHit){
        age++;
        if(age<=WARNING_TICKS) return true;
        double front=front(), previous=Math.max(0,front-speed);
        for(var p:world.getPlayers()){
            if(!p.isAlive()||p.isCreative()||p.isSpectator()||hit.contains(p.getUuid())) continue;
            var box=p.getBoundingBox();
            double dx=Math.max(0,Math.max(box.minX-origin.x,origin.x-box.maxX));
            double dz=Math.max(0,Math.max(box.minZ-origin.z,origin.z-box.maxZ));
            double near=Math.hypot(dx,dz);
            double far=Math.hypot(Math.max(Math.abs(box.minX-origin.x),Math.abs(box.maxX-origin.x)),
                                  Math.max(Math.abs(box.minZ-origin.z),Math.abs(box.maxZ-origin.z)));
            if(near<=front && far>=previous && box.minY<origin.y+height && box.maxY>origin.y){
                hit.add(p.getUuid());onHit.accept(p);
            }
        }
        return front<radius;
    }
}
