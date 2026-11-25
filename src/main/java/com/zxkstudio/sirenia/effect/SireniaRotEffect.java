package com.zxkstudio.sirenia.effect;

import com.zxkstudio.sirenia.config.SireniaConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Podredumbre de Sirenia: lo que provoca comer un Corazón de Sirenia podrido.
 * Funciona como un veneno que nunca mata (deja siempre medio corazón), con la pantalla teñida de cian y los
 * corazones celestes (la parte visual vive en el cliente). Solo se cura con un Corazón de Sirenia en buen estado.
 */
public class SireniaRotEffect extends StatusEffect {
	public SireniaRotEffect() {
		super(StatusEffectCategory.HARMFUL, 0x22D3D3);
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		int interval = SireniaConfig.get().rot_damage_interval_ticks;
		return interval <= 1 || duration % interval == 0;
	}

	@Override
	public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
		if (entity.getWorld().isClient) {
			return true;
		}
		float damage = SireniaConfig.get().rot_damage;
		if (damage > 0.0f && entity.getHealth() > 1.0f) {
			entity.damage(entity.getWorld().getDamageSources().magic(), Math.min(damage, entity.getHealth() - 1.0f));
		}
		return true;
	}
}
