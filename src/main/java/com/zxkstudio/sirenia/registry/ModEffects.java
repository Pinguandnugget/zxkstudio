package com.zxkstudio.sirenia.registry;

import com.zxkstudio.ZxkStudio;
import com.zxkstudio.sirenia.effect.SireniaRotEffect;
import com.zxkstudio.sirenia.effect.SirenomorphosisEffect;
import com.zxkstudio.sirenia.effect.ThalassophobiaEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public final class ModEffects {
	private ModEffects() {
	}

	public static RegistryEntry<StatusEffect> SIRENOMORPHOSIS;
	/** Lo que provoca el Corazón de Sirenia podrido. */
	public static RegistryEntry<StatusEffect> SIRENIA_ROT;
	/** Miedo al agua profunda (indicador). */
	public static RegistryEntry<StatusEffect> THALASSOPHOBIA;

	public static void register() {
		SIRENOMORPHOSIS = Registry.registerReference(
				Registries.STATUS_EFFECT,
				Identifier.of(ZxkStudio.MOD_ID, "sirenomorphosis"),
				new SirenomorphosisEffect());
		SIRENIA_ROT = Registry.registerReference(
				Registries.STATUS_EFFECT,
				Identifier.of(ZxkStudio.MOD_ID, "sirenia_rot"),
				new SireniaRotEffect());
		THALASSOPHOBIA = Registry.registerReference(
				Registries.STATUS_EFFECT,
				Identifier.of(ZxkStudio.MOD_ID, "thalassophobia"),
				new ThalassophobiaEffect());
	}
}
