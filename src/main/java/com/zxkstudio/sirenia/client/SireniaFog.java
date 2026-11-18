package com.zxkstudio.sirenia.client;

import com.zxkstudio.sirenia.config.SireniaConfig;
import com.zxkstudio.sirenia.registry.ModEffects;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

/**
 * Niebla de Sirenomorphosis. Valores tomados del procedimiento original de Dedsafio 3
 * (EnhancedUnderwaterVisionProcedure): bajo el agua alcance 300 y tono azulado (0,5 / 0,7 / 1);
 * fuera del agua alcance 20 y tono rosado (1 / 0,8 / 1). Solo cliente; no cambia ninguna opción gráfica.
 */
public final class SireniaFog {
	public static final int NONE = 0;
	public static final int WATER = 1;
	public static final int LAND = 2;

	private SireniaFog() {
	}

	public static int mode(Camera camera) {
		Entity focused = camera.getFocusedEntity();
		if (!(focused instanceof LivingEntity living) || !living.hasStatusEffect(ModEffects.SIRENOMORPHOSIS)) {
			return NONE;
		}
		SireniaConfig cfg = SireniaConfig.get();
		CameraSubmersionType type = camera.getSubmersionType();
		if (type == CameraSubmersionType.WATER) {
			return cfg.underwater_vision_enabled ? WATER : NONE;
		}
		if (type == CameraSubmersionType.NONE) {
			return cfg.land_fog_enabled ? LAND : NONE;
		}
		return NONE;
	}

	public static int currentMode() {
		return mode(MinecraftClient.getInstance().gameRenderer.getCamera());
	}

	public static float distance(int mode) {
		SireniaConfig cfg = SireniaConfig.get();
		return mode == WATER ? cfg.underwater_vision_distance : cfg.land_fog_distance;
	}

	public static float red(int mode) {
		return mode == WATER ? 0.5f : 1.0f;
	}

	public static float green(int mode) {
		return mode == WATER ? 0.7f : 0.8f;
	}

	public static float blue(int mode) {
		return 1.0f;
	}
}
