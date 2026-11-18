package com.zxkstudio.sirenia.registry;

import com.zxkstudio.ZxkStudio;
import com.zxkstudio.sirenia.blockentity.SireniaGraftBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {
	private ModBlockEntities() {
	}

	public static BlockEntityType<SireniaGraftBlockEntity> GRAFT;

	public static void register() {
		GRAFT = Registry.register(
				Registries.BLOCK_ENTITY_TYPE,
				Identifier.of(ZxkStudio.MOD_ID, "sirenia_graft"),
				FabricBlockEntityTypeBuilder.create(SireniaGraftBlockEntity::new, ModBlocks.SIRENIA_GRAFT).build());
	}
}
