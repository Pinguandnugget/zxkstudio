package zxk.leviathan;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
public class BossConfig {
 public double maxHealth=300000, phase2Threshold=.70, phase3Threshold=.35;
 public double biteDurationSeconds=10, biteTotemIntervalSeconds=1.5;
 public int stunDurationSeconds=10;
 public boolean destroyBlocks=false;
 public static BossConfig load(){
 var gson=new GsonBuilder().setPrettyPrinting().create(); var file=FabricLoader.getInstance().getConfigDir().resolve("leviathan_abisal.json");
 try { Files.createDirectories(file.getParent()); if(Files.exists(file)){var c=gson.fromJson(Files.readString(file),BossConfig.class); if(c==null||!Double.isFinite(c.maxHealth)||c.maxHealth<=0||c.maxHealth>1e12||!(c.phase3Threshold>0&&c.phase3Threshold<c.phase2Threshold&&c.phase2Threshold<1)||c.biteDurationSeconds!=10||c.biteTotemIntervalSeconds!=1.5) throw new IllegalArgumentException("Configuracion invalida; tiempos de mordida fijos en alpha"); return c;} var c=new BossConfig();Files.writeString(file,gson.toJson(c));return c; } catch(Exception e){throw new IllegalStateException("No se pudo leer configuracion Leviathan",e);}
 }
}
