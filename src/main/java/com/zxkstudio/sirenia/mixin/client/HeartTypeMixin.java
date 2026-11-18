package com.zxkstudio.sirenia.mixin.client;

import com.zxkstudio.ZxkStudio;
import com.zxkstudio.sirenia.client.SireniaClient;
import com.zxkstudio.sirenia.registry.ModEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Corazones celestes de la Podredumbre de Sirenia. Solo cliente.
 *
 * El juego elige la textura de los corazones según el tipo (normal, envenenado, marchito...). Aquí solo se
 * cambia la textura de los corazones "normal" y "envenenado" cuando el jugador tiene el efecto; el resto de
 * tipos (marchitar, congelación, absorción) y los demás jugadores no se tocan.
 */
@Mixin(targets = "net.minecraft.client.gui.hud.InGameHud$HeartType")
public abstract class HeartTypeMixin {
	/** Se llama una vez por dibujo de la barra de vida: aprovecha para saber si el jugador tiene el efecto. */
	@Inject(method = "fromPlayerState", at = @At("RETURN"), require = 0)
	private static void zxkstudio$checkRot(PlayerEntity player, CallbackInfoReturnable<?> cir) {
		SireniaClient.rotHearts = player.hasStatusEffect(ModEffects.SIRENIA_ROT);
	}

	@Inject(method = "getTexture", at = @At("RETURN"), cancellable = true, require = 0)
	private void zxkstudio$rotTexture(boolean hardcore, boolean half, boolean blinking,
			CallbackInfoReturnable<Identifier> cir) {
		if (!SireniaClient.rotHearts) {
			return;
		}
		String type = ((Enum<?>) (Object) this).name();
		if (!type.equals("NORMAL") && !type.equals("POISONED")) {
			return;
		}
		cir.setReturnValue(Identifier.of(ZxkStudio.MOD_ID,
				"hud/heart/rot_" + (half ? "half" : "full") + (blinking ? "_blinking" : "")));
	}
}
