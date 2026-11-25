package com.zxkstudio.sirenia.item;

import com.zxkstudio.sirenia.block.SireniaSaplingBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

/**
 * Semilla de Sirenia: se planta sobre barro (con agua cerca) y da un brote que crece hasta ser un árbol.
 * Si el sitio no cumple los requisitos avisa en la barra de acción y no se gasta la semilla.
 */
public class SireniaTreeSeedItem extends BlockItem {
	public SireniaTreeSeedItem(Block block, Settings settings) {
		super(block, settings);
	}

	@Override
	public ActionResult place(ItemPlacementContext context) {
		if (context.canPlace()) {
			String problem = SireniaSaplingBlock.problemAt(context.getWorld(), context.getBlockPos());
			if (problem != null) {
				PlayerEntity player = context.getPlayer();
				if (player != null && !context.getWorld().isClient) {
					player.sendMessage(Text.translatable("message.zxkstudio.sapling.fail." + problem), true);
				}
				return ActionResult.FAIL;
			}
		}
		return super.place(context);
	}
}
