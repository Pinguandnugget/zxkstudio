package com.zxkstudio;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.List;

/** Todo lo que el mod registra: efecto, ítem, dato guardado en el jugador y tags. */
public final class ModContent {
	private ModContent() {
	}

	/**
	 * Segundos que le quedan al jugador antes de la fase terminal (0 = ya está en fase terminal).
	 * Si el jugador no tiene este dato, no está infectado. No se copia al morir: morir cura.
	 */
	public static final AttachmentType<Integer> SANGRE_TIEMPO = AttachmentRegistry.create(
			Identifier.of(ZxkStudio.MOD_ID, "sangre_jurasica_tiempo"),
			builder -> builder.persistent(Codec.INT)
	);

	/** Dinosaurios que infectan al golpear al jugador. */
	public static final TagKey<EntityType<?>> DINOS = TagKey.of(
			RegistryKeys.ENTITY_TYPE, Identifier.of(ZxkStudio.MOD_ID, "sangre_jurasica/dinos"));

	/** Dinosaurios que se lanzan contra el jugador cuando está cerca. */
	public static final TagKey<EntityType<?>> HOSTILES = TagKey.of(
			RegistryKeys.ENTITY_TYPE, Identifier.of(ZxkStudio.MOD_ID, "sangre_jurasica/hostiles"));

	public static RegistryEntry<StatusEffect> SANGRE_EFECTO;
	public static Item ANTIDOTO_AMBAR;

	public static void register() {
		SANGRE_EFECTO = Registry.registerReference(
				Registries.STATUS_EFFECT,
				Identifier.of(ZxkStudio.MOD_ID, "sangre_jurasica"),
				new SangreJurasicaEffect());

		FoodComponent bebida = new FoodComponent.Builder()
				.nutrition(0)
				.saturationModifier(0.0f)
				.alwaysEdible()
				.build();

		LoreComponent lore = new LoreComponent(List.of(
				Text.translatable("item.zxkstudio.antidoto_ambar.lore1").formatted(Formatting.YELLOW, Formatting.ITALIC),
				Text.translatable("item.zxkstudio.antidoto_ambar.lore2").formatted(Formatting.GREEN)
		));

		ANTIDOTO_AMBAR = Registry.register(
				Registries.ITEM,
				Identifier.of(ZxkStudio.MOD_ID, "antidoto_ambar"),
				new AntidotoAmbarItem(new Item.Settings()
						.maxCount(16)
						.rarity(Rarity.RARE)
						.food(bebida)
						.component(DataComponentTypes.ITEM_NAME,
								Text.translatable("item.zxkstudio.antidoto_ambar").formatted(Formatting.GOLD))
						.component(DataComponentTypes.LORE, lore)));

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK)
				.register(entradas -> entradas.add(ANTIDOTO_AMBAR));
	}
}
