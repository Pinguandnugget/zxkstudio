package com.zxkstudio.sirenia;

import com.zxkstudio.ZxkStudio;
import com.zxkstudio.sirenia.config.SireniaConfig;
import com.zxkstudio.sirenia.effect.SireniaRot;
import com.zxkstudio.sirenia.fear.Thalassophobia;
import com.zxkstudio.sirenia.fear.ThalassophobiaCommand;
import com.zxkstudio.sirenia.effect.SirenomorphosisEvents;
import com.zxkstudio.sirenia.registry.ModBlockEntities;
import com.zxkstudio.sirenia.registry.ModBlocks;
import com.zxkstudio.sirenia.registry.ModComponents;
import com.zxkstudio.sirenia.registry.ModEffects;
import com.zxkstudio.sirenia.registry.ModItemGroups;
import com.zxkstudio.sirenia.registry.ModItems;
import com.zxkstudio.sirenia.worldgen.SireniaWorldgen;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;

/**
 * PotitoSirenia: Árbol de Sirenia, injertos de Corazón de Sirenia y efecto Sirenomorphosis.
 * Se arranca con una sola llamada desde ZxkStudio.onInitialize, así el resto del mod no cambia.
 */
public final class Sirenia {
	private Sirenia() {
	}

	public static void init() {
		SireniaConfig.load();
		// El orden importa: los bloques antes que sus ítems y entidades de bloque.
		ModComponents.register();
		ModBlocks.register();
		ModBlockEntities.register();
		ModItems.register();
		ModEffects.register();
		ModItemGroups.register();

		// Pelar con hacha y que arda como la madera normal.
		StrippableBlockRegistry.register(ModBlocks.SIRENIA_LOG, ModBlocks.STRIPPED_SIRENIA_LOG);
		StrippableBlockRegistry.register(ModBlocks.SIRENIA_WOOD, ModBlocks.STRIPPED_SIRENIA_WOOD);
		FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
		flammable.add(ModBlocks.SIRENIA_LOG, 5, 5);
		flammable.add(ModBlocks.STRIPPED_SIRENIA_LOG, 5, 5);
		flammable.add(ModBlocks.SIRENIA_WOOD, 5, 5);
		flammable.add(ModBlocks.STRIPPED_SIRENIA_WOOD, 5, 5);
		flammable.add(ModBlocks.SIRENIA_PLANKS, 5, 20);
		flammable.add(ModBlocks.SIRENIA_LEAVES, 30, 60);

		SirenomorphosisEvents.register();
		SireniaRot.register();
		com.zxkstudio.sirenia.effect.FruitSplitEvents.register();
		Thalassophobia.register();
		ThalassophobiaCommand.register();
		SireniaWorldgen.register();
		ZxkStudio.LOGGER.info("PotitoSirenia cargado: Árbol de Sirenia y Sirenomorphosis listos");
	}
}
