package com.zxkstudio.sirenia.registry;

import com.zxkstudio.ZxkStudio;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Pestaña propia del creativo para el contenido de Sirenia (no toca las pestañas existentes). */
public final class ModItemGroups {
	private ModItemGroups() {
	}

	public static void register() {
		Registry.register(Registries.ITEM_GROUP, Identifier.of(ZxkStudio.MOD_ID, "sirenia"),
				FabricItemGroup.builder()
						.displayName(Text.translatable("itemGroup.zxkstudio.sirenia"))
						.icon(() -> new ItemStack(ModItems.SIRENIA_FRUIT))
						.entries((context, entries) -> {
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
						})
						.build());
	}
}
