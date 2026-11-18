package com.zxkstudio.sirenia.registry;

import com.zxkstudio.ModContent;
import com.zxkstudio.ZxkStudio;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Pestaña única "zxkstudio" del creativo con todo lo que trae el mod: Sirenia, el Antídoto de Ámbar y los
 * objetos de Cyberpsychosis. Los objetos siguen apareciendo también donde ya estaban (por ejemplo, en
 * Comida y bebida); esto solo añade la pestaña propia.
 */
public final class ModItemGroups {
	private ModItemGroups() {
	}

	public static void register() {
		Registry.register(Registries.ITEM_GROUP, Identifier.of(ZxkStudio.MOD_ID, "main"),
				FabricItemGroup.builder()
						.displayName(Text.translatable("itemGroup.zxkstudio.main"))
						.icon(() -> new ItemStack(ModItems.SIRENIA_FRUIT))
						.entries((context, entries) -> {
							// Sirenia
							entries.add(ModItems.SIRENIA_SEED);
							entries.add(ModItems.SIRENIA_FRUIT);
							entries.add(ModItems.WILTED_SIRENIA_FRUIT);
							entries.add(ModItems.ROTTEN_SIRENIA_FRUIT);
							entries.add(ModItems.SIRENIA_LOG);
							entries.add(ModItems.STRIPPED_SIRENIA_LOG);
							entries.add(ModItems.SIRENIA_WOOD);
							entries.add(ModItems.STRIPPED_SIRENIA_WOOD);
							entries.add(ModItems.SIRENIA_PLANKS);
							entries.add(ModItems.SIRENIA_LEAVES);
							// Sangre Jurásica
							entries.add(ModContent.ANTIDOTO_AMBAR);
							// Cyberpsychosis
							entries.add(ModContent.INMUNOSUPRESOR);
							entries.add(ModContent.BLOQUEADOR_NEURAL);
						})
						.build());
	}
}
