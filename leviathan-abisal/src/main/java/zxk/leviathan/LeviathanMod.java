package zxk.leviathan;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.*;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
public class LeviathanMod implements ModInitializer {
 public static final String ID="leviathan_abisal";
 public static BossConfig CONFIG;
 public static final EntityType<LeviathanEntity> BOSS=Registry.register(Registries.ENTITY_TYPE,Identifier.of(ID,"leviathan"),EntityType.Builder.create(LeviathanEntity::new,SpawnGroup.MONSTER).dimensions(5,18).maxTrackingRange(128).build("leviathan_abisal:leviathan"));
 public void onInitialize(){CONFIG=BossConfig.load();FabricDefaultAttributeRegistry.register(BOSS,LeviathanEntity.attributes());}
}
