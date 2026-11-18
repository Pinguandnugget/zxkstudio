package com.zxkstudio.sirenia.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zxkstudio.sirenia.config.SireniaConfig;
import com.zxkstudio.sirenia.registry.ModEffects;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Visión submarina de Sirenomorphosis. Único mixin del mod, solo en el cliente.
 *
 * Fabric API no ofrece evento de niebla, así que se engancha al final de BackgroundRenderer.applyFog
 * y solo alarga la distancia de la niebla del agua mientras la cámara está sumergida y la entidad
 * observada tiene el efecto. No toca ninguna opción gráfica del jugador y se deshace sola al terminar.
 * Con require = 0, si otro mod cambiara ese método, simplemente no se aplica (nunca bloquea el arranque).
 */
@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
	@Inject(method = "applyFog", at = @At("RETURN"), require = 0)
	private static void zxkstudio$clearWaterFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance,
			boolean thickFog, float tickDelta, CallbackInfo ci) {
		if (fogType != BackgroundRenderer.FogType.FOG_TERRAIN) {
			return;
		}
		SireniaConfig cfg = SireniaConfig.get();
		if (!cfg.underwater_vision_enabled || camera.getSubmersionType() != CameraSubmersionType.WATER) {
			return;
		}
		Entity focused = camera.getFocusedEntity();
		if (focused instanceof LivingEntity living && living.hasStatusEffect(ModEffects.SIRENOMORPHOSIS)) {
			RenderSystem.setShaderFogStart(0.0f);
			RenderSystem.setShaderFogEnd(cfg.underwater_vision_distance);
		}
	}
}
