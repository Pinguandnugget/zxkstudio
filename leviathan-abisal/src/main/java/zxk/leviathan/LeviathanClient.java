package zxk.leviathan;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public class LeviathanClient implements ClientModInitializer {
 public void onInitializeClient(){EntityRendererRegistry.register(LeviathanMod.BOSS,ctx->new GeoEntityRenderer<LeviathanEntity>(ctx,new GeoModel<LeviathanEntity>(){
 public Identifier getModelResource(LeviathanEntity e){return Identifier.of(LeviathanMod.ID,"geo/boss.geo.json");}
 public Identifier getTextureResource(LeviathanEntity e){return Identifier.of(LeviathanMod.ID,"textures/entity/boss.png");}
 public Identifier getAnimationResource(LeviathanEntity e){return Identifier.of(LeviathanMod.ID,"animations/boss.animation.json");}
 }));}
}
