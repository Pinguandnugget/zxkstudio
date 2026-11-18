package com.zxkstudio.sirenia.client;

import com.zxkstudio.ZxkStudio;
import com.zxkstudio.sirenia.registry.ModBlocks;
import com.zxkstudio.sirenia.registry.ModItems;
import com.zxkstudio.sirenia.util.FruitAging;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

/** Parte de cliente: capas de dibujo y el aspecto del fruto según su edad real. */
public class SireniaClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SIRENIA_LEAVES, RenderLayer.getCutoutMipped());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SIRENIA_GRAFT, RenderLayer.getCutout());

		// El modelo del fruto se elige con su edad real, así un fruto guardado en un cofre se ve marchito
		// o podrido aunque su ID todavía no se haya actualizado.
		Identifier stage = Identifier.of(ZxkStudio.MOD_ID, "fruit_stage");
		for (Item item : new Item[] {ModItems.SIRENIA_FRUIT, ModItems.WILTED_SIRENIA_FRUIT, ModItems.ROTTEN_SIRENIA_FRUIT}) {
			ModelPredicateProviderRegistry.register(item, stage, (stack, world, entity, seed) -> {
				int s = FruitAging.effectiveStage(stack, FruitAging.nowMs());
				return s == FruitAging.ROTTEN ? 1.0f : s == FruitAging.WILTED ? 0.5f : 0.0f;
			});
		}
	}
}
