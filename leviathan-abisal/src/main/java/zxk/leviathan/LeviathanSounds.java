package zxk.leviathan;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
public final class LeviathanSounds {
    public static final SoundEvent ROAR=Registry.register(Registries.SOUND_EVENT,Identifier.of("leviathan_abisal","roar"),SoundEvent.of(Identifier.of("leviathan_abisal","roar")));
    public static void init(){}
    public static SoundEvent roar(){return LeviathanSounds.class.getResource("/assets/leviathan_abisal/sounds/roar.ogg")!=null?ROAR:SoundEvents.ENTITY_RAVAGER_ROAR;}
}
