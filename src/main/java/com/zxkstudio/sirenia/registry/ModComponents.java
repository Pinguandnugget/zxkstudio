package com.zxkstudio.sirenia.registry;

import com.mojang.serialization.Codec;
import com.zxkstudio.ZxkStudio;
import com.zxkstudio.sirenia.util.FruitClock;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModComponents {
	private ModComponents() {
	}

	/** Componente antiguo (solo lectura): frutos cosechados con la primera versión. Se migra a FRUIT_CLOCK. */
	public static ComponentType<Long> BIRTH_TIME;
	/** Reloj del fruto: instante real en que nació su injerto. Frutos de la misma fase se apilan. */
	public static ComponentType<FruitClock> FRUIT_CLOCK;

	public static void register() {
		BIRTH_TIME = Registry.register(
				Registries.DATA_COMPONENT_TYPE,
				Identifier.of(ZxkStudio.MOD_ID, "birth_time"),
				ComponentType.<Long>builder()
						.codec(Codec.LONG)
						.packetCodec(PacketCodecs.VAR_LONG)
						.build());
		FRUIT_CLOCK = Registry.register(
				Registries.DATA_COMPONENT_TYPE,
				Identifier.of(ZxkStudio.MOD_ID, "fruit_clock"),
				ComponentType.<FruitClock>builder()
						.codec(Codec.LONG.xmap(FruitClock::new, FruitClock::birthMs))
						.packetCodec(PacketCodecs.VAR_LONG.xmap(FruitClock::new, FruitClock::birthMs))
						.build());
	}
}
