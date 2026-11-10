package com.zxkstudio;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

	/** NONE: sin el gesto ni el sonido de beber; la animación la ponen los sonidos y efectos de usageTick. */
	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.NONE;
	}

	/** Se llama cada tick mientras el jugador "se inyecta". */
	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (!world.isClient() && user instanceof ServerPlayerEntity jugador) {
			CiberPsicosis.animacionInyeccion(jugador, remainingUseTicks);
		}
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		boolean creativo = user instanceof PlayerEntity p && p.isCreative();
		if (!world.isClient() && user instanceof ServerPlayerEntity jugador) {
			if (CiberPsicosis.bloqueaItems(jugador)) {
				CiberPsicosis.rechazarItem(jugador);
				return stack; // no se gasta
			}
			if (bloqueador) {
				CiberPsicosis.usarBloqueador(jugador);
			} else {
				CiberPsicosis.usarInmunosupresor(jugador);
			}
		}
		if (!creativo) {
			stack.decrement(1); // la jeringa se gasta; no devuelve botella
		}
		return stack;
	}
}
