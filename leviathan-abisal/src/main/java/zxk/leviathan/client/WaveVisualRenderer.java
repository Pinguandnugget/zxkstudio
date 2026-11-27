package zxk.leviathan.client;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.animation.AnimationState;
import zxk.leviathan.arena.WaveVisualEntity;
public final class WaveVisualRenderer extends GeoEntityRenderer<WaveVisualEntity>{
    public WaveVisualRenderer(EntityRendererFactory.Context ctx){super(ctx,new WaveModel());}
    @Override public boolean shouldRender(WaveVisualEntity e,Frustum f,double x,double y,double z){return true;}
    @Override public RenderLayer getRenderType(WaveVisualEntity e,Identifier texture,VertexConsumerProvider buffers,float partialTick){return RenderLayer.getEntityTranslucent(texture);}
    private static final class WaveModel extends GeoModel<WaveVisualEntity>{
        public Identifier getModelResource(WaveVisualEntity e){return Identifier.of("leviathan_abisal","geo/tsunami.geo.json");}
        public Identifier getTextureResource(WaveVisualEntity e){return Identifier.of("leviathan_abisal","textures/entity/tsunami.png");}
        public Identifier getAnimationResource(WaveVisualEntity e){return Identifier.of("leviathan_abisal","animations/tsunami.animation.json");}
        @Override public void setCustomAnimations(WaveVisualEntity e,long instanceId,AnimationState<WaveVisualEntity> state){
            super.setCustomAnimations(e,instanceId,state);
            int t=e.waveTick();
            float radius=t<=80?.025f:Math.max(.005f,Math.min(1f,(t-80)/200f));
            float height=t<=80?.18f+.32f*t/80f:t<=280?1f:Math.max(.001f,1f-(t-280)/16f);
            for(String name:new String[]{"wave_ring","foam_ring"}){
                var b=getAnimationProcessor().getBone(name);
                if(b!=null){b.setScaleX(radius);b.setScaleZ(radius);b.setScaleY(height);}
            }
            var b=getAnimationProcessor().getBone("warning_ring");
            if(b!=null){float v=t<=80?.03f+.006f*(float)Math.sin(t*.12):.001f;b.setScaleX(v);b.setScaleZ(v);b.setScaleY(t<=80?1:.001f);}
        }
    }
}
