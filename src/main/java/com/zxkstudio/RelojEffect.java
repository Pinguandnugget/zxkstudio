package com.zxkstudio;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Efecto "Inmunosupresores": su duración en pantalla es el tiempo que le queda al jugador
 * antes de entrar en cyberpsychosis. La lógica vive en CiberPsicosis.
 */
public class RelojEffect extends StatusEffect {
	public RelojEffect() {
		super(StatusEffectCategory.HARMFUL, 0x4DC3FF);
	}
}
