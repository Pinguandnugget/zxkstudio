package com.zxkstudio.rings;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ClientPlayerEntity.class)
public abstract class RingVisionMixin {
    @Inject(method="getUnderwaterVisibility",at=@At("RETURN"),cancellable=true)
    private void zxkstudio$ringVision(CallbackInfoReturnable<Float> cir){
        ClientPlayerEntity p=(ClientPlayerEntity)(Object)this;
        Rings.Ring r=Rings.equipped(p);
        if(r!=null&&r.branch()==4&&Rings.ocean(p))cir.setReturnValue(Math.max(cir.getReturnValue(),r.tier()==1?.6f:1f));
    }
}
