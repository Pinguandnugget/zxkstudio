package com.zxkstudio.sirenia.item;

import com.zxkstudio.sirenia.config.SireniaConfig;
import com.zxkstudio.sirenia.registry.ModEffects;
import com.zxkstudio.sirenia.util.FruitAging;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/**
 * Corazón de Sirenia (maduro, marchito o podrido). Los tres IDs comparten esta clase: el efecto real
 * al comerlo, el nombre y la descripción se calculan SIEMPRE con la edad del fruto, así que un fruto
 * guardado en un cofre se comporta bien aunque su ID todavía no se haya actualizado.
 */
public class SireniaFruitItem extends Item {
	public SireniaFruitItem(Settings settings) {
		super(settings);
	}

	@Override
	public Text getName(ItemStack stack) {
		int stage = FruitAging.effectiveStage(stack, FruitAging.nowMs());
		Formatting color = stage == FruitAging.ROTTEN ? Formatting.DARK_PURPLE
				: stage == FruitAging.WILTED ? Formatting.YELLOW : Formatting.AQUA;
		return Text.translatable("item.zxkstudio." + keyFor(stage)).formatted(color);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		long now = FruitAging.nowMs();
		int stage = FruitAging.effectiveStage(stack, now);
		tooltip.add(Text.translatable("tooltip.zxkstudio.fruit.stage." + stage).formatted(Formatting.GRAY, Formatting.ITALIC));
		long minutes = FruitAging.minutesToNextStage(stack, now);
		if (minutes >= 0 && stage == FruitAging.MATURE) {
			tooltip.add(Text.translatable("tooltip.zxkstudio.fruit.wilts_in", minutes).formatted(Formatting.DARK_AQUA));
		} else if (minutes >= 0 && stage == FruitAging.WILTED) {
			tooltip.add(Text.translatable("tooltip.zxkstudio.fruit.rots_in", minutes).formatted(Formatting.DARK_GREEN));
		}
	}

	/**
	 * Mantiene el ID del objeto al día mientras está en el inventario de un jugador.
	 * Solo se revisa una vez cada 2 segundos por objeto, repartido entre ranuras.
	 */
	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
		if (world.isClient || !(entity instanceof PlayerEntity player) || (entity.age + slot) % 40 != 0) {
			return;
		}
		long now = FruitAging.nowMs();
		FruitAging.ensureBirth(stack, now);
		int stage = FruitAging.effectiveStage(stack, now);
		Item wanted = FruitAging.itemForStage(stage);
		if (wanted == stack.getItem() || player.isUsingItem()) {
			return;
		}
		// Solo se sustituye si la ranura corresponde de verdad a este objeto (evita errores con la mano secundaria).
		if (slot >= 0 && slot < player.getInventory().main.size() && player.getInventory().getStack(slot) == stack) {
			ItemStack replacement = new ItemStack(wanted, stack.getCount());
			replacement.set(com.zxkstudio.sirenia.registry.ModComponents.BIRTH_TIME,
					stack.get(com.zxkstudio.sirenia.registry.ModComponents.BIRTH_TIME));
			player.getInventory().setStack(slot, replacement);
		}
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		int stage = FruitAging.effectiveStage(stack, FruitAging.nowMs()); // antes de gastar el objeto
		ItemStack result = super.finishUsing(stack, world, user);
		if (!world.isClient) {
			SireniaConfig cfg = SireniaConfig.get();
			if (stage == FruitAging.ROTTEN) {
				int ticks = cfg.rotten_effect_duration_seconds * 20;
				user.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, ticks, cfg.rotten_nausea_amplifier));
				user.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, ticks, cfg.rotten_poison_amplifier));
			} else {
				int seconds = stage == FruitAging.WILTED ? cfg.wilted_effect_duration_seconds
						: cfg.sirenomorphosis_duration_seconds;
				user.addStatusEffect(new StatusEffectInstance(ModEffects.SIRENOMORPHOSIS, seconds * 20, 0, false, true, true));
			}
		}
		return result;
	}

	public static String keyFor(int stage) {
		return stage == FruitAging.ROTTEN ? "rotten_sirenia_fruit"
				: stage == FruitAging.WILTED ? "wilted_sirenia_fruit" : "sirenia_fruit";
	}
}
