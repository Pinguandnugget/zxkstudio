package com.zxkstudio.sirenia.registry;

import com.zxkstudio.ZxkStudio;
import com.zxkstudio.sirenia.item.SireniaFruitItem;
import com.zxkstudio.sirenia.item.SireniaSeedItem;
import net.minecraft.block.Block;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public final class ModItems {
	private ModItems() {
	}

	public static Item SIRENIA_LOG;
	public static Item STRIPPED_SIRENIA_LOG;
	public static Item SIRENIA_WOOD;
	public static Item STRIPPED_SIRENIA_WOOD;
	public static Item SIRENIA_PLANKS;
	public static Item SIRENIA_LEAVES;
	public static Item SIRENIA_SEED;
	public static Item SIRENIA_TREE_SEED;
	public static Item SIRENIA_FRUIT;
	public static Item WILTED_SIRENIA_FRUIT;
	public static Item ROTTEN_SIRENIA_FRUIT;

	public static void register() {
		SIRENIA_LOG = block("sirenia_log", ModBlocks.SIRENIA_LOG);
		STRIPPED_SIRENIA_LOG = block("stripped_sirenia_log", ModBlocks.STRIPPED_SIRENIA_LOG);
		SIRENIA_WOOD = block("sirenia_wood", ModBlocks.SIRENIA_WOOD);
		STRIPPED_SIRENIA_WOOD = block("stripped_sirenia_wood", ModBlocks.STRIPPED_SIRENIA_WOOD);
		SIRENIA_PLANKS = block("sirenia_planks", ModBlocks.SIRENIA_PLANKS);
		SIRENIA_LEAVES = block("sirenia_leaves", ModBlocks.SIRENIA_LEAVES);

		SIRENIA_SEED = add("sirenia_seed", new SireniaSeedItem(new Item.Settings().rarity(Rarity.UNCOMMON)));
		SIRENIA_TREE_SEED = add("sirenia_tree_seed", new com.zxkstudio.sirenia.item.SireniaTreeSeedItem(ModBlocks.SIRENIA_SAPLING, new Item.Settings().rarity(Rarity.UNCOMMON)));
		SIRENIA_FRUIT = add("sirenia_fruit", new SireniaFruitItem(fruitSettings(4, 0.6f, Rarity.RARE)));
		WILTED_SIRENIA_FRUIT = add("wilted_sirenia_fruit", new SireniaFruitItem(fruitSettings(2, 0.2f, Rarity.UNCOMMON)));
		ROTTEN_SIRENIA_FRUIT = add("rotten_sirenia_fruit", new SireniaFruitItem(fruitSettings(1, 0.0f, Rarity.COMMON)));
	}

	private static Item.Settings fruitSettings(int nutrition, float saturation, Rarity rarity) {
		return new Item.Settings()
				.maxCount(16)
				.rarity(rarity)
				.food(new FoodComponent.Builder()
						.nutrition(nutrition)
						.saturationModifier(saturation)
						.alwaysEdible()
						.build());
	}

	private static Item block(String id, Block block) {
		return add(id, new BlockItem(block, new Item.Settings()));
	}

	private static Item add(String id, Item item) {
		return Registry.register(Registries.ITEM, Identifier.of(ZxkStudio.MOD_ID, id), item);
	}
}
