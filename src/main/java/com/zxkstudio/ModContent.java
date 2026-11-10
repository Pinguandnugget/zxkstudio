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
	public static final AttachmentType<Integer> SANGRE_TIEMPO = AttachmentRegistry.createPersistent(
			Identifier.of(ZxkStudio.MOD_ID, "sangre_jurasica_tiempo"),
			Codec.INT
	);

	/** Dinosaurios que infectan al golpear al jugador. */
	public static final TagKey<EntityType<?>> DINOS = TagKey.of(
			RegistryKeys.ENTITY_TYPE, Identifier.of(ZxkStudio.MOD_ID, "sangre_jurasica/dinos"));

	/** Dinosaurios que se lanzan contra el jugador cuando está cerca. */
	public static final TagKey<EntityType<?>> HOSTILES = TagKey.of(
			RegistryKeys.ENTITY_TYPE, Identifier.of(ZxkStudio.MOD_ID, "sangre_jurasica/hostiles"));

	/** Estado de Cyberpsychosis del jugador (reloj, bajas, inmunidad). Persiste y se copia al reaparecer. */
	public static final AttachmentType<CiberEstado> CIBER_ESTADO = AttachmentRegistry.createPersistent(
			Identifier.of(ZxkStudio.MOD_ID, "ciberpsicosis_estado"),
			CiberEstado.CODEC
	);

	public static RegistryEntry<StatusEffect> SANGRE_EFECTO;
	public static RegistryEntry<StatusEffect> RELOJ_EFECTO;
	public static Item ANTIDOTO_AMBAR;
	public static Item INMUNOSUPRESOR;
	public static Item BLOQUEADOR_NEURAL;

	public static void register() {
		SANGRE_EFECTO = Registry.registerReference(
				Registries.STATUS_EFFECT,
				Identifier.of(ZxkStudio.MOD_ID, "sangre_jurasica"),
				new SangreJurasicaEffect());

		RELOJ_EFECTO = Registry.registerReference(
				Registries.STATUS_EFFECT,
				Identifier.of(ZxkStudio.MOD_ID, "inmunosupresores"),
				new RelojEffect());

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

		INMUNOSUPRESOR = registrarBebidaCiber("inmunosupresor", Formatting.AQUA, false, Rarity.UNCOMMON, bebida);
		BLOQUEADOR_NEURAL = registrarBebidaCiber("bloqueador_neural", Formatting.LIGHT_PURPLE, true, Rarity.EPIC, bebida);

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entradas -> {
			entradas.add(ANTIDOTO_AMBAR);
			entradas.add(INMUNOSUPRESOR);
			entradas.add(BLOQUEADOR_NEURAL);
		});
	}

	private static Item registrarBebidaCiber(String id, Formatting color, boolean bloqueador, Rarity rareza,
			FoodComponent bebida) {
		String base = "item." + ZxkStudio.MOD_ID + "." + id;
		LoreComponent lore = new LoreComponent(List.of(
				Text.translatable(base + ".lore1").formatted(Formatting.GRAY, Formatting.ITALIC),
				Text.translatable(base + ".lore2").formatted(color),
				Text.translatable(base + ".lore3").formatted(color)));
		return Registry.register(
				Registries.ITEM,
				Identifier.of(ZxkStudio.MOD_ID, id),
				new InmunosupresorItem(new Item.Settings()
						.maxCount(64)
						.rarity(rareza)
						.food(bebida)
						.component(DataComponentTypes.ITEM_NAME,
								Text.translatable(base).formatted(color, Formatting.BOLD))
						.component(DataComponentTypes.LORE, lore), bloqueador));
	}
}
