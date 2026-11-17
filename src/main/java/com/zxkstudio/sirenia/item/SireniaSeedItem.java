package com.zxkstudio.sirenia.item;

import com.zxkstudio.sirenia.block.SireniaGraftBlock;
import com.zxkstudio.sirenia.util.GraftRules;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Germen de Sirenia. No es un BlockItem: no se puede plantar en tierra ni en ningún otro bloque.
 * Solo reacciona al clic derecho sobre la cara lateral de un tronco o madera pelados de Sirenia.
 */
public class SireniaSeedItem extends Item {
	public SireniaSeedItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos logPos = context.getBlockPos();
		Direction side = context.getSide();
		PlayerEntity player = context.getPlayer();

		if (side.getAxis().isVertical() || !SireniaGraftBlock.isGraftSupport(world.getBlockState(logPos))) {
			return ActionResult.PASS;
		}
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}

		GraftRules.Result result = GraftRules.checkPlacement(world, logPos, side);
		if (result != GraftRules.Result.OK) {
			if (player != null) {
				player.sendMessage(Text.translatable("message.zxkstudio.graft.fail." + result.key), true);
			}
			return ActionResult.FAIL; // La semilla no se consume.
		}

		if (!SireniaGraftBlock.plant(world, logPos.offset(side), side)) {
			return ActionResult.FAIL;
		}
		if (player == null || !player.isCreative()) {
			context.getStack().decrement(1);
		}
		return ActionResult.SUCCESS;
	}
}
