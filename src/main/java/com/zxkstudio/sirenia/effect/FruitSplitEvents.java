package com.zxkstudio.sirenia.effect;

import com.zxkstudio.sirenia.config.SireniaConfig;
import com.zxkstudio.sirenia.item.SireniaFruitItem;
import com.zxkstudio.sirenia.registry.ModItems;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;

/**
 * Fruto en una mano + hacha en la otra + clic derecho = el fruto se parte en 2-3 semillas.
 * Se resuelve solo en el servidor (el cliente solo muestra el balanceo) y gasta 1 fruto y 1 de durabilidad.
 */
public final class FruitSplitEvents {
	private FruitSplitEvents() {
	}

	public static void register() {
		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack held = player.getStackInHand(hand);
			ItemStack other = player.getStackInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
			if (!(held.getItem() instanceof AxeItem) || !(other.getItem() instanceof SireniaFruitItem)) {
				return TypedActionResult.pass(held);
			}
			if (player.getItemCooldownManager().isCoolingDown(held.getItem())) {
				return TypedActionResult.pass(held);
			}
			if (world.isClient) {
				return TypedActionResult.success(held);
			}
			SireniaConfig cfg = SireniaConfig.get();
			int count = cfg.tree_seed_min + world.random.nextInt(cfg.tree_seed_max - cfg.tree_seed_min + 1);
			if (!player.isCreative()) {
				other.decrement(1);
				held.damage(1, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
			}
			player.getInventory().offerOrDrop(new ItemStack(ModItems.SIRENIA_TREE_SEED, count));
			world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_AXE_STRIP, SoundCategory.PLAYERS, 1.0f, 1.3f);
			player.getItemCooldownManager().set(held.getItem(), 6); // evita repetir el clic en el mismo tick
			return TypedActionResult.success(held);
		});
	}
}
