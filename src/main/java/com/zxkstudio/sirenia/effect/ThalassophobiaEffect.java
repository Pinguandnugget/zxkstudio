package com.zxkstudio.sirenia.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Talasofobia. El efecto es solo el indicador (nombre, icono y cuenta atrás en el HUD); toda la lógica
 * (cuenta atrás, alucinaciones, pánico y cura) vive en {@link com.zxkstudio.sirenia.fear.Thalassophobia}.
 */
public class ThalassophobiaEffect extends StatusEffect {
	public ThalassophobiaEffect() {
		super(StatusEffectCategory.HARMFUL, 0x0B2A4A);
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		return false;
	}
}
