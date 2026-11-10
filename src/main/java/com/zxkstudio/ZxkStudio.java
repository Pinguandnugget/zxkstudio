package com.zxkstudio;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZxkStudio implements ModInitializer {
	public static final String MOD_ID = "zxkstudio";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		SangreConfig.cargar();
		CiberConfig.cargar();
		ModContent.register();
		SangreJurasica.register();
		SangreCommands.register();
		CiberPsicosis.register();
		CiberCommands.register();
		LOGGER.info("zxkstudio cargado: Sangre Jurásica y Cyberpsychosis listas");
	}
}
