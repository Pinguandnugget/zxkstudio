package com.zxkstudio.hatch;

import com.google.gson.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class HatchClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        BlockEntityRendererRegistry.register(Hatches.TYPE,Renderer::new);
    }

    private record Vertex(float x,float y,float z,float u,float v,float nx,float ny,float nz) {}
    private record Group(int side,List<Vertex> vertices) {}
    private static final class Renderer implements BlockEntityRenderer<HatchEntity> {
        private static final Identifier TEXTURE=Identifier.of("zxkstudio","textures/block/escotilla_abisal.png");
        private static final List<Group> GROUPS=load();
        // Local X left/right, Y vertical, Z depth. Each panel moves to its own side.
        private static final float[] TIMES={0,3,6,9,13,18,23,27,30,32};
        private static final float[] SLIDE={0,0,0,1.5f,5,12,19,23,24,24};
        private static final float[] DEPTH={0,-.25f,-2.2f,-2.2f,-2.2f,-2.2f,-2.2f,-2.2f,-2.2f,-2.2f};
        Renderer(BlockEntityRendererFactory.Context context) {}
        @Override public boolean rendersOutsideBoundingBox(HatchEntity entity) { return true; }
        @Override public int getRenderDistance() { return 96; }
        @Override public void render(HatchEntity entity,float tickDelta,MatrixStack matrices,VertexConsumerProvider consumers,int light,int overlay) {
            float t=entity.animation(tickDelta)*HatchEntity.DURATION;
            float slide=sample(SLIDE,t)/16f, depth=sample(DEPTH,t)/16f;
            matrices.push();
            matrices.translate(.5,0,.5);
            float yaw=switch(entity.getCachedState().get(HatchBlock.FACING)) {
                case EAST -> 90; case NORTH -> 180; case WEST -> -90; default -> 0;
            };
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
            VertexConsumer buffer=consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
            for(Group group:GROUPS) {
                matrices.push();
                if(group.side()!=0) matrices.translate(group.side()*slide,0,depth);
                MatrixStack.Entry matrix=matrices.peek();
                for(Vertex v:group.vertices()) {
                    buffer.vertex(matrix,v.x(),v.y(),v.z()).color(255,255,255,255)
                        .texture(v.u(),v.v()).overlay(overlay).light(light)
                        .normal(matrix,v.nx(),v.ny(),v.nz());
                }
                matrices.pop();
            }
            matrices.pop();
        }
        private static float sample(float[] values,float t) {
            for(int i=1;i<TIMES.length;i++) if(t<=TIMES[i]) {
                float q=Math.max(0,Math.min(1,(t-TIMES[i-1])/(TIMES[i]-TIMES[i-1])));
                return values[i-1]+q*(values[i]-values[i-1]);
            }
            return values[values.length-1];
        }
        private static float[] array(JsonObject o,String key) {
            JsonArray a=o.getAsJsonArray(key);
            float[] result=new float[a.size()];
            for(int i=0;i<result.length;i++) result[i]=a.get(i).getAsFloat();
            return result;
        }
        private static List<Group> load() {
            try(InputStream input=Renderer.class.getResourceAsStream("/assets/zxkstudio/escotilla/model.json")) {
                if(input==null) throw new IOException("Missing escotilla/model.json");
                JsonArray groups=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonArray();
                List<Group> result=new ArrayList<>();
                for(JsonElement element:groups) {
                    JsonObject group=element.getAsJsonObject();
                    String name=group.get("name").getAsString();
                    int side=name.equals("compuerta_izquierda")?-1:name.equals("compuerta_derecha")?1:0;
                    List<Vertex> vertices=new ArrayList<>();
                    for(JsonElement cube:group.getAsJsonArray("cubes")) addCube(cube.getAsJsonObject(),vertices);
                    result.add(new Group(side,List.copyOf(vertices)));
                }
                return List.copyOf(result);
            } catch(Exception exception) { throw new IllegalStateException("Invalid Escotilla Abisal V4 geometry",exception); }
        }
        private static void addCube(JsonObject cube,List<Vertex> out) {
            float[] a=array(cube,"from"),b=array(cube,"to"),o=array(cube,"origin"),r=array(cube,"rotation");
            float x=a[0],y=a[1],z=a[2],X=b[0],Y=b[1],Z=b[2];
            float[][][] positions={
                {{x,Y,Z},{x,y,Z},{X,y,Z},{X,Y,Z}},
                {{X,Y,z},{X,y,z},{x,y,z},{x,Y,z}},
                {{X,Y,Z},{X,y,Z},{X,y,z},{X,Y,z}},
                {{x,Y,z},{x,y,z},{x,y,Z},{x,Y,Z}},
                {{x,Y,z},{x,Y,Z},{X,Y,Z},{X,Y,z}},
                {{x,y,Z},{x,y,z},{X,y,z},{X,y,Z}}
            };
            float[][] normals={{0,0,1},{0,0,-1},{1,0,0},{-1,0,0},{0,1,0},{0,-1,0}};
            String[] keys={"south","north","east","west","up","down"};
            double angle=Math.toRadians(r[2]), c=Math.cos(angle),s=Math.sin(angle);
            JsonObject faces=cube.getAsJsonObject("faces");
            for(int f=0;f<keys.length;f++) {
                if(!faces.has(keys[f])) continue;
                JsonObject face=faces.getAsJsonObject(keys[f]);
                if(!face.has("texture") || face.get("texture").isJsonNull()) continue;
                float[] uv=array(face,"uv");
                int rot=face.has("rotation")?Math.floorMod(face.get("rotation").getAsInt()/90,4):0;
                float[][] corners={{uv[0],uv[1]},{uv[0],uv[3]},{uv[2],uv[3]},{uv[2],uv[1]}};
                float nx=(float)(normals[f][0]*c-normals[f][1]*s),ny=(float)(normals[f][0]*s+normals[f][1]*c);
                for(int i=0;i<4;i++) {
                    float[] p=positions[f][i], tex=corners[(i+rot)%4];
                    float px=(float)(o[0]+(p[0]-o[0])*c-(p[1]-o[1])*s);
                    float py=(float)(o[1]+(p[0]-o[0])*s+(p[1]-o[1])*c);
                    out.add(new Vertex((px-8)/16f,(py-16)/16f,(p[2]-8)/16f,tex[0]/128f,tex[1]/128f,nx,ny,normals[f][2]));
                }
            }
        }
    }
}
