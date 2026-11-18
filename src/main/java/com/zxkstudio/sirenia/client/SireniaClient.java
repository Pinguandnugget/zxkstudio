package com.zxkstudio.sirenia.client;

import com.zxkstudio.ZxkStudio;
import com.zxkstudio.sirenia.item.SireniaFruitItem;
import com.zxkstudio.sirenia.registry.ModBlocks;
import com.zxkstudio.sirenia.registry.ModEffects;
import com.zxkstudio.sirenia.registry.ModItems;
import com.zxkstudio.sirenia.util.FruitAging;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.util.math.MathHelper;

/** Parte de cliente: capas de dibujo, aspecto del fruto según su edad, tinte de la Podredumbre y descripciones. */
public class SireniaClient implements ClientModInitializer {
	/** Lo rellena HeartTypeMixin en cada dibujo de la barra de vida. */
	public static volatile boolean rotHearts;

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

		// Podredumbre de Sirenia: la pantalla se tiñe de cian a medias, con un pulso lento.
		HudRenderCallback.EVENT.register((context, tickCounter) -> {
			ClientPlayerEntity player = MinecraftClient.getInstance().player;
			if (player == null || !player.hasStatusEffect(ModEffects.SIRENIA_ROT)) {
				return;
			}
			float pulse = 0.5f + 0.5f * MathHelper.sin(player.age * 0.08f);
			int alpha = 80 + (int) (40 * pulse);
			context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(),
					(alpha << 24) | 0x0FD2D2);
		});

		// Descripciones: si el idioma trae "<clave del objeto>.desc1", ".desc2"..., se añaden al final del tooltip.
		// Los frutos usan su fase real (no solo su ID) para que un fruto guardado en un cofre diga lo correcto.
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			Identifier id = Registries.ITEM.getId(stack.getItem());
			if (!ZxkStudio.MOD_ID.equals(id.getNamespace())) {
				return;
			}
			String base = stack.getItem().getTranslationKey();
			if (stack.getItem() instanceof SireniaFruitItem) {
				base = "item." + ZxkStudio.MOD_ID + "." + SireniaFruitItem.keyFor(FruitAging.effectiveStage(stack, FruitAging.nowMs()));
			}
			Language language = Language.getInstance();
			for (int i = 1; i <= 5; i++) {
				String key = base + ".desc" + i;
				if (!language.hasTranslation(key)) {
					break;
				}
				lines.add(Text.translatable(key).formatted(Formatting.GRAY));
			}
		});
	}
}
