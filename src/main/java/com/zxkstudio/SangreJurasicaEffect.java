package com.zxkstudio;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Efecto que solo sirve de icono y nombre en pantalla mientras estás infectado.
 * La lógica de la infección vive en SangreJurasica.
 */
public class SangreJurasicaEffect extends StatusEffect {
	public SangreJurasicaEffect() {
		super(StatusEffectCategory.HARMFUL, 0x8B0000);
	}
}
