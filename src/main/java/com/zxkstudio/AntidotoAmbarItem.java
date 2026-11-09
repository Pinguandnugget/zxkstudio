package com.zxkstudio;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/** Antídoto de Ámbar: se bebe como una poción y cura la Sangre Jurásica. */
public class AntidotoAmbarItem extends Item {
	public AntidotoAmbarItem(Settings settings) {
		super(settings);
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.DRINK;
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (!world.isClient() && user instanceof ServerPlayerEntity jugador) {
			SangreJurasica.curar(jugador);
		}

		ItemStack resultado = super.finishUsing(stack, world, user);

		if (user instanceof PlayerEntity jugadorCreativo && jugadorCreativo.isCreative()) {
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
