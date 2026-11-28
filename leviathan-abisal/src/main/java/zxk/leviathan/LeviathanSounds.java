package zxk.leviathan;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
public final class LeviathanSounds {
    public static final SoundEvent ROAR=Registry.register(Registries.SOUND_EVENT,Identifier.of("leviathan_abisal","roar"),SoundEvent.of(Identifier.of("leviathan_abisal","roar")));
    public static final SoundEvent AMBIENT=register("ambient"), DISTANT_ROAR=register("distant_roar"), ALTERNATE=register("alternate");
    private static SoundEvent register(String name){Identifier id=Identifier.of("leviathan_abisal",name);return Registry.register(Registries.SOUND_EVENT,id,SoundEvent.of(id));}
    public static void init(){}
    public static SoundEvent roar(){return ROAR;}
}
