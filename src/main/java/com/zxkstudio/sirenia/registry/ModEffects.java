package com.zxkstudio.sirenia.registry;

import com.zxkstudio.ZxkStudio;
import com.zxkstudio.sirenia.effect.SirenomorphosisEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public final class ModEffects {
	private ModEffects() {
	}

	public static RegistryEntry<StatusEffect> SIRENOMORPHOSIS;

	public static void register() {
		SIRENOMORPHOSIS = Registry.registerReference(
				Registries.STATUS_EFFECT,
				Identifier.of(ZxkStudio.MOD_ID, "sirenomorphosis"),
				new SirenomorphosisEffect());
	}
}
