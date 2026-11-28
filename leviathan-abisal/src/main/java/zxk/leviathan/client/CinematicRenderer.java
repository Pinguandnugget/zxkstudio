package zxk.leviathan.client;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import zxk.leviathan.CinematicEntity;
public final class CinematicRenderer extends GeoEntityRenderer<CinematicEntity>{
    public CinematicRenderer(EntityRendererFactory.Context ctx){super(ctx,new GeoModel<CinematicEntity>(){
        public Identifier getModelResource(CinematicEntity e){return Identifier.of("leviathan_abisal","geo/full.geo.json");}
        public Identifier getTextureResource(CinematicEntity e){return Identifier.of("leviathan_abisal","textures/entity/full.png");}
        public Identifier getAnimationResource(CinematicEntity e){return Identifier.of("leviathan_abisal","animations/full.animation.json");}
    });}
    @Override public boolean shouldRender(CinematicEntity e,Frustum f,double x,double y,double z){return true;}
}
