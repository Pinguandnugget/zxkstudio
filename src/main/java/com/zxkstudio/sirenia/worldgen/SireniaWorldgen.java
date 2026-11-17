package com.zxkstudio.sirenia.worldgen;

import com.zxkstudio.ZxkStudio;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Registra los tres tipos de árbol y los añade a los biomas. Las listas de colocación están en
 * data/zxkstudio/worldgen/placed_feature; las probabilidades se leen de la configuración al generar.
 */
public final class SireniaWorldgen {
	private SireniaWorldgen() {
	}

	private static RegistryKey<PlacedFeature> placed(String name) {
		return RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(ZxkStudio.MOD_ID, name));
	}

	private static void feature(String name, SireniaTreeFeature.Kind kind) {
		Registry.register(Registries.FEATURE, Identifier.of(ZxkStudio.MOD_ID, name),
				new SireniaTreeFeature(DefaultFeatureConfig.CODEC, kind));
	}

	public static void register() {
		feature("sirenia_tree_mangrove", SireniaTreeFeature.Kind.MANGROVE);
		feature("sirenia_tree_coast", SireniaTreeFeature.Kind.COAST);
		feature("sirenia_tree_cave", SireniaTreeFeature.Kind.CAVE);

		// Manglares: bioma principal.
		BiomeModifications.addFeature(BiomeSelectors.includeByKey(BiomeKeys.MANGROVE_SWAMP),
				GenerationStep.Feature.VEGETAL_DECORATION, placed("sirenia_tree_mangrove"));
		// Costas de océanos cálidos.
		BiomeModifications.addFeature(BiomeSelectors.includeByKey(BiomeKeys.BEACH, BiomeKeys.WARM_OCEAN,
						BiomeKeys.LUKEWARM_OCEAN, BiomeKeys.DEEP_LUKEWARM_OCEAN),
				GenerationStep.Feature.VEGETAL_DECORATION, placed("sirenia_tree_coast"));
		// Cuevas frondosas.
		BiomeModifications.addFeature(BiomeSelectors.includeByKey(BiomeKeys.LUSH_CAVES),
				GenerationStep.Feature.VEGETAL_DECORATION, placed("sirenia_tree_cave"));
	}
}
