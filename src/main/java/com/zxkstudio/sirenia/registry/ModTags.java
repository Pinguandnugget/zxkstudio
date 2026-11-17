package com.zxkstudio.sirenia.registry;

import com.zxkstudio.ZxkStudio;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class ModTags {
	private ModTags() {
	}

	/** Los cuatro bloques de tronco/madera del Árbol de Sirenia. */
	public static final TagKey<Block> SIRENIA_LOGS = TagKey.of(RegistryKeys.BLOCK,
			Identifier.of(ZxkStudio.MOD_ID, "sirenia_logs"));

	/** Donde se puede injertar: tronco pelado y madera pelada. */
	public static final TagKey<Block> STRIPPED_SIRENIA_LOGS = TagKey.of(RegistryKeys.BLOCK,
			Identifier.of(ZxkStudio.MOD_ID, "stripped_sirenia_logs"));
}
