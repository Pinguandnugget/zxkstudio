package com.zxkstudio.rings;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class RingDamageMixin {
    @ModifyVariable(method="damage",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float zxkstudio$ringDamage(float amount,DamageSource source,float original){return Rings.reduce((LivingEntity)(Object)this,source,amount);}
    @Inject(method="heal",at=@At("HEAD"))
    private void zxkstudio$ringHeal(float amount,CallbackInfo ci){Rings.healed((LivingEntity)(Object)this,amount);}
}
