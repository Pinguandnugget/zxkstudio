package zxk.leviathan.arena;

import net.minecraft.util.math.Vec3d;

/** Pure motion planner. Caller validates anchor, collision and player state each tick. */
public final class GrappleMotion {
    public static final double REACH=40, RISE=12, MAX_SPEED=0.95;
    public static final int HOLD_TICKS=100, COOLDOWN_TICKS=160;
    private GrappleMotion(){}
    public static Vec3d target(Vec3d player,Vec3d anchor){
        if(anchor.y<=player.y+2 || player.distanceTo(anchor)>REACH) return null;
        return new Vec3d(anchor.x,Math.min(player.y+RISE,anchor.y-3.0),anchor.z);
    }
    public static Vec3d velocity(Vec3d player,Vec3d target){
        Vec3d delta=target.subtract(player); double distance=delta.length();
        if(distance<0.12)return Vec3d.ZERO;
        return delta.multiply(Math.min(MAX_SPEED,distance*0.28)/distance);
    }
}
