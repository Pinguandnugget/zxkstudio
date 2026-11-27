package zxk.leviathan.client.mixin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public abstract class LeviathanBossHudMixin {
    @Shadow @Final Map<UUID, ClientBossBar> bossBars;
    @Unique private static final Identifier LEVIATHAN_ART = Identifier.of("leviathan_abisal","textures/gui/bossbar.png");
    @Unique private static final int[] LEVIATHAN_MARKS = {37,62,86,111,136,160,185};
    @Unique private static boolean leviathan$isCustom(ClientBossBar bar) {
        return bar.getName().getContent() instanceof TranslatableTextContent t
            && t.getKey().equals("boss.leviathan_abisal.leviathan");
    }
    // Filter only our own bar from the vanilla drawing loop. The network map stays untouched.
    @Redirect(method="render",at=@At(value="INVOKE",target="Ljava/util/Map;values()Ljava/util/Collection;"))
    private Collection<ClientBossBar> leviathan$vanillaBars(Map<UUID,ClientBossBar> bars) {
        var normal=new ArrayList<ClientBossBar>();
        for(var bar:bars.values())if(!leviathan$isCustom(bar))normal.add(bar);
        return normal;
    }
    @Inject(method="render",at=@At("TAIL"))
    private void leviathan$drawCustom(DrawContext context, CallbackInfo ci){
        int normal=0;
        for(var bar:bossBars.values())if(!leviathan$isCustom(bar))normal++;
        int screenW=context.getScaledWindowWidth(),screenH=context.getScaledWindowHeight();
        int y=12+19*Math.min(normal,Math.max(1,(screenH/3-12)/19+1));
        float scale=Math.min(1f,(screenW-16)/256f);
        if(scale<=0)return;
        for(var bar:bossBars.values()){
            if(!leviathan$isCustom(bar))continue;
            if(y+84*scale>screenH)break;
            int x=Math.round((screenW-256*scale)/2);
            var matrix=context.getMatrices();
            matrix.push();
            matrix.translate(x,y,0);
            matrix.scale(scale,scale,1);
            context.drawTexture(LEVIATHAN_ART,0,0,0f,0f,256,84,256,84);
            float hp=Math.max(0,Math.min(1,bar.getPercent()));
            int cut=15+Math.round(187*hp);
            if(cut<202){
                // Only shade the depleted water channel, never the frame or portrait.
                context.fill(cut,36,202,45,0xED071824);
                for(int mark:LEVIATHAN_MARKS)if(mark>=cut)
                    context.drawTexture(LEVIATHAN_ART,mark,35,(float)mark,35f,1,10,256,84);
            }
            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,bar.getName(),108,23,0xC8F8FF);
            matrix.pop();
            y+=Math.round(84*scale)+4;
        }
    }
}
