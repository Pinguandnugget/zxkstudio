package com.zxkstudio.sirenia.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zxkstudio.sirenia.client.SireniaFog;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Visión de Sirenomorphosis (niebla y color). Solo cliente.
 *
 * Fabric API no ofrece evento de niebla, así que se engancha al final de tres métodos de BackgroundRenderer
 * y solo actúa mientras la cámara pertenece a una entidad con el efecto. Con require = 0, si otro mod cambiara
 * alguno de esos métodos, simplemente no se aplica (nunca bloquea el arranque).
 */
@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
	/** Alcance de la niebla del terreno. */
	@Inject(method = "applyFog", at = @At("RETURN"), require = 0)
	private static void zxkstudio$fogDistance(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance,
			boolean thickFog, float tickDelta, CallbackInfo ci) {
		if (fogType != BackgroundRenderer.FogType.FOG_TERRAIN) {
			return;
		}
		int mode = SireniaFog.mode(camera);
		if (mode != SireniaFog.NONE) {
			RenderSystem.setShaderFogStart(0.0f);
			RenderSystem.setShaderFogEnd(SireniaFog.distance(mode));
		}
	}

	/** Color de la niebla que llega al shader. */
	@Inject(method = "applyFogColor", at = @At("RETURN"), require = 0)
	private static void zxkstudio$fogColor(CallbackInfo ci) {
		int mode = SireniaFog.currentMode();
		if (mode != SireniaFog.NONE) {
			RenderSystem.setShaderFogColor(SireniaFog.red(mode), SireniaFog.green(mode), SireniaFog.blue(mode));
		}
	}

	/** Color de fondo, para que coincida con la niebla. */
	@Inject(method = "render", at = @At("RETURN"), require = 0)
	private static void zxkstudio$clearColor(Camera camera, float tickDelta, ClientWorld world, int viewDistance,
			float skyDarkness, CallbackInfo ci) {
		int mode = SireniaFog.mode(camera);
		if (mode != SireniaFog.NONE) {
			RenderSystem.clearColor(SireniaFog.red(mode), SireniaFog.green(mode), SireniaFog.blue(mode), 0.0f);
		}
	}
}
