package com.zxkstudio;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/**
 * Inmunosupresor y Bloqueador Neural (se bebe como una miel).
 * En el modo "solo_bajas", quien está en cyberpsychosis no puede curarse con ellos.
 */
public class InmunosupresorItem extends Item {
	private final boolean bloqueador;

	public InmunosupresorItem(Settings settings, boolean bloqueador) {
		super(settings);
		this.bloqueador = bloqueador;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.DRINK;
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		boolean rechazado = false;
		if (!world.isClient() && user instanceof ServerPlayerEntity jugador) {
			if (CiberPsicosis.bloqueaItems(jugador)) {
				rechazado = true;
				CiberPsicosis.rechazarItem(jugador);
			} else if (bloqueador) {
				CiberPsicosis.usarBloqueador(jugador);
			} else {
				CiberPsicosis.usarInmunosupresor(jugador);
			}
		}

		ItemStack resultado = super.finishUsing(stack, world, user);

		if (user instanceof PlayerEntity jugadorCreativo && jugadorCreativo.isCreative()) {
			return resultado;
		}
		if (rechazado) {
			// no se gasta: se devuelve el ítem que acaba de consumirse
			if (resultado.isEmpty()) {
				return new ItemStack(this);
			}
			resultado.increment(1);
			return resultado;
		}
		if (resultado.isEmpty()) {
			return new ItemStack(Items.GLASS_BOTTLE);
		}
		if (user instanceof PlayerEntity jugadorNormal) {
			jugadorNormal.getInventory().insertStack(new ItemStack(Items.GLASS_BOTTLE));
		}
		return resultado;
	}
}
