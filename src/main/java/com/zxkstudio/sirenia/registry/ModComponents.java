package com.zxkstudio.sirenia.registry;

import com.mojang.serialization.Codec;
import com.zxkstudio.ZxkStudio;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModComponents {
	private ModComponents() {
	}

	/** Instante real (ms, redondeado al minuto) en que nació el injerto del que salió el fruto. */
	public static ComponentType<Long> BIRTH_TIME;

	public static void register() {
		BIRTH_TIME = Registry.register(
				Registries.DATA_COMPONENT_TYPE,
				Identifier.of(ZxkStudio.MOD_ID, "birth_time"),
				ComponentType.<Long>builder()
						.codec(Codec.LONG)
						.packetCodec(PacketCodecs.VAR_LONG)
						.build());
	}
}
